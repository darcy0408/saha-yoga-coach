package io.saha.yoga.vision;

import nu.pattern.OpenCV;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * A {@link CameraCapture} that plays a video file instead of opening a device.
 *
 * <p>This exists because every open question about the vision pipeline funnels
 * through "a person in front of the camera", and a person is not always
 * available - the pipeline's behavior on a recorded body is the same evidence,
 * repeatable on demand. The pipeline behind this class cannot tell the
 * difference: frames arrive through the same interface, in the same BGRA
 * shape, and nothing downstream is told they came from a file.
 *
 * <p>The file is read where it lies and nothing is written anywhere. Clips of
 * people are test inputs, not fixtures: the repository's sample-data policy
 * forbids committing video or faces, so paths given here should point outside
 * the repository or to ignored locations.
 *
 * <p>{@code paced} delivers frames at the file's own frame rate, which is what
 * an interactive session wants - the smoother and the crop move in per-frame
 * steps, so pacing only matters to a human watching alongside. Unpaced
 * delivery runs as fast as the consumer returns, which is what a headless
 * check wants.
 *
 * <p>{@code loop} restarts the clip when it ends, for a practice that outlives
 * a short recording. The seam is honest but abrupt: the body teleports to its
 * opening position, and the crop eases across the jump rather than snapping,
 * exactly as it would if a real person moved that fast.
 */
public final class VideoFileCapture implements CameraCapture {
    /** For a container that declares no usable rate; a common default. */
    private static final double FALLBACK_FPS = 30;

    private final Path video;
    private final boolean loop;
    private final boolean paced;
    private final AtomicBoolean running = new AtomicBoolean();
    private volatile VideoCapture reader;
    private volatile Thread worker;
    private volatile double framesPerSecond = FALLBACK_FPS;

    public VideoFileCapture(Path video, boolean loop, boolean paced) {
        this.video = Objects.requireNonNull(video, "video");
        this.loop = loop;
        this.paced = paced;
    }

    /** The rate the container declares, once playback has started. */
    public double declaredFramesPerSecond() { return framesPerSecond; }

    /** Waits for playback to end on its own; true once it has. */
    public boolean join(long timeoutMillis) throws InterruptedException {
        var current = worker;
        if (current == null) return true;
        current.join(timeoutMillis);
        return !current.isAlive();
    }

    @Override public synchronized void start(Consumer<CameraFrame> frames, Consumer<String> status, Consumer<String> failures) {
        Objects.requireNonNull(frames, "frames");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(failures, "failures");
        if (!running.compareAndSet(false, true)) return;
        // a platform thread for the same reason as the camera: decode is a
        // blocking native call, which would pin a virtual thread's carrier
        worker = new Thread(() -> captureLoop(frames, status, failures), "saha-video-capture");
        worker.setDaemon(true);
        worker.start();
    }

    private void captureLoop(Consumer<CameraFrame> frames, Consumer<String> status, Consumer<String> failures) {
        Mat bgr = null;
        Mat bgra = null;
        try {
            status.accept("Loading the local OpenCV library...");
            OpenCV.loadLocally();
            if (!Files.isRegularFile(video)) {
                failures.accept("No video file at " + video + ".");
                return;
            }
            bgr = new Mat();
            bgra = new Mat();
            if (!openAndReadFirst(bgr)) {
                failures.accept("The video at " + video + " could not be decoded. The formats a phone records"
                        + " (.mov, .mp4) normally work; if this one does not, re-encode it with ffmpeg first.");
                return;
            }
            double declared = reader.get(Videoio.CAP_PROP_FPS);
            framesPerSecond = declared > 1 && declared <= 240 ? declared : FALLBACK_FPS;
            long frameNanos = (long) (1_000_000_000L / framesPerSecond);
            status.accept("Playing " + video.getFileName() + ": " + bgr.cols() + "x" + bgr.rows()
                    + " at " + Math.round(framesPerSecond) + " fps" + (loop ? ", looping." : "."));
            long next = System.nanoTime();
            while (running.get()) {
                publish(bgr, bgra, frames);
                if (paced) {
                    next += frameNanos;
                    if (!sleepUntil(next)) return;
                }
                if (!running.get()) return;
                if (!readNext(bgr)) {
                    if (!loop) {
                        status.accept("The video has ended.");
                        return;
                    }
                    if (!rewind(bgr)) {
                        failures.accept("The video at " + video + " could not be restarted for its next loop.");
                        return;
                    }
                }
            }
        } catch (Throwable error) {
            failures.accept("Video playback is unavailable: " + safeMessage(error));
        } finally {
            if (bgr != null) bgr.release();
            if (bgra != null) bgra.release();
            running.set(false);
            var current = reader;
            if (current != null) current.release();
            reader = null;
        }
    }

    private boolean openAndReadFirst(Mat bgr) {
        var candidate = new VideoCapture(video.toString());
        // opened is not the same as decodable, exactly as with a camera: the
        // file counts only once it has actually handed over a frame
        if (candidate.isOpened() && candidate.read(bgr) && !bgr.empty()) {
            reader = candidate;
            return true;
        }
        candidate.release();
        return false;
    }

    private boolean readNext(Mat bgr) {
        var current = reader;
        return current != null && current.read(bgr) && !bgr.empty();
    }

    /** Seeks back to the first frame, reopening the file for a container that refuses the seek. */
    private boolean rewind(Mat bgr) {
        var current = reader;
        if (current != null) {
            current.set(Videoio.CAP_PROP_POS_FRAMES, 0);
            if (current.read(bgr) && !bgr.empty()) return true;
            current.release();
            reader = null;
        }
        return running.get() && openAndReadFirst(bgr);
    }

    private void publish(Mat bgr, Mat bgra, Consumer<CameraFrame> frames) {
        Imgproc.cvtColor(bgr, bgra, Imgproc.COLOR_BGR2BGRA);
        var pixels = new byte[(int) (bgra.total() * bgra.channels())];
        bgra.get(0, 0, pixels);
        frames.accept(new CameraFrame(bgra.cols(), bgra.rows(), pixels));
    }

    private boolean sleepUntil(long deadlineNanos) {
        long millis = (deadlineNanos - System.nanoTime()) / 1_000_000;
        if (millis <= 0) return true;
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            running.set(false);
            return false;
        }
    }

    private static String safeMessage(Throwable error) {
        var message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    @Override public boolean isOpen() {
        var current = reader;
        return running.get() && current != null && current.isOpened();
    }

    @Override public synchronized void close() {
        running.set(false);
        var current = reader;
        if (current != null) current.release();
        var currentWorker = worker;
        if (currentWorker != null) currentWorker.interrupt();
        worker = null;
    }
}
