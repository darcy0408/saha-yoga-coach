package io.saha.yoga.vision;

import io.saha.yoga.analysis.AnalysisResult;
import io.saha.yoga.analysis.PoseAnalyzer;
import io.saha.yoga.domain.LandmarkName;
import io.saha.yoga.routine.PoseCatalog;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Exercises the real model through the production analysis boundary.
 *
 * The model weights are not committed, so these tests skip when it is absent
 * rather than failing a clone that has not run {@code scripts/fetch-model.ps1}.
 */
class PoseEstimatorTest {
    private static CameraFrame syntheticFrame(int width, int height) {
        // deterministic noise: enough structure for the model to run on, and it
        // never varies between runs
        var pixels = new byte[width * height * 4];
        var random = new Random(20260815);
        random.nextBytes(pixels);
        for (int i = 3; i < pixels.length; i += 4) pixels[i] = (byte) 255;
        return new CameraFrame(width, height, pixels);
    }

    private java.nio.file.Path model() {
        return PoseModelLocator.locate().orElse(java.nio.file.Path.of("models", PoseModelLocator.FILE_NAME));
    }

    private void assumeModel() {
        Assumptions.assumeTrue(Files.isRegularFile(model()),
                "verified model absent; run scripts/fetch-model.ps1");
        nu.pattern.OpenCV.loadLocally();
    }

    /**
     * The Thunder candidate, verified as far as a machine alone can.
     *
     * <p>Thunder (256-pixel input) is the fallback if the person crop is not
     * enough for overhead wrists and seated legs. This test is the tensor half
     * of the blocker on enabling any model: it proves the artifact loads in
     * this runtime, declares the documented input, and emits output the
     * production decoding path can read in range. What it cannot prove is
     * accuracy on a body, which stays with the live-camera session. Until then
     * the file is inert - {@link PoseModelLocator} only ever finds Lightning
     * by name, and Thunder would need the {@code saha.model} property set
     * deliberately.
     *
     * <p>Skips unless {@code scripts/fetch-model.ps1 -Thunder} has fetched and
     * checksum-verified the candidate.
     */
    @Test void theThunderCandidateWhenPresentLoadsAndAnswersInRange() throws Exception {
        var thunder = java.nio.file.Path.of("models", "movenet-singlepose-thunder.onnx");
        Assumptions.assumeTrue(Files.isRegularFile(thunder),
                "no Thunder candidate on disk; scripts/fetch-model.ps1 -Thunder fetches one");
        nu.pattern.OpenCV.loadLocally();
        try (var estimator = new PoseEstimator(thunder)) {
            assertEquals(256, estimator.inputSide(), "MoveNet SinglePose Thunder takes a 256x256 input");
            var frame = estimator.estimate(syntheticFrame(640, 480));
            for (var name : LandmarkName.values()) {
                var mark = frame.landmarks().get(name);
                assertNotNull(mark, name + " missing from the Thunder output");
                assertTrue(mark.x() >= 0 && mark.x() <= 1, name + " x out of range: " + mark.x());
                assertTrue(mark.y() >= 0 && mark.y() <= 480.0 / 640 + 1e-9, name + " y out of range: " + mark.y());
                assertTrue(mark.confidence() >= 0 && mark.confidence() <= 1, name + " confidence out of range");
            }
        }
    }

    @Test void modelDeclaresTheExpectedSquareInput() throws Exception {
        assumeModel();
        try (var estimator = new PoseEstimator(model())) {
            assertEquals(192, estimator.inputSide(), "MoveNet SinglePose Lightning takes a 192x192 input");
        }
    }

    @Test void everyLandmarkTheCoachNeedsIsProducedInRange() throws Exception {
        assumeModel();
        try (var estimator = new PoseEstimator(model())) {
            var frame = estimator.estimate(syntheticFrame(640, 480));
            for (var name : LandmarkName.values()) {
                var mark = frame.landmarks().get(name);
                assertNotNull(mark, name + " missing");
                assertTrue(mark.x() >= 0 && mark.x() <= 1, name + " x out of range: " + mark.x());
                assertTrue(mark.y() >= 0 && mark.y() <= 1, name + " y out of range: " + mark.y());
                assertTrue(mark.confidence() >= 0 && mark.confidence() <= 1, name + " confidence out of range");
            }
        }
    }

    @Test void handsAndToesContinuePastTheJointsTheModelEmits() throws Exception {
        assumeModel();
        try (var estimator = new PoseEstimator(model())) {
            var p = estimator.estimate(syntheticFrame(640, 480)).landmarks();
            // MoveNet has no hand or toe keypoint, so they are placed along the
            // limb's own direction rather than invented from nothing
            assertDistance(p.get(LandmarkName.LEFT_WRIST), p.get(LandmarkName.LEFT_HAND), "left hand");
            assertDistance(p.get(LandmarkName.RIGHT_WRIST), p.get(LandmarkName.RIGHT_HAND), "right hand");
            assertDistance(p.get(LandmarkName.LEFT_ANKLE), p.get(LandmarkName.LEFT_TOE), "left toe");
            assertDistance(p.get(LandmarkName.RIGHT_ANKLE), p.get(LandmarkName.RIGHT_TOE), "right toe");
        }
    }

    @Test void aspectRatioIsPreservedSoJointAnglesSurvive() throws Exception {
        assumeModel();
        try (var estimator = new PoseEstimator(model())) {
            // Both axes are divided by the frame width, so on a 4:3 frame nothing
            // can land below 0.75 - the guarantee that keeps a ninety-degree knee
            // from reading as something else.
            var p = estimator.estimate(syntheticFrame(640, 480)).landmarks();
            p.forEach((name, mark) -> assertTrue(mark.y() <= 480.0 / 640 + 1e-6,
                    name + " y=" + mark.y() + " implies the frame was stretched"));
        }
    }

    /**
     * The estimator now carries the crop from one frame to the next, so a
     * second frame no longer starts where the first did.
     *
     * <p>What this covers is the fallback: the model finds no body in noise, so
     * the crop resets to the whole frame every time and the answers must stay
     * identical and in range. The tracking path cannot be reached from here at
     * all - synthetic noise scores every joint below the gate, and the repo has
     * no photograph of a person to feed it. That path is covered by geometry in
     * {@link PersonCropTest} and by picture in {@link CropTransformTest}, and
     * end to end only by a live camera with someone in front of it.
     */
    @Test void aSecondFrameThroughTheSameEstimatorIsStillInRange() throws Exception {
        assumeModel();
        try (var estimator = new PoseEstimator(model())) {
            var first = estimator.estimate(syntheticFrame(640, 480)).landmarks();
            var second = estimator.estimate(syntheticFrame(640, 480)).landmarks();
            for (var name : LandmarkName.values()) {
                var mark = second.get(name);
                assertNotNull(mark, name + " missing from the second frame");
                assertTrue(mark.x() >= 0 && mark.x() <= 1, name + " x out of range: " + mark.x());
                assertTrue(mark.y() >= 0 && mark.y() <= 480.0 / 640 + 1e-9, name + " y out of range: " + mark.y());
                assertEquals(first.get(name).x(), mark.x(), 1e-9, name + " moved between two identical frames");
            }
        }
    }

    @Test void nothingIsReportedBelowTheBottomOfThePicture() throws Exception {
        assumeModel();
        try (var estimator = new PoseEstimator(model())) {
            // The square handed to the model is padded above and below a 4:3
            // frame, and a keypoint predicted into that padding used to map to
            // y as far as 0.875 - below the floor of a picture that ends at
            // 0.75. Nothing was photographed there, so nothing may be claimed.
            estimator.estimate(syntheticFrame(640, 480)).landmarks()
                    .forEach((name, mark) -> assertTrue(mark.y() <= 480.0 / 640 + 1e-9,
                            name + " at y=" + mark.y() + " is below the bottom of the frame"));
        }
    }

    /**
     * The diagnostics view reports what actually happened, not what should have.
     *
     * <p>On synthetic noise the model clears no torso gate, so the honest
     * report is: the whole frame was shown, at these coordinates, at this pace.
     * The region here must match what {@code estimate} really used - it is the
     * number a person will read off the screen while standing in front of the
     * camera deciding whether the crop works.
     */
    @Test void diagnosticsReportTheRegionTheModelWasActuallyShown() throws Exception {
        assumeModel();
        var source = new CameraLandmarkSource(new CameraCapture() {
            @Override public void start(java.util.function.Consumer<CameraFrame> frames,
                                        java.util.function.Consumer<String> status,
                                        java.util.function.Consumer<String> failures) {
                // synchronous on purpose: the contract is frames arrive on the
                // capture callback, not that a thread delivers them
                frames.accept(syntheticFrame(640, 480));
                frames.accept(syntheticFrame(640, 480));
            }
            @Override public boolean isOpen() { return true; }
            @Override public void close() { }
        }, new PoseEstimator(model()));
        assertTrue(source.diagnostics().isEmpty(), "diagnostics existed before any frame had been estimated");
        source.start(frame -> { }, status -> { }, failure -> { });
        var d = source.diagnostics().orElseThrow();
        // Noise finds no body, so the model saw the letterbox of the whole
        // 640x480 frame: a square of the full width, centred vertically.
        assertTrue(d.wholeFrame(), "noise cannot lock a crop, so the region must be the whole frame");
        assertEquals(0, d.regionX(), 1e-9);
        assertEquals(-80.0 / 640, d.regionY(), 1e-9);
        assertEquals(1.0, d.regionSize(), 1e-9);
        assertTrue(d.estimateMillis() > 0, "an estimate that took no time was not measured");
        assertTrue(d.landmarksPerSecond() > 0, "two frames make one interval, which is a rate");
        for (var name : LandmarkName.values()) {
            assertNotNull(d.raw().landmarks().get(name), name + " missing from the raw frame");
        }
        source.close();
    }

    @Test void anUnstartedCameraCannotProduceCoachingClaims() {
        // Before the first inference there is no observation. The honest answer
        // is an empty frame, which the analyzer must report as unreliable rather
        // than treating as a well-aligned pose.
        var source = new CameraLandmarkSource(new CameraCapture() {
            @Override public void start(java.util.function.Consumer<CameraFrame> frames,
                                        java.util.function.Consumer<String> status,
                                        java.util.function.Consumer<String> failures) { }
            @Override public boolean isOpen() { return false; }
            @Override public void close() { }
        }, null);
        assertFalse(source.hasLandmarks());
        var result = new PoseAnalyzer().analyze(new PoseCatalog().require("chair"), source.nextFrame());
        assertInstanceOf(AnalysisResult.Unreliable.class, result);
    }

    private void assertDistance(io.saha.yoga.domain.Landmark from, io.saha.yoga.domain.Landmark to, String what) {
        double distance = Math.hypot(from.x() - to.x(), from.y() - to.y());
        assertEquals(.055, distance, .012, what + " should sit just past its joint");
    }
}
