package net.xenvision.xendelay.commands;

import net.xenvision.xendelay.utils.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class XenDelayReloadCommand implements CommandExecutor {
    private final ConfigManager configManager;

    public XenDelayReloadCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Проверяем права на использование команды
        if (!sender.hasPermission("xendelay.admin")) {
            configManager.sendMessage(sender, "error_no_permission", "");
            return true;
        }

        if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
            configManager.sendMessage(sender, "error_usage_reload", "");
            return true;
        }

        configManager.reloadConfig();
        configManager.sendMessage(sender, "config_reloaded", "");

        return true;
    }
}