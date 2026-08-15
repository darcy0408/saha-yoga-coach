package io.saha.yoga.routine;

import io.saha.yoga.domain.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class RoutineGenerator {
    /**
     * A whole practice in the order a class actually runs: settle, warm the
     * spine on the floor, rise through standing shapes while the body is warm,
     * take the one balance while attention is sharpest, return to the floor for
     * strength and a gentle backbend, then unwind through seated folds and
     * twists into rest.
     *
     * The arc matters as much as the poses: warm-ups precede the standing
     * sequence, the deepest work sits in the middle, and everything after the
     * backbend is progressively quieter so the practice lands at rest instead
     * of stopping abruptly.
     */
    private static final List<String> ORDER = List.of(
            "easy_seat", "seated_side_reach",
            "cat_cow", "bird_dog", "downward_dog", "standing_fold",
            "upward_salute", "mountain", "chair", "warrior_one", "warrior_two", "triangle", "goddess",
            "tree",
            "low_lunge", "plank", "locust", "bridge",
            "seated_fold", "seated_twist", "head_to_knee",
            "rest");
    private static final Map<String, Integer> DURATIONS = Map.ofEntries(
            Map.entry("easy_seat",60), Map.entry("seated_side_reach",40),
            Map.entry("cat_cow",70), Map.entry("bird_dog",50), Map.entry("downward_dog",45), Map.entry("standing_fold",40),
            Map.entry("upward_salute",35), Map.entry("mountain",35), Map.entry("chair",45), Map.entry("warrior_one",50),
            Map.entry("warrior_two",50), Map.entry("triangle",45), Map.entry("goddess",40),
            Map.entry("tree",50),
            Map.entry("low_lunge",50), Map.entry("plank",30), Map.entry("locust",40), Map.entry("bridge",50),
            Map.entry("seated_fold",60), Map.entry("seated_twist",50), Map.entry("head_to_knee",45),
            Map.entry("rest",150));
    /** Where each phase begins, by index into {@link #ORDER}. */
    private static final Map<Integer, String> PHASE_STARTS = Map.of(
            0, "Centering", 2, "Warm-up", 6, "Standing", 13, "Balance", 14, "Floor work", 18, "Cooldown", 21, "Rest");
    private final PoseCatalog catalog;
    public RoutineGenerator(PoseCatalog catalog) { this.catalog = catalog; }
    public Routine beginner(Map<String, Integer> adjustments, List<String> explanations) {
        return beginner(adjustments, explanations, 3);
    }
    public Routine beginner(Map<String, Integer> adjustments, List<String> explanations, int intensity) {
        if (intensity < 1 || intensity > 5) throw new IllegalArgumentException("Intensity must be from 1 to 5");
        var items = new ArrayList<RoutineItem>();
        var phase = "Centering";
        for (int i = 0; i < ORDER.size(); i++) {
            var id = ORDER.get(i); var pose = catalog.require(id);
            phase = PHASE_STARTS.getOrDefault(i, phase);
            // Settling and final rest keep their length whatever the intensity;
            // scaling them would work against what they are for.
            boolean scalable = !phase.equals("Centering") && !phase.equals("Rest");
            int intensityDelta = scalable ? (intensity - 3) * 5 : 0;
            var duration = Math.max(30, DURATIONS.get(id) + adjustments.getOrDefault(id, 0) + intensityDelta);
            items.add(new RoutineItem(pose, duration, phase, "Selected for a balanced beginner practice at intensity " + intensity + " of 5."));
        }
        var reasons = new ArrayList<>(explanations);
        reasons.add("Intensity " + intensity + " of 5 adjusts hold time only; it does not add advanced poses.");
        return new Routine("Steady Start · about 20 minutes", items, reasons);
    }
}
