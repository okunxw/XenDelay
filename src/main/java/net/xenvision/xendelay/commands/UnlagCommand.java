package net.xenvision.xendelay.commands;

import net.xenvision.xendelay.utils.ConfigManager;
import net.xenvision.xendelay.utils.LagEffectManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnlagCommand implements CommandExecutor {
    private final ConfigManager configManager;

    public UnlagCommand(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        // Проверяем права на использование команды
        if (!sender.hasPermission("xendelay.use")) {
            configManager.sendMessage(sender, "error_no_permission", "");
            return true;
        }

        if (args.length == 0) {
            configManager.sendMessage(sender, "help", ""); // Показываем список команд
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            configManager.sendMessage(sender, "error_player_not_found", "");
            return true;
        }

        // Проверяем, если игрок имеет `xendelay.bypass`, его лаг нельзя удалить
        if (target.hasPermission("xendelay.bypass")) {
            configManager.sendMessage(sender, "error_cannot_unlag", target.getName());
            return true;
        }

        if (!LagEffectManager.isLagged(target)) {
            configManager.sendMessage(sender, "error_lag_not_active", target.getName());
            return true;
        }

        LagEffectManager.removeLag(target);
        configManager.sendMessage(sender, "lag_disabled", target.getName());

        return true;
    }
}