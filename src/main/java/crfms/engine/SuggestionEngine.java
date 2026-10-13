package crfms.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class SuggestionEngine {
    private final List<SuggestionRule> rules;
    private final int minResponses;

    public SuggestionEngine(List<SuggestionRule> rules, int minResponses) {
        this.rules = rules == null ? Collections.emptyList() : new ArrayList<>(rules);
        this.minResponses = minResponses;
    }

    public List<Suggestion> suggest(CategoryStats stats) {
        if (stats == null || stats.getResponses() < minResponses) return Collections.emptyList();
        List<Suggestion> result = new ArrayList<>();
        for (SuggestionRule rule : rules) {
            Optional<Suggestion> s = rule.evaluate(stats);
            if (s.isPresent()) result.add(s.get());
        }
        result.sort(Comparator.comparingDouble(Suggestion::getSeverity).reversed());
        return result;
    }
}