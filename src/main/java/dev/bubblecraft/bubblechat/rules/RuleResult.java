package dev.bubblecraft.bubblechat.rules;

public final class RuleResult {
    private final boolean cancelled;
    private final boolean silent;
    private final String message;

    private RuleResult(boolean cancelled, boolean silent, String message) {
        this.cancelled = cancelled;
        this.silent = silent;
        this.message = message;
    }

    public static RuleResult allow(String newMessage) {
        return new RuleResult(false, false, newMessage);
    }

    public static RuleResult cancel(boolean silent, String message) {
        return new RuleResult(true, silent, message);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isSilent() {
        return silent;
    }

    public String message() {
        return message;
    }
}
