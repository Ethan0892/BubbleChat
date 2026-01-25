package dev.bubblecraft.bubblechat.hook;

import dev.bubblecraft.bubblechat.data.PlayerData;
import dev.bubblecraft.bubblechat.service.ChatService;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BubbleChatExpansion extends PlaceholderExpansion {

    private final Plugin plugin;
    private final ChatService chatService;

    public BubbleChatExpansion(Plugin plugin, ChatService chatService) {
        this.plugin = plugin;
        this.chatService = chatService;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "bubblechat";
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", plugin.getPluginMeta().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null || player.getUniqueId() == null) return "";
        PlayerData pd = chatService.data(player.getUniqueId());

        return switch (params.toLowerCase()) {
            case "channel" -> pd.getChannel().name().toLowerCase();
            case "muted" -> String.valueOf(pd.isMutedNow(System.currentTimeMillis()));
            case "muted_until" -> String.valueOf(pd.getMutedUntilEpochMs());
            case "socialspy" -> String.valueOf(pd.isSocialSpy());
            case "ignored_count" -> String.valueOf(pd.getIgnored().size());
            default -> null;
        };
    }
}
