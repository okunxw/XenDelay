package net.xenvision.xendelay.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Handles plugin configuration and localization with performance optimizations.
 */
public class ConfigManager {

    private final JavaPlugin plugin;
    private FileConfiguration config;
    private volatile Map<String, List<String>> messageCache = new HashMap<>();
    private String language;
    private final Map<String, Long> messageCooldowns = new ConcurrentHashMap<>();

    private static final long MESSAGE_THROTTLE_MILLIS = 2000;
    private static final int THROTTLE_CLEANUP_THRESHOLD = 1000;
    private static final long THROTTLE_CLEANUP_AGE = TimeUnit.SECONDS.toMillis(10);
    private static final String[] DEFAULT_LANGUAGES = {"ru", "en", "fr"};

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reloadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "config.yml"));
    }

    private void loadLanguage() {
        language = config.getString("settings.locate", "ru");
    }

    private void ensureLanguageFiles() {
        Path locateDir = plugin.getDataFolder().toPath().resolve("locate");

        try {
            Files.createDirectories(locateDir);
        } catch (IOException e) {
            Bukkit.getLogger().warning("[XenDelay] Failed to create locate directory: " + e.getMessage());
        }

        for (String lang : DEFAULT_LANGUAGES) {
            Path targetFile = locateDir.resolve("messages_" + lang + ".yml");

            if (!Files.exists(targetFile)) {
                plugin.saveResource("locate/messages_" + lang + ".yml", false);
            }
        }
    }

    private void loadMessages() {
        Map<String, List<String>> newCache = new HashMap<>();
        File messagesFile = new File(plugin.getDataFolder(), "locate/messages_" + language + ".yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("locate/messages_" + language + ".yml", false);
        }

        FileConfiguration messages = YamlConfiguration.loadConfiguration(messagesFile);
        ConfigurationSection messagesSection = messages.getConfigurationSection("messages");

        if (messagesSection != null) {
            for (String key : messagesSection.getKeys(true)) {
                if (messages.isList("messages." + key)) {
                    newCache.put(key, messages.getStringList("messages." + key));
                } else if (messages.isString("messages." + key)) {
                    newCache.put(key, Collections.singletonList(messages.getString("messages." + key)));
                }
            }
        }

        messageCache = newCache;
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void sendMessage(CommandSender sender, String key, String... placeholders) {
        // Throttle cleanup
        if (messageCooldowns.size() > THROTTLE_CLEANUP_THRESHOLD) {
            long now = System.currentTimeMillis();
            messageCooldowns.entrySet().removeIf(entry -> now - entry.getValue() > THROTTLE_CLEANUP_AGE);
        }

        // Player-specific throttling
        if (sender instanceof Player) {
            String throttleKey = ((Player) sender).getUniqueId() + ":" + key;
            long now = System.currentTimeMillis();
            Long lastSent = messageCooldowns.get(throttleKey);

            if (lastSent != null && now - lastSent < MESSAGE_THROTTLE_MILLIS) {
                return;
            }

            messageCooldowns.put(throttleKey, now);
        }

        // Get message from cache
        List<String> lines = messageCache.getOrDefault(key, Collections.emptyList());
        if (lines.isEmpty()) {
            sender.sendMessage("§c[Ошибка] Сообщение '" + key + "' не найдено!");
            return;
        }

        // Build message
        StringBuilder messageBuilder = new StringBuilder();
        for (String line : lines) {
            messageBuilder.append(line).append('\n');
        }

        String message = messageBuilder.toString().trim();

        // Replace placeholders
        if (sender instanceof Player) {
            message = message.replace("%player%", sender.getName());
        }

        for (int i = 0; i < placeholders.length; i++) {
            message = message.replace("%" + (i + 1) + "%", placeholders[i]);
        }

        sender.sendMessage(Colorizer.colorize(message));
    }

    public void reloadConfig() {
        loadConfig();
        loadLanguage();
        ensureLanguageFiles();
        loadMessages();
    }

    public void setLanguage(String lang) {
        if (!language.equals(lang)) {
            language = lang;
            loadMessages();
        }
    }
}