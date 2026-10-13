package crfms.engine;

import java.util.Optional;

public final class ThresholdRule implements SuggestionRule {
    private final FeedbackCategory category;
    private final double threshold;
    private final String advice;

    public ThresholdRule(FeedbackCategory category, double threshold, String advice) {
        this.category = category;
        this.threshold = threshold;
        this.advice = advice == null ? "" : advice;
    }

    @Override
    public Optional<Suggestion> evaluate(CategoryStats stats) {
        if (stats == null || stats.getResponses() == 0) return Optional.empty();
        Double avg = stats.getAverages().get(category);
        if (avg == null) return Optional.empty();
        if (avg >= threshold) return Optional.empty();
        double severity = threshold - avg;
        return Optional.of(new Suggestion(advice, severity));
    }
}