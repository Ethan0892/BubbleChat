package dev.bubblecraft.bubblechat.command;

import dev.bubblecraft.bubblechat.data.PlayerData;
import dev.bubblecraft.bubblechat.service.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ToggleCommand implements CommandExecutor {

    private final ChatService chatService;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ToggleCommand(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!player.hasPermission("bubblechat.toggle")) {
            player.sendMessage(mm.deserialize("<red>No permission.</red>"));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(mm.deserialize("<red>Usage: /" + label + " <join|quit|death|announcements|pm></red>"));
            return true;
        }

        PlayerData pd = chatService.data(player);
        String key = args[0].toLowerCase();

        switch (key) {
            case "join" -> {
                pd.setHideJoinMessages(!pd.isHideJoinMessages());
                player.sendMessage(mm.deserialize(pd.isHideJoinMessages()
                        ? "<yellow>You will no longer see join messages.</yellow>"
                        : "<green>You will now see join messages.</green>"));
            }
            case "quit", "leave" -> {
                pd.setHideQuitMessages(!pd.isHideQuitMessages());
                player.sendMessage(mm.deserialize(pd.isHideQuitMessages()
                        ? "<yellow>You will no longer see quit messages.</yellow>"
                        : "<green>You will now see quit messages.</green>"));
            }
            case "death", "deaths" -> {
                pd.setHideDeathMessages(!pd.isHideDeathMessages());
                player.sendMessage(mm.deserialize(pd.isHideDeathMessages()
                        ? "<yellow>You will no longer see death messages.</yellow>"
                        : "<green>You will now see death messages.</green>"));
            }
            case "announcements", "motd" -> {
                if (!chatService.isAnnouncementsOptOutEnabled()) {
                    player.sendMessage(mm.deserialize("<gray>Announcements opt-out is disabled on this server.</gray>"));
                    return true;
                }
                pd.setAnnouncementsOptOut(!pd.isAnnouncementsOptOut());
                player.sendMessage(mm.deserialize(pd.isAnnouncementsOptOut()
                        ? "<yellow>You opted out of announcements.</yellow>"
                        : "<green>You opted in to announcements.</green>"));
            }
            case "pm", "pms", "msg", "messages" -> {
                pd.setBlockPrivateMessages(!pd.isBlockPrivateMessages());
                player.sendMessage(mm.deserialize(pd.isBlockPrivateMessages()
                        ? "<yellow>You will no longer receive private messages.</yellow>"
                        : "<green>You will now receive private messages.</green>"));
            }
            default -> player.sendMessage(mm.deserialize("<red>Usage: /" + label + " <join|quit|death|announcements|pm></red>"));
        }

        return true;
    }
}
