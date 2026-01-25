package dev.bubblecraft.bubblechat.command;

import dev.bubblecraft.bubblechat.data.PlayerData;
import dev.bubblecraft.bubblechat.service.ChatService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class AnnouncementsCommand implements CommandExecutor {

    private final ChatService chatService;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public AnnouncementsCommand(ChatService chatService) {
        this.chatService = chatService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }

        if (!player.hasPermission("bubblechat.announcements.optout")) {
            player.sendMessage(mm.deserialize("<red>You don't have permission to do that.</red>"));
            return true;
        }

        if (!chatService.isAnnouncementsOptOutEnabled()) {
            player.sendMessage(mm.deserialize("<gray>Announcements opt-out is disabled on this server.</gray>"));
            return true;
        }

        PlayerData pd = chatService.data(player);

        if (args.length == 0 || args[0].equalsIgnoreCase("toggle")) {
            pd.setAnnouncementsOptOut(!pd.isAnnouncementsOptOut());
            player.sendMessage(mm.deserialize(pd.isAnnouncementsOptOut()
                    ? "<yellow>You opted out of announcements.</yellow>"
                    : "<green>You opted in to announcements.</green>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("on")) {
            pd.setAnnouncementsOptOut(false);
            player.sendMessage(mm.deserialize("<green>You opted in to announcements.</green>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {
            pd.setAnnouncementsOptOut(true);
            player.sendMessage(mm.deserialize("<yellow>You opted out of announcements.</yellow>"));
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            player.sendMessage(mm.deserialize("<gray>Announcements: </gray>" + (pd.isAnnouncementsOptOut() ? "<yellow>off</yellow>" : "<green>on</green>")));
            return true;
        }

        player.sendMessage(mm.deserialize("<red>Usage: /" + label + " [on|off|toggle|status]</red>"));
        return true;
    }
}
