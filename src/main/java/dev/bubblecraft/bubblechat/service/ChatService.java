package dev.bubblecraft.bubblechat.service;

import dev.bubblecraft.bubblechat.data.ChatChannel;
import dev.bubblecraft.bubblechat.data.PlayerData;
import dev.bubblecraft.bubblechat.data.PlayerDataStore;
import dev.bubblecraft.bubblechat.hook.ChatColor2Hook;
import dev.bubblecraft.bubblechat.hook.DiscordSrvHook;
import dev.bubblecraft.bubblechat.hook.PlaceholderApiHook;
import dev.bubblecraft.bubblechat.rules.RuleEngine;
import dev.bubblecraft.bubblechat.rules.RuleResult;
import dev.bubblecraft.bubblechat.rules.RuleEngine.RuleNotifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.Title.Times;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.reflect.Method;
import java.util.regex.Pattern;

public final class ChatService {

    private final Plugin plugin;
    private final PlayerDataStore dataStore;
    private final PlaceholderApiHook placeholderApiHook;
    private final ChatColor2Hook chatColor2Hook;
    private final DiscordSrvHook discordSrvHook;
    private final RuleEngine ruleEngine;

    private final RuleNotifier ruleNotifier;

    private int autosaveTaskId = -1;

    private final Map<UUID, TrackedChatMessage> trackedChatMessages = new ConcurrentHashMap<>();

    private final MiniMessage mm = MiniMessage.miniMessage();

    public ChatService(Plugin plugin, PlayerDataStore dataStore, PlaceholderApiHook placeholderApiHook, ChatColor2Hook chatColor2Hook, DiscordSrvHook discordSrvHook, RuleEngine ruleEngine) {
        this.plugin = plugin;
        this.dataStore = dataStore;
        this.placeholderApiHook = placeholderApiHook;
        this.chatColor2Hook = chatColor2Hook;
        this.discordSrvHook = discordSrvHook;
        this.ruleEngine = ruleEngine;

        this.ruleNotifier = new RuleNotifier() {
            @Override
            public void notifyStaff(String message) {
                if (message == null || message.isBlank()) return;
                if (!plugin.getConfig().getBoolean("rules.staff-alerts-enabled", true)) return;

                Component c = mm.deserialize(message);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("bubblechat.alerts")) {
                        p.sendMessage(c);
                    }
                }

                if (discordSrvHook != null) {
                    // strip MiniMessage tags for Discord; keep it readable
                    String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(c);
                    ChatService.this.discordSrvHook.sendAlert(plain);
                }
            }

            @Override
            public void log(String line) {
                if (line == null || line.isBlank()) return;
                logRulesLine(line);
            }
        };
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public boolean isRadiusMode() {
        String mode = plugin.getConfig().getString("chat.mode", "channels");
        return mode != null && mode.equalsIgnoreCase("radius");
    }

    public void setRadiusGlobalMode(Player player, boolean global) {
        // In radius mode, "global" maps to shout mode.
        data(player).setShoutMode(global);
    }

    public boolean isAnnouncementsOptOutEnabled() {
        return plugin.getConfig().getBoolean("announcements.opt-out.enabled", true);
    }

    public void sendMotd(Player player) {
        if (player == null) return;
        if (!plugin.getConfig().getBoolean("announcements.enabled", true)) return;

        // MOTD is intentionally NOT blocked by announcements opt-out.
        // (Opt-out is for automatic join messages. /motd is user-initiated.)
        if (!plugin.getConfig().getBoolean("announcements.join.motd.enabled", true)) {
            player.sendMessage(mm.deserialize("<gray>MOTD is disabled.</gray>"));
            return;
        }

        for (String line : plugin.getConfig().getStringList("announcements.join.motd.messages")) {
            if (line != null && !line.isBlank()) player.sendMessage(mm.deserialize(line));
        }
    }

    public void clearChat(boolean silent, String clearedBy) {
        int lines = plugin.getConfig().getInt("chat.clear.lines", 150);
        if (lines <= 0) lines = 150;

        Component blank = Component.text(" ");
        for (Player p : Bukkit.getOnlinePlayers()) {
            for (int i = 0; i < lines; i++) {
                p.sendMessage(blank);
            }
        }

        if (!silent && plugin.getConfig().getBoolean("chat.clear.announce", true)) {
            String msg = plugin.getConfig().getString("chat.clear.message", "<gray>Chat was cleared.</gray>");
            if (msg != null && !msg.isBlank()) {
                Component announce = mm.deserialize(msg.replace("{by}", clearedBy == null ? "" : clearedBy));
                Bukkit.broadcast(announce);
            }
        }
    }

    public void handleJoin(Player player, boolean firstJoin) {
        if (!plugin.getConfig().getBoolean("announcements.enabled", true)) return;

        PlayerData pd = data(player);
        if (isAnnouncementsOptOutEnabled() && pd.isAnnouncementsOptOut()) return;

        long now = System.currentTimeMillis();
        long firstPlayed = player.getFirstPlayed();

        boolean newcomerEnabled = plugin.getConfig().getBoolean("announcements.join.motd.newcomer.enabled", true);
        int newcomerDays = plugin.getConfig().getInt("announcements.join.motd.newcomer.days", 7);
        boolean newcomer = newcomerEnabled && firstPlayed > 0 && (now - firstPlayed) <= (long) newcomerDays * 86_400_000L;

        // MOTD messages
        if (plugin.getConfig().getBoolean("announcements.join.motd.enabled", true)) {
            if (firstJoin) {
                for (String line : plugin.getConfig().getStringList("announcements.join.motd.first-join-messages")) {
                    if (line != null && !line.isBlank()) player.sendMessage(mm.deserialize(line));
                }
            } else if (newcomer) {
                for (String line : plugin.getConfig().getStringList("announcements.join.motd.newcomer.messages")) {
                    if (line != null && !line.isBlank()) player.sendMessage(mm.deserialize(line));
                }
            } else {
                for (String line : plugin.getConfig().getStringList("announcements.join.motd.messages")) {
                    if (line != null && !line.isBlank()) player.sendMessage(mm.deserialize(line));
                }
            }
        }

        // Title
        if (plugin.getConfig().getBoolean("announcements.join.title.enabled", false)) {
            Component title = mm.deserialize(plugin.getConfig().getString("announcements.join.title.title", ""));
            Component subtitle = mm.deserialize(plugin.getConfig().getString("announcements.join.title.subtitle", ""));
            int fadeIn = plugin.getConfig().getInt("announcements.join.title.fade-in-ticks", 10);
            int stay = plugin.getConfig().getInt("announcements.join.title.stay-ticks", 60);
            int fadeOut = plugin.getConfig().getInt("announcements.join.title.fade-out-ticks", 10);
            player.showTitle(Title.title(title, subtitle, Times.times(Duration.ofMillis(fadeIn * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(fadeOut * 50L))));
        }

        // Actionbar
        if (plugin.getConfig().getBoolean("announcements.join.actionbar.enabled", false)) {
            String msg = plugin.getConfig().getString("announcements.join.actionbar.message", "");
            if (msg != null && !msg.isBlank()) {
                player.sendActionBar(mm.deserialize(msg));
            }
        }

        // Sound
        if (plugin.getConfig().getBoolean("announcements.join.sound.enabled", false)) {
            String key = plugin.getConfig().getString("announcements.join.sound.key", "minecraft:entity.player.levelup");
            float vol = (float) plugin.getConfig().getDouble("announcements.join.sound.volume", 1.0);
            float pitch = (float) plugin.getConfig().getDouble("announcements.join.sound.pitch", 1.0);
            try {
                player.playSound(player.getLocation(), parseBukkitSound(key), vol, pitch);
            } catch (Exception ignored) {
            }
        }

        // Run commands
        if (plugin.getConfig().getBoolean("announcements.join.run-commands.enabled", false)) {
            for (String cmd : plugin.getConfig().getStringList("announcements.join.run-commands.console")) {
                if (cmd == null || cmd.isBlank()) continue;
                String resolved = cmd.replace("{player}", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), stripLeadingSlash(resolved));
            }
            for (String cmd : plugin.getConfig().getStringList("announcements.join.run-commands.player")) {
                if (cmd == null || cmd.isBlank()) continue;
                String resolved = cmd.replace("{player}", player.getName());
                player.performCommand(stripLeadingSlash(resolved));
            }
        }
    }

    public void handleJoinMessage(PlayerJoinEvent event) {
        if (event == null) return;

        if (!plugin.getConfig().getBoolean("messages.join.enabled", true)) return;
        if (plugin.getConfig().getBoolean("moderation.global-chat-muted", false)
                && plugin.getConfig().getBoolean("moderation.mute-hides-system-messages", false)) {
            event.joinMessage(null);
            return;
        }

        Player player = event.getPlayer();
        String format = plugin.getConfig().getString("messages.join.format", "<green>+ <white><player></white></green>");
        Component msg = renderSystemTemplate(player, format, null);

        // suppress vanilla join message
        event.joinMessage(null);
        broadcastSystem(msg, recipient -> !data(recipient).isHideJoinMessages());
    }

    public void handleQuit(PlayerQuitEvent event) {
        if (event == null) return;

        if (!plugin.getConfig().getBoolean("messages.quit.enabled", true)) return;
        if (plugin.getConfig().getBoolean("moderation.global-chat-muted", false)
                && plugin.getConfig().getBoolean("moderation.mute-hides-system-messages", false)) {
            event.quitMessage(null);
            return;
        }

        Player player = event.getPlayer();
        String format = plugin.getConfig().getString("messages.quit.format", "<red>- <white><player></white></red>");
        Component msg = renderSystemTemplate(player, format, null);

        event.quitMessage(null);
        broadcastSystem(msg, recipient -> !data(recipient).isHideQuitMessages());
    }

    public void handleDeath(PlayerDeathEvent event) {
        if (event == null) return;
        if (event.getEntity() == null) return;

        if (!plugin.getConfig().getBoolean("messages.death.enabled", true)) return;
        if (plugin.getConfig().getBoolean("moderation.global-chat-muted", false)
                && plugin.getConfig().getBoolean("moderation.mute-hides-system-messages", false)) {
            event.deathMessage(null);
            return;
        }

        Player player = event.getEntity();

        Component vanilla = event.deathMessage();
        boolean useVanilla = plugin.getConfig().getBoolean("messages.death.use-vanilla", true);
        Component inner = (useVanilla && vanilla != null) ? vanilla : Component.text(player.getName() + " died.");

        String format = plugin.getConfig().getString("messages.death.format", "<dark_red>☠</dark_red> <message>");
        Component msg = renderSystemTemplate(player, format, inner);

        event.deathMessage(null);
        broadcastSystem(msg, recipient -> !data(recipient).isHideDeathMessages());
    }

    public void sendMe(Player sender, String messagePlain) {
        if (sender == null) return;
        if (!plugin.getConfig().getBoolean("chat.me.enabled", true)) {
            sender.sendMessage(mm.deserialize("<gray>/me is disabled.</gray>"));
            return;
        }

        String format = plugin.getConfig().getString("chat.me.format", "<gray>*</gray> <white><player></white> <gray><message></gray>");
        format = applyPlaceholders(sender, format);

        Component msgComponent = renderPlayerMessage(sender, messagePlain);
        Component rendered = mm.deserialize(format, TagResolver.resolver(
                Placeholder.unparsed("player", sender.getName()),
                Placeholder.component("displayname", sender.displayName()),
                Placeholder.component("message", msgComponent)
        ));

        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (isIgnoring(recipient, sender.getUniqueId())) continue;
            recipient.sendMessage(rendered);
        }
        logChat(ChatChannel.GLOBAL, sender, "* " + messagePlain);
    }

    private void broadcastSystem(Component msg, java.util.function.Predicate<Player> canReceive) {
        if (msg == null) return;
        for (Player recipient : Bukkit.getOnlinePlayers()) {
            if (canReceive != null && !canReceive.test(recipient)) continue;
            recipient.sendMessage(msg);
        }
    }

    private Component renderSystemTemplate(Player subject, String template, Component message) {
        if (template == null) template = "";
        if (subject != null) {
            template = applyPlaceholders(subject, template);
        }

        TagResolver resolver = TagResolver.resolver(
                Placeholder.unparsed("player", subject == null ? "" : subject.getName()),
                Placeholder.component("displayname", subject == null ? Component.empty() : subject.displayName()),
                Placeholder.component("message", message == null ? Component.empty() : message)
        );

        return mm.deserialize(template, resolver);
    }

    private static Sound parseBukkitSound(String input) {
        if (input == null) return Sound.ENTITY_PLAYER_LEVELUP;
        String s = input.toUpperCase(Locale.ROOT).replace(':', '_').replace('.', '_');
        return Sound.valueOf(s);
    }

    private static String stripLeadingSlash(String cmd) {
        if (cmd == null) return "";
        return cmd.startsWith("/") ? cmd.substring(1) : cmd;
    }

    public PlayerData data(Player player) {
        return dataStore.get(player.getUniqueId());
    }

    public PlayerData data(UUID uuid) {
        return dataStore.get(uuid);
    }

    public void reload() {
        stopAutosaveTask();
        plugin.reloadConfig();
        if (ruleEngine != null) {
            ruleEngine.load();
        }
        startAutosaveTask();
    }

    public void startAutosaveTask() {
        int seconds = plugin.getConfig().getInt("storage.autosave-seconds", 60);
        if (seconds <= 0) return;
        autosaveTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            dataStore.save();
            cleanupTrackedMessages();
        }, seconds * 20L, seconds * 20L);
    }

    public void stopAutosaveTask() {
        if (autosaveTaskId != -1) {
            Bukkit.getScheduler().cancelTask(autosaveTaskId);
            autosaveTaskId = -1;
        }
    }

    public void handleChat(Player sender, Component originalComponent) {
        long now = System.currentTimeMillis();

        PlayerData pd = data(sender);

        if (pd.isMutedNow(now) && !sender.hasPermission("bubblechat.bypass.mute")) {
            long remainingMs = pd.getMutedUntilEpochMs() - now;
            sender.sendMessage(mm.deserialize("<red>You are muted for another " + formatDuration(remainingMs) + ".</red>"));
            if (pd.getMuteReason() != null && !pd.getMuteReason().isBlank()) {
                sender.sendMessage(mm.deserialize("<gray>Reason: " + escapeMini(pd.getMuteReason()) + "</gray>"));
            }
            return;
        }

        if (plugin.getConfig().getBoolean("moderation.global-chat-muted", false)
                && !sender.hasPermission("bubblechat.bypass.chatmute")) {
            sender.sendMessage(mm.deserialize("<red>Chat is currently muted.</red>"));
            return;
        }

        String plain = PlainTextComponentSerializer.plainText().serialize(originalComponent);

        if (plugin.getConfig().getBoolean("chat.message.strip-legacy-section-colors", true)) {
            plain = TextSanitizer.stripLegacySectionColors(plain);
        }

        int maxLen = plugin.getConfig().getInt("chat.message.max-length", 256);
        if (maxLen > 0 && plain.length() > maxLen) {
            sender.sendMessage(mm.deserialize("<red>Your message is too long (max " + maxLen + ").</red>"));
            return;
        }

        // Anti-spam
        if (plugin.getConfig().getBoolean("anti-spam.enabled", true) && !sender.hasPermission("bubblechat.bypass.spam")) {
            long cooldownMs = plugin.getConfig().getLong("anti-spam.cooldown-ms", 1100);
            if (cooldownMs > 0 && (now - pd.getLastChatAtEpochMs()) < cooldownMs) {
                sender.sendMessage(mm.deserialize("<red>Please slow down.</red>"));
                return;
            }

            String normalized = TextSanitizer.normalizeForDuplicateCheck(plain);
            long dupWindow = plugin.getConfig().getLong("anti-spam.duplicate-window-ms", 6000);
            int maxDuplicates = plugin.getConfig().getInt("anti-spam.max-duplicates", 1);

            if (pd.getLastChatNormalized() != null
                    && normalized.equals(pd.getLastChatNormalized())
                    && (now - pd.getLastChatAtEpochMs()) <= dupWindow) {
                pd.setDuplicateCount(pd.getDuplicateCount() + 1);
                if (pd.getDuplicateCount() > maxDuplicates) {
                    sender.sendMessage(mm.deserialize("<red>Please do not repeat messages.</red>"));
                    return;
                }
            } else {
                pd.setDuplicateCount(0);
                pd.setLastChatNormalized(normalized);
            }

            // Caps filter
            if (plugin.getConfig().getBoolean("anti-spam.caps.enabled", true)) {
                int minLen = plugin.getConfig().getInt("anti-spam.caps.min-length", 8);
                double maxRatio = plugin.getConfig().getDouble("anti-spam.caps.max-ratio", 0.65);
                if (plain.length() >= minLen) {
                    double ratio = uppercaseRatio(plain);
                    if (ratio > maxRatio) {
                        plain = plain.toLowerCase(Locale.ROOT);
                    }
                }
            }
        }

        pd.setLastChatAtEpochMs(now);

        // Filters
        if (plugin.getConfig().getBoolean("filters.enabled", true) && !sender.hasPermission("bubblechat.bypass.filter")) {
            plain = applyFilters(sender, plain);
            if (plain == null) {
                // blocked
                return;
            }
        }

        String mode = plugin.getConfig().getString("chat.mode", "channels");
        if (mode != null && mode.equalsIgnoreCase("radius")) {
            handleRadiusChat(sender, plain);
            return;
        }

        ChatChannel channel = pd.getChannel();
        if (channel == ChatChannel.STAFF && !sender.hasPermission("bubblechat.channel.staff")) {
            channel = ChatChannel.GLOBAL;
            pd.setChannel(channel);
        }

        // Rules (chat) for channel mode
        if (ruleEngine != null) {
            RuleResult rr = ruleEngine.applyChat(sender, plain, ruleNotifier);
            if (rr.isCancelled()) {
                if (rr.isSilent()) {
                    sender.sendMessage(renderChatMessage(sender, channel, rr.message()));
                }
                return;
            }
            plain = rr.message();
        }

        List<Player> recipients = computeRecipients(sender, channel);

        Component renderedMessage = renderChatMessage(sender, channel, plain);

        for (Player recipient : recipients) {
            if (isIgnoring(recipient, sender.getUniqueId())) continue;
            recipient.sendMessage(renderedMessage);
        }

        logChat(channel, sender, plain);

        // Mentions (best effort)
        if (plugin.getConfig().getBoolean("mentions.enabled", true)) {
            notifyMentions(sender, recipients, plain);
        }
    }

    public record ChatPlan(boolean cancelled, Set<UUID> viewerUuids, Component renderedBase, UUID messageId,
                           String removePermission, String removeButtonText) {
        public Component renderedFor(Player viewer) {
            if (viewer == null) return renderedBase;
            if (messageId == null) return renderedBase;

            String perm = (removePermission == null || removePermission.isBlank())
                    ? "bubblechat.moderation.remove"
                    : removePermission;
            if (!viewer.hasPermission(perm)) return renderedBase;

            String buttonText = (removeButtonText == null || removeButtonText.isBlank())
                    ? "[x]"
                    : removeButtonText;

            Component button = Component.text(" " + buttonText, NamedTextColor.DARK_RED)
                    .hoverEvent(HoverEvent.showText(Component.text("Remove message", NamedTextColor.RED)))
                    .clickEvent(ClickEvent.runCommand("/bchat remove " + messageId));
            return renderedBase.append(button);
        }
    }

    public ChatPlan planChat(Player sender, Component originalComponent, Object signedMessage) {
        // Main-thread only. Mirrors handleChat(), but returns a plan instead of broadcasting directly.
        String removePerm = plugin.getConfig().getString("chat.remove.permission", "bubblechat.moderation.remove");
        if (removePerm == null || removePerm.isBlank()) removePerm = "bubblechat.moderation.remove";
        String removeButton = plugin.getConfig().getString("chat.remove.button", "[x]");
        if (removeButton == null || removeButton.isBlank()) removeButton = "[x]";

        long now = System.currentTimeMillis();

        PlayerData pd = data(sender);

        if (pd.isMutedNow(now) && !sender.hasPermission("bubblechat.bypass.mute")) {
            long remainingMs = pd.getMutedUntilEpochMs() - now;
            sender.sendMessage(mm.deserialize("<red>You are muted for another " + formatDuration(remainingMs) + ".</red>"));
            if (pd.getMuteReason() != null && !pd.getMuteReason().isBlank()) {
                sender.sendMessage(mm.deserialize("<gray>Reason: " + escapeMini(pd.getMuteReason()) + "</gray>"));
            }
            return new ChatPlan(true, Set.of(), Component.empty(), null, removePerm, removeButton);
        }

        if (plugin.getConfig().getBoolean("moderation.global-chat-muted", false)
                && !sender.hasPermission("bubblechat.bypass.chatmute")) {
            sender.sendMessage(mm.deserialize("<red>Chat is currently muted.</red>"));
            return new ChatPlan(true, Set.of(), Component.empty(), null, removePerm, removeButton);
        }

        String plain = PlainTextComponentSerializer.plainText().serialize(originalComponent);

        if (plugin.getConfig().getBoolean("chat.message.strip-legacy-section-colors", true)) {
            plain = TextSanitizer.stripLegacySectionColors(plain);
        }

        int maxLen = plugin.getConfig().getInt("chat.message.max-length", 256);
        if (maxLen > 0 && plain.length() > maxLen) {
            sender.sendMessage(mm.deserialize("<red>Your message is too long (max " + maxLen + ").</red>"));
            return new ChatPlan(true, Set.of(), Component.empty(), null, removePerm, removeButton);
        }

        // Anti-spam
        if (plugin.getConfig().getBoolean("anti-spam.enabled", true) && !sender.hasPermission("bubblechat.bypass.spam")) {
            long cooldownMs = plugin.getConfig().getLong("anti-spam.cooldown-ms", 1100);
            if (cooldownMs > 0 && (now - pd.getLastChatAtEpochMs()) < cooldownMs) {
                sender.sendMessage(mm.deserialize("<red>Please slow down.</red>"));
                return new ChatPlan(true, Set.of(), Component.empty(), null, removePerm, removeButton);
            }

            String normalized = TextSanitizer.normalizeForDuplicateCheck(plain);
            long dupWindow = plugin.getConfig().getLong("anti-spam.duplicate-window-ms", 6000);
            int maxDuplicates = plugin.getConfig().getInt("anti-spam.max-duplicates", 1);

            if (pd.getLastChatNormalized() != null
                    && normalized.equals(pd.getLastChatNormalized())
                    && (now - pd.getLastChatAtEpochMs()) <= dupWindow) {
                pd.setDuplicateCount(pd.getDuplicateCount() + 1);
                if (pd.getDuplicateCount() > maxDuplicates) {
                    sender.sendMessage(mm.deserialize("<red>Please do not repeat messages.</red>"));
                    return new ChatPlan(true, Set.of(), Component.empty(), null, removePerm, removeButton);
                }
            } else {
                pd.setDuplicateCount(0);
                pd.setLastChatNormalized(normalized);
            }

            if (plugin.getConfig().getBoolean("anti-spam.caps.enabled", true)) {
                int minLen2 = plugin.getConfig().getInt("anti-spam.caps.min-length", 8);
                double maxRatio = plugin.getConfig().getDouble("anti-spam.caps.max-ratio", 0.65);
                if (plain.length() >= minLen2) {
                    double ratio = uppercaseRatio(plain);
                    if (ratio > maxRatio) {
                        plain = plain.toLowerCase(Locale.ROOT);
                    }
                }
            }
        }

        pd.setLastChatAtEpochMs(now);

        // Filters
        if (plugin.getConfig().getBoolean("filters.enabled", true) && !sender.hasPermission("bubblechat.bypass.filter")) {
            plain = applyFilters(sender, plain);
            if (plain == null) {
                return new ChatPlan(true, Set.of(), Component.empty(), null, removePerm, removeButton);
            }
        }

        String mode = plugin.getConfig().getString("chat.mode", "channels");
        if (mode != null && mode.equalsIgnoreCase("radius")) {
            return planRadius(sender, plain, signedMessage, removePerm, removeButton);
        }

        ChatChannel channel = pd.getChannel();
        if (channel == ChatChannel.STAFF && !sender.hasPermission("bubblechat.channel.staff")) {
            channel = ChatChannel.GLOBAL;
            pd.setChannel(channel);
        }

        if (ruleEngine != null) {
            RuleResult rr = ruleEngine.applyChat(sender, plain, ruleNotifier);
            if (rr.isCancelled()) {
                if (rr.isSilent()) {
                    sender.sendMessage(renderChatMessage(sender, channel, rr.message()));
                }
                return new ChatPlan(true, Set.of(), Component.empty(), null, removePerm, removeButton);
            }
            plain = rr.message();
        }

        List<Player> recipients = computeRecipients(sender, channel);
        Set<UUID> allowed = new HashSet<>();
        for (Player recipient : recipients) {
            if (isIgnoring(recipient, sender.getUniqueId())) continue;
            allowed.add(recipient.getUniqueId());
        }

        Component rendered = renderChatMessage(sender, channel, plain);
        logChat(channel, sender, plain);
        if (plugin.getConfig().getBoolean("mentions.enabled", true)) {
            notifyMentions(sender, recipients, plain);
        }

        UUID id = maybeTrackDeletableMessage(sender, signedMessage);
        return new ChatPlan(false, allowed, rendered, id, removePerm, removeButton);
    }

    private ChatPlan planRadius(Player sender, String messagePlain, Object signedMessage, String removePerm, String removeButton) {
        PlayerData pd = data(sender);

        boolean questionEnabled = plugin.getConfig().getBoolean("chat.question-enabled", true);
        boolean canShout = sender.hasPermission("bubblechat.chat.shout");
        boolean canQuestion = sender.hasPermission("bubblechat.chat.question");

        String trimmed = messagePlain == null ? "" : messagePlain;
        ChatType type = ChatType.NORMAL;

        if (pd.isShoutMode()) {
            type = ChatType.SHOUT;
        }

        if (!trimmed.isEmpty()) {
            char first = trimmed.charAt(0);
            if (first == '!' && canShout) {
                type = ChatType.SHOUT;
                trimmed = trimmed.substring(1).stripLeading();
            } else if (first == '?' && questionEnabled && canQuestion) {
                type = ChatType.QUESTION;
                trimmed = trimmed.substring(1).stripLeading();
            }
        }

        if (ruleEngine != null) {
            RuleResult rr = ruleEngine.applyChat(sender, trimmed, ruleNotifier);
            if (rr.isCancelled()) {
                if (rr.isSilent()) {
                    sender.sendMessage(renderRadiusChatMessage(sender, type, rr.message()));
                }
                return new ChatPlan(true, Set.of(), Component.empty(), null, removePerm, removeButton);
            }
            trimmed = rr.message();
        }

        int radius = plugin.getConfig().getInt("chat.radius", 0);
        boolean global = radius <= 0 || type == ChatType.SHOUT || type == ChatType.QUESTION;

        List<Player> recipients = new ArrayList<>();
        if (global) {
            recipients.addAll(Bukkit.getOnlinePlayers());
        } else {
            Location origin = sender.getLocation();
            double maxDistSq = (double) radius * (double) radius;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getWorld().equals(sender.getWorld())) continue;
                if (p.getLocation().distanceSquared(origin) <= maxDistSq) {
                    recipients.add(p);
                }
            }

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.hasPermission("bubblechat.chat.spy")) continue;
                if (!recipients.contains(p)) recipients.add(p);
            }
        }

        Set<UUID> allowed = new HashSet<>();
        for (Player recipient : recipients) {
            if (isIgnoring(recipient, sender.getUniqueId())) continue;
            allowed.add(recipient.getUniqueId());
        }

        Component rendered = renderRadiusChatMessage(sender, type, trimmed);
        logChat(ChatChannel.GLOBAL, sender, trimmed);

        if (plugin.getConfig().getBoolean("mentions.enabled", true)) {
            notifyMentions(sender, recipients, trimmed);
        }

        UUID id = maybeTrackDeletableMessage(sender, signedMessage);
        return new ChatPlan(false, allowed, rendered, id, removePerm, removeButton);
    }

    private UUID maybeTrackDeletableMessage(Player sender, Object signedMessage) {
        if (!plugin.getConfig().getBoolean("chat.remove.enabled", true)) return null;
        if (signedMessage == null) return null;
        if (!isDeleteMessageSupported(sender, signedMessage)) return null;

        UUID id = UUID.randomUUID();
        long ttlMs = plugin.getConfig().getLong("chat.remove.ttl-ms", 180000);
        trackedChatMessages.put(id, new TrackedChatMessage(signedMessage, System.currentTimeMillis(), ttlMs));
        return id;
    }

    public boolean removeTrackedMessage(Player actor, UUID messageId) {
        if (actor == null) return false;
        String perm = plugin.getConfig().getString("chat.remove.permission", "bubblechat.moderation.remove");
        if (perm == null || perm.isBlank()) perm = "bubblechat.moderation.remove";
        if (!actor.hasPermission(perm)) {
            actor.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return false;
        }

        TrackedChatMessage tracked = trackedChatMessages.get(messageId);
        if (tracked == null || tracked.isExpired()) {
            trackedChatMessages.remove(messageId);
            actor.sendMessage(mm.deserialize("<red>That message is no longer removable.</red>"));
            return false;
        }

        boolean any = false;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (deleteMessageReflective(p, tracked.signedMessage)) {
                any = true;
            }
        }

        if (!any) {
            actor.sendMessage(mm.deserialize("<red>This server build does not support deleting chat messages.</red>"));
            return false;
        }

        trackedChatMessages.remove(messageId);
        actor.sendMessage(mm.deserialize("<green>Message removed.</green>"));

        if (plugin.getConfig().getBoolean("chat.remove.announce", false)) {
            Bukkit.broadcast(mm.deserialize("<gray>A message was removed by <white>" + escapeMini(actor.getName()) + "</white>.</gray>"));
        }
        return true;
    }

    private boolean isDeleteMessageSupported(Player sample, Object signedMessage) {
        if (sample == null || signedMessage == null) return false;
        for (Method m : sample.getClass().getMethods()) {
            if (!m.getName().equals("deleteMessage")) continue;
            if (m.getParameterCount() != 1) continue;
            if (m.getParameterTypes()[0].isInstance(signedMessage)) return true;
        }
        return false;
    }

    private boolean deleteMessageReflective(Player player, Object signedMessage) {
        if (player == null || signedMessage == null) return false;
        try {
            for (Method m : player.getClass().getMethods()) {
                if (!m.getName().equals("deleteMessage")) continue;
                if (m.getParameterCount() != 1) continue;
                if (!m.getParameterTypes()[0].isInstance(signedMessage)) continue;
                m.invoke(player, signedMessage);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private void cleanupTrackedMessages() {
        if (trackedChatMessages.isEmpty()) return;
        for (Map.Entry<UUID, TrackedChatMessage> e : trackedChatMessages.entrySet()) {
            if (e.getValue() == null || e.getValue().isExpired()) {
                trackedChatMessages.remove(e.getKey());
            }
        }
    }

    private static final class TrackedChatMessage {
        private final Object signedMessage;
        private final long createdAt;
        private final long ttlMs;

        private TrackedChatMessage(Object signedMessage, long createdAt, long ttlMs) {
            this.signedMessage = signedMessage;
            this.createdAt = createdAt;
            this.ttlMs = ttlMs;
        }

        private boolean isExpired() {
            long ttl = Math.max(0, ttlMs);
            if (ttl == 0) return false;
            return (System.currentTimeMillis() - createdAt) > ttl;
        }
    }

    private void handleRadiusChat(Player sender, String messagePlain) {
        PlayerData pd = data(sender);

        boolean questionEnabled = plugin.getConfig().getBoolean("chat.question-enabled", true);
        boolean canShout = sender.hasPermission("bubblechat.chat.shout");
        boolean canQuestion = sender.hasPermission("bubblechat.chat.question");

        String trimmed = messagePlain == null ? "" : messagePlain;
        ChatType type = ChatType.NORMAL;

        boolean shoutMode = pd.isShoutMode();
        if (shoutMode) {
            type = ChatType.SHOUT;
        }

        if (!trimmed.isEmpty()) {
            char first = trimmed.charAt(0);
            if (first == '!' && canShout) {
                type = ChatType.SHOUT;
                trimmed = trimmed.substring(1).stripLeading();
            } else if (first == '?' && questionEnabled && canQuestion) {
                type = ChatType.QUESTION;
                trimmed = trimmed.substring(1).stripLeading();
            }
        }

        // Rules (chat) for radius mode, applied after shout/question prefixes are processed
        if (ruleEngine != null) {
            RuleResult rr = ruleEngine.applyChat(sender, trimmed, ruleNotifier);
            if (rr.isCancelled()) {
                if (rr.isSilent()) {
                    sender.sendMessage(renderRadiusChatMessage(sender, type, rr.message()));
                }
                return;
            }
            trimmed = rr.message();
        }

        int radius = plugin.getConfig().getInt("chat.radius", 0);
        boolean global = radius <= 0 || type == ChatType.SHOUT || type == ChatType.QUESTION;

        List<Player> recipients = new ArrayList<>();
        if (global) {
            recipients.addAll(Bukkit.getOnlinePlayers());
        } else {
            Location origin = sender.getLocation();
            double maxDistSq = (double) radius * (double) radius;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.getWorld().equals(sender.getWorld())) continue;
                if (p.getLocation().distanceSquared(origin) <= maxDistSq) {
                    recipients.add(p);
                }
            }

            // chat spy: see local chat regardless of radius
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.hasPermission("bubblechat.chat.spy")) continue;
                if (!recipients.contains(p)) recipients.add(p);
            }
        }

        Component rendered = renderRadiusChatMessage(sender, type, trimmed);

        for (Player recipient : recipients) {
            if (isIgnoring(recipient, sender.getUniqueId())) continue;
            recipient.sendMessage(rendered);
        }

        logChat(ChatChannel.GLOBAL, sender, trimmed);

        if (plugin.getConfig().getBoolean("mentions.enabled", true)) {
            notifyMentions(sender, recipients, trimmed);
        }
    }

    private Component renderRadiusChatMessage(Player sender, ChatType type, String messagePlain) {
        boolean useMini = plugin.getConfig().getBoolean("chat.radius-format-minimessage", false);

        String fmt = resolveRadiusFormat(sender, type);
        String fmtResolved = applyRadiusPlaceholders(sender, fmt);

        Component messageComponent = renderPlayerMessage(sender, messagePlain);

        if (!useMini) {
            String marker = "{MESSAGE}";
            int idx = fmtResolved.indexOf(marker);
            if (idx < 0) {
                // No marker, just append message.
                return legacy().deserialize(fmtResolved).append(Component.space()).append(messageComponent);
            }

            String left = fmtResolved.substring(0, idx);
            String right = fmtResolved.substring(idx + marker.length());

            return legacy().deserialize(left).append(messageComponent).append(legacy().deserialize(right));
        }

        // MiniMessage mode: allow either <message> placeholders or {MESSAGE} style.
        String miniTemplate = convertEssentialsTokensToMini(fmtResolved);

        String worldName = sender.getWorld().getName();
        String worldAlias = plugin.getConfig().getString("chat.world-aliases." + worldName, worldName);

        String prefix = placeholderApiHook == null ? "" : placeholderApiHook.apply(sender, "%vault_prefix%");
        String suffix = placeholderApiHook == null ? "" : placeholderApiHook.apply(sender, "%vault_suffix%");
        String group = resolvePrimaryGroupBestEffort(sender);

        TagResolver resolver = TagResolver.resolver(
                Placeholder.unparsed("username", sender.getName()),
                Placeholder.unparsed("worldname", worldName),
                Placeholder.unparsed("shortworldname", worldName.isEmpty() ? "" : String.valueOf(worldName.charAt(0))),
                Placeholder.component("displayname", sender.displayName()),
                Placeholder.component("nickname", sender.displayName()),
                Placeholder.component("world", legacy().deserialize(worldAlias == null ? "" : worldAlias)),
                Placeholder.unparsed("group", group),
                Placeholder.component("prefix", legacy().deserialize(safePlaceholder(prefix))),
                Placeholder.component("suffix", legacy().deserialize(safePlaceholder(suffix))),
                Placeholder.component("message", messageComponent)
        );

        // Best-effort PAPI inside format too.
        if (placeholderApiHook != null) {
            miniTemplate = placeholderApiHook.apply(sender, miniTemplate);
        }

        return mm.deserialize(miniTemplate, resolver);
    }

    private static String convertEssentialsTokensToMini(String input) {
        if (input == null) return "";
        return input
                .replace("{MESSAGE}", "<message>")
                .replace("{USERNAME}", "<username>")
                .replace("{DISPLAYNAME}", "<displayname>")
                .replace("{NICKNAME}", "<nickname>")
                .replace("{PREFIX}", "<prefix>")
                .replace("{SUFFIX}", "<suffix>")
                .replace("{GROUP}", "<group>")
                .replace("{WORLD}", "<world>")
                .replace("{WORLDNAME}", "<worldname>")
                .replace("{SHORTWORLDNAME}", "<shortworldname>");
    }

    private String resolveRadiusFormat(Player sender, ChatType type) {
        // group formats (string or map)
        String group = resolvePrimaryGroupBestEffort(sender);
        Object groupNode = plugin.getConfig().get("chat.group-formats." + group);
        String fromGroup = null;

        if (groupNode instanceof String s) {
            fromGroup = s;
        } else if (groupNode instanceof org.bukkit.configuration.ConfigurationSection section) {
            fromGroup = section.getString(type.configKey(), null);
        }

        if (fromGroup != null && !fromGroup.isBlank()) {
            return fromGroup;
        }

        // per-type formats
        String perType = plugin.getConfig().getString("chat.format-types." + type.configKey(), null);
        if (perType != null && !perType.isBlank()) {
            return perType;
        }

        return plugin.getConfig().getString("chat.format", "&f{DISPLAYNAME} &7 » &r{MESSAGE}");
    }

    private String applyRadiusPlaceholders(Player sender, String input) {
        if (input == null) return "";

        String worldName = sender.getWorld().getName();
        String worldAlias = plugin.getConfig().getString("chat.world-aliases." + worldName, worldName);

        String displayNameLegacy = legacy().serialize(sender.displayName());
        String shortWorld = worldName.isEmpty() ? "" : String.valueOf(worldName.charAt(0));
        String group = resolvePrimaryGroupBestEffort(sender);

        String out = input
                .replace("{USERNAME}", sender.getName())
                .replace("{DISPLAYNAME}", displayNameLegacy)
                .replace("{NICKNAME}", displayNameLegacy)
                .replace("{WORLD}", worldAlias)
                .replace("{WORLDNAME}", worldName)
                .replace("{SHORTWORLDNAME}", shortWorld)
                .replace("{GROUP}", group);

        // Best-effort PREFIX/SUFFIX via PlaceholderAPI if present.
        if (placeholderApiHook != null) {
            String prefix = placeholderApiHook.apply(sender, "%vault_prefix%");
            String suffix = placeholderApiHook.apply(sender, "%vault_suffix%");
            out = out.replace("{PREFIX}", safePlaceholder(prefix)).replace("{SUFFIX}", safePlaceholder(suffix));

            // Also allow any other %placeholders% in the format.
            out = placeholderApiHook.apply(sender, out);
        } else {
            out = out.replace("{PREFIX}", "").replace("{SUFFIX}", "");
        }

        return out;
    }

    private String resolvePrimaryGroupBestEffort(Player sender) {
        if (placeholderApiHook == null) return "default";
        String result = placeholderApiHook.apply(sender, "%luckperms_primary_group%");
        if (result == null) return "default";
        String trimmed = result.trim();
        if (trimmed.isEmpty()) return "default";
        // if placeholder wasn't replaced, it often still contains '%'
        if (trimmed.contains("%")) return "default";
        return trimmed;
    }

    private static LegacyComponentSerializer legacy() {
        // Support both legacy (&) codes and hex using the modern "&x&F&F..." format.
        // This also makes config formats capable of parsing "&#RRGGBB" once preprocessed.
        return LegacyComponentSerializer.builder()
                .character('&')
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build();
    }

    private static String safePlaceholder(String value) {
        if (value == null) return "";
        // If Vault placeholders aren't available, PlaceholderAPI can return the literal placeholder.
        return value.contains("%") ? "" : value;
    }

    private enum ChatType {
        NORMAL,
        SHOUT,
        QUESTION;

        public String configKey() {
            return switch (this) {
                case NORMAL -> "normal";
                case SHOUT -> "shout";
                case QUESTION -> "question";
            };
        }
    }

    private List<Player> computeRecipients(Player sender, ChatChannel channel) {
        List<Player> recipients = new ArrayList<>();
        switch (channel) {
            case GLOBAL -> recipients.addAll(Bukkit.getOnlinePlayers());
            case STAFF -> {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.hasPermission("bubblechat.channel.staff")) recipients.add(p);
                }
            }
            case LOCAL -> {
                int radius = plugin.getConfig().getInt("chat.local-radius-blocks", 100);
                Location origin = sender.getLocation();
                double maxDistSq = Math.max(0, radius) * (double) Math.max(0, radius);
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (!p.getWorld().equals(sender.getWorld())) continue;
                    if (p.getLocation().distanceSquared(origin) <= maxDistSq) {
                        recipients.add(p);
                    }
                }
            }
        }
        return recipients;
    }

    private Component renderChatMessage(Player sender, ChatChannel channel, String plainMessage) {
        String channelKey = channel.name().toLowerCase(Locale.ROOT);

        // Minimal, Essentials-style channels mode:
        // - optional per-channel legacy prefix (e.g. "&7[G]&r ")
        // - the same chat.format line used by radius mode
        if (plugin.getConfig().contains("chat.channel-prefixes")
                || plugin.getConfig().contains("chat.channel-prefixes." + channelKey)) {
            return renderChannelsLegacyMessage(sender, channel, plainMessage);
        }

        // Advanced per-channel MiniMessage formats (legacy behavior)
        String format = plugin.getConfig().getString(
                "chat.formats." + channelKey,
                "<white><player></white><gray>:</gray> <message>"
        );
        format = applyPlaceholders(sender, format);

        Component messageComponent = renderPlayerMessage(sender, plainMessage);

        TagResolver placeholders = TagResolver.resolver(
                Placeholder.unparsed("player", sender.getName()),
                Placeholder.component("displayname", sender.displayName()),
                Placeholder.unparsed("world", sender.getWorld().getName()),
                Placeholder.unparsed("channel", channelKey),
                Placeholder.component("message", messageComponent)
        );

        return mm.deserialize(format, placeholders);
    }

    private Component renderChannelsLegacyMessage(Player sender, ChatChannel channel, String messagePlain) {
        String channelKey = channel.name().toLowerCase(Locale.ROOT);

        String prefixLegacy = plugin.getConfig().getString(
                "chat.channel-prefixes." + channelKey,
                defaultChannelPrefixLegacy(channel)
        );
        prefixLegacy = applyRadiusPlaceholders(sender, prefixLegacy);

        String fmt = plugin.getConfig().getString("chat.format", "&f{DISPLAYNAME} &7 » &r{MESSAGE}");
        String fmtResolved = applyRadiusPlaceholders(sender, fmt);

        Component messageComponent = renderPlayerMessage(sender, messagePlain);

        String marker = "{MESSAGE}";
        int idx = fmtResolved.indexOf(marker);
        Component formatted;
        if (idx < 0) {
            formatted = legacy().deserialize(fmtResolved).append(Component.space()).append(messageComponent);
        } else {
            String left = fmtResolved.substring(0, idx);
            String right = fmtResolved.substring(idx + marker.length());
            formatted = legacy().deserialize(left).append(messageComponent).append(legacy().deserialize(right));
        }

        return legacy().deserialize(prefixLegacy).append(formatted);
    }

    private static String defaultChannelPrefixLegacy(ChatChannel channel) {
        return switch (channel) {
            case GLOBAL -> "&7[G]&r ";
            case LOCAL -> "&7[L]&r ";
            case STAFF -> "&c[Staff]&r ";
        };
    }

    private Component renderPlayerMessage(Player sender, String plainMessage) {
        boolean allowMini = plugin.getConfig().getBoolean("chat.message.allow-player-minimessage", true);
        boolean escapeWithoutPerm = plugin.getConfig().getBoolean("chat.message.escape-without-permission", true);

        boolean allowPapiInMessage = plugin.getConfig().getBoolean("placeholders.enabled-in-player-messages", false);
        String papiPerm = plugin.getConfig().getString("placeholders.permission", "bubblechat.placeholders");
        if (allowPapiInMessage && placeholderApiHook != null && sender.hasPermission(papiPerm)) {
            plainMessage = placeholderApiHook.apply(sender, plainMessage);
        }

        boolean itemEnabled = plugin.getConfig().getBoolean("chat.item.enabled", true);
        String itemToken = plugin.getConfig().getString("chat.item.token", "[item]");
        String itemPerm = plugin.getConfig().getString("chat.item.permission", "bubblechat.item");
        boolean wantsItem = itemEnabled
            && itemToken != null
            && !itemToken.isEmpty()
            && plainMessage.contains(itemToken)
            && sender.hasPermission(itemPerm);

        if (!allowMini) {
            if (!wantsItem) return renderLegacyOrPlainMessage(sender, plainMessage);
            return injectItemPlaceholderLegacyOrPlain(sender, plainMessage, itemToken);
        }

        boolean canFormat = sender.hasPermission("bubblechat.format");
        if (!canFormat && escapeWithoutPerm) {
            if (!wantsItem) return renderLegacyOrPlainMessage(sender, plainMessage);
            return injectItemPlaceholderLegacyOrPlain(sender, plainMessage, itemToken);
        }

        // basic guard: if they can format but not color, strip common color tags
        if (canFormat && !sender.hasPermission("bubblechat.color")) {
            if (!wantsItem) {
                String stripped = stripColorTags(plainMessage);
                return applyChatColor2BaseStyleToText(sender, mm.deserialize(stripped));
            }

            return injectItemPlaceholderMini(sender, plainMessage, itemToken, true);
        }

        if (!wantsItem) {
            return applyChatColor2BaseStyleToText(sender, mm.deserialize(plainMessage));
        }

        return injectItemPlaceholderMini(sender, plainMessage, itemToken, false);
    }

    private boolean isChatColor2Enabled() {
        if (chatColor2Hook == null || !chatColor2Hook.isAvailable()) return false;
        return plugin.getConfig().getBoolean("chat.chatcolor2.enabled", true);
    }

    private Component renderLegacyOrPlainMessage(Player sender, String message) {
        if (!isChatColor2Enabled()) {
            return Component.text(message);
        }
        return legacyDeserializeWithChatColor2(sender, message);
    }

    private Component injectItemPlaceholderLegacyOrPlain(Player sender, String message, String token) {
        if (!isChatColor2Enabled()) {
            return injectItemPlaceholderPlain(sender, message, token);
        }
        return injectItemPlaceholderLegacy(sender, message, token);
    }

    private Component injectItemPlaceholderLegacy(Player sender, String plainMessage, String token) {
        Component item = buildItemComponent(sender);
        Component out = Component.empty();
        String[] parts = plainMessage.split(java.util.regex.Pattern.quote(token), -1);
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (!part.isEmpty()) {
                out = out.append(legacyDeserializeWithChatColor2(sender, part));
            }
            if (i < parts.length - 1) {
                out = out.append(item);
            }
        }
        return out;
    }

    private Component legacyDeserializeWithChatColor2(Player sender, String input) {
        String msg = input == null ? "" : input;

        // Respect ChatColor2's own permission model for inline colour codes.
        boolean allowInline = sender != null && (sender.isOp() || sender.hasPermission("chatcolor.use-color-codes"));
        boolean allowHex = sender != null && (sender.isOp() || sender.hasPermission("chatcolor.use-hex-codes"));

        if (!allowInline) {
            msg = TextSanitizer.stripLegacyAmpersandColors(msg);
        } else {
            // Allow normal legacy codes; optionally strip hex if not permitted.
            if (!allowHex) {
                msg = msg.replaceAll("(?i)&#[0-9A-F]{6}", "");
                msg = msg.replaceAll("(?i)&x(&[0-9A-F]){6}", "");
            }
        }

        boolean applyPlayerColour = plugin.getConfig().getBoolean("chat.chatcolor2.apply-player-color", true);
        if (applyPlayerColour && sender != null) {
            String prefix = chatColor2Hook.getPlayerFullColourAmpersand(sender);
            if (prefix != null && !prefix.isBlank()) {
                msg = prefix + msg;
            }
        }

        // Support ChatColor2 hex input style: "&#RRGGBB".
        msg = TextSanitizer.convertAmpersandHexToLegacyXFormat(msg);

        return legacy().deserialize(msg);
    }

    private Component injectItemPlaceholderPlain(Player sender, String plainMessage, String token) {
        Component item = buildItemComponent(sender);
        Component out = Component.empty();
        String[] parts = plainMessage.split(java.util.regex.Pattern.quote(token), -1);
        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                out = out.append(Component.text(parts[i]));
            }
            if (i < parts.length - 1) {
                out = out.append(item);
            }
        }
        return out;
    }

    private Component injectItemPlaceholderMini(Player sender, String plainMessage, String token, boolean stripColorsFirst) {
        Component item = buildItemComponent(sender);
        Component out = Component.empty();
        Style baseStyle = chatColor2BaseStyleForPlayerMessage(sender);
        String[] parts = plainMessage.split(java.util.regex.Pattern.quote(token), -1);
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (stripColorsFirst) {
                part = stripColorTags(part);
            }
            if (!part.isEmpty()) {
                out = out.append(applyBaseStyle(mm.deserialize(part), baseStyle));
            }
            if (i < parts.length - 1) {
                out = out.append(item);
            }
        }
        return out;
    }

    private Component applyChatColor2BaseStyleToText(Player sender, Component textComponent) {
        return applyBaseStyle(textComponent, chatColor2BaseStyleForPlayerMessage(sender));
    }

    private Component applyBaseStyle(Component component, Style baseStyle) {
        if (baseStyle == null) return component;
        if (component == null) return Component.empty().style(baseStyle);
        return Component.empty().style(baseStyle).append(component);
    }

    private Style chatColor2BaseStyleForPlayerMessage(Player sender) {
        if (sender == null) return null;
        if (!isChatColor2Enabled()) return null;
        if (!plugin.getConfig().getBoolean("chat.chatcolor2.apply-in-minimessage", true)) return null;
        if (!plugin.getConfig().getBoolean("chat.chatcolor2.apply-player-color", true)) return null;

        String prefix = chatColor2Hook.getPlayerFullColourAmpersand(sender);
        if (prefix == null || prefix.isBlank()) return null;

        // Support CC2 hex input style as well.
        prefix = TextSanitizer.convertAmpersandHexToLegacyXFormat(prefix);
        return legacy().deserialize(prefix).style();
    }

    private Component buildItemComponent(Player sender) {
        ItemStack item = sender.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir()) {
            return Component.text("[empty hand]", NamedTextColor.DARK_GRAY)
                    .hoverEvent(HoverEvent.showText(Component.text("Nothing in hand", NamedTextColor.GRAY)));
        }

        String name = item.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
        int amount = Math.max(1, item.getAmount());

        Component hover = Component.text(name, NamedTextColor.AQUA)
                .append(Component.text(" x" + amount, NamedTextColor.GRAY));

        return Component.text("[" + name + "]", NamedTextColor.AQUA)
                .hoverEvent(HoverEvent.showText(hover));
    }

    private String applyFilters(Player sender, String message) {
        // word list
        List<String> blocked = plugin.getConfig().getStringList("filters.blocked-words");
        if (blocked != null && !blocked.isEmpty()) {
            for (String w : blocked) {
                if (w == null || w.isBlank()) continue;
                message = message.replaceAll("(?i)\\b" + Pattern.quote(w.trim()) + "\\b", "****");
            }
        }

        // link blocking
        boolean blockLinks = plugin.getConfig().getBoolean("filters.block-links-without-permission", true);
        String linkPerm = plugin.getConfig().getString("filters.link-permission", "bubblechat.links");
        if (blockLinks && !sender.hasPermission(linkPerm)) {
            // very simple URL-ish detection
            if (message.matches("(?i).*\\b(https?://|www\\.).*")) {
                sender.sendMessage(mm.deserialize("<red>Links are not allowed.</red>"));
                return null;
            }
        }

        // regex replacements
        // Note: Bukkit returns lists of maps, not a ConfigurationSection, so handle as raw list.
        List<?> raw = plugin.getConfig().getList("filters.regex-replacements");
        if (raw != null) {
            for (Object obj : raw) {
                if (!(obj instanceof java.util.Map<?, ?> map)) continue;
                Object pat = map.get("pattern");
                Object rep = map.get("replacement");
                if (!(pat instanceof String pattern) || !(rep instanceof String replacement)) continue;
                try {
                    message = message.replaceAll(pattern, replacement);
                } catch (Exception ignored) {
                    // ignore broken patterns
                }
            }
        }

        return message;
    }

    private void logChat(ChatChannel channel, Player sender, String message) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) return;
        File logFile = new File(plugin.getDataFolder(), "chat.log");
        String line = Instant.now() + " [" + channel.name() + "] " + sender.getName() + ": " + message;
        try (BufferedWriter w = new BufferedWriter(new FileWriter(logFile, true))) {
            w.write(line);
            w.newLine();
        } catch (IOException ignored) {
        }
    }

    private void logRulesLine(String line) {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) return;
        String fileName = plugin.getConfig().getString("logging.rules-log-file", "rules.log");
        File logFile = new File(plugin.getDataFolder(), fileName);
        String out = Instant.now() + " " + line;
        try (BufferedWriter w = new BufferedWriter(new FileWriter(logFile, true))) {
            w.write(out);
            w.newLine();
        } catch (IOException ignored) {
        }
    }

    // ===== Command spy + command rules =====

    public boolean isCommandSpyEnabled() {
        return plugin.getConfig().getBoolean("spy.commands.enabled", true);
    }

    public void notifyCommandSpy(Player actor, String commandLine) {
        Component c = mm.deserialize("<dark_gray>[CMD]</dark_gray> <gray>" + escapeMini(actor.getName()) + "</gray> <white>" + escapeMini(commandLine) + "</white>");
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(actor)) continue;
            if (!p.hasPermission("bubblechat.spy.commands")) continue;
            p.sendMessage(c);
        }
    }

    public RuleResult applyCommandRules(Player player, String commandLine) {
        if (ruleEngine == null) return RuleResult.allow(commandLine);
        return ruleEngine.applyCommand(player, commandLine, ruleNotifier);
    }

    private void notifyMentions(Player sender, List<Player> recipients, String plain) {
        boolean soundEnabled = plugin.getConfig().getBoolean("mentions.sound.enabled", true);
        String soundKey = plugin.getConfig().getString("mentions.sound.key", "minecraft:entity.experience_orb.pickup");
        float volume = (float) plugin.getConfig().getDouble("mentions.sound.volume", 1.0);
        float pitch = (float) plugin.getConfig().getDouble("mentions.sound.pitch", 1.2);

        String lower = plain.toLowerCase(Locale.ROOT);
        for (Player p : recipients) {
            if (p.equals(sender)) continue;
            String name = p.getName();
            if (lower.contains("@" + name.toLowerCase(Locale.ROOT)) || containsWord(lower, name.toLowerCase(Locale.ROOT))) {
                if (soundEnabled) {
                    try {
                        p.playSound(p.getLocation(), Sound.valueOf(soundKey.toUpperCase(Locale.ROOT).replace(':', '_').replace('.', '_')), volume, pitch);
                    } catch (Exception ignored) {
                        // ignore invalid sound
                    }
                }
            }
        }
    }

    private boolean containsWord(String textLower, String wordLower) {
        return textLower.matches(".*\\b" + Pattern.quote(wordLower) + "\\b.*");
    }

    private boolean isIgnoring(Player recipient, UUID senderUuid) {
        PlayerData r = dataStore.get(recipient.getUniqueId());
        return r.getIgnored().contains(senderUuid);
    }

    private static double uppercaseRatio(String s) {
        int letters = 0;
        int upper = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isUpperCase(c)) upper++;
            }
        }
        if (letters == 0) return 0.0;
        return (double) upper / (double) letters;
    }

    private static String stripColorTags(String input) {
        // Remove common MiniMessage color tags: <red>, <#ff00ff>, <color:...>
        return input
                .replaceAll("<#[0-9a-fA-F]{6}>", "")
                .replaceAll("</#[0-9a-fA-F]{6}>", "")
                .replaceAll("(?i)</?(black|dark_blue|dark_green|dark_aqua|dark_red|dark_purple|gold|gray|grey|dark_gray|dark_grey|blue|green|aqua|red|light_purple|yellow|white)>", "")
                .replaceAll("(?i)</?color(:[^>]+)?>", "");
    }

    private static String escapeMini(String s) {
        return s == null ? "" : s.replace("<", "\\<").replace(">", "\\>");
    }

    private static String formatDuration(long ms) {
        long seconds = Math.max(0, ms / 1000L);
        long m = seconds / 60;
        long s = seconds % 60;
        long h = m / 60;
        m = m % 60;
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    // ===== PMs / Reply / Social Spy =====

    public boolean isPrivateMessagesEnabled() {
        return plugin.getConfig().getBoolean("private-messages.enabled", true);
    }

    public void sendPrivateMessage(Player sender, Player target, String messagePlain) {
        if (!isPrivateMessagesEnabled()) {
            sender.sendMessage(mm.deserialize("<red>Private messages are disabled.</red>"));
            return;
        }

        if (isIgnoring(target, sender.getUniqueId())) {
            sender.sendMessage(mm.deserialize("<red>That player is ignoring you.</red>"));
            return;
        }

        if (data(target).isBlockPrivateMessages() && !sender.hasPermission("bubblechat.bypass.pmblock")) {
            sender.sendMessage(mm.deserialize("<red>That player is not accepting private messages.</red>"));
            return;
        }

        Component msgComponent = renderPlayerMessage(sender, messagePlain);

        String fmtSender = plugin.getConfig().getString("private-messages.format-to-sender", "<gray>[to <white><target></white>]</gray> <message>");
        String fmtTarget = plugin.getConfig().getString("private-messages.format-to-target", "<gray>[from <white><sender></white>]</gray> <message>");

        fmtSender = applyPlaceholders(sender, fmtSender);
        fmtTarget = applyPlaceholders(target, fmtTarget);

        sender.sendMessage(mm.deserialize(fmtSender, TagResolver.resolver(
                Placeholder.unparsed("target", target.getName()),
                Placeholder.component("message", msgComponent)
        )));

        Component clickableReply = mm.deserialize(fmtTarget, TagResolver.resolver(
                Placeholder.unparsed("sender", sender.getName()),
                Placeholder.component("message", msgComponent)
        )).hoverEvent(HoverEvent.showText(Component.text("Click to reply")))
                .clickEvent(ClickEvent.suggestCommand("/reply "));

        target.sendMessage(clickableReply);

        // update reply targets
        data(sender).setLastReplyTarget(target.getUniqueId());
        data(target).setLastReplyTarget(sender.getUniqueId());

        // Social spy
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(sender) || p.equals(target)) continue;
            if (!p.hasPermission("bubblechat.socialspy")) continue;
            if (!data(p).isSocialSpy()) continue;

            p.sendMessage(mm.deserialize("<dark_gray>[SS] <gray>" + escapeMini(sender.getName()) + " -> " + escapeMini(target.getName()) + ":</gray> ")
                    .append(msgComponent));
        }
    }

    private String applyPlaceholders(Player player, String input) {
        if (placeholderApiHook == null) return input;
        return placeholderApiHook.apply(player, input);
    }

    public void toggleSocialSpy(Player player) {
        PlayerData pd = data(player);
        pd.setSocialSpy(!pd.isSocialSpy());
        player.sendMessage(mm.deserialize(pd.isSocialSpy() ? "<green>SocialSpy enabled.</green>" : "<red>SocialSpy disabled.</red>"));
    }

    public void setChannel(Player player, ChatChannel channel) {
        if (channel == ChatChannel.STAFF && !player.hasPermission("bubblechat.channel.staff")) {
            player.sendMessage(mm.deserialize("<red>You don't have access to staff chat.</red>"));
            return;
        }
        data(player).setChannel(channel);
        player.sendMessage(mm.deserialize("<green>Channel set to " + channel.name().toLowerCase(Locale.ROOT) + ".</green>"));
    }

    public void mutePlayer(Player moderator, Player target, long durationMs, String reason) {
        if (!moderator.hasPermission("bubblechat.moderation.mute")) {
            moderator.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }

        long until = durationMs <= 0 ? Long.MAX_VALUE : System.currentTimeMillis() + durationMs;
        PlayerData pd = data(target);
        pd.setMutedUntilEpochMs(until);
        pd.setMuteReason(reason);

        moderator.sendMessage(mm.deserialize("<green>Muted " + escapeMini(target.getName()) + ".</green>"));
        target.sendMessage(mm.deserialize("<red>You have been muted.</red>"));
        if (reason != null && !reason.isBlank()) {
            target.sendMessage(mm.deserialize("<gray>Reason: " + escapeMini(reason) + "</gray>"));
        }
    }

    public void unmutePlayer(Player moderator, Player target) {
        if (!moderator.hasPermission("bubblechat.moderation.mute")) {
            moderator.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return;
        }

        PlayerData pd = data(target);
        pd.setMutedUntilEpochMs(0L);
        pd.setMuteReason(null);

        moderator.sendMessage(mm.deserialize("<green>Unmuted " + escapeMini(target.getName()) + ".</green>"));
        target.sendMessage(mm.deserialize("<green>You have been unmuted.</green>"));
    }

    public void toggleIgnore(Player player, Player target) {
        if (player.getUniqueId().equals(target.getUniqueId())) {
            player.sendMessage(mm.deserialize("<red>You cannot ignore yourself.</red>"));
            return;
        }

        Set<UUID> ignored = data(player).getIgnored();
        if (ignored.contains(target.getUniqueId())) {
            ignored.remove(target.getUniqueId());
            player.sendMessage(mm.deserialize("<green>You are no longer ignoring " + escapeMini(target.getName()) + ".</green>"));
        } else {
            ignored.add(target.getUniqueId());
            player.sendMessage(mm.deserialize("<yellow>You are now ignoring " + escapeMini(target.getName()) + ".</yellow>"));
        }
    }

    public void listIgnored(Player player) {
        Set<UUID> ignored = data(player).getIgnored();
        if (ignored.isEmpty()) {
            player.sendMessage(mm.deserialize("<gray>You are not ignoring anyone.</gray>"));
            return;
        }

        List<String> names = new ArrayList<>();
        for (UUID uuid : ignored) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) names.add(p.getName());
            else names.add(uuid.toString());
        }

        player.sendMessage(mm.deserialize("<gray>Ignored:</gray> <white>" + escapeMini(String.join(", ", names)) + "</white>"));
    }

    public void setGlobalChatMuted(boolean muted) {
        plugin.getConfig().set("moderation.global-chat-muted", muted);
        plugin.saveConfig();
    }

    public boolean isGlobalChatMuted() {
        return plugin.getConfig().getBoolean("moderation.global-chat-muted", false);
    }
}
