package dev.bubblecraft.bubblechat.command;

import dev.bubblecraft.bubblechat.service.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public final class BubbleChatCommand implements CommandExecutor {

    private final Plugin plugin;
    private final ChatService chatService;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public BubbleChatCommand(Plugin plugin, ChatService chatService) {
        this.plugin = plugin;
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("info")) {
            sender.sendMessage(mm.deserialize("<gray>BubbleChat</gray> <white>v" + plugin.getPluginMeta().getVersion() + "</white>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("bubblechat.reload")) {
                sender.sendMessage(mm.deserialize("<red>No permission.</red>"));
                return true;
            }
            chatService.reload();
            sender.sendMessage(mm.deserialize("<green>BubbleChat reloaded.</green>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("remove")) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(mm.deserialize("<red>Players only.</red>"));
                return true;
            }

            String perm = plugin.getConfig().getString("chat.remove.permission", "bubblechat.moderation.remove");
            if (perm == null || perm.isBlank()) perm = "bubblechat.moderation.remove";
            if (!player.hasPermission(perm)) {
                player.sendMessage(mm.deserialize("<red>No permission.</red>"));
                return true;
            }

            if (args.length < 2) {
                player.sendMessage(mm.deserialize("<red>Usage: /" + label + " remove <id></red>"));
                return true;
            }

            UUID id;
            try {
                id = UUID.fromString(args[1]);
            } catch (IllegalArgumentException ex) {
                player.sendMessage(mm.deserialize("<red>Invalid message id.</red>"));
                return true;
            }

            chatService.removeTrackedMessage(player, id);
            return true;
        }

        sender.sendMessage(mm.deserialize("<red>Usage: /" + label + " <reload|info|remove></red>"));
        return true;
    }
}
