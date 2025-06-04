package net.xenvision.xendelay.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages lag effects for players with optimized performance.
 * Uses UUID internally to avoid memory leaks.
 */
public class LagEffectManager {
    
    private final Plugin plugin;
    private final ConfigManager configManager;
    private final Map<UUID, BukkitTask> activeLagEffects = new ConcurrentHashMap<>();
    
    // Cached configuration values
    private volatile double lagIntensity;
    private volatile int lagFrequency;
    private volatile double lagDuration;
    private volatile double lagMessageProbability;
    private volatile boolean enableLagMessages;
    private boolean isConfigCached = false;

    public LagEffectManager(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Applies lag effect to player with atomic operations.
     */
    public void applyLagEffect(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Atomic check and task creation
        activeLagEffects.computeIfAbsent(playerId, id -> {
            loadConfigurationSettings();
            int maxTickCount = lagDuration > 0 ? (int) Math.round(lagDuration * 20 / lagFrequency) : -1;
            
            BukkitRunnable effectTask = new BukkitRunnable() {
                private int tickCounter = 0;

                @Override
                public void run() {
                    Player targetPlayer = Bukkit.getPlayer(id);
                    // Remove if player is offline
                    if (targetPlayer == null || !targetPlayer.isOnline()) {
                        removeLagEffect(id);
                        return;
                    }
                    
                    // Remove when duration expires
                    if (maxTickCount > 0 && tickCounter >= maxTickCount) {
                        removeLagEffect(id);
                        return;
                    }
                    
                    // Precalculate displacement
                    double offsetX = Math.random() * lagIntensity - (lagIntensity / 2);
                    double offsetZ = Math.random() * lagIntensity - (lagIntensity / 2);
                    targetPlayer.teleportAsync(targetPlayer.getLocation().add(offsetX, 0, offsetZ));
                    
                    // Random lag messages
                    if (enableLagMessages && Math.random() < lagMessageProbability) {
                        configManager.sendMessage(targetPlayer, "error_packet_loss", "");
                    }
                    
                    tickCounter++;
                }
            };
            
            return effectTask.runTaskTimer(plugin, 0, lagFrequency);
        });
    }

    /**
     * Loads and caches configuration settings.
     */
    private synchronized void loadConfigurationSettings() {
        if (!isConfigCached) {
            lagIntensity = configManager.getConfig().getDouble("lag_settings.lag_intensity", 0.7);
            lagFrequency = configManager.getConfig().getInt("lag_settings.lag_frequency", 20);
            lagDuration = configManager.getConfig().getDouble("lag_settings.lag_duration", -1);
            lagMessageProbability = configManager.getConfig().getDouble("lag_settings.lag_message_chance", 0.2);
            enableLagMessages = configManager.getConfig().getBoolean("lag_settings.enable_lag_messages", true);
            isConfigCached = true;
        }
    }

    /**
     * Removes lag effect from player.
     */
    public void removeLagEffect(Player player) {
        removeLagEffect(player.getUniqueId());
    }

    public void removeLagEffect(UUID playerId) {
        BukkitTask task = activeLagEffects.remove(playerId);
        
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Removes lag effects from all players.
     */
    public void clearAllLagEffects() {
        activeLagEffects.keySet().forEach(this::removeLagEffect);
    }

    /**
     * Checks if player has active lag effect.
     */
    public boolean hasActiveLagEffect(Player player) {
        return activeLagEffects.containsKey(player.getUniqueId());
    }

    /**
     * Returns an unmodifiable set of players with active lag effects.
     */
    public Set<Player> getAffectedPlayers() {
        return Collections.unmodifiableSet(
            activeLagEffects.keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet())
        );
    }
}
