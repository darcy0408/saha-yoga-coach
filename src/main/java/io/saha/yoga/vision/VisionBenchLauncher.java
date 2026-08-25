package io.saha.yoga.vision;

import nu.pattern.OpenCV;

import java.util.Arrays;
import java.util.Random;

/**
 * Times what one camera frame costs the capture thread, size by size.
 *
 * <p>Capture moved from 640x480 to the largest 4:3 mode the device offers,
 * which on the development machine is 1280x960 - four times the pixels. The
 * model's input is a fixed 192-pixel square either way, so the extra cost is
 * all in the work around the model: the BGRA-to-BGR conversion, the crop
 * render, and the defensive copy each frame pays on construction. This tool
 * measures that work, because "does the bigger frame cost frame rate" should
 * be a number before it is a decision - and it is answerable without a person
 * on camera, which the live-tracking questions are not.
 *
 * <p>What is timed: {@code new CameraFrame(...)} (the per-frame copy capture
 * pays) and {@link PoseEstimator#estimate(CameraFrame)} (conversion, crop
 * render and inference together). What is not: {@code camera.read}, the
 * preview publish, and drawing, which this process cannot reproduce honestly
 * without a device.
 *
 * <p>Frames are deterministic noise, so the crop never locks and every
 * estimate takes the whole-frame path - the dominant path until a body is
 * found, and the most expensive one, since it resizes the entire frame.
 */
public final class VisionBenchLauncher {
    private VisionBenchLauncher() { }

    private static final int WARMUP = 50;
    private static final int MEASURED = 200;

    public static void main(String[] args) throws Exception {
        var model = PoseModelLocator.locate();
        if (model.isEmpty()) {
            System.out.println("No verified model on disk; run scripts/fetch-model.ps1 first.");
            return;
        }
        System.out.println("Loading the local OpenCV library and the pose model...");
        OpenCV.loadLocally();
        try (var estimator = new PoseEstimator(model.get())) {
            System.out.println("Model input: " + estimator.inputSide() + "x" + estimator.inputSide()
                    + ". " + WARMUP + " warm-up frames, " + MEASURED + " measured, per size.\n");
            double before = measure(estimator, 640, 480);
            double after = measure(estimator, 1280, 960);
            System.out.printf("%nVerdict: 1280x960 costs %.1f ms per frame against %.1f at 640x480.%n", after, before);
            System.out.println(after < 33
                    ? "That is inside a 30 fps budget (33 ms); the capture size is not the bottleneck on this machine."
                    : "That exceeds a 30 fps budget (33 ms) on this machine; if practice stutters, the capture size is the first suspect.");
        }
    }

    /** Times one frame size and returns its median estimate cost in milliseconds. */
    private static double measure(PoseEstimator estimator, int width, int height) throws Exception {
        var pixels = new byte[width * height * 4];
        new Random(20260824).nextBytes(pixels);
        for (int i = 3; i < pixels.length; i += 4) pixels[i] = (byte) 255;

        // The copy the constructor takes is a real per-frame cost on the
        // capture thread, so it is reported, but separately. Median of many:
        // a single construction mostly measures the JIT warming up, and the
        // first draft of this launcher duly reported the small frame costing
        // three times the big one.
        var frame = new CameraFrame(width, height, pixels);
        var copies = new long[WARMUP];
        for (int i = 0; i < WARMUP; i++) {
            long start = System.nanoTime();
            frame = new CameraFrame(width, height, pixels);
            copies[i] = System.nanoTime() - start;
        }
        Arrays.sort(copies);
        double constructionMillis = copies[WARMUP / 2] / 1_000_000.0;

        for (int i = 0; i < WARMUP; i++) estimator.estimate(frame);
        var samples = new long[MEASURED];
        for (int i = 0; i < MEASURED; i++) {
            long start = System.nanoTime();
            estimator.estimate(frame);
            samples[i] = System.nanoTime() - start;
        }
        Arrays.sort(samples);
        double mean = Arrays.stream(samples).average().orElse(0) / 1_000_000.0;
        double median = samples[MEASURED / 2] / 1_000_000.0;
        double slowTail = samples[(int) (MEASURED * .9)] / 1_000_000.0;
        System.out.printf("%dx%d · estimate median %.1f ms · mean %.1f ms · slowest tenth from %.1f ms · frame copy %.2f ms%n",
                width, height, median, mean, slowTail, constructionMillis);
        return median;
    }
}
