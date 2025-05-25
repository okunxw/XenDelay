package net.xenvision.xendelay.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages lag effects for players.
 * Uses UUID internally to avoid memory leaks.
 */
public class LagEffectManager {
    private final Plugin plugin;
    private final ConfigManager configManager;
    private final Map<UUID, BukkitRunnable> lagTasks = new ConcurrentHashMap<>();

    public LagEffectManager(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Applies lag effect to player.
     */
    public void applyLag(Player player) {
        UUID uuid = player.getUniqueId();
        if (lagTasks.containsKey(uuid)) return;

        double lagIntensity = configManager.getConfig().getDouble("lag_settings.lag_intensity", 0.7);
        int lagFrequency = configManager.getConfig().getInt("lag_settings.lag_frequency", 20);
        double lagDuration = configManager.getConfig().getDouble("lag_settings.lag_duration", -1);
        double lagMessageChance = configManager.getConfig().getDouble("lag_settings.lag_message_chance", 0.2);
        boolean enableLagMessages = configManager.getConfig().getBoolean("lag_settings.enable_lag_messages", true);

        BukkitRunnable lagTask = new BukkitRunnable() {
            int timer = 0;

            @Override
            public void run() {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    removeLag(uuid);
                    cancel();
                    return;
                }
                if (lagDuration > 0 && timer >= Math.round(lagDuration * 20 / lagFrequency)) {
                    removeLag(uuid);
                    cancel();
                    return;
                }
                p.teleport(p.getLocation().add(Math.random() * lagIntensity - (lagIntensity / 2), 0, Math.random() * lagIntensity - (lagIntensity / 2)));
                if (enableLagMessages && Math.random() < lagMessageChance) {
                    configManager.sendMessage(p, "error_packet_loss", "");
                }
                timer++;
            }
        };
        lagTask.runTaskTimer(plugin, 0L, lagFrequency);
        lagTasks.put(uuid, lagTask);
    }

    /**
     * Removes lag effect from player.
     */
    public void removeLag(Player player) {
        removeLag(player.getUniqueId());
    }

    public void removeLag(UUID uuid) {
        BukkitRunnable task = lagTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Removes lag from all players.
     */
    public void removeLagFromAll() {
        for (UUID uuid : new ArrayList<>(lagTasks.keySet())) {
            removeLag(uuid);
        }
    }

    /**
     * Checks if player is lagged.
     */
    public boolean isLagged(Player player) {
        return lagTasks.containsKey(player.getUniqueId());
    }

    /**
     * Returns a set of lagged players.
     */
    public Set<Player> getLaggedPlayers() {
        return lagTasks.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}