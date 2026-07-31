package io.saha.yoga.illustration;

public final class IllustrationApprovalGate {
    public boolean mayTeachWith(PoseIllustration illustration) {
        return illustration.reviewState() == ReviewState.ENABLED
                && illustration.referenceUrls().stream().filter(url -> url.startsWith("https://")).distinct().count() >= 2;
    }
}
