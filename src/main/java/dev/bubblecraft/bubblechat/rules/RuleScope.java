package dev.bubblecraft.bubblechat.rules;

public enum RuleScope {
    CHAT,
    COMMAND;

    public static RuleScope parse(String s) {
        if (s == null) return null;
        return switch (s.trim().toLowerCase()) {
            case "chat" -> CHAT;
            case "command", "commands" -> COMMAND;
            default -> null;
        };
    }
}
