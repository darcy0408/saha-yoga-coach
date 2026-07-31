package io.saha.yoga.illustration;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class IllustrationApprovalGateTest {
    private final IllustrationApprovalGate gate = new IllustrationApprovalGate();

    @Test void rejectsEveryStateBeforeEnabled() {
        for (var state : List.of(ReviewState.DRAFT, ReviewState.REFERENCE_CHECKED, ReviewState.HUMAN_REVIEWED)) {
            assertFalse(gate.mayTeachWith(illustration(state, List.of("https://one.example", "https://two.example"))));
        }
    }

    @Test void enabledStillRequiresTwoDistinctSecureReferences() {
        assertFalse(gate.mayTeachWith(illustration(ReviewState.ENABLED, List.of())));
        assertFalse(gate.mayTeachWith(illustration(ReviewState.ENABLED, List.of("https://one.example", "https://one.example"))));
        assertFalse(gate.mayTeachWith(illustration(ReviewState.ENABLED, List.of("http://one.example", "https://two.example"))));
        assertTrue(gate.mayTeachWith(illustration(ReviewState.ENABLED, List.of("https://one.example", "https://two.example"))));
    }

    @Test void enabledIllustrationMustPlaceEveryRequiredSupportOnFloor() {
        var floating = new PoseIllustration("test", "Side", "Test setup",
                List.of("https://one.example", "https://two.example"), ReviewState.ENABLED,
                new Grounding(.9, Set.of(SupportContact.LEFT_FOOT, SupportContact.RIGHT_FOOT),
                        Map.of(SupportContact.LEFT_FOOT, .9, SupportContact.RIGHT_FOOT, .84)));
        assertFalse(gate.mayTeachWith(floating));
    }

    @Test void productionRegistryExposesNoDraftAsTeachingArt() {
        var registry = new PoseIllustrationRegistry();
        assertTrue(registry.status("chair").isPresent());
        assertTrue(registry.reviewed("chair").isEmpty());
        assertTrue(registry.reviewed("warrior_one").isEmpty());
        assertTrue(registry.reviewed("warrior_two").isEmpty());
    }

    private PoseIllustration illustration(ReviewState state, List<String> references) {
        return new PoseIllustration("test", "Side", "Test setup", references, state,
                new Grounding(.9, Set.of(SupportContact.LEFT_FOOT, SupportContact.RIGHT_FOOT),
                        Map.of(SupportContact.LEFT_FOOT, .9, SupportContact.RIGHT_FOOT, .9)));
    }
}
