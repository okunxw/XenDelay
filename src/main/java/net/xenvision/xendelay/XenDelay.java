package net.xenvision.xendelay;

import net.xenvision.xendelay.commands.FakelagCommand;
import net.xenvision.xendelay.commands.UnlagCommand;
import net.xenvision.xendelay.commands.XenDelayReloadCommand;
import net.xenvision.xendelay.listeners.PlayerMovementListener;
import net.xenvision.xendelay.listeners.PlayerActionListener;
import net.xenvision.xendelay.utils.ConfigManager;
import net.xenvision.xendelay.utils.LagEffectManager;
import net.xenvision.xendelay.config.ConfigWatcher;
import org.bukkit.plugin.java.JavaPlugin;

public class XenDelay extends JavaPlugin {

    private ConfigManager configManager;

    @Override
    public void onEnable() {
        getLogger().info("XenDelay успешно запущен.");

        // Создаём `config.yml`, если его нет (не перезаписывает)
        saveDefaultConfig();

        // Инициализация конфигурации
        configManager = new ConfigManager(this);

        // Запуск авто-перезагрузки `config.yml`
        new ConfigWatcher(this, configManager).startWatching();

        // Передаём `ConfigManager` в `LagEffectManager`
        LagEffectManager.setConfigManager(configManager);

        // Регистрация команд (с проверкой `null`)
        if (getCommand("fakelag") != null) getCommand("fakelag").setExecutor(new FakelagCommand(configManager));
        if (getCommand("unfakelag") != null) getCommand("unfakelag").setExecutor(new UnlagCommand(configManager));
        if (getCommand("xendelay") != null) getCommand("xendelay").setExecutor(new XenDelayReloadCommand(configManager));

        // Регистрация слушателей событий
        getServer().getPluginManager().registerEvents(new PlayerMovementListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerActionListener(configManager), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("XenDelay выключен.");
    }
}