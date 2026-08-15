package io.saha.yoga.analysis;

import io.saha.yoga.domain.*;
import io.saha.yoga.routine.PoseCatalog;
import io.saha.yoga.vision.DemoLandmarkSource;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PoseAnalyzerTest {
    @Test void demoFrameProducesNoMoreThanTwoSuggestions() {
        var result = new PoseAnalyzer().analyze(new PoseCatalog().require("warrior_two"), new DemoLandmarkSource().nextFrame());
        var reliable = assertInstanceOf(AnalysisResult.Reliable.class, result);
        assertTrue(reliable.suggestions().size() <= 2);
    }
    @Test void lowConfidenceSuppressesCorrections() {
        var pose = new PoseCatalog().require("chair"); var map = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        pose.requiredLandmarks().forEach(n -> map.put(n, new Landmark(.5, .5, .4)));
        assertInstanceOf(AnalysisResult.Unreliable.class, new PoseAnalyzer().analyze(pose, new LandmarkFrame(Instant.now(), map)));
    }

    @Test void poseWithoutRulesIsExplicitlyInstructionOnly() {
        var source = new DemoLandmarkSource();
        source.selectPose("easy_seat");
        var result = new PoseAnalyzer().analyze(new PoseCatalog().require("easy_seat"), source.targetFrame());
        var instruction = assertInstanceOf(AnalysisResult.InstructionOnly.class, result);
        assertTrue(instruction.guidance().contains("not available"));
    }

    @Test void everyAuthoredReferenceIsTruthfulAboutWhatWasMeasured() {
        var source = new DemoLandmarkSource();
        var analyzer = new PoseAnalyzer();
        for (var pose : new PoseCatalog().all()) {
            source.selectPose(pose.id());
            var result = analyzer.analyze(pose, source.targetFrame());
            if (pose.alignmentRules().isEmpty()) {
                assertInstanceOf(AnalysisResult.InstructionOnly.class, result, pose.id());
            } else {
                var reliable = assertInstanceOf(AnalysisResult.Reliable.class, result, pose.id());
                assertTrue(reliable.suggestions().isEmpty(), () -> pose.id() + " contradicts its reference: " + reliable.suggestions());
                assertEquals("Steady — keep breathing", reliable.status(), pose.id());
            }
        }
    }

    @Test void deliberateChairKneeDeviationProducesTheExpectedCue() {
        var source = new DemoLandmarkSource();
        var pose = new PoseCatalog().require("chair");
        source.selectPose(pose.id());
        var reference = source.targetFrame();
        var changed = new EnumMap<>(reference.landmarks());
        var knee = changed.get(LandmarkName.LEFT_KNEE);
        changed.put(LandmarkName.LEFT_KNEE, new Landmark(knee.x() - .18, knee.y() + .02, knee.confidence()));

        var result = assertInstanceOf(AnalysisResult.Reliable.class,
                new PoseAnalyzer().analyze(pose, new LandmarkFrame(Instant.now(), changed)));

        assertEquals("Almost aligned", result.status());
        assertEquals(List.of("Try a smaller knee bend and keep your knees tracking toward your toes."), result.suggestions());
    }

    @Test void measuredReferencesRemainValidWhenLeftAndRightAreSwapped() {
        var source = new DemoLandmarkSource();
        var analyzer = new PoseAnalyzer();
        for (var pose : new PoseCatalog().all().stream().filter(value -> !value.alignmentRules().isEmpty()).toList()) {
            source.selectPose(pose.id());
            var original = source.targetFrame().landmarks();
            var mirrored = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
            original.forEach((name, point) -> mirrored.put(swap(name), point));

            var result = assertInstanceOf(AnalysisResult.Reliable.class,
                    analyzer.analyze(pose, new LandmarkFrame(Instant.now(), mirrored)), pose.id());
            assertTrue(result.suggestions().isEmpty(), () -> pose.id() + " failed on the opposite lead side");
        }
    }

    private LandmarkName swap(LandmarkName name) {
        return switch (name) {
            case LEFT_SHOULDER -> LandmarkName.RIGHT_SHOULDER;
            case RIGHT_SHOULDER -> LandmarkName.LEFT_SHOULDER;
            case LEFT_ELBOW -> LandmarkName.RIGHT_ELBOW;
            case RIGHT_ELBOW -> LandmarkName.LEFT_ELBOW;
            case LEFT_WRIST -> LandmarkName.RIGHT_WRIST;
            case RIGHT_WRIST -> LandmarkName.LEFT_WRIST;
            case LEFT_HAND -> LandmarkName.RIGHT_HAND;
            case RIGHT_HAND -> LandmarkName.LEFT_HAND;
            case LEFT_HIP -> LandmarkName.RIGHT_HIP;
            case RIGHT_HIP -> LandmarkName.LEFT_HIP;
            case LEFT_KNEE -> LandmarkName.RIGHT_KNEE;
            case RIGHT_KNEE -> LandmarkName.LEFT_KNEE;
            case LEFT_ANKLE -> LandmarkName.RIGHT_ANKLE;
            case RIGHT_ANKLE -> LandmarkName.LEFT_ANKLE;
            case LEFT_TOE -> LandmarkName.RIGHT_TOE;
            case RIGHT_TOE -> LandmarkName.LEFT_TOE;
            case NOSE -> LandmarkName.NOSE;
        };
    }
}
