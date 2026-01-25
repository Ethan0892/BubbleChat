package dev.bubblecraft.bubblechat.hook;

import dev.bubblecraft.bubblechat.service.ChatService;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class PlaceholderApiHook {

    private final Plugin plugin;

    private PlaceholderApiHook(Plugin plugin) {
        this.plugin = plugin;
    }

    public static PlaceholderApiHook tryEnable(Plugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) {
            return null;
        }
        plugin.getLogger().info("PlaceholderAPI detected; enabling placeholders.");
        return new PlaceholderApiHook(plugin);
    }

    public String apply(Player player, String input) {
        if (input == null) return null;
        if (player == null) return input;
        try {
            return PlaceholderAPI.setPlaceholders(player, input);
        } catch (Throwable t) {
            plugin.getLogger().warning("PlaceholderAPI placeholder expansion failed: " + t.getMessage());
            return input;
        }
    }

    public void registerExpansion(ChatService chatService) {
        try {
            new BubbleChatExpansion(plugin, chatService).register();
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to register BubbleChat PlaceholderAPI expansion: " + t.getMessage());
        }
    }
}
