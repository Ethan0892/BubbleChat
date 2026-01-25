package dev.bubblecraft.bubblechat.chat;

import dev.bubblecraft.bubblechat.data.PlayerData;
import dev.bubblecraft.bubblechat.service.ChatService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class PlayerJoinListener implements Listener {

    private final ChatService chatService;

    public PlayerJoinListener(ChatService chatService) {
        this.chatService = chatService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        boolean firstJoin = !event.getPlayer().hasPlayedBefore();

        // Custom join message (separate from announcements)
        chatService.handleJoinMessage(event);

        // Announcements opt-out default on first join
        boolean optOutEnabled = chatService.getPlugin().getConfig().getBoolean("announcements.opt-out.enabled", true);
        if (firstJoin && optOutEnabled) {
            boolean defaultOptedOut = chatService.getPlugin().getConfig().getBoolean("announcements.opt-out.default-opted-out", false);
            chatService.data(event.getPlayer()).setAnnouncementsOptOut(defaultOptedOut);
        }

        // Essentials-style shout-default support when shout state isn't persisted.
        boolean persist = chatService.getPlugin().getConfig().getBoolean("chat.persist-shout", false);
        if (!persist) {
            boolean shoutDefault = chatService.getPlugin().getConfig().getBoolean("chat.shout-default", false);
            PlayerData pd = chatService.data(event.getPlayer());
            pd.setShoutMode(shoutDefault);
        }

        chatService.handleJoin(event.getPlayer(), firstJoin);
    }
}
