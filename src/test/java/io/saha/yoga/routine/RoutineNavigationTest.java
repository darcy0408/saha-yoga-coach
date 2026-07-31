package io.saha.yoga.routine;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RoutineNavigationTest {
    @Test void adjacentItemsRepresentDifferentPoses() {
        var routine = new RoutineGenerator(new PoseCatalog()).beginner(Map.of(), List.of());
        for (int index = 1; index < routine.items().size(); index++) {
            assertNotEquals(routine.items().get(index - 1).pose().id(), routine.items().get(index).pose().id());
        }
    }
}
