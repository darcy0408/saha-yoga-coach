package io.saha.yoga.illustration;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class TeachingAssetCatalogTest {
    private final TeachingAssetCatalog catalog = new TeachingAssetCatalog();

    @Test void retainsTraceableLicenseMetadataForEveryCandidate() {
        assertEquals(2, catalog.reviewCandidates().size());
        for (var asset : catalog.reviewCandidates()) {
            assertEquals("CC0 1.0 Public Domain Dedication", asset.licenseName());
            assertTrue(asset.sourceUrl().startsWith("https://openclipart.org/detail/"));
            assertEquals("Gerald_G", asset.creator());
        }
    }

    @Test void packagedResourcesMatchAuditedChecksums() throws Exception {
        for (var asset : catalog.reviewCandidates()) {
            try (InputStream stream = TeachingAssetCatalogTest.class.getResourceAsStream(asset.resourcePath())) {
                assertNotNull(stream, () -> "Missing teaching asset " + asset.resourcePath());
                var actual = HexFormat.of().withUpperCase().formatHex(MessageDigest.getInstance("SHA-256").digest(stream.readAllBytes()));
                assertEquals(asset.sha256(), actual);
            }
        }
    }

    @Test void reviewedCandidatesCannotLeakIntoCoachingBeforeEnablement() {
        assertTrue(catalog.forPose("warrior_two").isPresent());
        assertTrue(catalog.forPose("tree").isPresent());
        assertTrue(catalog.enabledForCoaching("warrior_two").isEmpty());
        assertTrue(catalog.enabledForCoaching("tree").isEmpty());
        assertTrue(catalog.enabledForCoaching("chair").isEmpty());
    }
}
