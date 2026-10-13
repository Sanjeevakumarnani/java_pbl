package crfms.engine;

public enum FeedbackCategory {
    EXAMPLES("Use of examples"),
    CLARITY("Concept clarity"),
    PREPARATION("Preparation"),
    PACE("Pace of teaching"),
    ENGAGEMENT("Interaction"),
    AVAILABILITY("Doubt guidance"),
    FAIRNESS("Fairness"),
    MATERIALS("Learning materials"),
    FEEDBACK_QUALITY("Feedback quality"),
    OVERALL("Overall");
    
    private final String label;
    FeedbackCategory(String label) { this.label = label; }
    public String getLabel() { return label; }
}