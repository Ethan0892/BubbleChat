package dev.bubblecraft.bubblechat;

import dev.bubblecraft.bubblechat.rules.RuleEngine;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

final class RuleEngineSmallCapsTest {

    @TempDir
    File tempDir;

    @Test
    void replySmallCapsTransformsMessage() throws Exception {
        // Arrange: write a minimal rules.yml in a fake plugin data folder
        File rulesFile = new File(tempDir, "rules.yml");
        try (FileWriter w = new FileWriter(rulesFile, StandardCharsets.UTF_8)) {
            w.write("rules:\n");
            w.write("  - id: test-smallcaps\n");
            w.write("    enabled: true\n");
            w.write("    scope: chat\n");
            w.write("    match: '(?i)\\bhello\\b'\n");
            w.write("    actions:\n");
            w.write("      - type: reply\n");
            w.write("        message: '<gray>Hello {player}</gray>'\n");
            w.write("        smallcaps: true\n");
        }

        Plugin plugin = mock(Plugin.class);
        when(plugin.getDataFolder()).thenReturn(tempDir);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("test"));

        // Config: rules enabled + points to rules.yml
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.set("rules.enabled", true);
        cfg.set("rules.file", "rules.yml");
        when(plugin.getConfig()).thenReturn(cfg);

        RuleEngine engine = new RuleEngine(plugin);
        engine.load();

        Player player = mock(Player.class);
        when(player.getName()).thenReturn("Steve");
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());

        ArgumentCaptor<Component> captor = ArgumentCaptor.forClass(Component.class);

        // Act
        engine.applyChat(player, "hello", null);

        // Assert
        verify(player, atLeastOnce()).sendMessage(captor.capture());
        String plain = PlainTextComponentSerializer.plainText().serialize(captor.getValue());
        assertEquals("ʜᴇʟʟᴏ ꜱᴛᴇᴠᴇ", plain);
    }
}
