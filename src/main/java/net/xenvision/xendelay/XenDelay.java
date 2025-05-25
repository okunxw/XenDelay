package net.xenvision.xendelay;

import net.xenvision.xendelay.commands.XenDelayCommand;
import net.xenvision.xendelay.config.ConfigWatcher;
import net.xenvision.xendelay.listeners.PlayerActionListener;
import net.xenvision.xendelay.listeners.PlayerMovementListener;
import net.xenvision.xendelay.utils.ConfigManager;
import net.xenvision.xendelay.utils.LagEffectManager;
import org.bukkit.plugin.java.JavaPlugin;

public class XenDelay extends JavaPlugin {
    private ConfigManager configManager;
    private LagEffectManager lagEffectManager;
    private ConfigWatcher configWatcher;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.lagEffectManager = new LagEffectManager(this, configManager);

        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerActionListener(configManager, lagEffectManager), this);
        getServer().getPluginManager().registerEvents(new PlayerMovementListener(lagEffectManager), this);

        // Register command
        getCommand("xendelay").setExecutor(new XenDelayCommand(configManager, lagEffectManager));
        getCommand("xendelay").setTabCompleter(new XenDelayCommand(configManager, lagEffectManager));

        // Start config watcher
        this.configWatcher = new ConfigWatcher(this, configManager);
        this.configWatcher.startWatching();

        getLogger().info("[XenDelay] Plugin enabled!");
    }

    @Override
    public void onDisable() {
        if (configWatcher != null) configWatcher.stopWatching();
        if (lagEffectManager != null) lagEffectManager.removeLagFromAll();
        getLogger().info("[XenDelay] Plugin disabled!");
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public LagEffectManager getLagEffectManager() {
        return lagEffectManager;
    }
}