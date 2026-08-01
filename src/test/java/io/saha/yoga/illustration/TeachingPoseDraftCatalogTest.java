package io.saha.yoga.illustration;

import org.junit.jupiter.api.Test;

import static io.saha.yoga.illustration.BodyAnchor.*;
import static org.junit.jupiter.api.Assertions.*;

class TeachingPoseDraftCatalogTest {
    private final TeachingPoseDraftCatalog catalog = new TeachingPoseDraftCatalog();

    @Test void everyDraftPlacesBothFeetOnOneFloor() {
        for (var pose : catalog.all()) {
            for (var contact : new BodyAnchor[]{FRONT_HEEL, FRONT_TOE, REAR_HEEL, REAR_TOE}) {
                assertEquals(pose.floorY(), pose.point(contact).y(), .001, pose.poseId()+" "+contact+" must touch floor");
            }
            assertTrue(pose.point(FRONT_TOE).x()-pose.point(FRONT_HEEL).x() <= .11, pose.poseId()+" front foot stays proportionate");
            assertTrue(pose.point(REAR_TOE).x()-pose.point(REAR_HEEL).x() <= .11, pose.poseId()+" rear foot stays proportionate");
        }
    }

    @Test void chairHasARecognizableGroundedSeatShape() {
        var chair = catalog.require("chair");
        assertTrue(chair.point(HIP).x() < chair.point(FRONT_KNEE).x(), "hips move behind knees");
        assertTrue(chair.point(FRONT_KNEE).y()-chair.point(HIP).y() <= .05, "thigh approaches horizontal in a visible squat");
        assertEquals(chair.point(FRONT_KNEE).x(), chair.point(FRONT_ANKLE).x(), .025, "front shin is nearly vertical");
        assertTrue(chair.point(FRONT_HAND).y() < chair.point(SHOULDER).y(), "arms reach overhead");
    }

    @Test void warriorOneHasDeepFrontLungeAndLongRearLeg() {
        var pose = catalog.require("warrior_one");
        assertFrontLunge(pose);
        assertTrue(angle(pose.point(HIP), pose.point(REAR_KNEE), pose.point(REAR_ANKLE)) > 160, "rear leg remains long");
        assertTrue(pose.point(FRONT_HAND).y() < pose.point(HEAD).y(), "hands reach overhead");
    }

    @Test void warriorTwoHasDeepLungeLevelArmsAndLongRearLeg() {
        var pose = catalog.require("warrior_two");
        assertFrontLunge(pose);
        assertEquals(pose.point(FRONT_HAND).y(),pose.point(REAR_HAND).y(),.001,"hands share one horizontal line");
        assertEquals(pose.point(FRONT_ELBOW).y(),pose.point(REAR_ELBOW).y(),.001,"elbows share one horizontal line");
        assertTrue(angle(pose.point(HIP), pose.point(REAR_KNEE), pose.point(REAR_ANKLE)) > 160, "rear leg remains long");
    }

    private void assertFrontLunge(TeachingPoseDraft pose) {
        assertEquals(pose.point(FRONT_KNEE).x(),pose.point(FRONT_ANKLE).x(),.001,"front knee stacks above ankle");
        assertTrue(pose.point(FRONT_KNEE).x()-pose.point(HIP).x() > .18,"front thigh reaches into a visible lunge");
        assertTrue(pose.point(FRONT_KNEE).y()-pose.point(HIP).y() < .14,"front thigh stays near horizontal");
    }

    private double angle(IllustrationPoint a, IllustrationPoint b, IllustrationPoint c) {
        double bax=a.x()-b.x(),bay=a.y()-b.y(),bcx=c.x()-b.x(),bcy=c.y()-b.y();
        double cosine=(bax*bcx+bay*bcy)/(Math.hypot(bax,bay)*Math.hypot(bcx,bcy));
        return Math.toDegrees(Math.acos(Math.max(-1,Math.min(1,cosine))));
    }
}
