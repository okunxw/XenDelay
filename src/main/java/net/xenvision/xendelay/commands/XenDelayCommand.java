package net.xenvision.xendelay.commands;

import net.xenvision.xendelay.utils.ConfigManager;
import net.xenvision.xendelay.utils.LagEffectManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.Locale;

/**
 * Handles /xendelay commands: lag, unlag, reload, language.
 */
public class XenDelayCommand implements CommandExecutor, TabCompleter {
    private final ConfigManager configManager;
    private final LagEffectManager lagEffectManager;

    public XenDelayCommand(ConfigManager configManager, LagEffectManager lagEffectManager) {
        this.configManager = configManager;
        this.lagEffectManager = lagEffectManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "lag":
                if (!sender.hasPermission("xendelay.lag")) {
                    configManager.sendMessage(sender, "no_permission");
                    return true;
                }
                if (args.length < 2) {
                    configManager.sendMessage(sender, "usage_lag");
                    return true;
                }
                Player toLag = Bukkit.getPlayer(args[1]);
                if (toLag == null) {
                    configManager.sendMessage(sender, "player_not_found", args[1]);
                    return true;
                }
                lagEffectManager.applyLag(toLag);
                configManager.sendMessage(sender, "lag_applied", toLag.getName());
                configManager.sendMessage(toLag, "you_are_lagged");
                break;
            case "unlag":
                if (!sender.hasPermission("xendelay.unlag")) {
                    configManager.sendMessage(sender, "no_permission");
                    return true;
                }
                if (args.length < 2) {
                    configManager.sendMessage(sender, "usage_unlag");
                    return true;
                }
                Player toUnlag = Bukkit.getPlayer(args[1]);
                if (toUnlag == null) {
                    configManager.sendMessage(sender, "player_not_found", args[1]);
                    return true;
                }
                lagEffectManager.removeLag(toUnlag);
                configManager.sendMessage(sender, "lag_removed", toUnlag.getName());
                configManager.sendMessage(toUnlag, "you_are_unlagged");
                break;
            case "reload":
                if (!sender.hasPermission("xendelay.reload")) {
                    configManager.sendMessage(sender, "no_permission");
                    return true;
                }
                configManager.reloadConfig();
                configManager.sendMessage(sender, "reload_success");
                break;
            case "language":
                if (!sender.hasPermission("xendelay.language")) {
                    configManager.sendMessage(sender, "no_permission");
                    return true;
                }
                if (args.length < 2) {
                    configManager.sendMessage(sender, "usage_language");
                    return true;
                }
                String lang = args[1].toLowerCase(Locale.ROOT);
                configManager.setLanguage(lang);
                configManager.sendMessage(sender, "language_set", lang);
                break;
            case "unlagall":
                if (!sender.hasPermission("xendelay.unlagall")) {
                    configManager.sendMessage(sender, "no_permission");
                    return true;
                }
                lagEffectManager.removeLagFromAll();
                configManager.sendMessage(sender, "lag_removed_all");
                break;
            default:
                sendHelp(sender);
                break;
            case "gui":
                if (!sender.hasPermission("xendelay.gui")) {
                    configManager.sendMessage(sender, "no_permission");
                    return true;
                }
                if (sender instanceof Player) {
                    new net.xenvision.xendelay.gui.LagGui(plugin, lagEffectManager, configManager).open((Player) sender);
                } else {
                    configManager.sendMessage(sender, "player_only");
                }
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        configManager.sendMessage(sender, "help_main");
        // Or: sender.sendMessage("§b/xendelay lag <игрок>\n§b/xendelay unlag <игрок>\n§b/xendelay reload\n§b/xendelay language <код>\n§b/xendelay unlagall");
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return java.util.Arrays.asList("lag", "unlag", "reload", "language", "unlagall");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("lag") || args[0].equalsIgnoreCase("unlag"))) {
            return null; // Let Bukkit suggest player names
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("language")) {
            return java.util.Arrays.asList("ru", "en", "fr");
        }
        return java.util.Collections.emptyList();
    }
}
