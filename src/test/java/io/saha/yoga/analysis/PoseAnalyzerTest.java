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
}

