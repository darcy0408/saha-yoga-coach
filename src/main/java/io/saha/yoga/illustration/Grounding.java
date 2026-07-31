package io.saha.yoga.illustration;

import java.util.Map;
import java.util.Set;

public record Grounding(
        double floorY,
        Set<SupportContact> requiredContacts,
        Map<SupportContact, Double> contactY
) {
    public Grounding {
        if (!Double.isFinite(floorY)) throw new IllegalArgumentException("floorY must be finite");
        requiredContacts = Set.copyOf(requiredContacts);
        contactY = Map.copyOf(contactY);
    }

    public boolean isSatisfied(double tolerance) {
        return !requiredContacts.isEmpty() && requiredContacts.stream().allMatch(contact -> {
            var y = contactY.get(contact);
            return y != null && Double.isFinite(y) && Math.abs(y - floorY) <= tolerance;
        });
    }
}
