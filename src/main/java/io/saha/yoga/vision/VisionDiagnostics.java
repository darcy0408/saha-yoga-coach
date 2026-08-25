package io.saha.yoga.vision;

import io.saha.yoga.domain.LandmarkFrame;

/**
 * One frame's worth of numbers about how the vision pipeline is doing.
 *
 * <p>This exists for exactly one situation: a person standing in front of the
 * live camera, checking whether the crop and the capture size actually fixed
 * what they were built to fix. Those checks are questions about numbers -
 * what did this wrist really score, how big was the patch the model was shown,
 * how long did a frame take - and without this record the person watching has
 * to answer them by impression.
 *
 * <p>{@code raw} is the estimate as the model produced it, before
 * {@link LandmarkSmoother} steadies it. The smoothed frame is the right thing
 * to draw and the wrong thing to diagnose with: its confidences are blended
 * across frames, so reading them would measure the smoother, not the model.
 *
 * <p>The region is in the landmarks' own space - both axes divided by the
 * frame width - so drawing it over the preview uses the very same transform as
 * the skeleton. A region of size 1 is the whole frame: no body is being
 * tracked, or the one being tracked fills the picture.
 *
 * @param raw                the unsmoothed estimate for this frame
 * @param regionX            left edge of the patch the model was shown
 * @param regionY            top edge of the patch the model was shown
 * @param regionSize         side of the (square) patch the model was shown
 * @param estimateMillis     how long this frame's estimate took, conversion included
 * @param landmarksPerSecond how many estimates are completing per second, smoothed
 */
public record VisionDiagnostics(LandmarkFrame raw, double regionX, double regionY, double regionSize,
                                double estimateMillis, double landmarksPerSecond) {

    /** True when the model was shown the whole frame rather than a patch of it. */
    public boolean wholeFrame() { return regionSize >= .999; }
}
