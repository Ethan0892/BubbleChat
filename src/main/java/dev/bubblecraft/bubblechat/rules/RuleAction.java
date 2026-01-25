package dev.bubblecraft.bubblechat.rules;

public record RuleAction(
        RuleActionType type,
        String message,
        boolean smallCaps,
        String replacement,
        String command,
        Long delayTicks,
        Long durationMs,
        String key
) {
}
