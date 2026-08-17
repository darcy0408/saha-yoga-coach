package io.saha.yoga.illustration;

import io.saha.yoga.routine.PoseCatalog;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PoseIconCatalogTest {
    private final PoseIconCatalog catalog = new PoseIconCatalog();

    @Test void everyIconCarriesDrawableGeometry() {
        assertFalse(catalog.iconNames().isEmpty());
        for (var name : catalog.iconNames()) {
            var icon = catalog.forIcon(name).orElseThrow();
            assertFalse(icon.paths().isEmpty(), name + " has no stroke paths");
            // absolute or relative move, both of which JavaFX's SVGPath accepts
            for (var path : icon.paths()) assertTrue(path.startsWith("M") || path.startsWith("m"), name + " path should start with a move command");
            for (var head : icon.circles()) assertTrue(head.radius() > 0, name + " head radius");
        }
    }

    @Test void mappedPosesBelongToTheRoutineCatalog() {
        var known = new PoseCatalog().all().stream().map(pose -> pose.id()).collect(java.util.stream.Collectors.toSet());
        for (var pose : known) catalog.forPose(pose).ifPresent(icon -> assertFalse(icon.paths().isEmpty(), pose));
        assertTrue(catalog.forPose("chair").isPresent());
        assertTrue(catalog.forPose("tree").isPresent());
        assertTrue(catalog.forPose("rest").isPresent());
    }

    @Test void posesWithoutAnHonestMatchStayUnillustrated() {
        // the pack has no bent-knee wide stance, so Warrior II must not be
        // silently mapped to its straight-legged near-miss
        assertTrue(catalog.forPose("warrior_two").isEmpty(), "warrior_two should have no icon");
    }

    @Test void unknownPoseHasNoIcon() {
        assertTrue(catalog.forPose("handstand").isEmpty());
    }

    @Test void everyRoutinePoseHasAFigure() {
        var unillustrated = new PoseCatalog().all().stream().map(pose -> pose.id())
                .filter(id -> catalog.forPose(id).isEmpty()).toList();
        // Warrior II falls back to the audited CC0 illustration, so it still
        // shows a figure. The seated twist deliberately shows none: a twist is
        // a rotation about the spine, which a flat line drawing cannot say, and
        // the icon for it was read by a reviewer as putting the legs somewhere
        // they should not go. Written guidance beats a picture that misleads.
        assertEquals(Set.of("warrior_two", "seated_twist"), Set.copyOf(unillustrated), "an unexpected pose lost its figure");
        assertTrue(new TeachingAssetCatalog().enabledForCoaching("warrior_two").isPresent(),
                "warrior_two must keep its fallback illustration");
    }
}
