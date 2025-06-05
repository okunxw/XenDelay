package net.xenvision.xendelay.commands;

import net.xenvision.xendelay.utils.ConfigManager;
import net.xenvision.xendelay.utils.LagEffectManager;
import net.xenvision.xendelay.gui.MenuBuilder;
import net.xenvision.xendelay.utils.MenuManager;
import net.xenvision.xendelay.utils.CrashManager;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class XenDelayCommand implements CommandExecutor, TabCompleter {

    // Constants for messages and permissions
    private static final String NO_PERMISSION_MSG = "no_permission";
    private static final String PLAYER_NOT_FOUND_MSG = "player_not_found";

    private static final List<String> CRASH_TYPES = Arrays.asList("entity", "sign", "payload");
    private static final List<String> LANGUAGES = Arrays.asList("ru", "en", "fr");

    private static final Set<String> SUBCOMMANDS = Set.of(
            "lag", "unlag", "crash", "reload", "language", "unlagall", "gui"
    );

    private final ConfigManager configManager;
    private final LagEffectManager lagEffectManager;
    private final MenuBuilder menuBuilder;
    private final MenuManager menuManager;
    private final CrashManager crashManager;

    public XenDelayCommand(ConfigManager configManager, LagEffectManager lagEffectManager,
                           MenuBuilder menuBuilder, MenuManager menuManager, CrashManager crashManager) {
        this.configManager = configManager;
        this.lagEffectManager = lagEffectManager;
        this.menuBuilder = menuBuilder;
        this.menuManager = menuManager;
        this.crashManager = crashManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "lag" -> handleLag(sender, args);
            case "unlag" -> handleUnlag(sender, args);
            case "crash" -> handleCrash(sender, args);
            case "reload" -> handleReload(sender);
            case "language" -> handleLanguage(sender, args);
            case "unlagall" -> handleUnlagAll(sender);
            case "gui" -> handleGui(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    // Handle 'lag' subcommand
    private void handleLag(CommandSender sender, String[] args) {
        if (checkPermission(sender, "xendelay.lag")) return;

        if (args.length < 2) {
            configManager.sendMessage(sender, "usage_lag");
            return;
        }

        Player target = getPlayer(sender, args[1]);
        if (target == null) return;

        lagEffectManager.applyLagEffect(target);
        configManager.sendMessage(sender, "lag_applied", target.getName());
    }

    // Handle 'unlag' subcommand
    private void handleUnlag(CommandSender sender, String[] args) {
        if (checkPermission(sender, "xendelay.unlag")) return;

        if (args.length < 2) {
            configManager.sendMessage(sender, "usage_unlag");
            return;
        }

        Player target = getPlayer(sender, args[1]);
        if (target == null) return;

        lagEffectManager.removeLagEffect(target);
        configManager.sendMessage(sender, "lag_removed", target.getName());
    }

    // Handle 'crash' subcommand
    private void handleCrash(CommandSender sender, String[] args) {
        if (checkPermission(sender, "xendelay.crash")) return;

        if (args.length < 3) {
            configManager.sendMessage(sender, "usage_crash");
            return;
        }

        Player target = getPlayer(sender, args[1]);
        if (target == null) return;

        String crashTypeStr = args[2].toUpperCase(Locale.ROOT);
        CrashManager.CrashType type;

        try {
            type = CrashManager.CrashType.valueOf(crashTypeStr);
        } catch (IllegalArgumentException e) {
            configManager.sendMessage(sender, "unknown_crash_type", args[2]);
            return;
        }

        crashManager.crashPlayer(target, type);
        configManager.sendMessage(sender, "crash_success", target.getName(), type.toString());
    }

    // Handle 'reload' subcommand
    private void handleReload(CommandSender sender) {
        if (checkPermission(sender, "xendelay.reload")) return;

        configManager.reloadConfig();
        configManager.sendMessage(sender, "reload_success");
    }

    // Handle 'language' subcommand
    private void handleLanguage(CommandSender sender, String[] args) {
        if (checkPermission(sender, "xendelay.language")) return;

        if (args.length < 2) {
            configManager.sendMessage(sender, "usage_language");
            return;
        }

        String lang = args[1].toLowerCase(Locale.ROOT);
        configManager.setLanguage(lang);
        configManager.sendMessage(sender, "language_set", lang);
    }

    // Handle 'unlagall' subcommand
    private void handleUnlagAll(CommandSender sender) {
        if (checkPermission(sender, "xendelay.unlagall")) return;

        lagEffectManager.clearAllLagEffects();
        configManager.sendMessage(sender, "lag_removed_all");
    }

    // Handle 'gui' subcommand
    private void handleGui(CommandSender sender) {
        if (checkPermission(sender, "xendelay.gui")) return;

        if (!(sender instanceof Player player)) {
            configManager.sendMessage(sender, "only_players");
            return;
        }

        menuBuilder.open(player);
    }

    // Utility method for permission checks
    private boolean checkPermission(CommandSender sender, String permission) {
        if (!sender.hasPermission(permission)) {
            configManager.sendMessage(sender, NO_PERMISSION_MSG);
            return true;
        }

        return false;
    }

    // Utility method to get player with null check
    private Player getPlayer(CommandSender sender, String name) {
        Player player = Bukkit.getPlayer(name);

        if (player == null) {
            configManager.sendMessage(sender, PLAYER_NOT_FOUND_MSG, name);
        }

        return player;
    }

    private void sendHelp(CommandSender sender) {
        configManager.sendMessage(sender, "help_main");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return getAvailableSubcommands(sender, args[0]);
        }

        String subCommand = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2) {
            if (("lag".equals(subCommand) && sender.hasPermission("xendelay.lag")) ||
                    ("unlag".equals(subCommand) && sender.hasPermission("xendelay.unlag"))) {
                return null; // Player name suggestions
            }

            if ("crash".equals(subCommand) && sender.hasPermission("xendelay.crash")) {
                return null; // Player name suggestions
            }

            if ("language".equals(subCommand) && sender.hasPermission("xendelay.language")) {
                return filterCompletions(LANGUAGES, args[1]);
            }

        } else if (args.length == 3 && "crash".equals(subCommand) && sender.hasPermission("xendelay.crash")) {
            return filterCompletions(CRASH_TYPES, args[2]);
        }

        return Collections.emptyList();
    }

    // Get subcommands available to the sender
    private List<String> getAvailableSubcommands(CommandSender sender, String input) {
        List<String> result = new ArrayList<>(7);

        for (String cmd : SUBCOMMANDS) {
            if (sender.hasPermission("xendelay." + cmd)) {
                result.add(cmd);
            }
        }

        return filterCompletions(result, input);
    }

    // Filter completion options based on input
    private List<String> filterCompletions(List<String> options, String input) {
        if (input.isEmpty()) return options;

        String filter = input.toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>(options.size());

        for (String option : options) {
            if (option.startsWith(filter)) {
                completions.add(option);
            }
        }

        return completions;
    }
}