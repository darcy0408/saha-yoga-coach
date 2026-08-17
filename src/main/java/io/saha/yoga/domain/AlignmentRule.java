package io.saha.yoga.domain;

/**
 * One angle the coach can measure, and what it expects of it.
 *
 * {@code graded} separates the two honest uses of a measurement. A graded rule
 * has a range its author is willing to correct against, so falling outside it
 * produces a cue. An ungraded one is measured and shown but never judged — for
 * shapes like cat–cow, where the whole pose is a movement between two
 * positions and no single number is the right answer. Showing the angle move
 * with the body is useful; pretending to know the correct value is not.
 */
public record AlignmentRule(
        String id, LandmarkName first, LandmarkName vertex, LandmarkName third,
        BilateralStrategy bilateralStrategy,
        double minimumDegrees, double maximumDegrees, String suggestion, String pastRangeSuggestion,
        int priority, boolean graded) {
    public AlignmentRule {
        if (minimumDegrees > maximumDegrees) throw new IllegalArgumentException("Invalid angle range");
        if (bilateralStrategy == null) throw new IllegalArgumentException("bilateralStrategy is required");
    }

    /**
     * The cue that fits which way the angle is wrong.
     *
     * One sentence for both directions is worse than none: a knee already
     * deeper than the range was being told to bend further toward the toes,
     * which is the opposite of what the body needed. {@code suggestion} covers
     * not having gone far enough; {@code pastRangeSuggestion} covers having
     * gone past.
     */
    public String suggestionFor(double angle) {
        boolean past = angle < minimumDegrees;
        if (past && pastRangeSuggestion != null && !pastRangeSuggestion.isBlank()) return pastRangeSuggestion;
        return suggestion;
    }

    /** A readable name for the thing being measured, from the rule's id. */
    public String label() {
        var words = id.replace('-', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }
}
