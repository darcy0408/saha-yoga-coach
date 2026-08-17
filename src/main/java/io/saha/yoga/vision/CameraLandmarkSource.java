package io.saha.yoga.vision;

import io.saha.yoga.domain.LandmarkFrame;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumMap;
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
    /** Where the verified model is expected; absent means the coach stays in demo mode. */
    public static final Path MODEL = Path.of("models", "movenet-singlepose-lightning.onnx");

    private final CameraCapture capture;
    private final PoseEstimator estimator;
    private final LandmarkSmoother smoother = new LandmarkSmoother();
    private final AtomicReference<LandmarkFrame> latest = new AtomicReference<>();
    private final AtomicReference<CameraFrame> latestImage = new AtomicReference<>();
    private volatile String description = "Camera landmarks · starting";
    private volatile boolean failed;

    /** Present only when a verified model file is actually on disk. */
    public static Optional<CameraLandmarkSource> ifModelPresent(int deviceIndex) {
        if (!Files.isRegularFile(MODEL)) return Optional.empty();
        try {
            return Optional.of(new CameraLandmarkSource(new OpenCvCameraCapture(deviceIndex), new PoseEstimator(MODEL)));
        } catch (Exception e) {
            return Optional.empty();
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
                latest.set(smoother.smooth(estimator.estimate(frame)));
                description = "Camera landmarks · on this device only";
            } catch (Exception e) {
                // One bad frame should not end the session; the confidence gate
                // handles the resulting gap, and the timer pauses on its own.
                failed = true;
                description = "Camera landmarks · last frame could not be read";
            }
        }, status, failures);
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

    @Override public String description() { return failed ? description : description; }

    @Override public void close() {
        capture.close();
        try {
            estimator.close();
        } catch (Exception ignored) {
            // closing on shutdown; nothing useful left to do with the failure
        }
    }
}
