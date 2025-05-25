package net.xenvision.xendelay.utils;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import org.bukkit.command.CommandSender;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private String language;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        plugin.saveDefaultConfig();

        language = config.getString("settings.locate", "ru");

        // Проверяем, что папка locate существует
        File locateFolder = new File(plugin.getDataFolder(), "locate");
        if (!locateFolder.exists()) {
            locateFolder.mkdirs();
        }

        // Копируем ВСЕ языковые файлы при первом запуске
        String[] availableLanguages = {"ru", "en", "fr"};
        for (String lang : availableLanguages) {
            File messagesFile = new File(plugin.getDataFolder(), "locate/messages_" + lang + ".yml");
            if (!messagesFile.exists() && plugin.getResource("locate/messages_" + lang + ".yml") != null) {
                plugin.saveResource("locate/messages_" + lang + ".yml", false);
            }
        }

        // Загружаем язык
        loadMessages();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void sendMessage(CommandSender sender, String key, String... placeholders) {
        String message = String.join("\n", messages.getStringList("messages." + key));
        if (message.isEmpty()) {
            message = "§c[Ошибка] Сообщение не найдено в messages.yml!";
        }

        // Если отправитель — игрок, заменяем `%player%` его ником
        if (sender instanceof Player) {
            message = message.replace("%player%", sender.getName());
        }

        // Заменяем плейсхолдеры `%1%`, `%2%`, `%3%`
        for (int i = 0; i < placeholders.length; i++) {
            message = message.replace("%" + (i + 1) + "%", placeholders[i]);
        }

        sender.sendMessage(Colorizer.colorize(message));
    }

    public void reloadConfig() {
        Bukkit.getLogger().info("Перезагрузка конфигурации...");

        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            Bukkit.getLogger().info("config.yml отсутствует, создаём новый...");
            plugin.saveResource("config.yml", false);
        }

        plugin.reloadConfig();
        this.config = YamlConfiguration.loadConfiguration(configFile);

        language = config.getString("settings.locate", "ru");
        Bukkit.getLogger().info("Обновленный язык: " + language);

        // Перезагружаем локализацию
        loadMessages();
    }

    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "locate/messages_" + language + ".yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("locate/messages_" + language + ".yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }
}