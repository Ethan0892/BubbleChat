package dev.bubblecraft.bubblechat.command;

import dev.bubblecraft.bubblechat.service.ChatService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class IgnoreListCommand implements CommandExecutor {

    private final ChatService chatService;

    public IgnoreListCommand(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        chatService.listIgnored(player);
        return true;
    }
}
