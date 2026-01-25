package dev.bubblecraft.bubblechat.command;

import dev.bubblecraft.bubblechat.data.ChatChannel;
import dev.bubblecraft.bubblechat.service.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class LocalCommand implements CommandExecutor {

    private final ChatService chatService;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public LocalCommand(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (chatService.isRadiusMode()) {
            chatService.setRadiusGlobalMode(player, false);
            player.sendMessage(mm.deserialize("<green>Chat set to local.</green>"));
            return true;
        }

        chatService.setChannel(player, ChatChannel.LOCAL);
        return true;
    }
}
