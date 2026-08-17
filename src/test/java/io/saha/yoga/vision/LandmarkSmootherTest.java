package io.saha.yoga.vision;

import io.saha.yoga.domain.Landmark;
import io.saha.yoga.domain.LandmarkFrame;
import io.saha.yoga.domain.LandmarkName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.*;

/** The flicker fix, driven without a camera. */
class LandmarkSmootherTest {
    private static final double DRAWN_AT = .30;
    private final LandmarkSmoother smoother = new LandmarkSmoother();

    private LandmarkFrame frame(Double x, Double y, Double confidence) {
        var p = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        if (x != null) p.put(LandmarkName.LEFT_KNEE, new Landmark(x, y, confidence));
        return new LandmarkFrame(Instant.now(), p);
    }

    private double knee(LandmarkFrame f) { return f.landmarks().get(LandmarkName.LEFT_KNEE).confidence(); }

    @Test void aSteadyJointSettlesOnItsRealValue() {
        LandmarkFrame out = null;
        for (int i = 0; i < 40; i++) out = smoother.smooth(frame(.5, .6, .8));
        assertEquals(.5, out.landmarks().get(LandmarkName.LEFT_KNEE).x(), .01);
        assertEquals(.8, knee(out), .02, "confidence should converge, not lag forever");
    }

    @Test void aSingleWobbleBelowTheThresholdDoesNotHideTheLimb() {
        // the exact cause of the flicker: a joint sitting near the threshold
        // whose score dips for one frame
        for (int i = 0; i < 20; i++) smoother.smooth(frame(.5, .6, .45));
        var dipped = smoother.smooth(frame(.5, .6, .05));
        assertTrue(knee(dipped) > DRAWN_AT, "one bad frame should not erase the limb: " + knee(dipped));
    }

    @Test void aJointHoveringNearTheThresholdStaysDrawn() {
        // scores that average comfortably above the line must not blink merely
        // because individual frames dip below it
        for (int i = 0; i < 20; i++) smoother.smooth(frame(.5, .6, .40));
        for (int i = 0; i < 16; i++) {
            var alternating = smoother.smooth(frame(.5, .6, i % 2 == 0 ? .25 : .55));
            assertTrue(knee(alternating) > DRAWN_AT, "alternating frame " + i + " dropped out: " + knee(alternating));
        }
    }

    @Test void wildlyAlternatingScoresSettleInsteadOfStrobing() {
        // even when the average sits below the line and the limb is correctly
        // hidden, the value must stop swinging - a stable answer either way is
        // what makes it watchable
        for (int i = 0; i < 30; i++) smoother.smooth(frame(.5, .6, i % 2 == 0 ? .05 : .45));
        double lowest = 1, highest = 0;
        for (int i = 0; i < 20; i++) {
            double value = knee(smoother.smooth(frame(.5, .6, i % 2 == 0 ? .05 : .45)));
            lowest = Math.min(lowest, value);
            highest = Math.max(highest, value);
        }
        assertTrue(highest - lowest < .12, "raw swing of 0.40 should be damped, was " + (highest - lowest));
    }

    @Test void aBodyThatActuallyLeavesStillFadesAway() {
        for (int i = 0; i < 20; i++) smoother.smooth(frame(.5, .6, .9));
        LandmarkFrame out = null;
        for (int i = 0; i < 25; i++) out = smoother.smooth(frame(.5, .6, .01));
        assertTrue(knee(out) < DRAWN_AT, "a genuine loss must still stop being drawn: " + knee(out));
    }

    @Test void aMissingLandmarkHoldsItsPlaceThenDecays() {
        for (int i = 0; i < 20; i++) smoother.smooth(frame(.5, .6, .9));
        var gap = smoother.smooth(frame(null, null, null));
        assertEquals(.5, gap.landmarks().get(LandmarkName.LEFT_KNEE).x(), 1e-9, "position is held, not lost");
        assertTrue(knee(gap) > DRAWN_AT, "one missing frame should bridge");
        LandmarkFrame out = null;
        for (int i = 0; i < 12; i++) out = smoother.smooth(frame(null, null, null));
        assertTrue(knee(out) < DRAWN_AT, "a landmark gone for good must fade: " + knee(out));
    }

    @Test void positionJitterIsDampened() {
        for (int i = 0; i < 20; i++) smoother.smooth(frame(.5, .6, .9));
        var jumped = smoother.smooth(frame(.9, .6, .9));
        double x = jumped.landmarks().get(LandmarkName.LEFT_KNEE).x();
        assertTrue(x > .5 && x < .9, "should move toward the new reading without snapping to it: " + x);
    }
}
