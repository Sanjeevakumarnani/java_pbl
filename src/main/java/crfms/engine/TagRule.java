package crfms.engine;

import java.util.Optional;

public final class TagRule implements SuggestionRule {
    private final String tag;
    private final double minShare;
    private final String advice;

    public TagRule(String tag, double minShare, String advice) {
        this.tag = tag == null ? "" : tag;
        this.minShare = minShare;
        this.advice = advice == null ? "" : advice;
    }

    @Override
    public Optional<Suggestion> evaluate(CategoryStats stats) {
        if (stats == null || stats.getResponses() <= 0) return Optional.empty();
        Integer count = stats.getTagCounts().get(tag);
        if (count == null || count <= 0) return Optional.empty();
        double share = (double) count / (double) stats.getResponses();
        if (share < minShare) return Optional.empty();
        return Optional.of(new Suggestion(advice, share));
    }
}