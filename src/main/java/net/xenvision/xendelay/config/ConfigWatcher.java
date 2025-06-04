package net.xenvision.xendelay.config;

import java.nio.file.*;
import java.io.IOException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.io.File;
import net.xenvision.xendelay.utils.ConfigManager;

/**
 * Monitors config.yml changes and triggers reloads
 */
public class ConfigWatcher {

    private final JavaPlugin plugin;
    private final Path configPath;
    private final ConfigManager configManager;
    private volatile boolean running = false;
    private Thread watchThread;
    private BukkitTask pendingReloadTask; // For delayed reload operations
    private long lastModified; // Timestamp of last file modification

    public ConfigWatcher(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configPath = plugin.getDataFolder().toPath().resolve("config.yml");
        this.configManager = configManager;
        this.lastModified = configPath.toFile().lastModified(); // Initialize timestamp
    }

    public void startWatching() {
        if (running) return;
        running = true;

        // Ensure directories exist
        try {
            Files.createDirectories(configPath.getParent());
        } catch (IOException e) {
            plugin.getLogger().severe("[XenDelay] Failed to create directories: " + e.getMessage());
            running = false;
            return;
        }

        watchThread = new Thread(() -> {
            try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                Path parentDir = configPath.getParent();
                parentDir.register(watchService, 
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_CREATE);

                while (running) {
                    WatchKey key = watchService.take();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();
                        
                        // Handle event overflow cases
                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            handleOverflow();
                            continue;
                        }

                        Path changed = ((WatchEvent<Path>) event).context();
                        if (changed != null && changed.toString().equals("config.yml")) {
                            handleConfigChange();
                        }
                    }

                    if (!key.reset()) {
                        plugin.getLogger().warning("[XenDelay] WatchKey reset failed");
                        break;
                    }
                }

            } catch (IOException | InterruptedException | ClosedWatchServiceException e) {
                if (running) { // Only log if intentionally running
                    plugin.getLogger().severe("[XenDelay] ConfigWatcher error: " + e.getMessage());
                }
            }
        }, "XenDelay-ConfigWatcher");
        
        watchThread.setDaemon(true);
        watchThread.start();
    }

    private void handleConfigChange() {
        File configFile = configPath.toFile();
        long currentModified = configFile.lastModified();
        
        // Verify file actually changed
        if (currentModified <= lastModified) return;
        
        lastModified = currentModified; // Update timestamp

        // Cooldown to handle multiple rapid changes
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        // Validate file integrity
        if (!configFile.exists() || configFile.length() == 0) {
            plugin.getLogger().severe("[XenDelay] ERROR: config.yml missing or empty!");
            return;
        }

        // Cancel previous scheduled reload if exists
        if (pendingReloadTask != null) {
            pendingReloadTask.cancel();
        }

        // Schedule reload in main thread (2 seconds delay)
        pendingReloadTask = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.reloadConfig();
            configManager.reloadConfig();
            plugin.getLogger().info("[XenDelay] Config reloaded successfully");
        }, 40); // 40 ticks = 2 seconds
    }

    private void handleOverflow() {
        plugin.getLogger().warning("[XenDelay] WatchService overflow detected");
        // Force config change check
        handleConfigChange();
    }

    public void stopWatching() {
        running = false;
        
        // Cancel any pending reload
        if (pendingReloadTask != null) {
            pendingReloadTask.cancel();
        }
        
        // Graceful thread termination
        if (watchThread != null && watchThread.isAlive()) {
            watchThread.interrupt();

            try {
                watchThread.join(1000); // Wait for thread termination
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
