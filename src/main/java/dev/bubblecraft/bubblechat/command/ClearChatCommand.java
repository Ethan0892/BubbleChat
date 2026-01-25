package dev.bubblecraft.bubblechat.command;

import dev.bubblecraft.bubblechat.service.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class ClearChatCommand implements CommandExecutor {

    private final ChatService chatService;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ClearChatCommand(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bubblechat.clearchat")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return true;
        }

        boolean silent = args.length > 0 && (args[0].equalsIgnoreCase("-s") || args[0].equalsIgnoreCase("silent"));
        chatService.clearChat(silent, sender.getName());
        return true;
    }
}
