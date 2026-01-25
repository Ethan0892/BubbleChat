package dev.bubblecraft.bubblechat.command;

import dev.bubblecraft.bubblechat.data.ChatChannel;
import dev.bubblecraft.bubblechat.service.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ChannelCommand implements CommandExecutor {

    private final ChatService chatService;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ChannelCommand(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(mm.deserialize("<red>Usage: /" + label + " <global|local|staff></red>"));
            return true;
        }

        ChatChannel channel = ChatChannel.parse(args[0]);
        if (channel == null) {
            player.sendMessage(mm.deserialize("<red>Unknown channel.</red>"));
            return true;
        }

        chatService.setChannel(player, channel);
        return true;
    }
}
