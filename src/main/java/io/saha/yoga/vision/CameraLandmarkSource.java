package io.saha.yoga.vision;

import io.saha.yoga.domain.LandmarkFrame;

import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Real landmarks, from this machine's camera.
 *
 * <p>Capture and inference run together on the capture thread; the interface
 * only ever reads the most recent completed frame, so a slow inference can
 * never stall the practice timer. Frames are used and discarded - nothing is
 * recorded, and no image leaves the process.
 *
 * <p>The coaching pipeline is unchanged behind this: the same
 * {@link LandmarkSource} interface the demo source implements, so confidence
 * gating, the alignment rules and the cue limit all apply exactly as they did
 * to synthetic landmarks.
 */
public final class CameraLandmarkSource implements LandmarkSource {
    /**
     * What happened when the coach tried to start estimating landmarks.
     *
     * These used to be one empty Optional between them. A missing weights file
     * is expected on a fresh clone and demo mode is the right answer; a model
     * that exists but will not load is a broken install, and telling someone
     * the same thing in both cases leaves them nothing to act on.
     */
    public sealed interface Startup {
        /** The model loaded; the source is built but not yet started. */
        record Ready(CameraLandmarkSource source, Path model) implements Startup {}
        /** No weights anywhere they were looked for. Expected before the fetch script runs. */
        record NoModel(List<Path> searched) implements Startup {}
        /** Weights are on disk but unusable - wrong file, bad export, no native runtime. */
        record Unusable(Path model, String reason) implements Startup {}
    }

    private final CameraCapture capture;
    private final PoseEstimator estimator;
    private final LandmarkSmoother smoother = new LandmarkSmoother();
    private final AtomicReference<LandmarkFrame> latest = new AtomicReference<>();
    private final AtomicReference<CameraFrame> latestImage = new AtomicReference<>();
    private final AtomicReference<VisionDiagnostics> diagnostics = new AtomicReference<>();
    // Touched only on the capture thread; the smoothed rate crosses threads
    // inside the immutable diagnostics record, never through these.
    private long previousFrameNanos;
    private double smoothedIntervalMillis;
    private volatile String description = "Camera landmarks · starting";

    /**
     * Builds a camera source for {@code deviceIndex}, or explains why it could not.
     *
     * The exception is turned into a sentence rather than swallowed: an ONNX
     * runtime that will not initialise, a truncated download and a model whose
     * tensors are not what the estimator expects all land here, and all three
     * used to look exactly like "no model installed".
     */
    public static Startup open(int deviceIndex) {
        var model = PoseModelLocator.locate();
        if (model.isEmpty()) return new Startup.NoModel(PoseModelLocator.candidates());
        var path = model.get();
        try {
            return new Startup.Ready(new CameraLandmarkSource(new OpenCvCameraCapture(deviceIndex), new PoseEstimator(path)), path);
        } catch (Throwable error) {
            // Throwable, not Exception: a missing native library arrives as an
            // UnsatisfiedLinkError, which is exactly the case worth reporting.
            var message = error.getMessage();
            return new Startup.Unusable(path, message == null || message.isBlank() ? error.getClass().getSimpleName() : message);
        }
    }

    CameraLandmarkSource(CameraCapture capture, PoseEstimator estimator) {
        this.capture = capture;
        this.estimator = estimator;
    }

    /**
     * Starts capture. {@code images} receives every frame for the preview;
     * {@code status} and {@code failures} mirror {@link CameraCapture}.
     */
    public void start(Consumer<CameraFrame> images, Consumer<String> status, Consumer<String> failures) {
        capture.start(frame -> {
            latestImage.set(frame);
            images.accept(frame);
            try {
                long before = System.nanoTime();
                var raw = estimator.estimate(frame);
                double estimateMillis = (System.nanoTime() - before) / 1_000_000.0;
                latest.set(smoother.smooth(raw));
                publishDiagnostics(raw, estimateMillis, before);
                description = "Camera landmarks · on this device only";
            } catch (Exception e) {
                // One bad frame should not end the session; the confidence gate
                // handles the resulting gap, and the timer pauses on its own.
                description = "Camera landmarks · last frame could not be read";
            }
        }, status, failures);
    }

    /**
     * Numbers about the most recent frame, for the diagnostics view.
     *
     * <p>The raw estimate goes in before the smoother touches it: smoothed
     * confidences are blended across frames, and reading them to judge the
     * model would measure the smoother instead.
     */
    public Optional<VisionDiagnostics> diagnostics() { return Optional.ofNullable(diagnostics.get()); }

    private void publishDiagnostics(LandmarkFrame raw, double estimateMillis, long frameNanos) {
        // The rate is measured between estimates, not between camera frames,
        // because inference runs on this same thread: this is the pace the
        // whole pipeline actually sustains, which is the number in question
        // when deciding whether the capture size costs frame rate.
        if (previousFrameNanos != 0) {
            double interval = (frameNanos - previousFrameNanos) / 1_000_000.0;
            smoothedIntervalMillis = smoothedIntervalMillis == 0 ? interval
                    : smoothedIntervalMillis + (interval - smoothedIntervalMillis) * .2;
        }
        previousFrameNanos = frameNanos;
        var region = estimator.lastShownRegion();
        double perSecond = smoothedIntervalMillis > 0 ? 1000.0 / smoothedIntervalMillis : 0;
        diagnostics.set(new VisionDiagnostics(raw, region.x(), region.y(), region.size(), estimateMillis, perSecond));
    }

    /** The most recent camera image, for drawing the overlay in the same space. */
    public Optional<CameraFrame> latestImage() { return Optional.ofNullable(latestImage.get()); }

    /** True once landmarks have actually been produced. */
    public boolean hasLandmarks() { return latest.get() != null; }

    public boolean isOpen() { return capture.isOpen(); }

    @Override public LandmarkFrame nextFrame() {
        var frame = latest.get();
        // Before the first inference completes there is nothing observed, and an
        // empty frame is the honest answer: every required landmark is missing,
        // so the analyzer reports low confidence rather than inventing a body.
        return frame != null ? frame : new LandmarkFrame(Instant.now(), new EnumMap<>(io.saha.yoga.domain.LandmarkName.class));
    }

    @Override public boolean isTransitioning() { return false; }

    @Override public String description() { return description; }

    @Override public void close() {
        capture.close();
        try {
            estimator.close();
        } catch (Exception ignored) {
            // closing on shutdown; nothing useful left to do with the failure
        }
    }
}
