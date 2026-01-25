package dev.bubblecraft.bubblechat.hook;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public final class DiscordSrvHook {

    private final Plugin plugin;

    // reflection handles (optional)
    private final Object discordSrv;
    private final Method getTextChannelById;
    private final Method sendMessage;

    private DiscordSrvHook(Plugin plugin, Object discordSrv, Method getTextChannelById, Method sendMessage) {
        this.plugin = plugin;
        this.discordSrv = discordSrv;
        this.getTextChannelById = getTextChannelById;
        this.sendMessage = sendMessage;
    }

    public static DiscordSrvHook tryEnable(Plugin plugin) {
        if (Bukkit.getPluginManager().getPlugin("DiscordSRV") == null) {
            return null;
        }
        if (!plugin.getConfig().getBoolean("discordsrv.enabled", false)) {
            return null;
        }

        try {
            Class<?> discordSrvClass = Class.forName("github.scarsz.discordsrv.DiscordSRV");
            Object instance = discordSrvClass.getMethod("getPlugin").invoke(null);

            // DiscordSRV.getPlugin().getJda().getTextChannelById(String)
            Method getJda = discordSrvClass.getMethod("getJda");
            Object jda = getJda.invoke(instance);
            Class<?> jdaClass = jda.getClass();
            Method getTextChannelById = jdaClass.getMethod("getTextChannelById", String.class);

            // github.scarsz.discordsrv.util.DiscordUtil.sendMessage(net.dv8tion.jda.api.entities.MessageChannel, String)
            Class<?> discordUtil = Class.forName("github.scarsz.discordsrv.util.DiscordUtil");
            Method sendMessage = discordUtil.getMethod("sendMessage", Class.forName("net.dv8tion.jda.api.entities.MessageChannel"), String.class);

            plugin.getLogger().info("DiscordSRV detected; enabling Discord alerts.");
            return new DiscordSrvHook(plugin, instance, getTextChannelById, sendMessage);
        } catch (Throwable t) {
            plugin.getLogger().warning("DiscordSRV hook failed to initialize: " + t.getMessage());
            return null;
        }
    }

    public void sendAlert(String text) {
        String channelId = plugin.getConfig().getString("discordsrv.alert-channel-id", "");
        if (channelId == null || channelId.isBlank()) return;

        try {
            Object jda = discordSrv.getClass().getMethod("getJda").invoke(discordSrv);
            Object channel = getTextChannelById.invoke(jda, channelId);
            if (channel == null) return;

            String prefix = plugin.getConfig().getString("discordsrv.alert-prefix", "[BubbleChat]");
            sendMessage.invoke(null, channel, prefix + " " + text);
        } catch (Throwable t) {
            plugin.getLogger().warning("DiscordSRV send failed: " + t.getMessage());
        }
    }
}
