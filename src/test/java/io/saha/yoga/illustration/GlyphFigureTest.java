package io.saha.yoga.illustration;

import io.saha.yoga.domain.Landmark;
import io.saha.yoga.domain.LandmarkFrame;
import io.saha.yoga.domain.LandmarkName;
import io.saha.yoga.routine.PoseCatalog;
import io.saha.yoga.vision.DemoLandmarkSource;
import io.saha.yoga.vision.LandmarkSource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.*;

class GlyphFigureTest {
    @Test void everyCatalogPoseProducesAFullFigure() {
        var source = new DemoLandmarkSource();
        for (var pose : new PoseCatalog().all()) {
            source.selectPose(pose.id());
            var figure = GlyphFigure.of(source.targetFrame());
            assertEquals(5, figure.strokes().size(), pose.id() + " strokes");
            assertTrue(figure.head().isPresent(), pose.id() + " head");
            for (var stroke : figure.strokes()) {
                int expected = stroke.limb() == GlyphFigure.Limb.SPINE ? 2 : 4;
                assertEquals(expected, stroke.points().size(), pose.id() + " " + stroke.limb());
                for (var point : stroke.points())
                    assertTrue(point.y() <= LandmarkSource.FLOOR_Y + 1e-9,
                            pose.id() + " " + stroke.limb() + " sank through the floor");
            }
        }
    }

    @Test void spineRunsBetweenShoulderAndHipMidpoints() {
        var source = new DemoLandmarkSource();
        source.selectPose("mountain");
        var landmarks = source.targetFrame().landmarks();
        var spine = strokeOf(GlyphFigure.of(source.targetFrame()), GlyphFigure.Limb.SPINE);
        assertEquals(mid(landmarks.get(LandmarkName.LEFT_SHOULDER).x(), landmarks.get(LandmarkName.RIGHT_SHOULDER).x()), spine.points().getFirst().x(), 1e-9, "neck x");
        assertEquals(mid(landmarks.get(LandmarkName.LEFT_SHOULDER).y(), landmarks.get(LandmarkName.RIGHT_SHOULDER).y()), spine.points().getFirst().y(), 1e-9, "neck y");
        assertEquals(mid(landmarks.get(LandmarkName.LEFT_HIP).x(), landmarks.get(LandmarkName.RIGHT_HIP).x()), spine.points().getLast().x(), 1e-9, "pelvis x");
        assertEquals(mid(landmarks.get(LandmarkName.LEFT_HIP).y(), landmarks.get(LandmarkName.RIGHT_HIP).y()), spine.points().getLast().y(), 1e-9, "pelvis y");
    }

    @Test void headCirclesTheNose() {
        var source = new DemoLandmarkSource();
        source.selectPose("tree");
        var nose = source.targetFrame().landmarks().get(LandmarkName.NOSE);
        var head = GlyphFigure.of(source.targetFrame()).head().orElseThrow();
        assertEquals(nose.x(), head.x(), 1e-9);
        assertEquals(nose.y(), head.y(), 1e-9);
    }

    @Test void missingLandmarksDropStrokesInsteadOfFailing() {
        var p = new EnumMap<LandmarkName, Landmark>(LandmarkName.class);
        p.put(LandmarkName.LEFT_SHOULDER, new Landmark(.4, .2, .9));
        p.put(LandmarkName.RIGHT_SHOULDER, new Landmark(.6, .2, .9));
        p.put(LandmarkName.LEFT_HIP, new Landmark(.45, .5, .9));
        p.put(LandmarkName.RIGHT_HIP, new Landmark(.55, .5, .9));
        p.put(LandmarkName.LEFT_ELBOW, new Landmark(.35, .35, .9));
        var figure = GlyphFigure.of(new LandmarkFrame(Instant.now(), p));
        assertTrue(figure.head().isEmpty(), "no nose, no head");
        assertEquals(2, figure.strokes().size(), "spine plus the reachable arm prefix");
        var arm = strokeOf(figure, GlyphFigure.Limb.LEFT_ARM);
        assertEquals(2, arm.points().size(), "neck to elbow only");
    }

    private GlyphFigure.Stroke strokeOf(GlyphFigure figure, GlyphFigure.Limb limb) {
        return figure.strokes().stream().filter(stroke -> stroke.limb() == limb).findFirst().orElseThrow();
    }

    private double mid(double a, double b) { return (a + b) / 2; }
}
