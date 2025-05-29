package net.xenvision.xendelay.gui;

import net.xenvision.xendelay.utils.LagEffectManager;
import net.xenvision.xendelay.utils.ConfigManager;
import net.xenvision.xendelay.utils.MenuManager;
import net.xenvision.xendelay.utils.Colorizer;
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

    private final Map<UUID, String> openMenus = new HashMap<>();
    private final Set<UUID> actionCooldown = new HashSet<>();
    private static final long COOLDOWN_TICKS = 60L;
    // Для хранения текущей страницы каждого админа
    private final Map<UUID, Integer> adminPages = new HashMap<>();
    // Кол-во слотов для игроков на одной странице (10..45 включительно = 36)
    private static final int PLAYERS_PER_PAGE = 36;
    private static final int PLAYER_SLOT_START = 10;
    private static final int PLAYER_SLOT_END = 45;

    public MenuBuilder(Plugin plugin, LagEffectManager lagEffectManager, ConfigManager configManager, MenuManager menuManager) {
        this.plugin = plugin;
        this.lagEffectManager = lagEffectManager;
        this.configManager = configManager;
        this.menuManager = menuManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

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
        open(admin, 0);
    }

    public void open(Player admin, int page) {
        FileConfiguration menuConfig = menuManager.getMenuConfig();
        ConfigurationSection menu = menuConfig.getConfigurationSection("menu");
        if (menu == null) {
            admin.sendMessage("§c[Ошибка] menu.yml не содержит секцию menu!");
            return;
        }
        String title = Colorizer.colorize(menu.getString("title", "XenDelay Menu"));
        int size = menu.getInt("size", 54);
        Inventory inv = Bukkit.createInventory(null, size, title);

        ConfigurationSection playersSection = menu.getConfigurationSection("items.players");
        Set<Integer> usedSlots = new HashSet<>();
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        int totalPages = (int) Math.max(1, Math.ceil((double) onlinePlayers.size() / PLAYERS_PER_PAGE));
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        adminPages.put(admin.getUniqueId(), page);

        // --- Динамические игроки с поддержкой страниц ---
        if (playersSection != null) {
            Material mat = Material.valueOf(playersSection.getString("material", "PLAYER_HEAD"));
            int from = page * PLAYERS_PER_PAGE;
            int to = Math.min(from + PLAYERS_PER_PAGE, onlinePlayers.size());
            List<Player> showPlayers = onlinePlayers.subList(from, to);
            int slot = PLAYER_SLOT_START;
            for (Player p : showPlayers) {
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
                hideFlags(meta);
                skull.setItemMeta(meta);
                if (slot > PLAYER_SLOT_END) break;
                inv.setItem(slot, skull);
                usedSlots.add(slot);
                slot++;
            }
        }

        // --- Кастомные кнопки страниц ---
        ConfigurationSection itemsSection = menu.getConfigurationSection("items");
        if (itemsSection != null) {
            // prev
            if (totalPages > 1 && page > 0 && itemsSection.isConfigurationSection("page_prev")) {
                ConfigurationSection prevSec = itemsSection.getConfigurationSection("page_prev");
                inv.setItem(prevSec.getInt("slot", 0), buildPageItem(prevSec, page + 1, totalPages));
            }
            // next
            if (totalPages > 1 && page < totalPages - 1 && itemsSection.isConfigurationSection("page_next")) {
                ConfigurationSection nextSec = itemsSection.getConfigurationSection("page_next");
                inv.setItem(nextSec.getInt("slot", 8), buildPageItem(nextSec, page + 1, totalPages));
            }
            // info — ВСЕГДА
            if (itemsSection.isConfigurationSection("page_info")) {
                ConfigurationSection infoSec = itemsSection.getConfigurationSection("page_info");
                inv.setItem(infoSec.getInt("slot", 4), buildPageItem(infoSec, page + 1, totalPages));
            }
        }

        // --- Статика (unlagall, reload, close и др.) ---
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                if (key.equals("players") || key.startsWith("page_")) continue;
                ConfigurationSection item = itemsSection.getConfigurationSection(key);
                if (item == null) continue;
                int slot = item.getInt("slot");
                if (usedSlots.contains(slot)) continue;
                // Не даём пересечься с кнопками страниц (45, 49, 53)
                if (isPageSlot(itemsSection, slot)) continue;
                Material material = Material.valueOf(item.getString("material", "STONE"));
                ItemStack stack = new ItemStack(material);
                ItemMeta meta = stack.getItemMeta();
                meta.setDisplayName(Colorizer.colorize(item.getString("display", key)));
                meta.setLore(item.getStringList("lore").stream().map(Colorizer::colorize).collect(Collectors.toList()));
                hideFlags(meta);
                stack.setItemMeta(meta);
                inv.setItem(slot, stack);
            }
        }

        admin.openInventory(inv);
        openMenus.put(admin.getUniqueId(), title);
    }

    // Хелпер для создания предмета страницы с подстановкой %page% и %total%
    private ItemStack buildPageItem(ConfigurationSection sec, int page, int total) {
        Material mat = Material.valueOf(sec.getString("material", "PAPER"));
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        String display = sec.getString("display", "");
        display = display.replace("%page%", String.valueOf(page)).replace("%total%", String.valueOf(total));
        meta.setDisplayName(Colorizer.colorize(display));
        List<String> lore = new ArrayList<>();
        for (String l : sec.getStringList("lore")) {
            lore.add(Colorizer.colorize(l.replace("%page%", String.valueOf(page)).replace("%total%", String.valueOf(total))));
        }
        meta.setLore(lore);
        hideFlags(meta);
        stack.setItemMeta(meta);
        return stack;
    }

    // Проверка, не является ли слот одним из page_ секций
    private boolean isPageSlot(ConfigurationSection itemsSection, int slot) {
        for (String key : itemsSection.getKeys(false)) {
            if (!key.startsWith("page_")) continue;
            ConfigurationSection sec = itemsSection.getConfigurationSection(key);
            if (sec != null && sec.getInt("slot") == slot) return true;
        }
        return false;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player admin = (Player) e.getWhoClicked();
        String invTitle = e.getView().getTitle();
        if (!openMenus.getOrDefault(admin.getUniqueId(), "").equals(invTitle)) return;

        if (actionCooldown.contains(admin.getUniqueId())) {
            configManager.sendMessage(admin, "cooldown");
            e.setCancelled(true);
            return;
        }
        actionCooldown.add(admin.getUniqueId());
        e.setCancelled(true);

        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) {
            removeCooldownLater(admin, COOLDOWN_TICKS);
            return;
        }
        ItemMeta meta = clicked.getItemMeta();
        String name = meta.getDisplayName();

        int slot = e.getRawSlot();
        FileConfiguration menuConfig = menuManager.getMenuConfig();
        ConfigurationSection menu = menuConfig.getConfigurationSection("menu");
        if (menu == null) {
            removeCooldownLater(admin, COOLDOWN_TICKS);
            return;
        }
        ConfigurationSection itemsSection = menu.getConfigurationSection("items");
        String keyAction = null;
        ConfigurationSection itemSection = null;

        Integer page = adminPages.getOrDefault(admin.getUniqueId(), 0);
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        int totalPages = (int) Math.max(1, Math.ceil((double) onlinePlayers.size() / PLAYERS_PER_PAGE));

        // --- Обработка кастомных page_ кнопок ---
        if (itemsSection != null) {
            // prev
            if (page > 0 && itemsSection.isConfigurationSection("page_prev")) {
                ConfigurationSection prevSec = itemsSection.getConfigurationSection("page_prev");
                if (slot == prevSec.getInt("slot", 45)) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> open(admin, page - 1), 1L);
                    removeCooldownLater(admin, COOLDOWN_TICKS);
                    return;
                }
            }
            // next
            if (page < totalPages - 1 && itemsSection.isConfigurationSection("page_next")) {
                ConfigurationSection nextSec = itemsSection.getConfigurationSection("page_next");
                if (slot == nextSec.getInt("slot", 53)) {
                    Bukkit.getScheduler().runTaskLater(plugin, () -> open(admin, page + 1), 1L);
                    removeCooldownLater(admin, COOLDOWN_TICKS);
                    return;
                }
            }
            // info -- обычно ничего не делает, просто return
            if (itemsSection.isConfigurationSection("page_info")) {
                ConfigurationSection infoSec = itemsSection.getConfigurationSection("page_info");
                if (slot == infoSec.getInt("slot", 49)) {
                    removeCooldownLater(admin, COOLDOWN_TICKS);
                    return;
                }
            }
        }

        // --- Динамические игроки ---
        assert itemsSection != null;
        ConfigurationSection playersSection = itemsSection.getConfigurationSection("players");
        assert playersSection != null;

        if (slot >= PLAYER_SLOT_START && slot <= PLAYER_SLOT_END && clicked.getType() == Material.PLAYER_HEAD) {
            itemSection = playersSection;
            keyAction = "players";
        } else {
            for (String key : itemsSection.getKeys(false)) {
                if (key.equals("players") || key.startsWith("page_")) continue;
                ConfigurationSection it = itemsSection.getConfigurationSection(key);
                if (it != null && it.getInt("slot") == slot) {
                    itemSection = it;
                    keyAction = key;
                    break;
                }
            }
        }
        if (itemSection == null) {
            removeCooldownLater(admin, COOLDOWN_TICKS);
            return;
        }

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

        if (action == null) {
            removeCooldownLater(admin, COOLDOWN_TICKS);
            return;
        }
        switch (action) {
            case "toggle_lag": {
                String playerName = org.bukkit.ChatColor.stripColor(name);
                Player target = Bukkit.getPlayerExact(playerName);
                if (target == null) {
                    configManager.sendMessage(admin, "player_not_found", playerName);
                    removeCooldownLater(admin, COOLDOWN_TICKS);
                    return;
                }
                if (!lagEffectManager.isLagged(target)) {
                    lagEffectManager.applyLag(target);
                    configManager.sendMessage(admin, "lag_applied", target.getName());
                    Bukkit.getScheduler().runTaskLater(plugin, () -> open(admin, page), 1L);
                } else {
                    configManager.sendMessage(admin, "lag_already_applied", target.getName());
                }
                removeCooldownLater(admin, COOLDOWN_TICKS);
                return;
            }
            case "toggle_unlag": {
                String playerName = org.bukkit.ChatColor.stripColor(name);
                Player target = Bukkit.getPlayerExact(playerName);
                if (target == null) {
                    configManager.sendMessage(admin, "player_not_found", playerName);
                    removeCooldownLater(admin, COOLDOWN_TICKS);
                    return;
                }
                if (lagEffectManager.isLagged(target)) {
                    lagEffectManager.removeLag(target);
                    configManager.sendMessage(admin, "lag_removed", target.getName());
                } else {
                    configManager.sendMessage(admin, "lag_already_removed", target.getName());
                }
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(admin, page), 3L);
                removeCooldownLater(admin, COOLDOWN_TICKS);
                break;
            }
            case "unlag_all": {
                lagEffectManager.removeLagFromAll();
                configManager.sendMessage(admin, "lag_removed_all");
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(admin, page), 1L);
                removeCooldownLater(admin, COOLDOWN_TICKS);
                break;
            }
            case "reload_config": {
                configManager.reloadConfig();
                menuManager.reloadMenu();
                configManager.sendMessage(admin, "reload_success");
                Bukkit.getScheduler().runTaskLater(plugin, () -> open(admin, page), 1L);
                removeCooldownLater(admin, COOLDOWN_TICKS);
                break;
            }
            case "close": {
                admin.closeInventory();
                removeCooldownLater(admin, COOLDOWN_TICKS);
                break;
            }
            default:
                admin.sendMessage("§e[INFO] Неизвестное действие: " + action);
                removeCooldownLater(admin, COOLDOWN_TICKS);
        }
    }

    private void removeCooldownLater(Player admin, long ticks) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> actionCooldown.remove(admin.getUniqueId()), ticks);
    }
}