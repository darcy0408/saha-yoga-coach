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
            pose("easy_seat", "Easy Seat", 60, "Sit comfortably, lengthen your spine, and let your breath settle.", "Sit on a folded blanket or a chair if your hips feel tight.", List.of(),
                    "Cross your shins comfortably, one in front of the other.",
                    "Rest your hands on your thighs and lengthen up through the crown of your head."),
            pose("seated_side_reach", "Seated Side Reach", 40, "Reach one arm overhead and lean gently to the side.", "Keep the reach small, or rest the other hand on your thigh.", List.of(),
                    "Press down through the sitting bone on the side you are reaching from.",
                    "Reach up first and then over, so the side of your waist lengthens rather than folds."),
            pose("upward_salute", "Upward Salute", 35, "Sweep both arms overhead and lengthen from your hips.", "Take the arms shoulder-width apart, or stop at shoulder height.",
                    // the reach itself, not the elbow: an arm can be perfectly
                    // straight and still hanging by your side
                    List.of(reach("overhead-reach", BilateralStrategy.STRAIGHTEST, 110, 180, "Try sweeping your arms higher, until they frame your ears.")),
                    "Stand with your feet about hip-width apart and your weight even.",
                    "Sweep the arms out and up, keeping your shoulders soft away from your ears."),
            pose("standing_fold", "Standing Forward Fold", 40, "Hinge from the hips and let your head and arms hang heavy.", "Bend your knees generously or rest your hands on your shins.",
                    // a deep fold closes this angle almost completely, so only
                    // the upper bound carries meaning here
                    List.of(torso("fold-depth", BilateralStrategy.MOST_BENT, 0, 110, "Try hinging further from the hips, bending your knees as much as you need.")),
                    "Soften your knees before you fold, and keep them soft the whole way down.",
                    "Let your head hang heavy and release your neck."),
            pose("downward_dog", "Downward Dog", 45, "Press the floor away and lift your hips up and back.", "Bend your knees, or take the pose with hands on a chair seat.",
                    List.of(torso("hip-fold", BilateralStrategy.MOST_BENT, 55, 130, "Try lifting your hips higher and letting your chest move toward your thighs.")),
                    "Start on your hands and knees, with your hands a little ahead of your shoulders.",
                    "Tuck your toes and lift your hips up and back; bend your knees as much as you need."),
            pose("goddess", "Goddess", 40, "Step wide, turn your toes out, and bend both knees.", "Bend less deeply, or hold a chair back for support.",
                    List.of(knee("goddess-knee", BilateralStrategy.WORST_MATCH, 85, 140, "Try bending both knees a little more, keeping them over your toes.",
                            "That is deeper than this pose asks for — lift a little and keep the knees over the toes.")),
                    "Step your feet wide, about a leg's length apart.",
                    "Turn both toes out toward the corners, then bend your knees the same way they point."),
            pose("plank", "Plank", 30, "Hold one long line from your heels to the crown of your head.", "Lower your knees to the floor and keep the same long line.",
                    List.of(torso("body-line", BilateralStrategy.WORST_MATCH, 150, 180, "Your hips are dropping or lifting — try drawing them back into one long line.")),
                    "From hands and knees, stack your shoulders directly over your wrists.",
                    "Step back one foot at a time, and press the floor away beneath you."),
            // A pose done face down is the one where the screen cannot be seen
            // at all, so it is the one that most needs to be heard. It carried
            // no rule, so it could never say anything: the lift is the whole
            // pose, and lying flat opens this angle past its range while a
            // comfortable lift closes it.
            pose("locust", "Locust", 40, "Lying face down, lift your chest and legs a comfortable amount.", "Lift only the chest, or only the legs, and keep it small.",
                    List.of(torso("body-lift", BilateralStrategy.WORST_MATCH, 140, 175,
                            "Try lifting your chest and legs a little higher.",
                            "That is a deeper backbend than this pose asks for — lower a little and lengthen instead.")),
                    "Lie face down with your arms alongside your body and your forehead resting down.",
                    "Lift your chest and legs on an in-breath, keeping the back of your neck long."),
            pose("seated_twist", "Seated Twist", 50, "Sit tall and turn gently from the middle of your spine.", "Turn only partway, or sit on a folded blanket.", List.of(),
                    "Sit tall first — the length comes before the turn.",
                    "Turn from the middle of your back and let your head follow last."),
            pose("head_to_knee", "Head to Knee", 45, "Extend one leg, fold the other in, and hinge over the long leg.", "Bend the extended knee or loop a strap around the foot.",
                    List.of(torso("fold-depth", BilateralStrategy.MOST_BENT, 20, 115, "Try hinging a little further over the long leg, leading with your chest.")),
                    "Extend one leg and draw the other foot in toward your inner thigh.",
                    "Turn your chest to face the long leg before you begin to fold."),
            pose("chair", "Chair", 40, "Sit your hips back as if reaching for a chair.", "Reduce the depth or touch a chair behind you.",
                    List.of(knee("chair-knee", BilateralStrategy.WORST_MATCH, 80, 135, "Try sitting your hips back a little further, knees tracking toward your toes.",
                            "You are deeper than this pose needs — rise a little and let the weight settle in your heels.")),
                    "Stand with your feet hip-width apart and your weight back in your heels.",
                    "Sit your hips back as though a chair were behind you, then let your arms rise."),
            pose("warrior_one", "Warrior I", 45, "Step one foot back and lift through your chest.", "Shorten your stance and keep hands at hips.",
                    List.of(knee("front-knee", BilateralStrategy.MOST_BENT, 80, 145, "Try bending your front knee gently toward your toes.",
                            "Your front knee is past your toes — ease back until the knee stacks over the ankle.")),
                    "Step one foot back a good stride, and turn the back toes out a little.",
                    "Bend the front knee toward a right angle, then lift your chest and reach your arms up."),
            pose("warrior_two", "Warrior II", 50, "Open your hips and arms while looking over the front hand.", "Shorten your stance if this feels uncomfortable.",
                    List.of(knee("front-knee", BilateralStrategy.MOST_BENT, 80, 140, "Move your front knee slightly toward your toes.",
                            "Your front knee is past your toes — ease back until the knee stacks over the ankle.")),
                    "Step your feet wide, front toes pointing forward and back toes turned in.",
                    "Bend the front knee, then open your arms level and look out over the front hand."),
            pose("triangle", "Triangle", 45, "Lengthen both sides of your waist as you reach over the front leg.", "Rest your hand higher on your shin or a chair.", List.of(knee("front-leg", BilateralStrategy.WORST_MATCH, 155, 180, "Consider softening or lengthening your front knee.")),
                    "Step wide, turn the front foot out, and keep the front leg long.",
                    "Reach forward over the front leg first, then rest your hand wherever it lands on the leg."),
            pose("tree", "Tree", 40, "Balance on one leg with the lifted foot below or above the knee.", "Keep toes on the floor or hold a chair.", List.of(knee("standing-leg", BilateralStrategy.STRAIGHTEST, 155, 180, "Try keeping a soft, steady bend in your standing knee.")),
                    "Find a steady point to look at before you lift the foot.",
                    "Place the foot on your ankle, calf, or inner thigh — never against the side of the knee."),
            pose("cat_cow", "Cat–Cow", 60, "On hands and knees, move your spine gently with your breath.", "Place padding under your knees or make the motion smaller.",
                    // both the arch and the round are correct, so this angle is
                    // shown moving with the breath rather than graded against a
                    // number neither position deserves
                    List.of(observed("spine-angle", LandmarkName.LEFT_SHOULDER, LandmarkName.LEFT_HIP, LandmarkName.LEFT_KNEE,
                            "Let the angle rise and fall with your breath; there is no single right value here.")),
                    "Set your hands under your shoulders and your knees under your hips.",
                    "Breathe in to arch and lift your chest, breathe out to round — let the breath lead."),
            pose("low_lunge", "Low Lunge", 50, "Step one foot forward and let your hips ease ahead.", "Pad the back knee and keep hands on blocks or thigh.",
                    List.of(knee("front-knee", BilateralStrategy.MOST_BENT, 75, 130, "Try letting your hips travel forward so the front knee bends more deeply.",
                            "Your front knee has travelled past your ankle — draw the hips back a little.")),
                    "From hands and knees, step one foot forward between your hands.",
                    "Lower the back knee down, pad it if you need to, and lift your chest."),
            pose("bridge", "Bridge", 50, "Lie down, press through your feet, and lift your hips comfortably.", "Lift only a little or keep your hips down for pelvic tilts.",
                    List.of(torso("hip-lift", BilateralStrategy.STRAIGHTEST, 120, 180, "Try pressing through your feet to lift your hips a little higher.")),
                    "Lie on your back, knees bent, feet flat and close enough to brush your fingertips.",
                    "Press down through your feet to lift your hips, and keep your head still on the floor."),
            pose("seated_fold", "Seated Forward Fold", 60, "Sit tall and hinge forward only as far as comfortable.", "Bend your knees or loop a strap around your feet.",
                    List.of(torso("fold-depth", BilateralStrategy.MOST_BENT, 20, 115, "Try hinging from the hips rather than rounding, only as far as is comfortable.")),
                    "Sit up onto the front of your sitting bones with both legs long.",
                    "Hinge from the hips leading with your chest, and stop wherever the breath stays easy."),
            pose("rest", "Final Rest", 180, "Rest in a position where your breathing feels easy.", "Place support under your knees or rest on your side.", List.of(),
                    "Let your legs fall open and rest your arms a little away from your sides.",
                    "There is nothing to do here — let your breath find its own rhythm.")
    ); }
    private final Map<String, Pose> byId = poses.stream().collect(Collectors.toUnmodifiableMap(Pose::id, Function.identity()));

    private static AlignmentRule knee(String id, BilateralStrategy strategy, double min, double max, String cue) {
        return knee(id, strategy, min, max, cue, "");
    }

    /** {@code deeperThanRange} is said when the knee has gone past the range, not short of it. */
    private static AlignmentRule knee(String id, BilateralStrategy strategy, double min, double max, String cue, String deeperThanRange) {
        return new AlignmentRule(id, LandmarkName.LEFT_HIP, LandmarkName.LEFT_KNEE, LandmarkName.LEFT_ANKLE, strategy, min, max, cue, deeperThanRange, 1, true);
    }

    /**
     * The shoulder-hip-knee angle: how far the torso is folded or opened over
     * the legs. It is the one spine-related measurement two shoulders, two hips
     * and two knees can actually support, and it carries most of these poses -
     * a sagging plank, a shallow fold and unlifted hips in bridge are all this
     * angle being wrong.
     */
    private static AlignmentRule torso(String id, BilateralStrategy strategy, double min, double max, String cue) {
        return torso(id, strategy, min, max, cue, "");
    }

    /** {@code deeperThanRange} is said when the torso has folded or arched past the range. */
    private static AlignmentRule torso(String id, BilateralStrategy strategy, double min, double max, String cue, String deeperThanRange) {
        return new AlignmentRule(id, LandmarkName.LEFT_SHOULDER, LandmarkName.LEFT_HIP, LandmarkName.LEFT_KNEE, strategy, min, max, cue, deeperThanRange, 1, true);
    }

    /** Measured and shown, never judged - for shapes where no single angle is correct. */
    private static AlignmentRule observed(String id, LandmarkName first, LandmarkName vertex, LandmarkName third, String note) {
        return new AlignmentRule(id, first, vertex, third, BilateralStrategy.WORST_MATCH, 0, 180, note, "", 9, false);
    }

    /**
     * The reach itself: the angle at the shoulder between the hip and the elbow.
     *
     * The elbow rather than the wrist, deliberately. Raising your arms fully
     * overhead is exactly when your hands leave the top of a webcam's frame, so
     * a rule that needs the wrist stops measuring at the moment the pose
     * becomes correct - it reported nothing however high the arms went. The
     * upper arm carries the same information and stays in shot.
     */
    private static AlignmentRule reach(String id, BilateralStrategy strategy, double min, double max, String cue) {
        return new AlignmentRule(id, LandmarkName.LEFT_HIP, LandmarkName.LEFT_SHOULDER, LandmarkName.LEFT_ELBOW, strategy, min, max, cue, "", 2, true);
    }
    /**
     * The first instruction is the pose in one sentence, which is what the
     * teaching card shows and what the coach says on arriving. Anything after
     * it is a practical step for getting into the shape, spoken while there is
     * still time to act on it.
     *
     * These are setup steps, never corrections: a body that cannot be seen can
     * still be told where to put its feet, so they claim nothing about what the
     * camera did or did not measure.
     */
    private static Pose pose(String id, String name, int seconds, String instruction, String modification, List<AlignmentRule> rules, String... entrySteps) {
        var instructions = new java.util.ArrayList<String>(1 + entrySteps.length);
        instructions.add(instruction);
        instructions.addAll(List.of(entrySteps));
        return new Pose(id, name, Difficulty.BEGINNER, seconds, instructions, List.of(modification), FULL, rules, CAUTION);
    }
    /**
     * Poses held on one side at a time, which therefore need doing on both.
     *
     * The routine gives each of these a single hold, so the coach calls the
     * change at the halfway mark rather than quietly working only one leg.
     */
    private static final java.util.Set<String> ONE_SIDED = java.util.Set.of(
            "warrior_one", "warrior_two", "triangle", "tree", "low_lunge", "head_to_knee", "seated_side_reach");

    public boolean isOneSided(String poseId) { return ONE_SIDED.contains(poseId); }

    public List<Pose> all() { return poses; }
    public Pose require(String id) { var pose = byId.get(id); if (pose == null) throw new IllegalArgumentException("Unknown pose: " + id); return pose; }
}
