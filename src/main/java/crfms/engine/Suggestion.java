package crfms.engine;

import java.util.Objects;

public final class Suggestion {
    private final String text;
    private final double severity;

    public Suggestion(String text, double severity) {
        this.text = Objects.requireNonNullElse(text, "");
        this.severity = severity;
    }

    public String getText() {
        return text;
    }

    public double getSeverity() {
        return severity;
    }
}