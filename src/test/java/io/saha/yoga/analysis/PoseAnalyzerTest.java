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
        // spread the points so a failure here means the confidence gate let
        // this through, not that the geometry rejected a degenerate triangle
        double y = .2;
        for (var name : LandmarkName.values()) map.put(name, new Landmark(.4 + (y += .02), y, .15));
        assertInstanceOf(AnalysisResult.Unreliable.class, new PoseAnalyzer().analyze(pose, new LandmarkFrame(Instant.now(), map)));
    }

    /** A twist is a rotation about the spine, which one flat view cannot measure. */
    @Test void poseWithoutRulesIsExplicitlyInstructionOnly() {
        var source = new DemoLandmarkSource();
        source.selectPose("seated_twist");
        var result = new PoseAnalyzer().analyze(new PoseCatalog().require("seated_twist"), source.targetFrame());
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
                assertFalse(reliable.status().startsWith("Almost"), pose.id() + " should not report a miss against its own reference");
                assertEquals(pose.alignmentRules().size(), reliable.measurements().size(),
                        pose.id() + " should report every angle it measured");
                // an ungraded measurement is shown but never ticked, so a shape
                // with no correct value cannot be reported as correct
                reliable.measurements().forEach(m -> {
                    if (!m.graded()) assertFalse(m.inRange(), pose.id() + " " + m.label() + " must not claim a verdict");
                });
            }
        }
    }

    /**
     * The chime rings on the frame a measured pose crosses into range, so this
     * pins the condition that rings it rather than the sound itself.
     *
     * Without this, a reference pose drifting out of its own range is silent in
     * two ways at once: no ding, and no failing test either. That is exactly
     * what happened when both warriors were authored into a lunge shallower
     * than their rule allows.
     */
    @Test void aMeasuredPoseHeldOnItsReferenceReachesTheStateThatRingsTheChime() {
        var source = new DemoLandmarkSource();
        var analyzer = new PoseAnalyzer();
        // an ungraded pose is excluded on purpose rather than overlooked:
        // cat-cow is a movement between two correct positions, so it is
        // measured and shown but never judged, and must never ding
        var measured = new PoseCatalog().all().stream()
                .filter(pose -> pose.alignmentRules().stream().anyMatch(AlignmentRule::graded)).toList();
        assertFalse(measured.isEmpty(), "no measured poses to check");
        for (var pose : measured) {
            source.selectPose(pose.id());
            var reliable = assertInstanceOf(AnalysisResult.Reliable.class, analyzer.analyze(pose, source.targetFrame()), pose.id());
            // the same predicate the practice screen uses to ring it
            boolean aligned = reliable.suggestions().isEmpty()
                    && reliable.measurements().stream().anyMatch(AnalysisResult.Measurement::inRange);
            assertTrue(aligned, () -> pose.id() + " can never ring the chime: " + reliable.status() + " " + reliable.suggestions());
        }
    }

    /**
     * A good lunge, seen at an angle, must not be corrected by the leg behind.
     *
     * The hidden leg still scores above the confidence gate while its
     * coordinates are largely inferred, and MOST_BENT takes whichever angle is
     * smaller - so it won the comparison and a knee stacked over its ankle was
     * told it had travelled past it.
     */
    @Test void aLegHiddenBehindTheOtherDoesNotOverruleTheOneInPlainView() {
        var pose = new PoseCatalog().require("low_lunge");
        var points = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        for (var name : LandmarkName.values()) points.put(name, new Landmark(.5, .5, .9));
        // the near leg, plainly visible, bent to a right angle over its ankle
        points.put(LandmarkName.LEFT_HIP, new Landmark(.40, .50, .85));
        points.put(LandmarkName.LEFT_KNEE, new Landmark(.40, .72, .85));
        points.put(LandmarkName.LEFT_ANKLE, new Landmark(.62, .72, .85));
        // the far leg, barely made out behind it, folded into a shape no one
        // is holding - above the gate, well below the near leg
        points.put(LandmarkName.RIGHT_HIP, new Landmark(.42, .50, .40));
        points.put(LandmarkName.RIGHT_KNEE, new Landmark(.42, .72, .40));
        points.put(LandmarkName.RIGHT_ANKLE, new Landmark(.44, .52, .40));

        var result = assertInstanceOf(AnalysisResult.Reliable.class,
                new PoseAnalyzer().analyze(pose, new LandmarkFrame(Instant.now(), points)));

        assertEquals(90, result.measurements().getFirst().degrees(), 1,
                "the leg in plain view is the one that should answer");
        assertTrue(result.suggestions().isEmpty(),
                () -> "a correct lunge was corrected by the leg behind it: " + result.suggestions());
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
        // moving the knee back straightens the leg, so the cue must ask for
        // more bend. The single cue this rule used to carry said the opposite.
        assertEquals(List.of("Try sitting your hips back a little further, knees tracking toward your toes."), result.suggestions());
    }

    @Test void goingPastTheRangeIsCorrectedTheOtherWay() {
        var source = new DemoLandmarkSource();
        var pose = new PoseCatalog().require("chair");
        source.selectPose(pose.id());
        var changed = new EnumMap<>(source.targetFrame().landmarks());
        var knee = changed.get(LandmarkName.LEFT_KNEE);
        // forward and down: a knee already deeper than the pose asks for
        changed.put(LandmarkName.LEFT_KNEE, new Landmark(knee.x() + .16, knee.y() - .10, knee.confidence()));

        var result = assertInstanceOf(AnalysisResult.Reliable.class,
                new PoseAnalyzer().analyze(pose, new LandmarkFrame(Instant.now(), changed)));
        assertFalse(result.suggestions().isEmpty(), "a knee past the range should still be cued");
        assertTrue(result.suggestions().getFirst().contains("deeper than this pose needs"),
                "a body past the range must not be told to go further: " + result.suggestions().getFirst());
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
