package io.saha.yoga.domain;

import java.time.Instant;

public record SessionMetric(String poseId, Instant completedAt, int heldSeconds,
                            double stability, int corrections, boolean skipped,
                            double averageConfidence, boolean comfortable) {}

