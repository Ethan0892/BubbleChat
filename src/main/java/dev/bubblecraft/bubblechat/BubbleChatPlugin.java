package dev.bubblecraft.bubblechat;

import dev.bubblecraft.bubblechat.chat.ChatListener;
import dev.bubblecraft.bubblechat.chat.CommandListener;
import dev.bubblecraft.bubblechat.chat.PlayerDeathListener;
import dev.bubblecraft.bubblechat.chat.PlayerJoinListener;
import dev.bubblecraft.bubblechat.chat.PlayerQuitListener;
import dev.bubblecraft.bubblechat.command.BubbleChatCommand;
import dev.bubblecraft.bubblechat.command.ChannelCommand;
import dev.bubblecraft.bubblechat.command.GlobalCommand;
import dev.bubblecraft.bubblechat.command.IgnoreCommand;
import dev.bubblecraft.bubblechat.command.IgnoreListCommand;
import dev.bubblecraft.bubblechat.command.LocalCommand;
import dev.bubblecraft.bubblechat.command.MuteCommand;
import dev.bubblecraft.bubblechat.command.PrivateMessageCommand;
import dev.bubblecraft.bubblechat.command.ReplyCommand;
import dev.bubblecraft.bubblechat.command.SocialSpyCommand;
import dev.bubblecraft.bubblechat.command.MeCommand;
import dev.bubblecraft.bubblechat.command.ToggleCommand;
import dev.bubblecraft.bubblechat.command.UnmuteCommand;
import dev.bubblecraft.bubblechat.command.ChatMuteCommand;
import dev.bubblecraft.bubblechat.command.ShoutCommand;
import dev.bubblecraft.bubblechat.command.AnnouncementsCommand;
import dev.bubblecraft.bubblechat.command.MotdCommand;
import dev.bubblecraft.bubblechat.command.ClearChatCommand;
import dev.bubblecraft.bubblechat.data.PlayerDataStore;
import dev.bubblecraft.bubblechat.hook.ChatColor2Hook;
import dev.bubblecraft.bubblechat.hook.DiscordSrvHook;
import dev.bubblecraft.bubblechat.hook.PlaceholderApiHook;
import dev.bubblecraft.bubblechat.rules.RuleEngine;
import dev.bubblecraft.bubblechat.service.ChatService;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public final class BubbleChatPlugin extends JavaPlugin {

    private PlayerDataStore playerDataStore;
    private ChatService chatService;
    private PlaceholderApiHook placeholderApiHook;
    private ChatColor2Hook chatColor2Hook;
    private DiscordSrvHook discordSrvHook;
    private RuleEngine ruleEngine;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Metrics (bStats) - https://bstats.org/plugin/bukkit/BubbleChat/28824
        new Metrics(this, 28824);

        this.playerDataStore = new PlayerDataStore(this);
        this.playerDataStore.load();

        this.placeholderApiHook = PlaceholderApiHook.tryEnable(this);
        this.chatColor2Hook = ChatColor2Hook.tryEnable(this, placeholderApiHook);
        this.discordSrvHook = DiscordSrvHook.tryEnable(this);

        this.ruleEngine = new RuleEngine(this);
        this.ruleEngine.load();

        this.chatService = new ChatService(this, playerDataStore, placeholderApiHook, chatColor2Hook, discordSrvHook, ruleEngine);

        if (placeholderApiHook != null) {
            placeholderApiHook.registerExpansion(chatService);
        }

        getServer().getPluginManager().registerEvents(new ChatListener(this, chatService), this);
        getServer().getPluginManager().registerEvents(new CommandListener(chatService), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(chatService), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(chatService), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(chatService), this);

        getCommand("bubblechat").setExecutor(new BubbleChatCommand(this, chatService));
        getCommand("ch").setExecutor(new ChannelCommand(chatService));
        getCommand("local").setExecutor(new LocalCommand(chatService));
        getCommand("global").setExecutor(new GlobalCommand(chatService));
        getCommand("msg").setExecutor(new PrivateMessageCommand(chatService));
        getCommand("reply").setExecutor(new ReplyCommand(chatService));
        getCommand("socialspy").setExecutor(new SocialSpyCommand(chatService));
        getCommand("mute").setExecutor(new MuteCommand(chatService));
        getCommand("unmute").setExecutor(new UnmuteCommand(chatService));
        getCommand("ignore").setExecutor(new IgnoreCommand(chatService));
        getCommand("ignorelist").setExecutor(new IgnoreListCommand(chatService));
        getCommand("chatmute").setExecutor(new ChatMuteCommand(chatService));
        getCommand("shout").setExecutor(new ShoutCommand(chatService));
        getCommand("announcements").setExecutor(new AnnouncementsCommand(chatService));
        getCommand("motd").setExecutor(new MotdCommand(chatService));
        getCommand("clearchat").setExecutor(new ClearChatCommand(chatService));
        getCommand("me").setExecutor(new MeCommand(chatService));
        getCommand("toggle").setExecutor(new ToggleCommand(chatService));

        chatService.startAutosaveTask();
    }

    @Override
    public void onDisable() {
        if (chatService != null) {
            chatService.stopAutosaveTask();
        }
        if (playerDataStore != null) {
            playerDataStore.save();
        }
    }
}
