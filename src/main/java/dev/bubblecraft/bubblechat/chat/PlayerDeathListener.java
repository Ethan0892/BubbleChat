package dev.bubblecraft.bubblechat.chat;

import dev.bubblecraft.bubblechat.service.ChatService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class PlayerDeathListener implements Listener {

    private final ChatService chatService;

    public PlayerDeathListener(ChatService chatService) {
        this.chatService = chatService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent event) {
        chatService.handleDeath(event);
    }
}
