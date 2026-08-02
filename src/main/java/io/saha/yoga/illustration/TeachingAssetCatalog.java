package io.saha.yoga.illustration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TeachingAssetCatalog {
    private static final String ROOT = "/io/saha/yoga/illustrations/cc0/";

    private final List<TeachingAsset> assets = List.of(
            candidate("warrior_two", "Warrior II", "openclipart-8248.png",
                    "https://openclipart.org/detail/8248/yoga-poses-stylized",
                    "127F652B3B27EF3CE30950E90E5A67595956120CF00A41F01D4B8C011C0B085C",
                    "Deep front-knee lunge, long stance, level arms, and visible feet."),
            candidate("tree", "Tree", "openclipart-8249.png",
                    "https://openclipart.org/detail/8249/yoga-poses-stylized",
                    "C3C15223746C2D1794A653902B68136A63BE12CBAB634CDEC44F8F0B79C029FE",
                    "Clear standing foot and lifted-foot placement with an upright torso."));
    private final Map<String, TeachingAsset> byPose = assets.stream()
            .collect(Collectors.toUnmodifiableMap(TeachingAsset::poseId, Function.identity()));

    public List<TeachingAsset> reviewCandidates() {
        return assets;
    }

    public Optional<TeachingAsset> forPose(String poseId) {
        return Optional.ofNullable(byPose.get(poseId));
    }

    public Optional<TeachingAsset> enabledForCoaching(String poseId) {
        return forPose(poseId).filter(TeachingAsset::mayAppearDuringCoaching);
    }

    private static TeachingAsset candidate(String id, String name, String file, String source, String hash, String note) {
        return new TeachingAsset(id, name, ROOT + file, "Gerald_G", source,
                "CC0 1.0 Public Domain Dedication", "https://creativecommons.org/publicdomain/zero/1.0/",
                hash, ReviewState.HUMAN_REVIEWED, note);
    }
}
