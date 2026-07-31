package io.saha.yoga.illustration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class PoseIllustrationRegistry {
    private final Map<String, PoseIllustration> byPose;
    private final IllustrationApprovalGate gate = new IllustrationApprovalGate();

    public PoseIllustrationRegistry() {
        this(List.of(
                draft("mountain", "Front view", "Stand with a comfortable base and relaxed arms."),
                draft("chair", "Side view", "Send the hips back, bend the knees, and keep the chest lifted."),
                draft("warrior_one", "Three-quarter view", "Bend the front knee into a clear lunge while grounding the rear foot."),
                draft("warrior_two", "Three-quarter view", "Stack the front knee over the ankle and extend through both arms."),
                draft("cat_cow", "Side view", "Stack wrists below shoulders and knees below hips."),
                draft("tree", "Front view", "Ground the standing foot and place the lifted foot away from the knee joint.")
        ));
    }

    public PoseIllustrationRegistry(List<PoseIllustration> illustrations) {
        byPose = illustrations.stream().collect(Collectors.toUnmodifiableMap(PoseIllustration::poseId, Function.identity()));
    }

    public Optional<PoseIllustration> reviewed(String poseId) {
        return Optional.ofNullable(byPose.get(poseId)).filter(gate::mayTeachWith);
    }

    public Optional<PoseIllustration> status(String poseId) {
        return Optional.ofNullable(byPose.get(poseId));
    }

    private static PoseIllustration draft(String id, String view, String setup) {
        return new PoseIllustration(id, view, setup, List.of(), ReviewState.DRAFT);
    }
}
