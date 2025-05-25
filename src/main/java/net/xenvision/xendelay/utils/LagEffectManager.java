package net.xenvision.xendelay.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class LagEffectManager {
    private static final Map<Player, BukkitRunnable> lagTasks = new HashMap<>();
    private static ConfigManager configManager;
    private static final Set<Player> laggedPlayers = new HashSet<>();

    public static void setConfigManager(ConfigManager manager) {
        configManager = manager;
    }

    public static void applyLag(Player player) {
        if (lagTasks.containsKey(player)) {
            return;
        }

        double lagIntensity = configManager.getConfig().getDouble("lag_settings.lag_intensity", 0.7);
        int lagFrequency = configManager.getConfig().getInt("lag_settings.lag_frequency", 20);
        double lagDuration = configManager.getConfig().getDouble("lag_settings.lag_duration", -1);
        double lagMessageChance = configManager.getConfig().getDouble("lag_settings.lag_message_chance", 0.2);
        boolean enableLagMessages = configManager.getConfig().getBoolean("lag_settings.enable_lag_messages", true);

        BukkitRunnable lagTask = new BukkitRunnable() {
            int timer = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    removeLag(player);
                    return;
                }

                if (lagDuration > 0 && timer >= Math.round(lagDuration * 20)) {
                    removeLag(player);
                    cancel();
                    return;
                }

                player.teleport(player.getLocation().add(Math.random() * lagIntensity - (lagIntensity / 2), 0, Math.random() * lagIntensity - (lagIntensity / 2)));

                if (enableLagMessages && Math.random() < lagMessageChance) {
                    configManager.sendMessage(player, "error_packet_loss", "");
                }

                timer++;
            }
        };

        lagTask.runTaskTimer(Objects.requireNonNull(Bukkit.getPluginManager().getPlugin("XenDelay")), 0L, lagFrequency);
        lagTasks.put(player, lagTask);
    }

    public static void removeLag(Player player) {
        if (lagTasks.containsKey(player)) {
            lagTasks.get(player).cancel();
            lagTasks.remove(player);
        }
    }

    public static boolean isLagged(Player player) {
        return lagTasks.containsKey(player);
    }

    public static Set<Player> getLaggedPlayers() {
        return new HashSet<>(laggedPlayers);
    }
}