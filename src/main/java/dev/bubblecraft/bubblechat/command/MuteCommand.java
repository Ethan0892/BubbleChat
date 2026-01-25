package dev.bubblecraft.bubblechat.command;

import dev.bubblecraft.bubblechat.service.ChatService;
import dev.bubblecraft.bubblechat.service.DurationParser;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public final class MuteCommand implements CommandExecutor {

    private final ChatService chatService;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MuteCommand(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player mod)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (args.length < 1) {
            mod.sendMessage(mm.deserialize("<red>Usage: /" + label + " <player> [duration] [reason]</red>"));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            mod.sendMessage(mm.deserialize("<red>That player is not online.</red>"));
            return true;
        }

        long durationMs = 0L;
        String reason = null;

        if (args.length >= 2) {
            long parsed = DurationParser.parseToMillis(args[1]);
            if (parsed > 0) {
                durationMs = parsed;
                if (args.length >= 3) {
                    reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                }
            } else {
                // treat everything after player as reason
                reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
            }
        }

        chatService.mutePlayer(mod, target, durationMs, reason);
        return true;
    }
}
