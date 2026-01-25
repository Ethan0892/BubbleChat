package dev.bubblecraft.bubblechat.command;

import dev.bubblecraft.bubblechat.service.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class ChatMuteCommand implements CommandExecutor {

    private final ChatService chatService;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ChatMuteCommand(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bubblechat.moderation.mute")) {
            sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return true;
        }

        if (args.length == 0) {
            boolean newState = !chatService.isGlobalChatMuted();
            chatService.setGlobalChatMuted(newState);
            sender.sendMessage(mm.deserialize(newState ? "<yellow>Global chat muted.</yellow>" : "<green>Global chat unmuted.</green>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            chatService.setGlobalChatMuted(true);
            sender.sendMessage(mm.deserialize("<yellow>Global chat muted.</yellow>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {
            chatService.setGlobalChatMuted(false);
            sender.sendMessage(mm.deserialize("<green>Global chat unmuted.</green>"));
            return true;
        }

        sender.sendMessage(mm.deserialize("<red>Usage: /" + label + " [on|off]</red>"));
        return true;
    }
}
