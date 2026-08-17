package io.saha.yoga.analysis;

import io.saha.yoga.domain.Landmark;
import io.saha.yoga.domain.LandmarkFrame;
import io.saha.yoga.domain.LandmarkName;
import io.saha.yoga.routine.PoseCatalog;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.*;

/** A pose must not be blocked by a joint it never measures. */
class VisibilityTest {
    private final PoseCatalog catalog = new PoseCatalog();
    private final PoseAnalyzer analyzer = new PoseAnalyzer();

    /** A plausible standing body; only the confidences differ between cases. */
    private LandmarkFrame frame(double torso, double leftLeg, double rightLeg) {
        var p = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        for (var name : LandmarkName.values()) {
            double confidence = switch (name) {
                case LEFT_KNEE, LEFT_ANKLE, LEFT_TOE -> leftLeg;
                case RIGHT_KNEE, RIGHT_ANKLE, RIGHT_TOE -> rightLeg;
                default -> torso;
            };
            double[] at = switch (name) {
                case NOSE -> new double[]{.50, .10};
                case LEFT_SHOULDER -> new double[]{.45, .25};
                case RIGHT_SHOULDER -> new double[]{.55, .25};
                case LEFT_ELBOW -> new double[]{.42, .40};
                case RIGHT_ELBOW -> new double[]{.58, .40};
                case LEFT_WRIST -> new double[]{.40, .54};
                case RIGHT_WRIST -> new double[]{.60, .54};
                case LEFT_HAND -> new double[]{.39, .59};
                case RIGHT_HAND -> new double[]{.61, .59};
                case LEFT_HIP -> new double[]{.46, .55};
                case RIGHT_HIP -> new double[]{.54, .55};
                case LEFT_KNEE -> new double[]{.44, .74};
                case RIGHT_KNEE -> new double[]{.56, .74};
                case LEFT_ANKLE -> new double[]{.45, .92};
                case RIGHT_ANKLE -> new double[]{.55, .92};
                case LEFT_TOE -> new double[]{.43, .96};
                case RIGHT_TOE -> new double[]{.57, .96};
            };
            p.put(name, new Landmark(at[0], at[1], confidence));
        }
        return new LandmarkFrame(Instant.now(), p);
    }

    @Test void aSeatedPoseCoachesWithTheFeetOutOfFrame() {
        // the exact situation on camera: torso tracked well, feet below the
        // bottom edge. Easy Seat is measured at the head and shoulders, which
        // are still in plain view, so it must go on coaching.
        var result = analyzer.analyze(catalog.require("easy_seat"), frame(.92, .05, .05));
        assertInstanceOf(AnalysisResult.Reliable.class, result);
    }

    @Test void aPoseThatMeasuresKneesStillNeedsToSeeALeg() {
        var result = analyzer.analyze(catalog.require("chair"), frame(.92, .05, .05));
        var unreliable = assertInstanceOf(AnalysisResult.Unreliable.class, result);
        assertTrue(unreliable.guidance().contains("feet"), "should name what is missing: " + unreliable.guidance());
        assertTrue(unreliable.guidance().contains("knees"), "should name what is missing: " + unreliable.guidance());
    }

    @Test void oneVisibleSideIsEnoughForABilateralRule() {
        // standing side-on to the camera hides the far leg; the rules already
        // evaluate both sides and pick one, so the near leg suffices
        var result = analyzer.analyze(catalog.require("chair"), frame(.92, .9, .05));
        assertInstanceOf(AnalysisResult.Reliable.class, result);
    }

    @Test void anEmptyRoomStillStopsEverything() {
        var result = analyzer.analyze(catalog.require("easy_seat"), frame(.2, .9, .9));
        var unreliable = assertInstanceOf(AnalysisResult.Unreliable.class, result);
        assertTrue(unreliable.guidance().contains("shoulders"), "should name what is missing: " + unreliable.guidance());
    }

    @Test void sittingCrossLeggedDoesNotDemandTheHips() {
        // Thighs cross the hip joints when seated, so the model scores them low
        // however far back you sit. Easy Seat must therefore be measured from
        // something else entirely - it takes its bearing from the head and
        // shoulders - and must go on coaching with the hips as good as unseen.
        var p = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        var seen = frame(.9, .9, .9).landmarks();
        seen.forEach((name, mark) -> {
            double confidence = switch (name) { case LEFT_HIP, RIGHT_HIP -> .08; default -> mark.confidence(); };
            p.put(name, new Landmark(mark.x(), mark.y(), confidence));
        });
        var result = analyzer.analyze(catalog.require("easy_seat"), new LandmarkFrame(Instant.now(), p));
        assertInstanceOf(AnalysisResult.Reliable.class, result);
    }
}
