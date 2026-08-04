package io.saha.yoga.routine;

import io.saha.yoga.domain.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RoutineGenerator {
    private static final List<String> ORDER = List.of("mountain", "cat_cow", "chair", "warrior_one", "warrior_two",
            "triangle", "tree", "bird_dog", "low_lunge", "bridge", "seated_fold", "rest");
    private static final Map<String, Integer> DURATIONS = Map.ofEntries(
            Map.entry("mountain",90), Map.entry("cat_cow",120), Map.entry("chair",75), Map.entry("warrior_one",100),
            Map.entry("warrior_two",110), Map.entry("triangle",90), Map.entry("tree",90), Map.entry("bird_dog",100),
            Map.entry("low_lunge",110), Map.entry("bridge",100), Map.entry("seated_fold",105), Map.entry("rest",210));
    private final PoseCatalog catalog;
    public RoutineGenerator(PoseCatalog catalog) { this.catalog = catalog; }
    public Routine beginner(Map<String, Integer> adjustments, List<String> explanations) {
        return beginner(adjustments, explanations, 3);
    }
    public Routine beginner(Map<String, Integer> adjustments, List<String> explanations, int intensity) {
        if (intensity < 1 || intensity > 5) throw new IllegalArgumentException("Intensity must be from 1 to 5");
        var items = new ArrayList<RoutineItem>();
        for (int i = 0; i < ORDER.size(); i++) {
            var id = ORDER.get(i); var pose = catalog.require(id);
            int intensityDelta = i >= 2 && i < 10 ? (intensity - 3) * 5 : 0;
            var duration = Math.max(30, DURATIONS.get(id) + adjustments.getOrDefault(id, 0) + intensityDelta);
            var phase = i < 2 ? "Warm-up" : i >= 10 ? "Cooldown" : "Main sequence";
            items.add(new RoutineItem(pose, duration, phase, "Selected for a balanced beginner practice at intensity " + intensity + " of 5."));
        }
        var reasons = new ArrayList<>(explanations);
        reasons.add("Intensity " + intensity + " of 5 adjusts hold time only; it does not add advanced poses.");
        return new Routine("Steady Start · about 20 minutes", items, reasons);
    }
}
