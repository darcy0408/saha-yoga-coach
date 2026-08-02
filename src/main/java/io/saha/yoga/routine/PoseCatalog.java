package io.saha.yoga.routine;

import io.saha.yoga.domain.*;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PoseCatalog {
    private static final List<LandmarkName> FULL = List.of(LandmarkName.LEFT_SHOULDER, LandmarkName.RIGHT_SHOULDER,
            LandmarkName.LEFT_HIP, LandmarkName.RIGHT_HIP, LandmarkName.LEFT_KNEE, LandmarkName.RIGHT_KNEE,
            LandmarkName.LEFT_ANKLE, LandmarkName.RIGHT_ANKLE);
    private static final String CAUTION = "General caution only; not medical advice. Stop if you feel pain, dizziness, numbness, weakness, or unusual discomfort.";
    private static final LazyConstant<List<Pose>> POSES = LazyConstant.of(PoseCatalog::buildPoses);
    private final List<Pose> poses = POSES.get();
    private static List<Pose> buildPoses() { return List.of(
            pose("mountain", "Mountain", 45, "Stand tall with feet comfortable and breathe steadily.", "Widen your stance or stand near a chair.", List.of()),
            pose("chair", "Chair", 40, "Sit your hips back as if reaching for a chair.", "Reduce the depth or touch a chair behind you.", List.of(knee("chair-knee", 80, 135, "Try a smaller knee bend and keep your knees tracking toward your toes."))),
            pose("warrior_one", "Warrior I", 45, "Step one foot back and lift through your chest.", "Shorten your stance and keep hands at hips.", List.of(knee("front-knee", 80, 145, "Try bending your front knee gently toward your toes."))),
            pose("warrior_two", "Warrior II", 50, "Open your hips and arms while looking over the front hand.", "Shorten your stance if this feels uncomfortable.", List.of(knee("front-knee", 80, 140, "Move your front knee slightly toward your toes."))),
            pose("triangle", "Triangle", 45, "Lengthen both sides of your waist as you reach over the front leg.", "Rest your hand higher on your shin or a chair.", List.of(knee("front-leg", 155, 180, "Consider softening or lengthening your front knee."))),
            pose("tree", "Tree", 40, "Balance on one leg with the lifted foot below or above the knee.", "Keep toes on the floor or hold a chair.", List.of(knee("standing-leg", 155, 180, "Try keeping a soft, steady bend in your standing knee."))),
            pose("cat_cow", "Cat–Cow", 60, "On hands and knees, move your spine gently with your breath.", "Place padding under your knees or make the motion smaller.", List.of()),
            pose("bird_dog", "Bird Dog", 45, "From hands and knees, reach opposite arm and leg long.", "Move only one limb or keep toes on the floor.", List.of()),
            pose("low_lunge", "Low Lunge", 50, "Step one foot forward and let your hips ease ahead.", "Pad the back knee and keep hands on blocks or thigh.", List.of(knee("front-knee", 75, 130, "Try stacking your front knee more comfortably over your ankle."))),
            pose("bridge", "Bridge", 50, "Lie down, press through your feet, and lift your hips comfortably.", "Lift only a little or keep your hips down for pelvic tilts.", List.of()),
            pose("seated_fold", "Seated Forward Fold", 60, "Sit tall and hinge forward only as far as comfortable.", "Bend your knees or loop a strap around your feet.", List.of()),
            pose("rest", "Final Rest", 180, "Rest in a position where your breathing feels easy.", "Place support under your knees or rest on your side.", List.of())
    ); }
    private final Map<String, Pose> byId = poses.stream().collect(Collectors.toUnmodifiableMap(Pose::id, Function.identity()));

    private static AlignmentRule knee(String id, double min, double max, String cue) {
        return new AlignmentRule(id, LandmarkName.LEFT_HIP, LandmarkName.LEFT_KNEE, LandmarkName.LEFT_ANKLE, min, max, cue, 1);
    }
    private static Pose pose(String id, String name, int seconds, String instruction, String modification, List<AlignmentRule> rules) {
        return new Pose(id, name, Difficulty.BEGINNER, seconds, List.of(instruction), List.of(modification), FULL, rules, CAUTION);
    }
    public List<Pose> all() { return poses; }
    public Pose require(String id) { var pose = byId.get(id); if (pose == null) throw new IllegalArgumentException("Unknown pose: " + id); return pose; }
}
