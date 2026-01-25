package dev.bubblecraft.bubblechat.command;

import dev.bubblecraft.bubblechat.data.PlayerData;
import dev.bubblecraft.bubblechat.service.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ReplyCommand implements CommandExecutor {

    private final ChatService chatService;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ReplyCommand(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(mm.deserialize("<red>Usage: /" + label + " <message></red>"));
            return true;
        }

        PlayerData pd = chatService.data(player);
        if (pd.getLastReplyTarget() == null) {
            player.sendMessage(mm.deserialize("<red>No one to reply to.</red>"));
            return true;
        }

        Player target = Bukkit.getPlayer(pd.getLastReplyTarget());
        if (target == null) {
            player.sendMessage(mm.deserialize("<red>That player is not online.</red>"));
            return true;
        }

        String message = String.join(" ", args);
        chatService.sendPrivateMessage(player, target, message);
        return true;
    }
}
