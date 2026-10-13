package crfms.bot;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Exact tokenization per master prompt.
 */
public final class TextTokenizer {
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final String[] STOP_WORDS_ARR = {
            "a", "an", "the", "is", "are", "was", "to", "of", "in", "on", "for", "and", "or", "do", "does", "how", "what", "why", "can", "i", "me", "my", "we", "you", "it", "please", "tell", "about", "with", "between", "this", "that"
    };
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(STOP_WORDS_ARR));

    private TextTokenizer() {}

    public static LinkedHashSet<String> tokenize(String text) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        if (text == null || text.isBlank()) return tokens;
        String lower = text.toLowerCase(Locale.ROOT);
        String[] parts = NON_ALNUM.split(lower);
        for (String p : parts) {
            if (p == null || p.isBlank()) continue;
            if (p.length() < 2) continue;
            if (STOP_WORDS.contains(p)) continue;
            tokens.add(p);
        }
        return tokens;
    }
}