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

    private void assumeModel() {
        Assumptions.assumeTrue(Files.isRegularFile(CameraLandmarkSource.MODEL),
                "verified model absent; run scripts/fetch-model.ps1");
        nu.pattern.OpenCV.loadLocally();
    }

    @Test void modelDeclaresTheExpectedSquareInput() throws Exception {
        assumeModel();
        try (var estimator = new PoseEstimator(CameraLandmarkSource.MODEL)) {
            assertEquals(192, estimator.inputSide(), "MoveNet SinglePose Lightning takes a 192x192 input");
        }
    }

    @Test void everyLandmarkTheCoachNeedsIsProducedInRange() throws Exception {
        assumeModel();
        try (var estimator = new PoseEstimator(CameraLandmarkSource.MODEL)) {
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
        try (var estimator = new PoseEstimator(CameraLandmarkSource.MODEL)) {
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
        try (var estimator = new PoseEstimator(CameraLandmarkSource.MODEL)) {
            // Both axes are divided by the frame width, so on a 4:3 frame nothing
            // can land below 0.75 - the guarantee that keeps a ninety-degree knee
            // from reading as something else.
            var p = estimator.estimate(syntheticFrame(640, 480)).landmarks();
            p.forEach((name, mark) -> assertTrue(mark.y() <= 480.0 / 640 + 1e-6,
                    name + " y=" + mark.y() + " implies the frame was stretched"));
        }
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
