package net.xenvision.xendelay;

import net.xenvision.xendelay.commands.XenDelayCommand;
import net.xenvision.xendelay.config.ConfigWatcher;
import net.xenvision.xendelay.gui.MenuBuilder;
import net.xenvision.xendelay.listeners.PlayerActionListener;
import net.xenvision.xendelay.listeners.PlayerMovementListener;
import net.xenvision.xendelay.utils.ConfigManager;
import net.xenvision.xendelay.utils.LagEffectManager;
import net.xenvision.xendelay.utils.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class XenDelay extends JavaPlugin {
    private ConfigManager configManager;
    private LagEffectManager lagEffectManager;
    private ConfigWatcher configWatcher;
    private MenuManager menuManager;
    private MenuBuilder menuBuilder;

    @Override
    public void onEnable() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new net.xenvision.xendelay.placeholder.XenDelayExpansion(lagEffectManager).register();
            getLogger().info("PlaceholderAPI hook registered!");
        }

        this.configManager = new ConfigManager(this);
        this.lagEffectManager = new LagEffectManager(this, configManager);

        this.menuManager = new MenuManager(this);
        this.menuBuilder = new MenuBuilder(this, lagEffectManager, configManager, menuManager);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerActionListener(configManager, lagEffectManager), this);
        getServer().getPluginManager().registerEvents(new PlayerMovementListener(lagEffectManager), this);

        // Register command
        getCommand("xendelay").setExecutor(new XenDelayCommand(configManager, lagEffectManager));
        getCommand("xendelay").setTabCompleter(new XenDelayCommand(configManager, lagEffectManager));

        // Start config watcher
        this.configWatcher = new ConfigWatcher(this, configManager);
        this.configWatcher.startWatching();
    }

    @Override
    public void onDisable() {
        if (configWatcher != null) configWatcher.stopWatching();
        if (lagEffectManager != null) lagEffectManager.removeLagFromAll();
        getLogger().info("Plugin disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LagEffectManager getLagEffectManager() {
        return lagEffectManager;
    }
}