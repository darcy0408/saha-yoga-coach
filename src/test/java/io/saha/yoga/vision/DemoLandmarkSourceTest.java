package io.saha.yoga.vision;

import io.saha.yoga.domain.LandmarkName;
import io.saha.yoga.domain.Landmark;
import io.saha.yoga.routine.PoseCatalog;
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

    @Test void everyPoseKeepsHumanLikeLimbSegmentLengths() {
        var source = new DemoLandmarkSource();
        for (var pose : new PoseCatalog().all()) {
            source.selectPose(pose.id());
            var p = source.nextFrame().landmarks();
            assertSegment(p.get(LandmarkName.LEFT_SHOULDER), p.get(LandmarkName.LEFT_ELBOW), pose.id()+" left upper arm");
            assertSegment(p.get(LandmarkName.LEFT_ELBOW), p.get(LandmarkName.LEFT_WRIST), pose.id()+" left forearm");
            assertSegment(p.get(LandmarkName.RIGHT_SHOULDER), p.get(LandmarkName.RIGHT_ELBOW), pose.id()+" right upper arm");
            assertSegment(p.get(LandmarkName.RIGHT_ELBOW), p.get(LandmarkName.RIGHT_WRIST), pose.id()+" right forearm");
            assertSegment(p.get(LandmarkName.LEFT_HIP), p.get(LandmarkName.LEFT_KNEE), pose.id()+" left thigh");
            assertSegment(p.get(LandmarkName.LEFT_KNEE), p.get(LandmarkName.LEFT_ANKLE), pose.id()+" left shin");
            assertSegment(p.get(LandmarkName.RIGHT_HIP), p.get(LandmarkName.RIGHT_KNEE), pose.id()+" right thigh");
            assertSegment(p.get(LandmarkName.RIGHT_KNEE), p.get(LandmarkName.RIGHT_ANKLE), pose.id()+" right shin");
        }
    }

    private void assertSegment(Landmark a, Landmark b, String message) {
        double length = Math.hypot(a.x()-b.x(), a.y()-b.y());
        assertTrue(length >= .10 && length <= .25, message+" length was "+length);
    }
}
