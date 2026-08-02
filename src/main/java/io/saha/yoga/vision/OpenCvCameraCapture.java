package io.saha.yoga.vision;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

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

    @Override public synchronized void start(Consumer<CameraFrame> frames, Consumer<String> failures) {
        Objects.requireNonNull(frames, "frames");
        Objects.requireNonNull(failures, "failures");
        if (!running.compareAndSet(false, true)) return;
        worker = Thread.ofVirtual().name("saha-camera-capture").start(() -> captureLoop(frames, failures));
    }

    private void captureLoop(Consumer<CameraFrame> frames, Consumer<String> failures) {
        try {
            OpenCV.loadLocally();
            camera = new VideoCapture(deviceIndex);
            camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 640);
            camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);
            if (!camera.isOpened()) {
                failures.accept("Camera " + deviceIndex + " could not be opened. Check Windows camera permission or try another device.");
                return;
            }
            var bgr = new Mat();
            var bgra = new Mat();
            try {
                while (running.get()) {
                    if (!camera.read(bgr) || bgr.empty()) continue;
                    Imgproc.cvtColor(bgr, bgra, Imgproc.COLOR_BGR2BGRA);
                    var pixels = new byte[(int) (bgra.total() * bgra.channels())];
                    bgra.get(0, 0, pixels);
                    frames.accept(new CameraFrame(bgra.cols(), bgra.rows(), pixels));
                }
            } finally {
                bgr.release();
                bgra.release();
            }
        } catch (Throwable error) {
            failures.accept("Local camera preview is unavailable: " + safeMessage(error));
        } finally {
            running.set(false);
            var current = camera;
            if (current != null) current.release();
            camera = null;
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
