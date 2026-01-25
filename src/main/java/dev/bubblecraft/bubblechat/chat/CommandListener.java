package dev.bubblecraft.bubblechat.chat;

import dev.bubblecraft.bubblechat.rules.RuleResult;
import dev.bubblecraft.bubblechat.service.ChatService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public final class CommandListener implements Listener {

    private final ChatService chatService;

    public CommandListener(ChatService chatService) {
        this.chatService = chatService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage();

        // Command spy
        if (chatService.isCommandSpyEnabled() && !player.hasPermission("bubblechat.spy.exempt")) {
            chatService.notifyCommandSpy(player, msg);
        }

        // Rules (scope=command)
        RuleResult res = chatService.applyCommandRules(player, msg);
        if (res.isCancelled()) {
            event.setCancelled(true);
        } else if (!res.message().equals(msg)) {
            event.setMessage(res.message());
        }
    }
}
