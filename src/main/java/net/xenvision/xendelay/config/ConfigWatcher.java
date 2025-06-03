package net.xenvision.xendelay.config;

import java.nio.file.*;
import java.io.IOException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import net.xenvision.xendelay.utils.ConfigManager;

/**
 * Watches for config.yml changes and triggers reloads.
 */
public class ConfigWatcher {

    private final JavaPlugin plugin;
    private final Path configPath;
    private final ConfigManager configManager;
    private boolean running = false;
    private Thread watchThread;

    public ConfigWatcher(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configPath = plugin.getDataFolder().toPath().resolve("config.yml");
        this.configManager = configManager;
    }

    public void startWatching() {
        if (running) return;
        running = true;
        watchThread = new Thread(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                configPath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

                while (running) {
                    WatchKey key = watchService.take();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        if (event.context().toString().equals("config.yml")) {
                            plugin.getLogger().info("[XenDelay] Detected config.yml change, checking...");

                            File configFile = configPath.toFile();
                            if (configFile.exists() && configFile.length() > 0) {
                                plugin.getLogger().info("[XenDelay] config.yml looks fine, reloading...");
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    plugin.reloadConfig();
                                    configManager.reloadConfig();
                                });
                            } else {
                                plugin.getLogger().severe("[XenDelay] ERROR: config.yml is corrupted or empty!");
                            }
                        }
                    }

                    key.reset();
                }
            } catch (IOException | InterruptedException e) {
                plugin.getLogger().severe("[XenDelay] ConfigWatcher error: " + e.getMessage());
            }
        }, "XenDelay-ConfigWatcher");
        watchThread.setDaemon(true);
        watchThread.start();
    }

    public void stopWatching() {
        running = false;
        if (watchThread != null && watchThread.isAlive()) {
            watchThread.interrupt();
        }
    }
}
