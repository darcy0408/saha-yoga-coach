package io.saha.yoga.speech;

import io.saha.yoga.domain.Pose;

import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Decides what is worth saying out loud, and when.
 *
 * <p>Reading a screen is not possible from inside most of these poses, which is
 * the whole reason for speaking — but a voice that repeats itself every tenth
 * of a second is worse than silence. Two rules keep it calm: a line is never
 * repeated back to back, and ordinary cues wait for a gap so they land as
 * occasional guidance rather than a running commentary.
 *
 * <p>A new pose always interrupts that gap. It is the one thing a practitioner
 * cannot infer from the room, and hearing it late means moving late.
 */
public final class SpokenCoach {
    /** Quiet time between ordinary cues. */
    static final long CUE_GAP_NANOS = 7_000_000_000L;
    /**
     * Hard floor between two framing warnings.
     *
     * Confidence sitting near the gate flickers between states, and each
     * recovery re-arms the warning, which is how "your hips are out of view"
     * ended up being said over and over at someone who could not do anything
     * about it. The flag alone was not enough; this makes rapid repetition
     * impossible however the states bounce.
     */
    static final long FRAMING_GAP_NANOS = 45_000_000_000L;

    private final LongSupplier clock;
    private String lastLine;
    // a flag rather than a sentinel timestamp: nanoTime may be negative, and
    // subtracting a sentinel from it overflows and suppresses the first cue
    private boolean hasSpoken;
    private boolean nagged;
    private long lastAt;
    private long lastFramingAt;

    public SpokenCoach() { this(System::nanoTime); }

    SpokenCoach(LongSupplier clock) { this.clock = clock; }

    /** A pose change is always announced, with its first instruction. */
    public Optional<String> announce(Pose pose) {
        return emit(pose.displayName() + ". " + pose.instructions().getFirst());
    }

    /** An alignment or framing cue, spoken only if it is new and the voice has been quiet. */
    public Optional<String> cue(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        if (text.equals(lastLine)) return Optional.empty();
        if (hasSpoken && clock.getAsLong() - lastAt < CUE_GAP_NANOS) return Optional.empty();
        return emit(text);
    }

    /**
     * A framing problem, said once and then left alone.
     *
     * Repeating "your shoulders are out of view" every few seconds at someone
     * who is trying to fix exactly that is nagging, not coaching. Call
     * {@link #framingResolved()} when the view recovers to arm it again.
     */
    public Optional<String> framing(String text) {
        if (nagged) return Optional.empty();
        if (hasSpoken && clock.getAsLong() - lastFramingAt < FRAMING_GAP_NANOS) return Optional.empty();
        nagged = true;
        if (text == null || text.isBlank()) return Optional.empty();
        lastFramingAt = clock.getAsLong();
        return emit(text);
    }

    /** Re-arms the framing warning once the camera can see enough again. */
    public void framingResolved() { nagged = false; }

    /** Said once when the practice ends. */
    public Optional<String> finish(boolean completed) {
        return emit(completed ? "Practice complete. Take a moment before you get up." : "Practice stopped.");
    }

    private Optional<String> emit(String line) {
        lastLine = line;
        lastAt = clock.getAsLong();
        hasSpoken = true;
        return Optional.of(line);
    }
}
