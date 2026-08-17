package io.saha.yoga.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

/**
 * A short, warm two-note chime for the moment a pose comes into range.
 *
 * <p>The tone is synthesised rather than loaded from a file, so there is no
 * audio asset to license, credit, or ship — the same reason the pose figures
 * are drawn from geometry. It is a pair of partials with an exponential decay,
 * which is what makes a struck bell sound struck rather than switched on.
 *
 * <p>Playback happens on a virtual thread and failures are swallowed: a machine
 * with no working audio device should lose the ding, not the practice.
 */
public final class Chime {
    private static final float SAMPLE_RATE = 44_100;
    /** A rising major third: the interval reads as "good" almost universally. */
    private static final double LOW_HZ = 880, HIGH_HZ = 1108.73;
    private static final double SECONDS = .22;

    private volatile boolean muted;

    public void setMuted(boolean value) { muted = value; }

    public boolean isMuted() { return muted; }

    public void play() {
        if (muted) return;
        Thread.ofVirtual().name("saha-chime").start(() -> {
            try {
                var format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
                try (SourceDataLine line = AudioSystem.getSourceDataLine(format)) {
                    line.open(format);
                    line.start();
                    byte[] tone = render();
                    line.write(tone, 0, tone.length);
                    line.drain();
                }
            } catch (Exception ignored) {
                // no audio device, or it is busy; the practice continues silently
            }
        });
    }

    /** Signed 16-bit little-endian samples of the chime. Package-private so it can be measured. */
    static byte[] render() {
        int count = (int) (SAMPLE_RATE * SECONDS);
        var out = new byte[count * 2];
        for (int i = 0; i < count; i++) {
            double t = i / (double) SAMPLE_RATE;
            // the second note enters a beat after the first, so it reads as
            // two notes rather than one chord
            double first = Math.sin(2 * Math.PI * LOW_HZ * t);
            double second = t < SECONDS * .35 ? 0 : Math.sin(2 * Math.PI * HIGH_HZ * (t - SECONDS * .35));
            double decay = Math.exp(-4.2 * t / SECONDS);
            // a short fade-in removes the click a hard start would make
            double attack = Math.min(1, t / .004);
            double value = (first * .6 + second * .55) * decay * attack * .32;
            short sample = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, value * Short.MAX_VALUE));
            out[i * 2] = (byte) (sample & 0xFF);
            out[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
        }
        return out;
    }
}
