package io.saha.yoga.vision;

import io.saha.yoga.domain.Landmark;
import io.saha.yoga.domain.LandmarkName;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Chooses which square patch of the camera frame the model actually looks at.
 *
 * <p>MoveNet takes a 192-pixel square. Letterboxing a whole 640x480 frame into
 * it leaves a standing body about 144 pixels tall and a raised wrist two or
 * three across, which is why wrists were lost overhead and why a seated body's
 * legs scored too low to draw. MoveNet's own reference pipeline never feeds it
 * a whole frame: it crops to the person found in the previous frame and sends
 * that patch instead. This is that missing step.
 *
 * <p>The region is deliberately far larger than the body it was measured from.
 * A crop tight enough to clip a limb costs the model that limb, and the next
 * region is then measured from a body with the limb already missing - a
 * collapse that ends with the crop shut around a torso and the legs gone for
 * good. The 1.9x torso and 1.2x body factors below are the reference
 * implementation's, and stopping that collapse is what they are for.
 *
 * <p>Not thread-safe; it holds the previous frame's region and is driven from
 * {@link PoseEstimator}'s single inference thread.
 */
final class PersonCrop {
    /**
     * The square patch, in source-frame pixels.
     *
     * <p>It may extend past the frame edges - a body standing at the left edge
     * needs a region whose left half is off-frame, and padding that half is
     * honest where sliding the region inward would silently re-centre the body.
     */
    record Region(double x, double y, double size) {
        /**
         * The letterbox: the smallest square holding the whole frame.
         *
         * <p>This is what the estimator did for every frame before the crop
         * existed, expressed as a region, so the fallback and the crop share
         * one transform rather than two that can drift apart.
         */
        static Region wholeFrame(int width, int height) {
            double side = Math.max(width, height);
            return new Region((width - side) / 2, (height - side) / 2, side);
        }

        /** Moves a fraction of the way toward {@code target}. */
        Region follow(Region target, double weight) {
            return new Region(x + (target.x - x) * weight,
                    y + (target.y - y) * weight,
                    size + (target.size - size) * weight);
        }
    }

    /**
     * Confidence at which a joint counts as actually seen: the same 0.30 at
     * which the interface draws a landmark solid and trusts it for geometry.
     * (The analyzer's reliability gate is a separate, stricter 0.35.)
     */
    private static final double SEEN = .30;
    /**
     * How much bigger than the torso the region has to be.
     *
     * <p>The torso is the part of a body the model reports confidently in
     * almost every pose, so it is the only measurement available when the limbs
     * are exactly what has been lost - and losing them is the case that matters.
     *
     * <p>The number is not arbitrary. Measured from the hips, a standing body
     * reaches about 1.75 shoulder-heights to the crown and about the same to
     * the toes, so a square of 1.9 shoulder-heights either way holds a whole
     * body given nothing but the shoulders and hips to go on.
     */
    private static final double TORSO_FACTOR = 1.9;
    /** Margin around the joints that were seen, once there are some. */
    private static final double BODY_FACTOR = 1.2;
    /**
     * How fast the region follows the body.
     *
     * <p>Mapping the answer back into frame coordinates cancels the region's
     * own movement, so a region that jumps does not drag the skeleton with it.
     * What it does do is re-frame the body between one inference and the next,
     * and the model answers a re-framed body slightly differently each time.
     * Easing absorbs that; the margins above absorb the lag it costs.
     */
    private static final double FOLLOW = .5;
    /**
     * The region may not shrink below an eighth of the frame.
     *
     * <p>One spurious detection could otherwise size a region from a few
     * pixels, and the frame after that would be inference on magnified noise.
     * Losing the torso already resets to the whole frame, so this only has to
     * cover the single frame before that happens.
     */
    private static final double SMALLEST_FRACTION = 1 / 8.0;

    /** The four joints bounding the torso, and the only ones sized from when limbs are lost. */
    private static final List<LandmarkName> TORSO = List.of(
            LandmarkName.LEFT_SHOULDER, LandmarkName.RIGHT_SHOULDER,
            LandmarkName.LEFT_HIP, LandmarkName.RIGHT_HIP);

    private Region current;

    /** The region to feed the next frame: the whole frame until a body has been found in one. */
    Region regionFor(int width, int height) {
        var region = current;
        return region != null ? region : Region.wholeFrame(width, height);
    }

    /**
     * Records where the body actually turned out to be, ready for the next frame.
     *
     * <p>{@code points} arrive in the estimator's output space - both axes
     * divided by the frame width - because that is what the estimator has in
     * hand at the point it calls this.
     */
    void observe(Map<LandmarkName, Landmark> points, int width, int height) {
        var measured = measure(points, width, height);
        // Losing the body snaps straight back to the whole frame rather than
        // easing there: easing would spend those frames looking hard at a patch
        // the person has already walked out of.
        current = measured == null ? null : current == null ? measured : current.follow(measured, FOLLOW);
    }

    /** The region this set of joints implies, or null to fall back to the whole frame. */
    private static Region measure(Map<LandmarkName, Landmark> points, int width, int height) {
        // The hips, not the middle of the torso.
        //
        // A square has to be centred on the middle of what it must contain, and
        // for a standing body that is the hips: head and feet are about equally
        // far from them. Centring on the middle of the torso instead sits a
        // third of the way up the body, so reaching the feet means reaching the
        // same distance above the head, and the region swells until it is the
        // whole frame again - which is exactly what it did.
        double centreX = 0, centreY = 0;
        int hips = 0;
        for (var name : List.of(LandmarkName.LEFT_HIP, LandmarkName.RIGHT_HIP)) {
            var mark = points.get(name);
            // Sizing from a joint the model is inventing would put the region
            // wherever the invention landed; only joints actually seen count.
            if (mark == null || mark.confidence() < SEEN) continue;
            centreX += mark.x() * width;
            centreY += mark.y() * width;
            hips++;
        }
        boolean shoulder = Stream.of(LandmarkName.LEFT_SHOULDER, LandmarkName.RIGHT_SHOULDER)
                .map(points::get)
                .anyMatch(mark -> mark != null && mark.confidence() >= SEEN);
        // A hip and a shoulder between them establish which way up the body is
        // and roughly how long it is. Either alone is a fragment that could be
        // sized from almost anything.
        if (hips == 0 || !shoulder) return null;
        centreX /= hips;
        centreY /= hips;

        double torso = 0, body = 0;
        for (var entry : points.entrySet()) {
            var mark = entry.getValue();
            if (mark.confidence() < SEEN) continue;
            double reach = Math.max(Math.abs(mark.x() * width - centreX), Math.abs(mark.y() * width - centreY));
            body = Math.max(body, reach);
            if (TORSO.contains(entry.getKey())) torso = Math.max(torso, reach);
        }

        double half = Math.max(torso * TORSO_FACTOR, body * BODY_FACTOR);
        // No point reaching further outside the frame than the frame itself
        // reaches: past that the region is buying padding rather than picture.
        half = Math.min(half, Math.max(Math.max(centreX, width - centreX), Math.max(centreY, height - centreY)));
        double frame = Math.max(width, height);
        // A region as big as the frame is the frame, and the fallback path
        // already does that without carrying a stale region forward.
        if (half * 2 >= frame) return null;
        half = Math.max(half, frame * SMALLEST_FRACTION / 2);
        return new Region(centreX - half, centreY - half, half * 2);
    }
}
