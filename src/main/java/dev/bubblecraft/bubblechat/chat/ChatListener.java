package dev.bubblecraft.bubblechat.chat;

import dev.bubblecraft.bubblechat.service.ChatService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

public final class ChatListener implements Listener {

    private final Plugin plugin;
    private final ChatService chatService;

    public ChatListener(Plugin plugin, ChatService chatService) {
        this.plugin = plugin;
        this.chatService = chatService;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player sender = event.getPlayer();
        Component original = event.message();

        Object signedMessage = null;
        try {
            Method m = event.getClass().getMethod("signedMessage");
            signedMessage = m.invoke(event);
        } catch (Throwable ignored) {
        }

        final Object signedMessageFinal = signedMessage;

        // Compute filtering/rules/recipients on the main thread.
        Future<ChatService.ChatPlan> f = Bukkit.getScheduler().callSyncMethod(plugin, () -> chatService.planChat(sender, original, signedMessageFinal));

        ChatService.ChatPlan plan;
        try {
            plan = f.get();
        } catch (Exception e) {
            // Fail open: keep old safe path.
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> chatService.handleChat(sender, original));
            return;
        }

        if (plan.cancelled()) {
            event.setCancelled(true);
            return;
        }

        boolean applied = applyViewersAndRenderer(event, plan);
        if (!applied) {
            // Fallback: keep chat working (but message removal won't be available).
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> chatService.handleChat(sender, original));
        }
    }

    private boolean applyViewersAndRenderer(AsyncChatEvent event, ChatService.ChatPlan plan) {
        try {
            Set<UUID> allowed = plan.viewerUuids();

            // Filter viewers
            try {
                Method viewersMethod = event.getClass().getMethod("viewers");
                Object viewersObj = viewersMethod.invoke(event);
                if (viewersObj instanceof Collection<?> viewers) {
                    viewers.removeIf(v -> (v instanceof Player p) && !allowed.contains(p.getUniqueId()));
                }
            } catch (Throwable ignored) {
            }

            // Set renderer
            Class<?> chatRendererClass = Class.forName("io.papermc.paper.chat.ChatRenderer");
            Object renderer = Proxy.newProxyInstance(
                    chatRendererClass.getClassLoader(),
                    new Class<?>[]{chatRendererClass},
                    (proxy, method, args) -> {
                        if (!method.getName().equals("render")) return null;
                        // args: (Player source, Component sourceDisplayName, Component message, Audience viewer)
                        Object viewer = (args != null && args.length >= 4) ? args[3] : null;
                        if (viewer instanceof Player p) {
                            return plan.renderedFor(p);
                        }
                        return plan.renderedBase();
                    }
            );

            Method rendererMethod = event.getClass().getMethod("renderer", chatRendererClass);
            rendererMethod.invoke(event, renderer);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
