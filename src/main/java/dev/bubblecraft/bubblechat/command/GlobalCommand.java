package dev.bubblecraft.bubblechat.command;

import dev.bubblecraft.bubblechat.data.ChatChannel;
import dev.bubblecraft.bubblechat.service.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class GlobalCommand implements CommandExecutor {

    private final ChatService chatService;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public GlobalCommand(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (chatService.isRadiusMode()) {
            if (!player.hasPermission("bubblechat.chat.shout")) {
                player.sendMessage(mm.deserialize("<red>No permission.</red>"));
                return true;
            }
            chatService.setRadiusGlobalMode(player, true);
            player.sendMessage(mm.deserialize("<yellow>Chat set to global.</yellow>"));
            return true;
        }

        chatService.setChannel(player, ChatChannel.GLOBAL);
        return true;
    }
}
