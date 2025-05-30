package net.xenvision.xendelay.commands;

import net.xenvision.xendelay.utils.ConfigManager;
import net.xenvision.xendelay.utils.LagEffectManager;
import net.xenvision.xendelay.gui.MenuBuilder;
import net.xenvision.xendelay.utils.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.List;

public class XenDelayCommand implements CommandExecutor, TabCompleter {
    private final ConfigManager configManager;
    private final LagEffectManager lagEffectManager;
    private final MenuBuilder menuBuilder;
    private final MenuManager menuManager;

    public XenDelayCommand(ConfigManager configManager, LagEffectManager lagEffectManager, MenuBuilder menuBuilder, MenuManager menuManager) {
        this.configManager = configManager;
        this.lagEffectManager = lagEffectManager;
        this.menuBuilder = menuBuilder;
        this.menuManager = menuManager;
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
            case "gui":
                if (!sender.hasPermission("xendelay.gui")) {
                    configManager.sendMessage(sender, "no_permission");
                    return true;
                }
                if (sender instanceof Player) {
                    menuBuilder.open((Player) sender);
                } else {
                    configManager.sendMessage(sender, "only_players");
                }
                break;
            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        configManager.sendMessage(sender, "help_main");
    }

    @Override
    public java.util.List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>();
            if (sender.hasPermission("xendelay.lag")) options.add("lag");
            if (sender.hasPermission("xendelay.unlag")) options.add("unlag");
            if (sender.hasPermission("xendelay.reload")) options.add("reload");
            if (sender.hasPermission("xendelay.language")) options.add("language");
            if (sender.hasPermission("xendelay.unlagall")) options.add("unlagall");
            if (sender.hasPermission("xendelay.gui")) options.add("gui");
            // Фильтруем по уже введённому куску слова
            if (args[0].isEmpty()) return options;
            String arg = args[0].toLowerCase();
            return options.stream()
                    .filter(opt -> opt.startsWith(arg))
                    .collect(Collectors.toList());
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("lag") || args[0].equalsIgnoreCase("unlag"))) {
            return null; // Let Bukkit suggest player names
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("language") && sender.hasPermission("xendelay.language")) {
            return Arrays.asList("ru", "en", "fr");
        }
        return Collections.emptyList();
    }
}