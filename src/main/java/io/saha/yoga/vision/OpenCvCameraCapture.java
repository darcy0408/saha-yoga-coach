package io.saha.yoga.vision;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** Local, non-recording OpenCV capture. No frame leaves the supplied callback. */
public final class OpenCvCameraCapture implements CameraCapture {
    private final int deviceIndex;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile VideoCapture camera;
    private volatile Thread worker;

    public OpenCvCameraCapture(int deviceIndex) {
        if (deviceIndex < 0) throw new IllegalArgumentException("Camera index cannot be negative");
        this.deviceIndex = deviceIndex;
    }

    @Override public synchronized void start(Consumer<CameraFrame> frames, Consumer<String> status, Consumer<String> failures) {
        Objects.requireNonNull(frames, "frames");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(failures, "failures");
        if (!running.compareAndSet(false, true)) return;
        // a platform thread, not a virtual one: every call in this loop is a
        // blocking native call, which pins its carrier for the whole practice
        worker = new Thread(() -> captureLoop(frames, status, failures), "saha-camera-capture");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * A backend counts as working only once it has handed over a frame.
     *
     * Opening is not the same as working. Media Foundation will happily open a
     * device it cannot then read, and every grab comes back E_HANDLE - which
     * looked from the outside like a camera that would not open, while the
     * capture loop span on a failing read at full speed, reported nothing, and
     * never fell back. Proving a backend with a real frame before accepting it
     * turns that into a backend that is simply skipped.
     */
    private record Backend(String name, int id) { }

    /**
     * DirectShow first, and the automatic backend only as a last resort.
     *
     * On this hardware DirectShow opens the camera and reads a frame in about a
     * second, while Media Foundation - which is what the automatic backend
     * chooses on Windows - never returns from open() at all. The old code asked
     * for the automatic backend the moment DirectShow failed, so one busy
     * camera turned into a permanently hung application, and the run that hung
     * kept holding the device, which is what made DirectShow fail on the next
     * attempt. Media Foundation is not asked for by name at all.
     */
    private static final List<Backend> BACKENDS = List.of(
            new Backend("DirectShow", Videoio.CAP_DSHOW),
            new Backend("the automatic backend", Videoio.CAP_ANY));

    /** How long a backend has to produce its first frame before it is abandoned. */
    private static final long FIRST_FRAME_DEADLINE_NANOS = 4_000_000_000L;
    /** How long open() itself is given before the backend is abandoned mid-call. */
    private static final long OPEN_DEADLINE_MILLIS = 5_000;
    /** Consecutive failed reads mid-practice before the camera is called gone. */
    private static final int MISSES_BEFORE_GIVING_UP = 150;

    private void captureLoop(Consumer<CameraFrame> frames, Consumer<String> status, Consumer<String> failures) {
        // declared here so the finally can release them, but constructed only
        // after loadLocally(): a Mat is a handle to native memory, and building
        // one before its library is loaded throws UnsatisfiedLinkError
        Mat bgr = null;
        Mat bgra = null;
        try {
            status.accept("Loading the local OpenCV camera library...");
            OpenCV.loadLocally();
            bgr = new Mat();
            bgra = new Mat();
            if (!openAWorkingBackend(bgr, status)) {
                failures.accept("Camera " + deviceIndex + " could not deliver any video. Close anything else using the camera"
                        + " (Teams, Zoom, or a browser tab), or check Settings · Privacy & security · Camera · Let desktop apps access your camera."
                        + " Run gradlew cameraCheck to see which devices this machine can open.");
                return;
            }
            status.accept("Camera opened. Preview is live.");
            publish(bgr, bgra, frames);
            int misses = 0;
            while (running.get()) {
                if (!camera.read(bgr) || bgr.empty()) {
                    // a failing read must not become a hot loop: it pegged a
                    // core and drowned the log while saying nothing
                    if (++misses > MISSES_BEFORE_GIVING_UP) {
                        failures.accept("The camera stopped delivering video. Another application may have taken it.");
                        return;
                    }
                    sleepBriefly();
                    continue;
                }
                misses = 0;
                publish(bgr, bgra, frames);
            }
        } catch (Throwable error) {
            failures.accept("Local camera preview is unavailable: " + safeMessage(error));
        } finally {
            if (bgr != null) bgr.release();
            if (bgra != null) bgra.release();
            running.set(false);
            var current = camera;
            if (current != null) current.release();
            camera = null;
        }
    }

    /** Tries each backend in turn, keeping the first that actually hands over a frame. */
    private boolean openAWorkingBackend(Mat bgr, Consumer<String> status) {
        for (var backend : BACKENDS) {
            if (!running.get()) return false;
            status.accept("Opening camera " + deviceIndex + " with " + backend.name() + "...");
            var candidate = openWithinDeadline(backend, status);
            if (candidate == null) continue;
            try {
                candidate.set(Videoio.CAP_PROP_BUFFERSIZE, 1);
                if (negotiateSize(candidate, bgr, status)) {
                    camera = candidate;
                    return true;
                }
                status.accept(backend.name() + " opened camera " + deviceIndex + " but sent no video; trying the next backend...");
            } catch (Throwable error) {
                status.accept(backend.name() + " failed: " + safeMessage(error));
            }
            candidate.release();
        }
        return false;
    }

    /**
     * Opens on a thread that can be walked away from.
     *
     * open() is a native call that can never return, and a backend that hangs
     * must cost this practice a few seconds rather than the whole session. If
     * the call does eventually come back, the thread releases the device itself
     * rather than leaving it held by a capture nobody is holding a reference to.
     */
    private VideoCapture openWithinDeadline(Backend backend, Consumer<String> status) {
        var opened = new java.util.concurrent.atomic.AtomicReference<VideoCapture>();
        var abandoned = new AtomicBoolean();
        var finished = new java.util.concurrent.CountDownLatch(1);
        var attempt = new Thread(() -> {
            var candidate = new VideoCapture();
            boolean usable = false;
            try {
                usable = candidate.open(deviceIndex, backend.id()) && candidate.isOpened();
            } catch (Throwable ignored) {
                // reported by the caller as a backend that did not open
            } finally {
                if (usable && !abandoned.get()) opened.set(candidate); else candidate.release();
                finished.countDown();
            }
        }, "saha-camera-open-" + deviceIndex);
        attempt.setDaemon(true);
        attempt.start();
        try {
            if (!finished.await(OPEN_DEADLINE_MILLIS, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                abandoned.set(true);
                status.accept(backend.name() + " stopped responding while opening camera " + deviceIndex + "; trying the next backend...");
                return null;
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            abandoned.set(true);
            return null;
        }
        var candidate = opened.get();
        if (candidate == null) status.accept(backend.name() + " did not open camera " + deviceIndex + ".");
        return candidate;
    }

    /**
     * The size to ask a camera for, and why it is this shape.
     *
     * <p>The model is shown a square cropped to the person, so the frame's own
     * size decides how many real pixels that square is cut from. At 640x480 the
     * crop already has enough to fill the model's 192-pixel input for a body
     * anywhere near the camera; the detail bought here is for a body far enough
     * back that the crop would otherwise have to magnify it.
     *
     * <p>4:3 rather than the 16:9 most webcams would rather give. Widescreen
     * trades away vertical view, and vertical is the axis a standing body needs
     * - this coach has already lost a session to a person who did not fit in
     * the frame.
     */
    private static final int PREFERRED_WIDTH = 1280, PREFERRED_HEIGHT = 960;
    /** The size that has always worked here, for a device that will not give the above. */
    private static final int FALLBACK_WIDTH = 640, FALLBACK_HEIGHT = 480;

    /**
     * Takes the most detail this device will give without changing shape.
     *
     * <p>A camera asked for a size it does not have does not refuse: it sends
     * the nearest mode it does have, which is usually widescreen. So the answer
     * is checked against a real frame rather than trusted, and a device that
     * answers with the wrong shape is asked again for the size known to work.
     */
    private boolean negotiateSize(VideoCapture candidate, Mat bgr, Consumer<String> status) {
        candidate.set(Videoio.CAP_PROP_FRAME_WIDTH, PREFERRED_WIDTH);
        candidate.set(Videoio.CAP_PROP_FRAME_HEIGHT, PREFERRED_HEIGHT);
        if (!waitForFirstFrame(candidate, bgr)) return false;
        if (isFourThree(bgr)) {
            status.accept("Camera " + deviceIndex + " is sending " + bgr.cols() + "x" + bgr.rows() + " video.");
            return true;
        }
        status.accept("Camera " + deviceIndex + " answered with " + bgr.cols() + "x" + bgr.rows()
                + ", which is not 4:3 and would cut the top and bottom off a standing pose; asking for "
                + FALLBACK_WIDTH + "x" + FALLBACK_HEIGHT + " instead.");
        candidate.set(Videoio.CAP_PROP_FRAME_WIDTH, FALLBACK_WIDTH);
        candidate.set(Videoio.CAP_PROP_FRAME_HEIGHT, FALLBACK_HEIGHT);
        // If it will not give that either, whatever it is sending is still
        // video, and a wide picture beats no practice.
        return waitForFirstFrame(candidate, bgr) || !bgr.empty();
    }

    private static boolean isFourThree(Mat frame) {
        return frame.rows() > 0 && Math.abs((double) frame.cols() / frame.rows() - 4.0 / 3) < .02;
    }

    private boolean waitForFirstFrame(VideoCapture candidate, Mat bgr) {
        long deadline = System.nanoTime() + FIRST_FRAME_DEADLINE_NANOS;
        while (running.get() && System.nanoTime() < deadline) {
            if (candidate.read(bgr) && !bgr.empty()) return true;
            sleepBriefly();
        }
        return false;
    }

    private void publish(Mat bgr, Mat bgra, Consumer<CameraFrame> frames) {
        Imgproc.cvtColor(bgr, bgra, Imgproc.COLOR_BGR2BGRA);
        var pixels = new byte[(int) (bgra.total() * bgra.channels())];
        bgra.get(0, 0, pixels);
        frames.accept(new CameraFrame(bgra.cols(), bgra.rows(), pixels));
    }

    private void sleepBriefly() {
        try {
            Thread.sleep(20);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }

    private static String safeMessage(Throwable error) {
        var message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    @Override public boolean isOpen() {
        var current = camera;
        return running.get() && current != null && current.isOpened();
    }

    @Override public synchronized void close() {
        running.set(false);
        var current = camera;
        if (current != null) current.release();
        var currentWorker = worker;
        if (currentWorker != null) currentWorker.interrupt();
        worker = null;
    }
}
