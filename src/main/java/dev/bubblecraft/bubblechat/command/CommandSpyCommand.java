package dev.bubblecraft.bubblechat.command;

import dev.bubblecraft.bubblechat.service.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class CommandSpyCommand implements CommandExecutor {

    private final ChatService chatService;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public CommandSpyCommand(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!player.hasPermission("bubblechat.spy.commands")) {
            player.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return true;
        }

        chatService.toggleCommandSpy(player);
        return true;
    }
}
