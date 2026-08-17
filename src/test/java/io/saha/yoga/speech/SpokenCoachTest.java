package io.saha.yoga.speech;

import io.saha.yoga.routine.PoseCatalog;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpokenCoachTest {
    private long now;
    private final SpokenCoach coach = new SpokenCoach(() -> now);
    private final PoseCatalog catalog = new PoseCatalog();

    @Test void aPoseIsAnnouncedWithItsInstruction() {
        var spoken = coach.announce(catalog.require("tree")).orElseThrow();
        assertTrue(spoken.startsWith("Tree."), spoken);
        assertTrue(spoken.contains("Balance on one leg"), spoken);
    }

    @Test void theSameCueIsNotRepeated() {
        now = SpokenCoach.CUE_GAP_NANOS * 10;
        assertTrue(coach.cue("Soften your front knee.").isPresent());
        now += SpokenCoach.CUE_GAP_NANOS * 2;
        assertTrue(coach.cue("Soften your front knee.").isEmpty(), "identical cue should stay quiet");
    }

    @Test void cuesWaitForAGapSoTheVoiceIsNotConstant() {
        now = SpokenCoach.CUE_GAP_NANOS * 10;
        assertTrue(coach.cue("First cue.").isPresent());
        now += SpokenCoach.CUE_GAP_NANOS / 2;
        assertTrue(coach.cue("Second cue.").isEmpty(), "too soon after the last line");
        now += SpokenCoach.CUE_GAP_NANOS;
        assertTrue(coach.cue("Second cue.").isPresent(), "allowed once the gap has passed");
    }

    @Test void aNewPoseInterruptsTheGap() {
        now = SpokenCoach.CUE_GAP_NANOS * 10;
        assertTrue(coach.cue("Sink your hips.").isPresent());
        // moving late because the voice was still in its quiet period would be
        // worse than the interruption
        assertTrue(coach.announce(catalog.require("chair")).isPresent());
    }

    @Test void aFramingWarningIsNotRepeatedWhenConfidenceFlickers() {
        now = SpokenCoach.FRAMING_GAP_NANOS * 10;
        assertTrue(coach.framing("Your hips are out of view.").isPresent());
        // the view recovers for an instant and drops again, which is what the
        // gate does when confidence sits right on the threshold. Nothing more
        // is said for the whole quiet window, however many times it bounces.
        // twenty bounces spread over half the quiet window, so the loop cannot
        // walk past it and make this test about the wrong thing
        long step = SpokenCoach.FRAMING_GAP_NANOS / 40;
        for (int i = 0; i < 20; i++) {
            coach.framingResolved();
            now += step;
            assertTrue(coach.framing("Your hips are out of view.").isEmpty(), "repeat " + i + " should stay quiet");
        }
    }

    @Test void aFramingWarningCanBeSaidAgainMuchLater() {
        now = SpokenCoach.FRAMING_GAP_NANOS * 10;
        assertTrue(coach.framing("Your feet are out of view.").isPresent());
        coach.framingResolved();
        now += SpokenCoach.FRAMING_GAP_NANOS * 2;
        assertTrue(coach.framing("Your feet are out of view.").isPresent(), "a genuinely new episode should speak");
    }

    @Test void gettingIntoThePoseIsTalkedThroughAfterItIsAnnounced() {
        var warrior = catalog.require("warrior_one");
        now = SpokenCoach.SETUP_GAP_NANOS * 10;
        coach.announce(warrior);
        // straight after the announcement there is nothing more to add yet
        assertTrue(coach.setup(warrior).isEmpty(), "a step on top of the announcement is a recitation");
        now += SpokenCoach.SETUP_GAP_NANOS;
        var first = coach.setup(warrior).orElseThrow();
        now += SpokenCoach.SETUP_GAP_NANOS;
        var second = coach.setup(warrior).orElseThrow();
        assertNotEquals(first, second, "each step should move the practitioner on");
        assertEquals(warrior.instructions().get(1), first);
        assertEquals(warrior.instructions().get(2), second);
        // and then it stops, rather than looping the setup for the whole hold
        now += SpokenCoach.SETUP_GAP_NANOS * 10;
        assertTrue(coach.setup(warrior).isEmpty(), "a pose runs out of setup steps");
    }

    @Test void everyPoseCanTalkSomeoneIntoIt() {
        for (var pose : catalog.all()) {
            assertTrue(pose.instructions().size() >= 3,
                    pose.id() + " should carry its summary and at least two practical steps");
            pose.instructions().forEach(line ->
                    assertFalse(line.isBlank(), pose.id() + " has a blank instruction"));
        }
    }

    @Test void theStepsRestartWhenTheNextPoseArrives() {
        var chair = catalog.require("chair");
        var tree = catalog.require("tree");
        now = SpokenCoach.SETUP_GAP_NANOS * 10;
        coach.announce(chair);
        now += SpokenCoach.SETUP_GAP_NANOS;
        assertEquals(chair.instructions().get(1), coach.setup(chair).orElseThrow());
        coach.announce(tree);
        now += SpokenCoach.SETUP_GAP_NANOS;
        assertEquals(tree.instructions().get(1), coach.setup(tree).orElseThrow(),
                "the new pose starts from its own first step");
    }

    @Test void blankCuesAreNeverSpoken() {
        now = SpokenCoach.CUE_GAP_NANOS * 10;
        assertTrue(coach.cue("   ").isEmpty());
        assertTrue(coach.cue(null).isEmpty());
    }
}
