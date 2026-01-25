package dev.bubblecraft.bubblechat.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Optional integration with ChatColor2.
 *
 * BubbleChat uses Adventure Components; ChatColor2 primarily targets legacy chat events.
 * This hook retrieves a player's configured ChatColor2 colour string (best-effort), so
 * BubbleChat can apply it when rendering messages.
 */
public final class ChatColor2Hook {

    private final Plugin plugin;
    private final Plugin chatColor2;
    private final PlaceholderApiHook placeholderApiHook;

    // Reflection fallback (no compile-time dependency)
    private volatile Object reflectedPlayerDataStore;
    private volatile Method reflectedGetColour;

    private ChatColor2Hook(Plugin plugin, Plugin chatColor2, PlaceholderApiHook placeholderApiHook) {
        this.plugin = plugin;
        this.chatColor2 = chatColor2;
        this.placeholderApiHook = placeholderApiHook;
    }

    public static ChatColor2Hook tryEnable(Plugin plugin, PlaceholderApiHook placeholderApiHook) {
        Plugin cc2 = Bukkit.getPluginManager().getPlugin("ChatColor2");
        if (cc2 == null) {
            // Some builds historically used different plugin names.
            cc2 = Bukkit.getPluginManager().getPlugin("ChatColor");
        }
        if (cc2 == null || !cc2.isEnabled()) {
            return null;
        }

        plugin.getLogger().info("ChatColor2 detected; enabling ChatColor2 compatibility.");
        return new ChatColor2Hook(plugin, cc2, placeholderApiHook);
    }

    public boolean isAvailable() {
        return chatColor2 != null && chatColor2.isEnabled();
    }

    /**
     * Returns a ChatColor2-style colour prefix for the given player.
     *
     * Output is in ampersand legacy format (e.g. "&a&l" or "&x&1&2&3&4&5&6").
     * Returns empty string when unset/unknown.
     */
    public String getPlayerFullColourAmpersand(Player player) {
        if (player == null || !isAvailable()) return "";

        // Prefer PlaceholderAPI since it's stable and public.
        if (placeholderApiHook != null) {
            String expanded = placeholderApiHook.apply(player, "%cc_full_color%");
            if (expanded != null) {
                String trimmed = expanded.trim();
                // If PlaceholderAPI couldn't resolve, it often still contains '%'
                if (!trimmed.isEmpty() && !trimmed.contains("%")) {
                    // CC2 returns section sign formatted; convert to ampersand.
                    return trimmed.replace('§', '&');
                }
            }
        }

        // Reflection fallback: reach into ChatColor2's PlayerDataStore.
        try {
            ensureReflectionWired();
            Object store = reflectedPlayerDataStore;
            Method getter = reflectedGetColour;
            if (store == null || getter == null) return "";

            Object result = getter.invoke(store, player.getUniqueId());
            if (!(result instanceof String s)) return "";
            return s == null ? "" : s;
        } catch (Throwable t) {
            // Keep this quiet-ish: we don't want log spam on every chat message.
            plugin.getLogger().fine("ChatColor2 reflection hook unavailable: " + t.getMessage());
            return "";
        }
    }

    private void ensureReflectionWired() {
        if (reflectedPlayerDataStore != null && reflectedGetColour != null) return;

        synchronized (this) {
            if (reflectedPlayerDataStore != null && reflectedGetColour != null) return;

            try {
                // Main class is com.sulphate.chatcolor2.main.ChatColor
                Class<?> mainClass = chatColor2.getClass();

                // Field: private PlayerDataStore playerDataStore;
                Field storeField = mainClass.getDeclaredField("playerDataStore");
                storeField.setAccessible(true);
                Object store = storeField.get(chatColor2);

                if (store == null) {
                    return;
                }

                // Method: getColour(UUID)
                Method getColour = store.getClass().getMethod("getColour", UUID.class);

                reflectedPlayerDataStore = store;
                reflectedGetColour = getColour;
            } catch (Throwable ignored) {
                // Leave as null; caller will gracefully fall back.
            }
        }
    }
}
