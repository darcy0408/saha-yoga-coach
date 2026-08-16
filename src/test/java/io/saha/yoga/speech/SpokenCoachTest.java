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

    @Test void blankCuesAreNeverSpoken() {
        now = SpokenCoach.CUE_GAP_NANOS * 10;
        assertTrue(coach.cue("   ").isEmpty());
        assertTrue(coach.cue(null).isEmpty());
    }
}
