package dev.bubblecraft.bubblechat.command;

import dev.bubblecraft.bubblechat.data.PlayerData;
import dev.bubblecraft.bubblechat.service.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ShoutCommand implements CommandExecutor {

    private final ChatService chatService;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ShoutCommand(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!player.hasPermission("bubblechat.chat.shout")) {
            player.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return true;
        }

        PlayerData pd = chatService.data(player);
        pd.setShoutMode(!pd.isShoutMode());
        player.sendMessage(mm.deserialize(pd.isShoutMode() ? "<yellow>Shout mode enabled.</yellow>" : "<green>Shout mode disabled.</green>"));
        return true;
    }
}
