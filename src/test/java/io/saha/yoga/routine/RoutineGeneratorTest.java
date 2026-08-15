package io.saha.yoga.routine;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RoutineGeneratorTest {
    @Test void intensityChangesHoldTimeWithoutChangingThePoseSequence() {
        var generator = new RoutineGenerator(new PoseCatalog());
        var gentle = generator.beginner(Map.of(), List.of(), 1);
        var baseline = generator.beginner(Map.of(), List.of(), 3);
        var active = generator.beginner(Map.of(), List.of(), 5);

        assertTrue(gentle.totalSeconds() < baseline.totalSeconds());
        assertTrue(active.totalSeconds() > baseline.totalSeconds());
        assertEquals(baseline.items().stream().map(item -> item.pose().id()).toList(),
                active.items().stream().map(item -> item.pose().id()).toList());
        assertTrue(active.explanations().getLast().contains("hold time only"));
    }

    @Test void beginnerRoutineIsAWholeTwentyMinutePractice() {
        var routine = new RoutineGenerator(new PoseCatalog()).beginner(Map.of(), List.of());
        assertTrue(routine.items().size() >= 20, "a full practice, not a handful of poses");
        // holds only; the five-second transitions between poses add about
        // another minute and a half of wall time on top
        assertTrue(routine.totalSeconds() >= 17 * 60 && routine.totalSeconds() <= 21 * 60,
                "holds totalled " + routine.totalSeconds() + "s");
        assertEquals("Centering", routine.items().getFirst().phase());
        assertEquals("Rest", routine.items().getLast().phase());
        assertEquals("rest", routine.items().getLast().pose().id());
    }

    @Test void thePracticeRunsInAnOrderThatMakesSense() {
        var routine = new RoutineGenerator(new PoseCatalog()).beginner(Map.of(), List.of());
        var phases = routine.items().stream().map(item -> item.phase()).distinct().toList();
        assertEquals(List.of("Centering", "Warm-up", "Standing", "Balance", "Floor work", "Cooldown", "Rest"), phases,
                "each phase should appear once, in order, rather than interleaved");
        var ids = routine.items().stream().map(item -> item.pose().id()).toList();
        assertTrue(ids.indexOf("cat_cow") < ids.indexOf("warrior_one"), "warm the spine before standing work");
        assertTrue(ids.indexOf("warrior_two") < ids.indexOf("seated_fold"), "standing work precedes the cooldown");
        assertTrue(ids.indexOf("bridge") < ids.indexOf("rest"), "the backbend settles before final rest");
        assertEquals(ids.size(), ids.stream().distinct().count(), "no pose should repeat");
    }

    @Test void settlingAndRestKeepTheirLengthAtEveryIntensity() {
        var generator = new RoutineGenerator(new PoseCatalog());
        var gentle = generator.beginner(Map.of(), List.of(), 1);
        var active = generator.beginner(Map.of(), List.of(), 5);
        assertEquals(seconds(gentle, "easy_seat"), seconds(active, "easy_seat"));
        assertEquals(seconds(gentle, "rest"), seconds(active, "rest"));
        assertTrue(seconds(gentle, "chair") < seconds(active, "chair"));
    }

    private int seconds(io.saha.yoga.domain.Routine routine, String poseId) {
        return routine.items().stream().filter(item -> item.pose().id().equals(poseId)).findFirst().orElseThrow().durationSeconds();
    }
    @Test void appliesBoundedAdjustment() {
        var routine = new RoutineGenerator(new PoseCatalog()).beginner(Map.of("tree", -200), List.of());
        assertEquals(30, routine.items().stream().filter(i -> i.pose().id().equals("tree")).findFirst().orElseThrow().durationSeconds());
    }
}
