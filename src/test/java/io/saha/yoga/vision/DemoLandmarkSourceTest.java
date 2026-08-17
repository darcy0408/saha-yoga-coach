package io.saha.yoga.vision;

import io.saha.yoga.domain.LandmarkName;
import io.saha.yoga.domain.Landmark;
import io.saha.yoga.routine.PoseCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DemoLandmarkSourceTest {
    @Test void changesBodyShapeWhenPoseChanges() {
        var source = new DemoLandmarkSource();
        source.selectPose("tree");
        var treeWrist = source.targetFrame().landmarks().get(LandmarkName.LEFT_WRIST);
        source.selectPose("warrior_two");
        var warriorWrist = source.targetFrame().landmarks().get(LandmarkName.LEFT_WRIST);
        assertNotEquals(treeWrist, warriorWrist);
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
            assertLength(p.get(LandmarkName.LEFT_ANKLE),p.get(LandmarkName.LEFT_TOE),.08,pose.id()+" left foot");
            assertLength(p.get(LandmarkName.RIGHT_ANKLE),p.get(LandmarkName.RIGHT_TOE),.08,pose.id()+" right foot");
        }
    }

    @Test void referenceAlignmentRelationshipsArePreserved() {
        var source=new DemoLandmarkSource();
        source.selectPose("warrior_two"); var warrior=source.targetFrame().landmarks();
        assertEquals(warrior.get(LandmarkName.LEFT_KNEE).x(),warrior.get(LandmarkName.LEFT_ANKLE).x(),.03,"front knee over ankle");
        assertTrue(warrior.get(LandmarkName.LEFT_TOE).x()<warrior.get(LandmarkName.LEFT_ANKLE).x(),"front foot points toward gaze");
        assertTrue(warrior.get(LandmarkName.RIGHT_TOE).y()<warrior.get(LandmarkName.RIGHT_ANKLE).y(),"back foot turns inward");
        source.selectPose("chair"); var chair=source.targetFrame().landmarks();
        assertTrue(chair.get(LandmarkName.LEFT_KNEE).x()>chair.get(LandmarkName.LEFT_HIP).x(),"chair hips sit back from knees");
        assertEquals(chair.get(LandmarkName.LEFT_KNEE).x(),chair.get(LandmarkName.LEFT_ANKLE).x(),.08,"chair knee stays near ankle");
        source.selectPose("cat_cow"); var table=source.targetFrame().landmarks();
        assertEquals(table.get(LandmarkName.LEFT_SHOULDER).x(),table.get(LandmarkName.LEFT_WRIST).x(),.08,"wrist beneath shoulder");
        assertEquals(table.get(LandmarkName.LEFT_HIP).x(),table.get(LandmarkName.LEFT_KNEE).x(),.08,"knee beneath hip");
        source.selectPose("triangle"); var triangle=source.targetFrame().landmarks();
        assertEquals(triangle.get(LandmarkName.LEFT_HAND).x(),triangle.get(LandmarkName.LEFT_KNEE).x(),.10,"lower hand reaches toward shin");
    }

    @Test void callingTheChangeOfSidesTurnsTheTargetAround() {
        var source = new DemoLandmarkSource();
        source.selectPose("warrior_two");
        var first = source.targetFrame().landmarks();
        source.switchSides();
        var second = source.targetFrame().landmarks();
        // the knee that was bent is now the other knee, in the mirrored place
        assertEquals(1 - first.get(LandmarkName.LEFT_KNEE).x(), second.get(LandmarkName.RIGHT_KNEE).x(), 1e-9,
                "the bent front knee should change sides");
        assertEquals(first.get(LandmarkName.LEFT_KNEE).y(), second.get(LandmarkName.RIGHT_KNEE).y(), 1e-9,
                "switching sides should not change height");
        assertEquals(1 - first.get(LandmarkName.LEFT_TOE).x(), second.get(LandmarkName.RIGHT_TOE).x(), 1e-9,
                "the feet turn around with the legs");
        source.switchSides();
        assertEquals(first.get(LandmarkName.LEFT_KNEE).x(), source.targetFrame().landmarks().get(LandmarkName.LEFT_KNEE).x(), 1e-9,
                "switching back should return the pose it started from");
    }

    @Test void aNewPoseStartsOnTheSideItWasAuthoredOn() {
        var source = new DemoLandmarkSource();
        source.selectPose("warrior_two");
        var authored = source.targetFrame().landmarks().get(LandmarkName.LEFT_KNEE).x();
        source.switchSides();
        source.selectPose("warrior_two");
        assertEquals(authored, source.targetFrame().landmarks().get(LandmarkName.LEFT_KNEE).x(), 1e-9,
                "a mirrored hold should not leak into the next pose");
    }

    @Test void everyPoseRestsOnTheFloorReference() {
        var source = new DemoLandmarkSource();
        for (var pose : new PoseCatalog().all()) {
            source.selectPose(pose.id());
            var p = source.targetFrame().landmarks();
            double lowest = p.values().stream().mapToDouble(Landmark::y).max().orElseThrow();
            assertEquals(LandmarkSource.FLOOR_Y, lowest, 1e-9,
                    pose.id() + " should touch the floor the view draws, not float above it");
            p.forEach((name, mark) -> assertTrue(mark.y() <= LandmarkSource.FLOOR_Y + 1e-9,
                    pose.id() + " " + name + " sank through the floor"));
        }
    }

    @Test void theDisplayedFrameIsGroundedToo() {
        // the view draws nextFrame(), not targetFrame(): grounding the target
        // alone would still leave every animated frame floating
        var source = new DemoLandmarkSource();
        source.selectPose("warrior_two");
        for (int i = 0; i < 3; i++) {
            var p = source.nextFrame().landmarks();
            double lowest = p.values().stream().mapToDouble(Landmark::y).max().orElseThrow();
            assertEquals(LandmarkSource.FLOOR_Y, lowest, 1e-9, "displayed frame " + i + " floats");
        }
    }

    @Test void floorPosesRespectAnatomyAndGravity() {
        var source = new DemoLandmarkSource();
        source.selectPose("low_lunge"); var lunge = source.targetFrame().landmarks();
        assertEquals(LandmarkSource.FLOOR_Y, lunge.get(LandmarkName.RIGHT_KNEE).y(), .06, "low lunge rests the back knee on the floor");
        assertEquals(lunge.get(LandmarkName.LEFT_KNEE).x(), lunge.get(LandmarkName.LEFT_ANKLE).x(), .06, "front shin stays vertical over the foot");
        assertTrue(lunge.get(LandmarkName.LEFT_WRIST).y() < lunge.get(LandmarkName.NOSE).y(), "arms reach overhead");
        assertTrue(lunge.get(LandmarkName.LEFT_HIP).y() > lunge.get(LandmarkName.LEFT_KNEE).y() - .08, "hips sink toward front-knee height");
        source.selectPose("bridge"); var bridge = source.targetFrame().landmarks();
        assertEquals(LandmarkSource.FLOOR_Y, bridge.get(LandmarkName.LEFT_SHOULDER).y(), .07, "bridge keeps the shoulders on the floor");
        assertEquals(LandmarkSource.FLOOR_Y, bridge.get(LandmarkName.LEFT_TOE).y(), .05, "bridge keeps the feet on the floor");
        assertTrue(bridge.get(LandmarkName.LEFT_KNEE).y() < bridge.get(LandmarkName.LEFT_HIP).y(), "knees ride above the lifted hips");
        assertTrue(bridge.get(LandmarkName.LEFT_HIP).y() < bridge.get(LandmarkName.LEFT_SHOULDER).y(), "hips lift off the floor");
        assertEquals(bridge.get(LandmarkName.LEFT_KNEE).x(), bridge.get(LandmarkName.LEFT_ANKLE).x(), .05, "shins stay vertical so feet sit under knees");
        source.selectPose("seated_fold"); var fold = source.targetFrame().landmarks();
        assertEquals(LandmarkSource.FLOOR_Y, fold.get(LandmarkName.LEFT_HIP).y(), .06, "seated fold grounds the sitting bones");
        assertEquals(fold.get(LandmarkName.LEFT_KNEE).y(), fold.get(LandmarkName.NOSE).y(), .08, "the head folds down toward the shins");
        assertTrue(fold.get(LandmarkName.LEFT_TOE).y() < fold.get(LandmarkName.LEFT_ANKLE).y(), "feet stay flexed toward the ceiling");
    }

    /**
     * The seat is the contact these poses stand on, and nothing else checks it.
     *
     * everyPoseRestsOnTheFloorReference() only asserts that the LOWEST point
     * touches the floor, which a cross-legged figure satisfied while its pelvis
     * floated a thigh's length up - a chair that is not there. Naming the hips
     * is what makes that visible.
     */
    @Test void crossLeggedPosesRestTheirSeatOnTheFloor() {
        var source = new DemoLandmarkSource();
        for (var id : new String[]{"easy_seat", "seated_side_reach", "seated_twist"}) {
            source.selectPose(id);
            var p = source.targetFrame().landmarks();
            assertEquals(LandmarkSource.FLOOR_Y, p.get(LandmarkName.LEFT_HIP).y(), .04, id + " left sitting bone is off the floor");
            assertEquals(LandmarkSource.FLOOR_Y, p.get(LandmarkName.RIGHT_HIP).y(), .04, id + " right sitting bone is off the floor");
            // the shins fold in front rather than hanging: both knees stay out
            // wider than the hips, and no lower than the seat they fold from
            assertTrue(p.get(LandmarkName.LEFT_KNEE).x() < p.get(LandmarkName.LEFT_HIP).x() - .10, id + " left knee should splay out from the hip");
            assertTrue(p.get(LandmarkName.RIGHT_KNEE).x() > p.get(LandmarkName.RIGHT_HIP).x() + .10, id + " right knee should splay out from the hip");
            assertTrue(p.get(LandmarkName.LEFT_KNEE).y() <= p.get(LandmarkName.LEFT_HIP).y(), id + " left knee should not sink below the seat");
            assertTrue(p.get(LandmarkName.RIGHT_KNEE).y() <= p.get(LandmarkName.RIGHT_HIP).y(), id + " right knee should not sink below the seat");
            // hands belong on or above the knees, never planted through the floor
            assertTrue(p.get(LandmarkName.LEFT_HAND).y() < LandmarkSource.FLOOR_Y, id + " left hand sank to the floor");
            assertTrue(p.get(LandmarkName.RIGHT_HAND).y() < LandmarkSource.FLOOR_Y, id + " right hand sank to the floor");
        }
    }

    /** The side reach is the one cross-legged pose with an arm overhead. */
    @Test void theSeatedSideReachActuallyReachesOverhead() {
        var source = new DemoLandmarkSource();
        source.selectPose("seated_side_reach");
        var p = source.targetFrame().landmarks();
        assertTrue(p.get(LandmarkName.LEFT_HAND).y() < p.get(LandmarkName.NOSE).y(), "the reaching hand rises past the head");
        assertTrue(p.get(LandmarkName.RIGHT_HAND).y() > p.get(LandmarkName.RIGHT_SHOULDER).y(), "the other hand stays down");
    }

    @Test void upwardSaluteStandsOnBothFeet() {
        var source = new DemoLandmarkSource();
        source.selectPose("upward_salute");
        var p = source.targetFrame().landmarks();
        assertEquals(LandmarkSource.FLOOR_Y, p.get(LandmarkName.LEFT_TOE).y(), .001, "left foot down");
        assertEquals(LandmarkSource.FLOOR_Y, p.get(LandmarkName.RIGHT_TOE).y(), .001, "right foot down");
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
