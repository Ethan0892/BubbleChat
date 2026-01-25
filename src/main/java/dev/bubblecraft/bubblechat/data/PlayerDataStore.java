package dev.bubblecraft.bubblechat.data;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataStore {

    private final Plugin plugin;
    private final File file;

    private final Map<UUID, PlayerData> data = new ConcurrentHashMap<>();

    public PlayerDataStore(Plugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public PlayerData get(UUID uuid) {
        return data.computeIfAbsent(uuid, u -> new PlayerData());
    }

    public void load() {
        if (!file.exists()) return;
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(file);

        boolean persistShout = plugin.getConfig().getBoolean("chat.persist-shout", false);

        for (String key : cfg.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                continue;
            }

            PlayerData pd = new PlayerData();

            String channel = cfg.getString(key + ".channel", "global");
            ChatChannel parsed = ChatChannel.parse(channel);
            if (parsed != null) pd.setChannel(parsed);

            pd.setMutedUntilEpochMs(cfg.getLong(key + ".mutedUntil", 0L));
            pd.setMuteReason(cfg.getString(key + ".muteReason", null));
            pd.setSocialSpy(cfg.getBoolean(key + ".socialSpy", false));
            pd.setShoutMode(persistShout && cfg.getBoolean(key + ".shoutMode", false));
            pd.setAnnouncementsOptOut(cfg.getBoolean(key + ".announcementsOptOut", false));

            pd.setHideJoinMessages(cfg.getBoolean(key + ".hideJoin", false));
            pd.setHideQuitMessages(cfg.getBoolean(key + ".hideQuit", false));
            pd.setHideDeathMessages(cfg.getBoolean(key + ".hideDeath", false));
            pd.setBlockPrivateMessages(cfg.getBoolean(key + ".blockPm", false));

            for (String ignoredUuid : cfg.getStringList(key + ".ignored")) {
                try {
                    pd.getIgnored().add(UUID.fromString(ignoredUuid));
                } catch (IllegalArgumentException ignored) {
                    // ignore
                }
            }

            String lastReply = cfg.getString(key + ".lastReply", null);
            if (lastReply != null) {
                try {
                    pd.setLastReplyTarget(UUID.fromString(lastReply));
                } catch (IllegalArgumentException ignored) {
                    // ignore
                }
            }

            data.put(uuid, pd);
        }

        plugin.getLogger().info("Loaded " + data.size() + " player data entries.");
    }

    public void save() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Could not create plugin data folder: " + plugin.getDataFolder());
            return;
        }

        YamlConfiguration cfg = new YamlConfiguration();

        boolean persistShout = plugin.getConfig().getBoolean("chat.persist-shout", false);

        for (Map.Entry<UUID, PlayerData> entry : data.entrySet()) {
            String key = entry.getKey().toString();
            PlayerData pd = entry.getValue();

            cfg.set(key + ".channel", pd.getChannel().name().toLowerCase());
            cfg.set(key + ".mutedUntil", pd.getMutedUntilEpochMs());
            cfg.set(key + ".muteReason", pd.getMuteReason());
            cfg.set(key + ".socialSpy", pd.isSocialSpy());
            cfg.set(key + ".announcementsOptOut", pd.isAnnouncementsOptOut());

            cfg.set(key + ".hideJoin", pd.isHideJoinMessages());
            cfg.set(key + ".hideQuit", pd.isHideQuitMessages());
            cfg.set(key + ".hideDeath", pd.isHideDeathMessages());
            cfg.set(key + ".blockPm", pd.isBlockPrivateMessages());

            if (persistShout) {
                cfg.set(key + ".shoutMode", pd.isShoutMode());
            }

            cfg.set(key + ".ignored", pd.getIgnored().stream().map(UUID::toString).toList());
            cfg.set(key + ".lastReply", pd.getLastReplyTarget() == null ? null : pd.getLastReplyTarget().toString());
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save data.yml: " + e.getMessage());
        }
    }
}
