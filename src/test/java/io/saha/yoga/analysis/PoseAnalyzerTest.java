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
        source.selectPose("mountain");
        var result = new PoseAnalyzer().analyze(new PoseCatalog().require("mountain"), source.targetFrame());
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
}
