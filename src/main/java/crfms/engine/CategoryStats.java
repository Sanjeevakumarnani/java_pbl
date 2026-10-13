package crfms.engine;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public final class CategoryStats {
    private final int responses;
    private final Map<FeedbackCategory, Double> averages;
    private final Map<String, Integer> tagCounts;

    public CategoryStats(int responses,
                         Map<FeedbackCategory, Double> averages,
                         Map<String, Integer> tagCounts) {
        this.responses = responses;
        this.averages = averages == null ? Collections.emptyMap() : Collections.unmodifiableMap(new EnumMap<>(averages));
        this.tagCounts = tagCounts == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(tagCounts));
    }

    public int getResponses() {
        return responses;
    }

    public Map<FeedbackCategory, Double> getAverages() {
        return averages;
    }

    public Map<String, Integer> getTagCounts() {
        return tagCounts;
    }
}