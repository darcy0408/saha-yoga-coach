package io.saha.yoga.vision;

import io.saha.yoga.domain.Landmark;
import io.saha.yoga.domain.LandmarkName;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The crop geometry, with no model and no camera in the way.
 *
 * <p>Every body here is written in the estimator's output space - both axes
 * divided by the frame width - because that is the space the estimator hands
 * over. On a 640x480 frame that puts the floor at 0.75, not at 1.
 */
class PersonCropTest {
    private static final int WIDTH = 640, HEIGHT = 480;

    /** A body standing upright, centred, filling most of the frame's height. */
    private static Map<LandmarkName, Landmark> standing(double confidence) {
        var points = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        points.put(LandmarkName.NOSE, new Landmark(.50, .10, .9));
        points.put(LandmarkName.LEFT_SHOULDER, new Landmark(.44, .20, .9));
        points.put(LandmarkName.RIGHT_SHOULDER, new Landmark(.56, .20, .9));
        points.put(LandmarkName.LEFT_HIP, new Landmark(.46, .40, .9));
        points.put(LandmarkName.RIGHT_HIP, new Landmark(.54, .40, .9));
        points.put(LandmarkName.LEFT_KNEE, new Landmark(.46, .56, confidence));
        points.put(LandmarkName.RIGHT_KNEE, new Landmark(.54, .56, confidence));
        points.put(LandmarkName.LEFT_ANKLE, new Landmark(.46, .70, confidence));
        points.put(LandmarkName.RIGHT_ANKLE, new Landmark(.54, .70, confidence));
        return points;
    }

    /** Where a region's edges fall, in the same width-normalized space as the landmarks. */
    private static double[] edges(PersonCrop.Region region) {
        return new double[]{region.x() / WIDTH, region.y() / WIDTH,
                (region.x() + region.size()) / WIDTH, (region.y() + region.size()) / WIDTH};
    }

    private static void assertContains(PersonCrop.Region region, Map<LandmarkName, Landmark> body, String what) {
        var edge = edges(region);
        body.forEach((name, mark) -> {
            assertTrue(mark.x() >= edge[0] && mark.x() <= edge[2],
                    what + ": " + name + " at x=" + mark.x() + " is outside " + edge[0] + ".." + edge[2]);
            assertTrue(mark.y() >= edge[1] && mark.y() <= edge[3],
                    what + ": " + name + " at y=" + mark.y() + " is outside " + edge[1] + ".." + edge[3]);
        });
    }

    @Test void beforeAnyBodyIsFoundTheModelSeesTheWholeFrame() {
        var region = new PersonCrop().regionFor(WIDTH, HEIGHT);
        // The letterbox the estimator used for every frame before the crop
        // existed: the smallest square holding a 640x480 frame, centred on it.
        assertEquals(0, region.x(), 1e-9);
        assertEquals(-80, region.y(), 1e-9);
        assertEquals(640, region.size(), 1e-9);
    }

    @Test void aBodyIsGivenMoreOfTheModelsPixelsThanTheWholeFrameWould() {
        var crop = new PersonCrop();
        crop.observe(standing(.9), WIDTH, HEIGHT);
        var region = crop.regionFor(WIDTH, HEIGHT);
        // The point of the whole exercise: the square the model is shown has to
        // be materially smaller than the frame, or the body still lands on the
        // same few pixels it always did.
        assertTrue(region.size() < 640 * .85,
                "region is " + region.size() + "px of a 640px frame, which buys the body almost nothing");
        assertContains(region, standing(.9), "a standing body");
    }

    /**
     * The failure this class exists to prevent.
     *
     * <p>A body sitting cross-legged scores its legs below the gate - that is
     * the original bug. If the region were measured from the joints that were
     * confidently seen, it would close around the torso, the legs would fall
     * outside the patch the model is shown, and the next region would be
     * measured from a body with no legs at all. They would never come back.
     */
    @Test void legsTooFaintToBeMeasuredAreStillInsideTheRegion() {
        var crop = new PersonCrop();
        var faintLegs = standing(.12);
        crop.observe(faintLegs, WIDTH, HEIGHT);
        assertContains(crop.regionFor(WIDTH, HEIGHT), faintLegs, "a body whose legs scored 0.12");
    }

    @Test void aBodySeenOnlyFromTheTorsoStillGetsARegionAWholeBodyFitsIn() {
        var crop = new PersonCrop();
        var torsoOnly = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        standing(.9).forEach((name, mark) -> torsoOnly.put(name,
                name.name().endsWith("SHOULDER") || name.name().endsWith("HIP") ? mark
                        : new Landmark(mark.x(), mark.y(), .05)));
        crop.observe(torsoOnly, WIDTH, HEIGHT);
        var region = crop.regionFor(WIDTH, HEIGHT);
        // Shoulders at 0.20 and hips at 0.40 span a fifth of the frame width.
        // A whole standing body is about three times that, and the region has
        // to hold one on the strength of the torso alone.
        assertTrue(region.size() / WIDTH > .55,
                "a region of " + region.size() / WIDTH + " frame widths cannot hold the body under that torso");
        assertContains(region, standing(.9), "the body under a torso-only reading");
    }

    @Test void losingTheBodySnapsBackToTheWholeFrame() {
        var crop = new PersonCrop();
        crop.observe(standing(.9), WIDTH, HEIGHT);
        assertNotEquals(640, crop.regionFor(WIDTH, HEIGHT).size(), "precondition: the crop was tracking");

        var gone = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        standing(.9).forEach((name, mark) -> gone.put(name, new Landmark(mark.x(), mark.y(), .05)));
        crop.observe(gone, WIDTH, HEIGHT);
        // Immediately, not eased: easing would spend those frames looking hard
        // at a patch the person has already left.
        assertEquals(640, crop.regionFor(WIDTH, HEIGHT).size(), 1e-9);
    }

    @Test void halfATorsoIsNotEnoughToTrackFrom() {
        var crop = new PersonCrop();
        var shouldersOnly = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        shouldersOnly.put(LandmarkName.LEFT_SHOULDER, new Landmark(.44, .20, .9));
        shouldersOnly.put(LandmarkName.RIGHT_SHOULDER, new Landmark(.56, .20, .9));
        crop.observe(shouldersOnly, WIDTH, HEIGHT);
        // Shoulders alone say nothing about which way up the body is or how far
        // down it goes, so there is nothing to size a region from.
        assertEquals(640, crop.regionFor(WIDTH, HEIGHT).size(), 1e-9);
    }

    @Test void aBodyFillingTheFrameKeepsTheWholeFrame() {
        var crop = new PersonCrop();
        var large = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        large.put(LandmarkName.LEFT_SHOULDER, new Landmark(.20, .05, .9));
        large.put(LandmarkName.RIGHT_SHOULDER, new Landmark(.80, .05, .9));
        large.put(LandmarkName.LEFT_HIP, new Landmark(.25, .45, .9));
        large.put(LandmarkName.RIGHT_HIP, new Landmark(.75, .45, .9));
        crop.observe(large, WIDTH, HEIGHT);
        // Cropping would only throw away picture, so the fallback stands.
        assertEquals(640, crop.regionFor(WIDTH, HEIGHT).size(), 1e-9);
    }

    @Test void theRegionEasesTowardsTheBodyRatherThanJumping() {
        var crop = new PersonCrop();
        var left = standing(.9);
        crop.observe(left, WIDTH, HEIGHT);
        double before = crop.regionFor(WIDTH, HEIGHT).x();

        var moved = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        left.forEach((name, mark) -> moved.put(name, new Landmark(mark.x() + .20, mark.y(), mark.confidence())));
        crop.observe(moved, WIDTH, HEIGHT);
        double after = crop.regionFor(WIDTH, HEIGHT).x();

        double step = after - before;
        double jump = .20 * WIDTH;
        assertTrue(step > 0, "the region did not follow the body at all");
        assertTrue(step < jump * .9,
                "the region moved " + step + "px of the body's " + jump + "px, which is a jump rather than a follow");
    }

    @Test void aRegionSizedFromNoiseCannotMagnifyIt() {
        var crop = new PersonCrop();
        var speck = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        speck.put(LandmarkName.LEFT_SHOULDER, new Landmark(.500, .400, .9));
        speck.put(LandmarkName.RIGHT_SHOULDER, new Landmark(.502, .400, .9));
        speck.put(LandmarkName.LEFT_HIP, new Landmark(.500, .403, .9));
        speck.put(LandmarkName.RIGHT_HIP, new Landmark(.502, .403, .9));
        crop.observe(speck, WIDTH, HEIGHT);
        // A "torso" two pixels across would otherwise be blown up to fill the
        // model's whole input, and the frame after that is inference on noise.
        assertTrue(crop.regionFor(WIDTH, HEIGHT).size() >= 640 / 8.0,
                "a four-pixel detection sized the region down to " + crop.regionFor(WIDTH, HEIGHT).size() + "px");
    }

    @Test void aBodyAtTheEdgeGetsARegionThatHangsOffIt() {
        var crop = new PersonCrop();
        var edge = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        standing(.9).forEach((name, mark) -> edge.put(name, new Landmark(mark.x() - .42, mark.y(), mark.confidence())));
        crop.observe(edge, WIDTH, HEIGHT);
        var region = crop.regionFor(WIDTH, HEIGHT);
        // Sliding the region inward to keep it on the frame would re-centre the
        // body inside the patch and quietly move every joint with it.
        assertTrue(region.x() < 0, "region starts at x=" + region.x() + ", so it was pushed back onto the frame");
        assertContains(region, edge, "a body at the left edge");
    }
}
