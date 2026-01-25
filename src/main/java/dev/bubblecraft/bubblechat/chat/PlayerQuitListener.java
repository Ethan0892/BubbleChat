package dev.bubblecraft.bubblechat.chat;

import dev.bubblecraft.bubblechat.service.ChatService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener implements Listener {

    private final ChatService chatService;

    public PlayerQuitListener(ChatService chatService) {
        this.chatService = chatService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        chatService.handleQuit(event);
    }
}
