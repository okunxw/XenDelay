package net.xenvision.xendelay.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;

public class MenuManager {
    
    private final Plugin plugin;
    private FileConfiguration menuConfig;

    public MenuManager(Plugin plugin) {
        this.plugin = plugin;
        loadMenu();
    }

    public void loadMenu() {
        File file = new File(plugin.getDataFolder(), "menu.yml");
        
        if (!file.exists()) {
            plugin.saveResource("menu.yml", false);
        }
        
        this.menuConfig = YamlConfiguration.loadConfiguration(file);
    }

    public void reloadMenu() {
        loadMenu();
    }

    public FileConfiguration getMenuConfig() {
        return menuConfig;
    }
}
