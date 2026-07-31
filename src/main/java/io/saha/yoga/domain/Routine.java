package io.saha.yoga.domain;

import java.util.List;

public record Routine(String name, List<RoutineItem> items, List<String> explanations) {
    public Routine { items = List.copyOf(items); explanations = List.copyOf(explanations); }
    public int totalSeconds() { return items.stream().mapToInt(RoutineItem::durationSeconds).sum(); }
}

