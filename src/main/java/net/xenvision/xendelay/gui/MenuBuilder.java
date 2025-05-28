package net.xenvision.xendelay.gui;

import net.xenvision.xendelay.utils.LagEffectManager;
import net.xenvision.xendelay.utils.ConfigManager;
import net.xenvision.xendelay.utils.MenuManager;
import net.xenvision.xendelay.utils.Colorizer; // Используем наш Colorizer!
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.stream.Collectors;

public class MenuBuilder implements Listener {
    private final LagEffectManager lagEffectManager;
    private final ConfigManager configManager;
    private final MenuManager menuManager;
    private final Plugin plugin;

    // Храним мапу: player -> последний открытый inventory (для action-обработки)
    private final Map<UUID, String> openMenus = new HashMap<>();

    public MenuBuilder(Plugin plugin, LagEffectManager lagEffectManager, ConfigManager configManager, MenuManager menuManager) {
        this.plugin = plugin;
        this.lagEffectManager = lagEffectManager;
        this.configManager = configManager;
        this.menuManager = menuManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Добавляет флаги для скрытия технических подсказок у предметов меню.
     */
    private static void hideFlags(ItemMeta meta) {
        if (meta == null) return;
        meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_POTION_EFFECTS,
                ItemFlag.HIDE_DYE
        );
    }

    public void open(Player admin) {
        FileConfiguration menuConfig = menuManager.getMenuConfig();
        ConfigurationSection menu = menuConfig.getConfigurationSection("menu");
        if (menu == null) {
            admin.sendMessage("§c[Ошибка] menu.yml не содержит секцию menu!");
            return;
        }
        String title = Colorizer.colorize(menu.getString("title", "XenDelay Menu"));
        int size = menu.getInt("size", 36);
        Inventory inv = Bukkit.createInventory(null, size, title);

        // --- Динамические игроки ---
        ConfigurationSection playersSection = menu.getConfigurationSection("items.players");
        Set<Integer> usedSlots = new HashSet<>();
        if (playersSection != null) {
            int start = playersSection.getInt("slot_start", 0);
            int end = playersSection.getInt("slot_end", 8);
            Material mat = Material.valueOf(playersSection.getString("material", "PLAYER_HEAD"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                ItemStack skull = new ItemStack(mat);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                meta.setOwningPlayer(p);
                String status = lagEffectManager.isLagged(p) ? Colorizer.colorize("&#f75c47✔ Лагнут") : Colorizer.colorize("&#27e1c1✘ Норма");
                String display = playersSection.getString("display", "%player_colored%")
                        .replace("%player_colored%", (lagEffectManager.isLagged(p) ? Colorizer.colorize("&#f75c47") : Colorizer.colorize("&#27e1c1")) + p.getName());
                meta.setDisplayName(Colorizer.colorize(display));
                List<String> lore = new ArrayList<>();
                List<String> loreConfig = playersSection.getStringList("lore");
                if (!loreConfig.isEmpty()) {
                    for (String l : loreConfig) {
                        lore.add(Colorizer.colorize(l.replace("%status%", status)));
                    }
                }
                meta.setLore(lore);
                hideFlags(meta); // <-- скрываем технические строки
                skull.setItemMeta(meta);
                int slot = start++;
                if (slot > end) break;
                inv.setItem(slot, skull);
                usedSlots.add(slot);
            }
        }

        // --- Статические предметы ---
        ConfigurationSection itemsSection = menu.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                if (key.equals("players")) continue;
                ConfigurationSection item = itemsSection.getConfigurationSection(key);
                if (item == null) continue;
                int slot = item.getInt("slot");
                // Не давим на игроков
                if (usedSlots.contains(slot)) continue;
                Material material = Material.valueOf(item.getString("material", "STONE"));
                ItemStack stack = new ItemStack(material);
                ItemMeta meta = stack.getItemMeta();
                meta.setDisplayName(Colorizer.colorize(item.getString("display", key)));
                meta.setLore(item.getStringList("lore").stream().map(Colorizer::colorize).collect(Collectors.toList()));
                hideFlags(meta); // <-- скрываем технические строки
                stack.setItemMeta(meta);
                inv.setItem(slot, stack);
            }
        }

        admin.openInventory(inv);
        openMenus.put(admin.getUniqueId(), title); // Для проверки при клике
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player admin = (Player) e.getWhoClicked();
        String invTitle = e.getView().getTitle();
        // Проверяем, что это наш GUI (лучше по title, чем contains!)
        if (!openMenus.getOrDefault(admin.getUniqueId(), "").equals(invTitle)) return;
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;
        ItemMeta meta = clicked.getItemMeta();
        String name = meta.getDisplayName();

        // --- Определяем action по слоту ---
        int slot = e.getRawSlot();
        FileConfiguration menuConfig = menuManager.getMenuConfig();
        ConfigurationSection menu = menuConfig.getConfigurationSection("menu");
        if (menu == null) return;
        ConfigurationSection itemsSection = menu.getConfigurationSection("items");
        String keyAction = null;
        ConfigurationSection itemSection = null;

        // Проверяем: динамические игроки?
        assert itemsSection != null;
        ConfigurationSection playersSection = itemsSection.getConfigurationSection("players");
        assert playersSection != null;
        int start = playersSection.getInt("slot_start", 0);
        int end = playersSection.getInt("slot_end", 8);
        if (slot >= start && slot <= end && clicked.getType() == Material.PLAYER_HEAD) {
            itemSection = playersSection;
            keyAction = "players";
        } else {
            // Проверяем статичные предметы
            for (String key : itemsSection.getKeys(false)) {
                if (key.equals("players")) continue;
                ConfigurationSection it = itemsSection.getConfigurationSection(key);
                if (it != null && it.getInt("slot") == slot) {
                    itemSection = it;
                    keyAction = key;
                    break;
                }
            }
        }
        if (itemSection == null) return;

        // --- Получаем action по клику ---
        ConfigurationSection actions = itemSection.getConfigurationSection("actions");
        String action = null;
        ClickType click = e.getClick();
        if (actions != null) {
            switch (click) {
                case LEFT:
                    if (actions.contains("left")) action = actions.getString("left");
                    break;
                case RIGHT:
                    if (actions.contains("right")) action = actions.getString("right");
                    break;
                case SHIFT_LEFT:
                    if (actions.contains("shift_left")) action = actions.getString("shift_left");
                    break;
                case SHIFT_RIGHT:
                    if (actions.contains("shift_right")) action = actions.getString("shift_right");
                    break;
                case MIDDLE:
                    if (actions.contains("middle")) action = actions.getString("middle");
                    break;
                default:
                    break;
            }
        }

        // --- Обработка действий ---
        if (action == null) return;
        switch (action) {
            case "toggle_lag": {
                String playerName = org.bukkit.ChatColor.stripColor(name);
                Player target = Bukkit.getPlayerExact(playerName);
                if (target == null) {
                    configManager.sendMessage(admin, "player_not_found", playerName);
                    return;
                }
                if (!lagEffectManager.isLagged(target)) {
                    lagEffectManager.applyLag(target);
                    configManager.sendMessage(admin, "lag_applied", target.getName());
                    Bukkit.getScheduler().runTaskLater(plugin, () -> open(admin), 1L);
                } else {
                    configManager.sendMessage(admin, "lag_already_applied", target.getName());
                }
                return;
            }
            case "toggle_unlag": {
                String playerName = org.bukkit.ChatColor.stripColor(name);
                Player target = Bukkit.getPlayerExact(playerName);
                if (target == null) {
                    configManager.sendMessage(admin, "player_not_found", playerName);
                    return;
                }
                if (lagEffectManager.isLagged(target)) {
                    lagEffectManager.removeLag(target);
                    configManager.sendMessage(admin, "lag_removed", target.getName());
                } else {
                    configManager.sendMessage(admin, "lag_already_removed", target.getName());
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(admin), 1L);
                break;
            }
            case "unlag_all": {
                lagEffectManager.removeLagFromAll();
                configManager.sendMessage(admin, "lag_removed_all");
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(admin), 1L);
                break;
            }
            case "reload_config": {
                configManager.reloadConfig();
                menuManager.reloadMenu();
                configManager.sendMessage(admin, "reload_success");
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(admin), 1L);
                break;
            }
            case "close": {
                admin.closeInventory();
                break;
            }
            // Добавляй свои кастомные action'ы здесь!
            default:
                admin.sendMessage("§e[INFO] Неизвестное действие: " + action);
        }
    }
}