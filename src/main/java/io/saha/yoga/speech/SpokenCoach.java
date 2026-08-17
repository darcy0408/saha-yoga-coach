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

    /** Long enough that encouragement stays encouragement. */
    static final long PRAISE_GAP_NANOS = 25_000_000_000L;

    /**
     * Quiet time between the setup steps for a pose.
     *
     * Shorter than the cue gap on purpose: these are said while someone is
     * still moving into the shape, and a step that arrives after they have
     * settled is a step they cannot use.
     */
    static final long SETUP_GAP_NANOS = 5_000_000_000L;
    private static final String[] PRAISE = {
            "Great — that is the shape.",
            "Lovely. Hold it there.",
            "That is it. Keep breathing.",
            "Nice and steady."
    };

    private final LongSupplier clock;
    private int praiseIndex;
    private String lastLine;
    // a flag rather than a sentinel timestamp: nanoTime may be negative, and
    // subtracting a sentinel from it overflows and suppresses the first cue
    private boolean hasSpoken;
    private boolean nagged;
    private long lastAt;
    private long lastFramingAt;
    private String setupPose;
    private int setupStep;

    public SpokenCoach() { this(System::nanoTime); }

    SpokenCoach(LongSupplier clock) { this.clock = clock; }

    /** A pose change is always announced, with its first instruction. */
    public Optional<String> announce(Pose pose) {
        setupPose = pose.id();
        setupStep = 1;
        return emit(pose.displayName() + ". " + pose.instructions().getFirst());
    }

    /**
     * The next practical step for getting into the pose.
     *
     * A coach that only speaks to correct you leaves you to work out how to
     * arrive in the shape on your own, which is the part a beginner most needs
     * said out loud - and the part they cannot read off a screen from inside a
     * forward fold. These are setup steps, not corrections: they say where to
     * put a foot, and claim nothing about what the camera measured.
     *
     * They are handed out one at a time so the voice stays a coach rather than
     * a recitation, and they stop as soon as the pose runs out of steps.
     */
    public Optional<String> setup(Pose pose) {
        if (!pose.id().equals(setupPose)) { setupPose = pose.id(); setupStep = 1; }
        if (setupStep >= pose.instructions().size()) return Optional.empty();
        if (hasSpoken && clock.getAsLong() - lastAt < SETUP_GAP_NANOS) return Optional.empty();
        return emit(pose.instructions().get(setupStep++));
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

    /**
     * Occasional praise while a measured pose is holding inside its range.
     *
     * A coach that only ever speaks to correct you is one you stop wanting to
     * hear. This waits out a long gap so it lands as encouragement rather than
     * chatter, and it cycles the wording so the same word is not repeated back
     * at you all practice.
     */
    public Optional<String> praise() {
        if (hasSpoken && clock.getAsLong() - lastAt < PRAISE_GAP_NANOS) return Optional.empty();
        var line = PRAISE[praiseIndex++ % PRAISE.length];
        return emit(line);
    }

    /** Said once at the midpoint of a pose that should be done on both sides. */
    public Optional<String> switchSides() {
        return emit("Switch sides.");
    }

    /** Said once when the practice ends. Finishing twenty poses deserves to be marked. */
    public Optional<String> finish(boolean completed) {
        return emit(completed
                ? "That is the whole practice. Well done — twenty poses, start to finish. Rest here as long as you like."
                : "Practice stopped. Come back whenever you are ready.");
    }

    private Optional<String> emit(String line) {
        lastLine = line;
        lastAt = clock.getAsLong();
        hasSpoken = true;
        return Optional.of(line);
    }
}
