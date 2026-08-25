package io.saha.yoga.vision;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The picture half of the crop: what the model is actually shown, and whether
 * the answer can be mapped back to where it came from.
 *
 * <p>These need OpenCV but not the weights, so they run on a fresh clone that
 * has never fetched a model. A mark is painted at a known place in a synthetic
 * frame and then looked for in the rendered square, which is the only way to
 * catch an off-by-one in the transform that a coordinate assertion alone would
 * agree with.
 */
class CropTransformTest {
    private static final int SIDE = 192;
    private static final int WIDTH = 640, HEIGHT = 480;

    @BeforeAll static void loadOpenCv() { nu.pattern.OpenCV.loadLocally(); }

    /** A dark frame with one white square centred on {@code (x, y)}. */
    private static Mat frameMarkedAt(double x, double y, int size) {
        var frame = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3, new Scalar(0, 0, 0));
        Imgproc.rectangle(frame, new Point(x - size / 2.0, y - size / 2.0),
                new Point(x + size / 2.0, y + size / 2.0), new Scalar(255, 255, 255), -1);
        return frame;
    }

    /** Where the white pixels sit in the rendered square, as {@code {minX, minY, maxX, maxY}}. */
    private static int[] markInCanvas(byte[] pixels) {
        int minX = SIDE, minY = SIDE, maxX = -1, maxY = -1;
        for (int row = 0; row < SIDE; row++) {
            for (int column = 0; column < SIDE; column++) {
                // Rendered pixels are RGB; the mark is the only bright thing.
                if ((pixels[(row * SIDE + column) * 3] & 0xFF) < 200) continue;
                minX = Math.min(minX, column);
                minY = Math.min(minY, row);
                maxX = Math.max(maxX, column);
                maxY = Math.max(maxY, row);
            }
        }
        assertTrue(maxX >= 0, "the mark does not appear in the rendered square at all");
        return new int[]{minX, minY, maxX, maxY};
    }

    private static byte[] render(Mat frame, PersonCrop.Region region) {
        try {
            return PoseEstimator.render(frame, region, SIDE);
        } finally {
            frame.release();
        }
    }

    @Test void theWholeFrameStillLandsWhereTheOldLetterboxPutIt() {
        // 640x480 scaled by 192/640 = 0.3 leaves a 144-pixel band with 24 rows
        // of padding above and below, and the centre of the frame at the centre
        // of the square. This is the transform that shipped before the crop;
        // the fallback path has to reproduce it exactly.
        var mark = markInCanvas(render(frameMarkedAt(320, 240, 20), PersonCrop.Region.wholeFrame(WIDTH, HEIGHT)));
        assertEquals(96, (mark[0] + mark[2]) / 2.0, 1, "centre of the frame is not at the centre of the square");
        assertEquals(96, (mark[1] + mark[3]) / 2.0, 1, "centre of the frame is not at the centre of the square");
        assertEquals(20 * .3, mark[2] - mark[0], 2, "the mark was not scaled by 192/640");
    }

    @Test void theTopOfTheFrameSitsBelowThePadding() {
        var mark = markInCanvas(render(frameMarkedAt(320, 4, 8), PersonCrop.Region.wholeFrame(WIDTH, HEIGHT)));
        // A 4:3 frame in a square leaves (192 - 144) / 2 = 24 rows of padding.
        assertEquals(24, mark[1], 2, "the picture does not start where the letterbox padding ends");
    }

    @Test void aCroppedRegionFillsTheSquareWithJustThatRegion() {
        // A 160-pixel region scaled to 192 magnifies by 1.2, and no padding is
        // left over because the region is square and inside the frame.
        var region = new PersonCrop.Region(240, 160, 160);
        var mark = markInCanvas(render(frameMarkedAt(320, 240, 20), region));
        assertEquals((320 - 240) * 1.2, (mark[0] + mark[2]) / 2.0, 1.5, "the mark is not where the region puts it");
        assertEquals((240 - 160) * 1.2, (mark[1] + mark[3]) / 2.0, 1.5, "the mark is not where the region puts it");
        assertEquals(20 * 1.2, mark[2] - mark[0], 2, "the region did not magnify the mark");
    }

    /**
     * The whole point of the exercise, measured rather than asserted.
     *
     * <p>A seated body about 200 pixels across is the case that was scoring too
     * low to draw. Under the letterbox it reached the model at 0.3 scale; under
     * a region drawn around it, it arrives near full size.
     */
    @Test void croppingGivesABodyFarMoreOfTheModelsPixels() {
        int body = 200;
        var letterboxed = markInCanvas(render(frameMarkedAt(320, 240, body), PersonCrop.Region.wholeFrame(WIDTH, HEIGHT)));
        var cropped = markInCanvas(render(frameMarkedAt(320, 240, body), new PersonCrop.Region(320 - 130, 240 - 130, 260)));
        int under = letterboxed[2] - letterboxed[0];
        int over = cropped[2] - cropped[0];
        assertEquals(60, under, 3, "precondition: the letterbox should give a 200px body about 60 of the model's pixels");
        assertTrue(over > under * 2,
                "cropping took the body from " + under + " to " + over + " pixels, which is not worth doing");
    }

    /**
     * The two halves joined up: the crop decides, the transform obeys.
     *
     * <p>Both are checked apart from each other above. This drives the region
     * from the joints a real seated body would report - legs too faint to be
     * measured - and then looks at how much of the model's square that body
     * ends up occupying, which is the number the whole change is about.
     */
    @Test void aBodyTheCropChoseForItselfArrivesLargeAndWhole() {
        var crop = new PersonCrop();
        var seated = new java.util.EnumMap<io.saha.yoga.domain.LandmarkName, io.saha.yoga.domain.Landmark>(
                io.saha.yoga.domain.LandmarkName.class);
        // A body about 200 pixels tall, sitting left of centre, with the legs
        // scoring below the gate exactly as a cross-legged body's do.
        seated.put(io.saha.yoga.domain.LandmarkName.LEFT_SHOULDER, new io.saha.yoga.domain.Landmark(.34, .28, .9));
        seated.put(io.saha.yoga.domain.LandmarkName.RIGHT_SHOULDER, new io.saha.yoga.domain.Landmark(.44, .28, .9));
        seated.put(io.saha.yoga.domain.LandmarkName.LEFT_HIP, new io.saha.yoga.domain.Landmark(.35, .44, .9));
        seated.put(io.saha.yoga.domain.LandmarkName.RIGHT_HIP, new io.saha.yoga.domain.Landmark(.43, .44, .9));
        seated.put(io.saha.yoga.domain.LandmarkName.LEFT_KNEE, new io.saha.yoga.domain.Landmark(.30, .52, .11));
        seated.put(io.saha.yoga.domain.LandmarkName.RIGHT_KNEE, new io.saha.yoga.domain.Landmark(.48, .52, .11));
        crop.observe(seated, WIDTH, HEIGHT);
        var region = crop.regionFor(WIDTH, HEIGHT);

        // Paint the body where those joints say it is: shoulders to knees.
        int centreX = (int) (.39 * WIDTH), top = (int) (.24 * WIDTH), bottom = (int) (.56 * WIDTH);
        var frame = new Mat(HEIGHT, WIDTH, CvType.CV_8UC3, new Scalar(0, 0, 0));
        Imgproc.rectangle(frame, new Point(centreX - 30, top), new Point(centreX + 30, bottom),
                new Scalar(255, 255, 255), -1);
        var cropped = markInCanvas(render(frame.clone(), region));
        var letterboxed = markInCanvas(render(frame, PersonCrop.Region.wholeFrame(WIDTH, HEIGHT)));

        int over = cropped[3] - cropped[1], under = letterboxed[3] - letterboxed[1];
        assertTrue(over > under * 1.6,
                "the body reaches the model at " + over + " pixels tall against " + under + " before, which is not the point");
        // Whole, not clipped: a region that cut the body off would be worse
        // than the letterbox, because the model would lose the part it cut.
        assertTrue(cropped[1] > 0 && cropped[3] < SIDE - 1,
                "the body was clipped by its own region, at rows " + cropped[1] + ".." + cropped[3]);
    }

    @Test void aRegionHangingOffTheEdgeIsPaddedRatherThanSlidBackOn() {
        // A body at the left edge needs a region whose left half is off-frame.
        // Sliding it back on would re-centre the body inside the square and
        // move every joint the model reports with it.
        var region = new PersonCrop.Region(-100, 140, 200);
        var pixels = render(frameMarkedAt(20, 240, 20), region);
        var mark = markInCanvas(pixels);
        double scale = SIDE / 200.0;
        assertEquals((20 - -100) * scale, (mark[0] + mark[2]) / 2.0, 2, "the mark moved when the region hung off the edge");
        // The off-frame half is padding, not picture.
        assertEquals(114, pixels[(96 * SIDE + 10) * 3] & 0xFF, 2, "the off-frame half should be padded grey");
    }

    /**
     * The inverse the estimator uses on every keypoint.
     *
     * <p>MoveNet answers in fractions of the square it was shown, and the
     * estimator turns that back into a place in the frame with the region's own
     * offset and span. If that inverse is wrong the skeleton is drawn somewhere
     * the body is not, which is the most expensive way for this to fail.
     */
    @Test void aPlaceInTheSquareMapsBackToTheFrameItCameFrom() {
        // Each region gets a mark that is actually inside it: the whole frame,
        // a region well within it, and one hanging off the left edge.
        var cases = new Object[][]{
                {PersonCrop.Region.wholeFrame(WIDTH, HEIGHT), 300.0, 250.0},
                {new PersonCrop.Region(240, 160, 160), 300.0, 250.0},
                {new PersonCrop.Region(-100, 140, 200), 40.0, 250.0}};
        for (var each : cases) {
            var region = (PersonCrop.Region) each[0];
            double sourceX = (double) each[1], sourceY = (double) each[2];
            var mark = markInCanvas(render(frameMarkedAt(sourceX, sourceY, 12), region));
            // Read the mark's position out of the square exactly as the model
            // reports one: as a fraction of the square's side.
            double fractionX = (mark[0] + mark[2]) / 2.0 / SIDE;
            double fractionY = (mark[1] + mark[3]) / 2.0 / SIDE;
            assertEquals(sourceX, region.x() + fractionX * region.size(), 2, "x did not survive the round trip through " + region);
            assertEquals(sourceY, region.y() + fractionY * region.size(), 2, "y did not survive the round trip through " + region);
        }
    }
}
