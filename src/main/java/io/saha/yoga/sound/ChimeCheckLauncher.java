package io.saha.yoga.sound;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;

/**
 * Plays the chime on demand, and says whether this machine can play it at all.
 *
 * The chime rings when a measured pose crosses into range, which is a moment
 * that needs a person in front of a camera holding a shape correctly. That
 * makes "I have never heard it" ambiguous: it could be a silent chime, a muted
 * one, a machine with no working audio line, or simply a practice that never
 * reached the state that rings it. This separates the first three from the last
 * without needing anyone to stand up.
 *
 * Chime.play() swallows audio failures on purpose - a machine with no sound
 * should lose the ding, not the practice - so the device check here is done in
 * the open rather than inferred from silence.
 */
public final class ChimeCheckLauncher {
    private ChimeCheckLauncher() { }

    public static void main(String[] args) throws InterruptedException {
        var format = new AudioFormat(44_100, 16, 1, true, false);
        boolean supported = AudioSystem.isLineSupported(new javax.sound.sampled.DataLine.Info(javax.sound.sampled.SourceDataLine.class, format));
        System.out.println("Audio output line for " + format + ": " + (supported ? "available" : "NOT AVAILABLE"));
        if (!supported) {
            System.out.println("The practice would run silently on this machine. Nothing is wrong with the chime.");
            return;
        }
        var chime = new Chime();
        System.out.println("Muted: " + chime.isMuted() + "  (practice mutes it only when spoken guidance is turned off)");
        System.out.println("Playing the chime twice: a short rising two-note ding, about a fifth of a second each.");
        chime.play();
        Thread.sleep(900);
        chime.play();
        Thread.sleep(900);
        System.out.println("Done. This is the sound a pose makes when it comes into range.");
    }
}
