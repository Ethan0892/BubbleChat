package dev.bubblecraft.bubblechat.command;

import dev.bubblecraft.bubblechat.service.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class UnmuteCommand implements CommandExecutor {

    private final ChatService chatService;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public UnmuteCommand(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player mod)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length < 1) {
            mod.sendMessage(mm.deserialize("<red>Usage: /" + label + " <player></red>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            mod.sendMessage(mm.deserialize("<red>That player is not online.</red>"));
            return true;
        }

        chatService.unmutePlayer(mod, target);
        return true;
    }
}
