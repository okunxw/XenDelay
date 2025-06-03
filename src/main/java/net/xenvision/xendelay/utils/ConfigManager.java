package net.xenvision.xendelay.utils;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Handles plugin configuration and localization.
 */
public class ConfigManager {
    
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private String language;
    private final Map<String, Long> messageCooldowns = new HashMap<>(); // Throttle for player messages

    private static final long MESSAGE_THROTTLE_MILLIS = 2000;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
        loadLanguage();
        ensureLanguageFiles();
        loadMessages();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        this.config = YamlConfiguration.loadConfiguration(configFile);
    }

    private void loadLanguage() {
        this.language = config.getString("settings.locate", "ru");
    }

    private void ensureLanguageFiles() {
        File locateFolder = new File(plugin.getDataFolder(), "locate");
        
        if (!locateFolder.exists()) {
            locateFolder.mkdirs();
        }
        
        String[] availableLanguages = {"ru", "en", "fr"};
        for (String lang : availableLanguages) {
            File messagesFile = new File(plugin.getDataFolder(), "locate/messages_" + lang + ".yml");
            
            if (!messagesFile.exists() && plugin.getResource("locate/messages_" + lang + ".yml") != null) {
                plugin.saveResource("locate/messages_" + lang + ".yml", false);
            }
        }
    }

    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "locate/messages_" + language + ".yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("locate/messages_" + language + ".yml", false);
        }
        
        this.messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void sendMessage(CommandSender sender, String key, String... placeholders) {
        String baseKey = sender instanceof Player ? ((Player) sender).getUniqueId() + ":" + key : "CONSOLE:" + key;
        long now = System.currentTimeMillis();
        
        if (sender instanceof Player) {
            if (messageCooldowns.containsKey(baseKey) && now - messageCooldowns.get(baseKey) < MESSAGE_THROTTLE_MILLIS) {
                return; // Throttle
            }
            
            messageCooldowns.put(baseKey, now);
        }
        
        String message = String.join("\n", messages.getStringList("messages." + key));
        if (message.isEmpty()) {
            message = "§c[Ошибка] Сообщение не найдено в messages.yml!";
        }
        
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
        this.language = lang;
        loadMessages();
    }
}
