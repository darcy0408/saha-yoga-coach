package io.saha.yoga.routine;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RoutineGeneratorTest {
    @Test void beginnerRoutineIsApproximatelyTwentyMinutesAndHasTwelvePoses() {
        var routine = new RoutineGenerator(new PoseCatalog()).beginner(Map.of(), List.of());
        assertEquals(12, routine.items().size());
        assertTrue(routine.totalSeconds() >= 18 * 60 && routine.totalSeconds() <= 22 * 60);
        assertEquals("Warm-up", routine.items().getFirst().phase());
        assertEquals("Cooldown", routine.items().getLast().phase());
    }
    @Test void appliesBoundedAdjustment() {
        var routine = new RoutineGenerator(new PoseCatalog()).beginner(Map.of("tree", -200), List.of());
        assertEquals(30, routine.items().stream().filter(i -> i.pose().id().equals("tree")).findFirst().orElseThrow().durationSeconds());
    }
}

