package io.saha.yoga.personalization;

import io.saha.yoga.domain.SessionMetric;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PersonalizationEngineTest {
    private SessionMetric success(int days) { return new SessionMetric("tree", Instant.now().minusSeconds(days*86400L), 40, .85, 0, false, .9, true); }
    @Test void increasesOnlyAfterThreeComfortableStableSessions() {
        var result = new PersonalizationEngine().recommend(List.of(success(3), success(2), success(1)));
        assertEquals(10, result.durationAdjustments().get("tree")); assertFalse(result.explanations().isEmpty());
    }
    @Test void discomfortReducesDuration() {
        var metric = new SessionMetric("tree", Instant.now(), 5, .2, 0, false, .9, false);
        assertEquals(-20, new PersonalizationEngine().recommend(List.of(metric)).durationAdjustments().get("tree"));
    }
    @Test void skippedPoseIsNotMisreportedAsDiscomfort() {
        var skipped = new SessionMetric("tree", Instant.now(), 0, 0, 0, true, .9, false);
        var result = new PersonalizationEngine().recommend(List.of(skipped, skipped));
        assertTrue(result.explanations().stream().noneMatch(reason -> reason.contains("discomfort")));
        assertEquals(-15, result.durationAdjustments().get("tree"));
    }
}
