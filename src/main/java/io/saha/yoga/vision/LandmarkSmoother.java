package io.saha.yoga.vision;

import io.saha.yoga.domain.Landmark;
import io.saha.yoga.domain.LandmarkFrame;
import io.saha.yoga.domain.LandmarkName;

import java.time.Instant;
import java.util.EnumMap;

/**
 * Steadies a stream of estimated landmarks.
 *
 * <p>Per-joint confidence from a single-frame model wobbles, and a joint whose
 * score crosses the drawing threshold every other frame makes the limb flicker
 * in and out. Watching your own body strobe is unnerving in a way that has
 * nothing to do with the pose, and the same jitter reaches the angles, so cues
 * can appear and vanish for no reason a person could act on.
 *
 * <p>Two exponential moving averages fix both: one over position, which removes
 * the shimmer, and a slower one over confidence, which keeps a joint present
 * through a brief dip and still lets it fade out when the body genuinely leaves.
 * A landmark the model omits entirely holds its last position while its
 * confidence decays, so a momentary dropout bridges instead of blinking.
 *
 * <p>Not thread-safe: it is fed from the single capture thread.
 */
public final class LandmarkSmoother {
    /** Position follows quickly; the body moves and the figure should keep up. */
    private static final double POSITION_WEIGHT = .55;
    /**
     * The confidence at which a position is followed at full speed.
     *
     * Mirrors the analyzer's reliability gate, kept as a local number rather
     * than a dependency from vision on analysis. A joint the model is at least
     * this sure of is an observation and the figure should keep up with it.
     *
     * <p>Below it, position is followed proportionally more slowly, because a
     * low score does not mean the joint is roughly right - it means the
     * coordinates are largely invented, and they thrash from frame to frame.
     * Following that at full weight is what made a hidden knee crawl around the
     * screen. Damping it in proportion to how little the model knows leaves the
     * joint parked near the last place it was actually seen, which for a body
     * holding still is where it still is.
     */
    private static final double TRUSTED_CONFIDENCE = .35;
    /** Confidence follows slowly, which is what stops the flicker. */
    private static final double CONFIDENCE_WEIGHT = .25;
    /** How fast a landmark the model stopped reporting fades away. */
    private static final double DECAY = .6;

    private final EnumMap<LandmarkName, Landmark> smoothed = new EnumMap<>(LandmarkName.class);

    public LandmarkFrame smooth(LandmarkFrame incoming) {
        for (var name : LandmarkName.values()) {
            var fresh = incoming.landmarks().get(name);
            var previous = smoothed.get(name);
            if (fresh == null) {
                // hold the last place we saw it and let the score fall away, so
                // one missed frame does not erase a limb
                if (previous != null) smoothed.put(name, new Landmark(previous.x(), previous.y(), previous.confidence() * DECAY));
                continue;
            }
            if (previous == null) {
                smoothed.put(name, fresh);
                continue;
            }
            // how much of this reading to believe: full weight for a joint in
            // plain view, proportionally less for one the model is guessing at
            double positionWeight = POSITION_WEIGHT * Math.min(1, fresh.confidence() / TRUSTED_CONFIDENCE);
            smoothed.put(name, new Landmark(
                    blend(fresh.x(), previous.x(), positionWeight),
                    blend(fresh.y(), previous.y(), positionWeight),
                    blend(fresh.confidence(), previous.confidence(), CONFIDENCE_WEIGHT)));
        }
        return new LandmarkFrame(incoming.capturedAt() == null ? Instant.now() : incoming.capturedAt(), smoothed);
    }

    private static double blend(double fresh, double previous, double weight) {
        return fresh * weight + previous * (1 - weight);
    }
}
