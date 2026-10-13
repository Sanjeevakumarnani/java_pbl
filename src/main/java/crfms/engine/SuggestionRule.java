package crfms.engine;

import java.util.Optional;

public interface SuggestionRule {
    Optional<Suggestion> evaluate(CategoryStats stats);
}