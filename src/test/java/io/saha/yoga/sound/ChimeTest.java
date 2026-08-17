package io.saha.yoga.sound;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The chime is checked as samples, so no audio device is needed to test it. */
class ChimeTest {
    private short sampleAt(byte[] pcm, int index) {
        return (short) ((pcm[index * 2] & 0xFF) | (pcm[index * 2 + 1] << 8));
    }

    private int peakOver(byte[] pcm, int from, int to) {
        int peak = 0;
        for (int i = from; i < to; i++) peak = Math.max(peak, Math.abs(sampleAt(pcm, i)));
        return peak;
    }

    @Test void theToneIsTheExpectedLengthAndStaysInsideTheSampleRange() {
        var pcm = Chime.render();
        assertEquals((int) (44_100 * .22) * 2, pcm.length, "16-bit mono at 44.1 kHz");
        for (int i = 0; i < pcm.length / 2; i++) {
            int value = Math.abs(sampleAt(pcm, i));
            assertTrue(value <= Short.MAX_VALUE, "sample " + i + " clipped");
        }
    }

    @Test void itStartsFromSilenceSoThereIsNoClick() {
        var pcm = Chime.render();
        assertEquals(0, Math.abs(sampleAt(pcm, 0)), 1, "a hard start would click");
        assertTrue(peakOver(pcm, 0, 40) < peakOver(pcm, 400, 900), "the attack should ramp up");
    }

    @Test void itDecaysLikeSomethingStruck() {
        var pcm = Chime.render();
        int total = pcm.length / 2;
        int early = peakOver(pcm, total / 10, total / 5);
        int late = peakOver(pcm, total - total / 5, total);
        assertTrue(late * 3 < early, "should fade away, not stop dead: early " + early + ", late " + late);
    }

    @Test void mutingStopsItWithoutTouchingAnAudioDevice() {
        var chime = new Chime();
        assertFalse(chime.isMuted());
        chime.setMuted(true);
        assertTrue(chime.isMuted());
        assertDoesNotThrow(chime::play);
    }
}
