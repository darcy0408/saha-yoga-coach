package io.saha.yoga.illustration;

import java.util.Locale;

public record TeachingAsset(
        String poseId,
        String displayName,
        String resourcePath,
        String creator,
        String sourceUrl,
        String licenseName,
        String licenseUrl,
        String sha256,
        ReviewState reviewState,
        String reviewNote
) {
    public TeachingAsset {
        require(poseId, "poseId");
        require(displayName, "displayName");
        require(resourcePath, "resourcePath");
        require(creator, "creator");
        require(sourceUrl, "sourceUrl");
        require(licenseName, "licenseName");
        require(licenseUrl, "licenseUrl");
        require(sha256, "sha256");
        require(reviewNote, "reviewNote");
        if (!resourcePath.startsWith("/")) throw new IllegalArgumentException("resourcePath must be absolute");
        if (!sourceUrl.startsWith("https://") || !licenseUrl.startsWith("https://")) {
            throw new IllegalArgumentException("asset references must use HTTPS");
        }
        sha256 = sha256.toUpperCase(Locale.ROOT);
        if (!sha256.matches("[0-9A-F]{64}")) throw new IllegalArgumentException("sha256 must contain 64 hex characters");
        if (reviewState == null) throw new IllegalArgumentException("reviewState is required");
    }

    public boolean mayAppearDuringCoaching() {
        return reviewState == ReviewState.ENABLED;
    }

    private static void require(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
    }
}
