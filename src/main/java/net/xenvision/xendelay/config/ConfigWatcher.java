package net.xenvision.xendelay.config;

import java.nio.file.*;
import java.io.IOException;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.File;
import net.xenvision.xendelay.utils.ConfigManager;

public class ConfigWatcher {
    private final JavaPlugin plugin;
    private final Path configPath;
    private final ConfigManager configManager; // Добавляем ссылку на ConfigManager
    private boolean running = true;

    public ConfigWatcher(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configPath = plugin.getDataFolder().toPath().resolve("config.yml");
        this.configManager = configManager;
    }

    public void startWatching() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
                    configPath.getParent().register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                    while (running) {
                        WatchKey key = watchService.take();
                        for (WatchEvent<?> event : key.pollEvents()) {
                            if (event.context().toString().equals("config.yml")) {
                                plugin.getLogger().info("Обнаружено изменение в config.yml, проверка...");

                                File configFile = configPath.toFile();
                                if (configFile.exists() && configFile.length() > 0) {
                                    plugin.getLogger().info("config.yml в порядке, перезагружаюсь...");
                                    plugin.reloadConfig();
                                    configManager.reloadConfig(); // Теперь конфиг действительно обновляется!
                                } else {
                                    plugin.getLogger().severe("Ошибка: config.yml повреждён или пуст!");
                                }
                            }
                        }
                        key.reset();
                    }
                } catch (IOException | InterruptedException e) {
                    plugin.getLogger().severe("Ошибка слежения за config.yml: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(plugin);
    }
}