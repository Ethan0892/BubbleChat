package dev.bubblecraft.bubblechat.data;

public enum ChatChannel {
    GLOBAL,
    LOCAL,
    STAFF;

    public static ChatChannel parse(String input) {
        if (input == null) return null;
        return switch (input.trim().toLowerCase()) {
            case "g", "global" -> GLOBAL;
            case "l", "local" -> LOCAL;
            case "s", "staff" -> STAFF;
            default -> null;
        };
    }
}
