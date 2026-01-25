package dev.bubblecraft.bubblechat.rules;

public enum RuleActionType {
    CANCEL,
    SILENT_CANCEL,
    REPLACE,
    REPLY,
    RUN_COMMAND_CONSOLE,
    RUN_COMMAND_PLAYER,
    COOLDOWN,
    NOTIFY_STAFF,
    LOG;

    public static RuleActionType parse(String s) {
        if (s == null) return null;
        return switch (s.trim().toLowerCase()) {
            case "cancel" -> CANCEL;
            case "silent_cancel", "silent-cancel", "silentcancel" -> SILENT_CANCEL;
            case "replace" -> REPLACE;
            case "reply", "message" -> REPLY;
            case "run_command_console", "run-command-console", "console_command", "console-command" -> RUN_COMMAND_CONSOLE;
            case "run_command_player", "run-command-player", "player_command", "player-command" -> RUN_COMMAND_PLAYER;
            case "cooldown", "delay" -> COOLDOWN;
            case "notify_staff", "notify-staff", "notifystaff" -> NOTIFY_STAFF;
            case "log" -> LOG;
            default -> null;
        };
    }
}
