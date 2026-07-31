package io.saha.yoga.vision;

import io.saha.yoga.domain.LandmarkName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DemoLandmarkSourceTest {
    @Test void changesBodyShapeWhenPoseChanges() {
        var source = new DemoLandmarkSource();
        source.selectPose("mountain");
        var mountainWrist = source.nextFrame().landmarks().get(LandmarkName.LEFT_WRIST);
        source.selectPose("warrior_two");
        var warriorWrist = source.nextFrame().landmarks().get(LandmarkName.LEFT_WRIST);
        assertNotEquals(mountainWrist, warriorWrist);
    }
}
