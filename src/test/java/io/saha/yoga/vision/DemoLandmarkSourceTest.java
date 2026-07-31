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
        var mountainWrist = source.targetFrame().landmarks().get(LandmarkName.LEFT_WRIST);
        source.selectPose("warrior_two");
        var warriorWrist = source.targetFrame().landmarks().get(LandmarkName.LEFT_WRIST);
        assertNotEquals(mountainWrist, warriorWrist);
        assertTrue(source.isTransitioning());
    }

    @Test void everyPoseKeepsHumanLikeLimbSegmentLengths() {
        var source = new DemoLandmarkSource();
        for (var pose : new PoseCatalog().all()) {
            source.selectPose(pose.id());
            var p = source.targetFrame().landmarks();
            assertLength(p.get(LandmarkName.LEFT_SHOULDER), p.get(LandmarkName.LEFT_ELBOW), .15, pose.id()+" left upper arm");
            assertLength(p.get(LandmarkName.LEFT_ELBOW), p.get(LandmarkName.LEFT_WRIST), .14, pose.id()+" left forearm");
            assertLength(p.get(LandmarkName.RIGHT_SHOULDER), p.get(LandmarkName.RIGHT_ELBOW), .15, pose.id()+" right upper arm");
            assertLength(p.get(LandmarkName.RIGHT_ELBOW), p.get(LandmarkName.RIGHT_WRIST), .14, pose.id()+" right forearm");
            assertLength(p.get(LandmarkName.LEFT_HIP), p.get(LandmarkName.LEFT_KNEE), .23, pose.id()+" left thigh");
            assertLength(p.get(LandmarkName.LEFT_KNEE), p.get(LandmarkName.LEFT_ANKLE), .22, pose.id()+" left shin");
            assertLength(p.get(LandmarkName.RIGHT_HIP), p.get(LandmarkName.RIGHT_KNEE), .23, pose.id()+" right thigh");
            assertLength(p.get(LandmarkName.RIGHT_KNEE), p.get(LandmarkName.RIGHT_ANKLE), .22, pose.id()+" right shin");
            var shoulderCenter = midpoint(p.get(LandmarkName.LEFT_SHOULDER),p.get(LandmarkName.RIGHT_SHOULDER));
            var hipCenter = midpoint(p.get(LandmarkName.LEFT_HIP),p.get(LandmarkName.RIGHT_HIP));
            assertLength(shoulderCenter,hipCenter,.24,pose.id()+" torso");
            assertHand(p.get(LandmarkName.LEFT_WRIST), p.get(LandmarkName.LEFT_HAND), pose.id()+" left hand");
            assertHand(p.get(LandmarkName.RIGHT_WRIST), p.get(LandmarkName.RIGHT_HAND), pose.id()+" right hand");
        }
    }

    private void assertLength(Landmark a, Landmark b, double expected, String message) {
        double length = Math.hypot(a.x()-b.x(), a.y()-b.y());
        assertEquals(expected,length,.001,message);
    }
    private Landmark midpoint(Landmark a,Landmark b) { return new Landmark((a.x()+b.x())/2,(a.y()+b.y())/2,1); }
    private void assertHand(Landmark wrist, Landmark hand, String message) {
        double length = Math.hypot(wrist.x()-hand.x(), wrist.y()-hand.y());
        assertEquals(.055, length, .001, message);
    }
}
