package net.xenvision.xendelay;

import net.xenvision.xendelay.commands.XenDelayCommand;
import net.xenvision.xendelay.config.ConfigWatcher;
import net.xenvision.xendelay.listeners.PlayerActionListener;
import net.xenvision.xendelay.listeners.PlayerMovementListener;
import net.xenvision.xendelay.placeholder.XenDelayExpansion;
import net.xenvision.xendelay.utils.*;
import net.xenvision.xendelay.gui.MenuBuilder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Level;

public class XenDelay extends JavaPlugin {

    private ConfigManager configManager;
    private LagEffectManager lagEffectManager;
    private ConfigWatcher configWatcher;
    private MenuManager menuManager;
    private CrashManager crashManager;
    private MenuBuilder menuBuilder;

    @Override
    public void onEnable() {
        // Initialize core components
        this.configManager = new ConfigManager(this);
        this.lagEffectManager = new LagEffectManager(this, configManager);

        // Initialize optional components
        this.menuManager = new MenuManager(this);
        this.crashManager = new CrashManager(this);
        this.menuBuilder = new MenuBuilder(
                this,
                lagEffectManager,
                configManager,
                menuManager,
                crashManager
        );

        // Register PlaceholderAPI if available
        registerPlaceholderAPI();

        // Register event listeners
        registerListeners();

        // Register commands
        registerCommands();

        // Set up configuration file watcher
        setupConfigWatcher();

        getLogger().info("Plugin enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Stop configuration file watcher
        if (configWatcher != null) {
            configWatcher.stopWatching();
        }

        // Remove all active lag effects
        if (lagEffectManager != null) {
            lagEffectManager.removeLagFromAll();
        }

        getLogger().info("Plugin disabled!");
    }

    private void registerPlaceholderAPI() {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new XenDelayExpansion(lagEffectManager).register();
            getLogger().info("PlaceholderAPI integration registered!");
        }
    }

    private void registerListeners() {
        var pluginManager = getServer().getPluginManager();

        pluginManager.registerEvents(
                new PlayerActionListener(configManager, lagEffectManager),
                this
        );

        pluginManager.registerEvents(
                new PlayerMovementListener(lagEffectManager),
                this
        );

        pluginManager.registerEvents(menuBuilder, this);
    }

    private void registerCommands() {
        XenDelayCommand command = new XenDelayCommand(
                configManager,
                lagEffectManager,
                menuBuilder,
                menuManager,
                crashManager
        );

        getCommand("xendelay").setExecutor(command);
        getCommand("xendelay").setTabCompleter(command);
    }

    private void setupConfigWatcher() {
        try {
            this.configWatcher = new ConfigWatcher(this, configManager);
            this.configWatcher.startWatching();
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize configuration watcher", e);
        }
    }

    // Accessor methods (only include if needed externally)
    public ConfigManager getConfigManager() {
        return configManager;
    }
}