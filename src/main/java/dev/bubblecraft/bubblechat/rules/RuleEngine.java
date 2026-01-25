package dev.bubblecraft.bubblechat.rules;

import dev.bubblecraft.bubblechat.util.SmallCaps;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RuleEngine {

    private final Plugin plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final List<Rule> rules = new ArrayList<>();

    // per-player per-rule cooldowns
    private final ConcurrentMap<String, Long> cooldownUntilEpochMs = new ConcurrentHashMap<>();

    public RuleEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        rules.clear();

        String fileName = plugin.getConfig().getString("rules.file", "rules.yml");
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }

        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        List<?> list = cfg.getList("rules");
        if (list == null) {
            plugin.getLogger().warning("No rules found in " + fileName);
            return;
        }

        for (Object obj : list) {
            if (!(obj instanceof Map<?, ?> map)) continue;
            String id = asString(map.get("id"));
            boolean enabled = asBoolean(map.get("enabled"), true);
            RuleScope scope = RuleScope.parse(asString(map.get("scope")));
            String match = asString(map.get("match"));

            if (id == null || id.isBlank() || scope == null || match == null || match.isBlank()) continue;

            Pattern pattern;
            try {
                pattern = Pattern.compile(match);
            } catch (Exception e) {
                plugin.getLogger().warning("Rule '" + id + "' has invalid regex: " + e.getMessage());
                continue;
            }

            List<RuleAction> actions = new ArrayList<>();
            Object actionsObj = map.get("actions");
            if (actionsObj instanceof List<?> actionList) {
                for (Object a : actionList) {
                    if (!(a instanceof Map<?, ?> am)) continue;
                    RuleActionType type = RuleActionType.parse(asString(am.get("type")));
                    if (type == null) continue;
                    String message = asString(am.get("message"));
                        boolean smallCaps = asBoolean(am.get("smallcaps"), false)
                            || asBoolean(am.get("small-caps"), false)
                            || asBoolean(am.get("small_caps"), false);
                    String replacement = asString(am.get("replacement"));

                    String command = asString(am.get("command"));
                    Long delayTicks = asLong(am.get("delay-ticks"));
                    if (delayTicks == null) delayTicks = asLong(am.get("delayTicks"));

                    Long durationMs = asLong(am.get("duration-ms"));
                    if (durationMs == null) durationMs = asLong(am.get("durationMs"));

                    String key = asString(am.get("key"));

                    actions.add(new RuleAction(type, message, smallCaps, replacement, command, delayTicks, durationMs, key));
                }
            }

            rules.add(new Rule(id, enabled, scope, pattern, actions));
        }

        plugin.getLogger().info("Loaded " + rules.size() + " rules.");
    }

    public RuleResult applyChat(Player player, String message, RuleNotifier notifier) {
        return apply(player, message, RuleScope.CHAT, notifier);
    }

    public RuleResult applyCommand(Player player, String commandLine, RuleNotifier notifier) {
        return apply(player, commandLine, RuleScope.COMMAND, notifier);
    }

    private RuleResult apply(Player player, String input, RuleScope scope, RuleNotifier notifier) {
        if (!plugin.getConfig().getBoolean("rules.enabled", true)) {
            return RuleResult.allow(input);
        }

        String message = input;

        for (Rule rule : rules) {
            if (!rule.enabled() || rule.scope() != scope) continue;

            Matcher matcher = rule.pattern().matcher(message);
            if (!matcher.find()) continue;

            // Cooldowns (pre-check): if any cooldown action blocks, stop early
            for (RuleAction action : rule.actions()) {
                if (action.type() != RuleActionType.COOLDOWN) continue;

                long durationMs = action.durationMs() == null ? 0L : action.durationMs();
                if (durationMs <= 0) continue;

                String key = action.key();
                if (key == null || key.isBlank()) {
                    key = rule.id();
                }
                String cooldownKey = cooldownKey(player.getUniqueId(), key);
                long now = System.currentTimeMillis();
                long until = cooldownUntilEpochMs.getOrDefault(cooldownKey, 0L);
                if (until > now) {
                    long remaining = until - now;
                    if (action.message() != null && !action.message().isBlank()) {
                        String out = formatVars(action.message(), player, message, matcher, remaining);
                        if (action.smallCaps()) out = SmallCaps.toSmallCapsMiniMessage(out);
                        player.sendMessage(mm.deserialize(out));
                    }
                    return RuleResult.cancel(false, message);
                }
                cooldownUntilEpochMs.put(cooldownKey, now + durationMs);
            }

            for (RuleAction action : rule.actions()) {
                if (action.type() == RuleActionType.REPLACE) {
                    String replacement = action.replacement() == null ? "" : action.replacement();
                    message = rule.pattern().matcher(message).replaceAll(replacement);
                }
            }

            boolean cancel = false;
            boolean silent = false;

            for (RuleAction action : rule.actions()) {
                switch (action.type()) {
                    case CANCEL -> {
                        cancel = true;
                        silent = false;
                        if (action.message() != null && !action.message().isBlank()) {
                            String out = formatVars(action.message(), player, message, matcher, null);
                            if (action.smallCaps()) out = SmallCaps.toSmallCapsMiniMessage(out);
                            player.sendMessage(mm.deserialize(out));
                        } else {
                            player.sendMessage(mm.deserialize("<red>Your message was blocked.</red>"));
                        }
                    }
                    case SILENT_CANCEL -> {
                        cancel = true;
                        silent = true;
                        // sender sees nothing special
                    }
                    case REPLY -> {
                        if (action.message() != null && !action.message().isBlank()) {
                            String out = formatVars(action.message(), player, message, matcher, null);
                            if (action.smallCaps()) out = SmallCaps.toSmallCapsMiniMessage(out);
                            player.sendMessage(mm.deserialize(out));
                        }
                    }
                    case RUN_COMMAND_CONSOLE -> {
                        if (action.command() != null && !action.command().isBlank()) {
                            String cmd = formatVars(action.command(), player, message, matcher, null);
                            long delay = action.delayTicks() == null ? 0L : action.delayTicks();
                            if (delay <= 0) {
                                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripLeadingSlash(cmd));
                            } else {
                                Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripLeadingSlash(cmd)), delay);
                            }
                        }
                    }
                    case RUN_COMMAND_PLAYER -> {
                        if (action.command() != null && !action.command().isBlank()) {
                            String cmd = formatVars(action.command(), player, message, matcher, null);
                            long delay = action.delayTicks() == null ? 0L : action.delayTicks();
                            if (delay <= 0) {
                                player.performCommand(stripLeadingSlash(cmd));
                            } else {
                                Bukkit.getScheduler().runTaskLater(plugin, () -> player.performCommand(stripLeadingSlash(cmd)), delay);
                            }
                        }
                    }
                    case COOLDOWN -> {
                        // handled above
                    }
                    case NOTIFY_STAFF -> {
                        if (notifier != null) {
                            notifier.notifyStaff(formatVars(action.message(), player, message, matcher, null));
                        }
                    }
                    case LOG -> {
                        if (notifier != null) {
                            notifier.log(formatVars(action.message(), player, message, matcher, null));
                        }
                    }
                    case REPLACE -> {
                        // already done
                    }
                }
            }

            if (cancel) {
                return RuleResult.cancel(silent, message);
            }
        }

        return RuleResult.allow(message);
    }

    private static String formatVars(String template, Player player, String message, Matcher matcher, Long remainingMs) {
        if (template == null) return "";

        String out = template
                .replace("{player}", player.getName())
                .replace("{uuid}", player.getUniqueId().toString())
                .replace("{message}", message);

        if (remainingMs != null) {
            out = out.replace("{remaining_ms}", String.valueOf(remainingMs));
            out = out.replace("{remaining_s}", String.valueOf(Math.max(0, remainingMs / 1000L)));
        }

        if (matcher != null) {
            int groups = matcher.groupCount();
            for (int i = 1; i <= groups && i <= 9; i++) {
                String g = matcher.group(i);
                if (g == null) g = "";
                out = out.replace("{" + i + "}", g);
            }
        }

        return out;
    }

    private static String asString(Object o) {
        return o instanceof String s ? s : (o == null ? null : String.valueOf(o));
    }

    private static boolean asBoolean(Object o, boolean def) {
        if (o instanceof Boolean b) return b;
        if (o instanceof String s) {
            return s.trim().equalsIgnoreCase("true");
        }
        return def;
    }

    private static Long asLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static String cooldownKey(UUID uuid, String key) {
        return uuid + ":" + key;
    }

    private static String stripLeadingSlash(String cmd) {
        if (cmd == null) return "";
        return cmd.startsWith("/") ? cmd.substring(1) : cmd;
    }

    public interface RuleNotifier {
        void notifyStaff(String message);
        void log(String line);
    }
}
