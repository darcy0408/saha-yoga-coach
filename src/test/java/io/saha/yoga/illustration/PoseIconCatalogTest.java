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
        // the pack has no bent-knee wide stance, no plain standing figure and
        // nothing on all fours with a limb extended, so these must not be
        // silently mapped to a near-miss
        for (var pose : Set.of("mountain", "warrior_two", "bird_dog")) {
            assertTrue(catalog.forPose(pose).isEmpty(), pose + " should have no icon");
        }
    }

    @Test void unknownPoseHasNoIcon() {
        assertTrue(catalog.forPose("handstand").isEmpty());
    }
}
