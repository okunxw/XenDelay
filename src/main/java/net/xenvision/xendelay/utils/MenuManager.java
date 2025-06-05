package net.xenvision.xendelay.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;

public class MenuManager {

    private final Plugin plugin;
    private final File configFile;
    private FileConfiguration menuConfig;
    private long lastModified; // Tracks file modification timestamp

    public MenuManager(Plugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "menu.yml");
        // Configuration loading is deferred until first access
    }

    private synchronized void loadMenu() {
        // Skip reload if file hasn't changed
        if (menuConfig != null && configFile.lastModified() == lastModified) {
            return;
        }

        // Create default config if missing
        if (!configFile.exists()) {
            try {
                plugin.saveResource("menu.yml", false);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().severe("menu.yml resource not found in JAR file");
                // Create empty file to prevent NPE
                try {
                    if (configFile.createNewFile()) {
                        plugin.getLogger().info("Created empty menu.yml file");
                    }

                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to create menu.yml: " + e.getMessage());
                }
            }
        }

        try {
            // Load configuration with UTF-8 encoding
            menuConfig = YamlConfiguration.loadConfiguration(configFile);
            lastModified = configFile.lastModified();
        } catch (Exception ex) {
            plugin.getLogger().severe("Error loading menu.yml: " + ex.getMessage());
            menuConfig = new YamlConfiguration(); // Fallback to empty config
        }
    }

    public void reloadMenu() {
        loadMenu(); // Synchronization handled internally
    }

    public synchronized FileConfiguration getMenuConfig() {
        if (menuConfig == null) {
            loadMenu(); // Lazy initialization on first access
        }

        return menuConfig;
    }
}