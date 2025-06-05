package net.xenvision.xendelay.gui;

import net.xenvision.xendelay.utils.LagEffectManager;
import net.xenvision.xendelay.utils.ConfigManager;
import net.xenvision.xendelay.utils.MenuManager;
import net.xenvision.xendelay.utils.Colorizer;
import net.xenvision.xendelay.utils.CrashManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MenuBuilder implements Listener {

    private final LagEffectManager lagEffectManager;
    private final ConfigManager configManager;
    private final MenuManager menuManager;
    private final Plugin plugin;
    private final CrashManager crashManager;

    private final Map<UUID, String> openMenus = new ConcurrentHashMap<>();
    private final Set<UUID> actionCooldown = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final long COOLDOWN_TICKS = 60L;
    private final Map<UUID, Integer> adminPages = new ConcurrentHashMap<>();
    private static final int PLAYERS_PER_PAGE = 36;
    private static final int PLAYER_SLOT_START = 10;
    private static final int PLAYER_SLOT_END = 45;
    private final NamespacedKey playerUuidKey;

    // Cache for static menu items
    private final Map<String, ItemStack> staticItemCache = new HashMap<>();
    private FileConfiguration lastMenuConfig;

    public MenuBuilder(Plugin plugin, LagEffectManager lagEffectManager, ConfigManager configManager,
                       MenuManager menuManager, CrashManager crashManager) {
        this.plugin = plugin;
        this.lagEffectManager = lagEffectManager;
        this.configManager = configManager;
        this.menuManager = menuManager;
        this.crashManager = crashManager;
        this.playerUuidKey = new NamespacedKey(plugin, "player-uuid");
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
            admin.sendMessage("§c[Error] menu.yml does not contain a menu section!");
            return;
        }

        String title = Colorizer.colorize(menu.getString("title", "XenDelay Menu"));
        int size = menu.getInt("size", 54);
        Inventory inv = Bukkit.createInventory(null, size, title);

        ConfigurationSection playersSection = menu.getConfigurationSection("items.players");
        Set<Integer> usedSlots = new HashSet<>(PLAYERS_PER_PAGE + 10);
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        int playerCount = onlinePlayers.size();
        int totalPages = Math.max(1, (playerCount + PLAYERS_PER_PAGE - 1) / PLAYERS_PER_PAGE);
        page = Math.max(0, Math.min(page, totalPages - 1));
        adminPages.put(admin.getUniqueId(), page);

        // Dynamic player heads with pagination
        if (playersSection != null) {
            Material mat = Material.valueOf(playersSection.getString("material", "PLAYER_HEAD"));
            int from = page * PLAYERS_PER_PAGE;
            int to = Math.min(from + PLAYERS_PER_PAGE, playerCount);
            int slot = PLAYER_SLOT_START;

            // Use iterator for efficient access
            Iterator<? extends Player> playerIterator = onlinePlayers.iterator();
            for (int i = 0; i < to; i++) {
                Player p = playerIterator.next();
                if (i < from) continue;

                ItemStack skull = new ItemStack(mat);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                if (meta == null) continue;

                meta.setOwningPlayer(p);
                String status = lagEffectManager.hasActiveLagEffect(p) ?
                        Colorizer.colorize("&#f75c47✔ Lagged") :
                        Colorizer.colorize("&#27e1c1✘ Normal");

                String displayTemplate = playersSection.getString("display", "%player_colored%");
                String display = displayTemplate
                        .replace("%player_colored%", (lagEffectManager.hasActiveLagEffect(p) ?
                                Colorizer.colorize("&#f75c47") :
                                Colorizer.colorize("&#27e1c1")) + p.getName());

                meta.setDisplayName(Colorizer.colorize(display));

                List<String> loreConfig = playersSection.getStringList("lore");
                List<String> lore = new ArrayList<>(loreConfig.size());
                for (String l : loreConfig) {
                    lore.add(Colorizer.colorize(l.replace("%status%", status)));
                }

                meta.setLore(lore);
                hideFlags(meta);

                // Store player UUID in item metadata
                meta.getPersistentDataContainer().set(
                        playerUuidKey,
                        PersistentDataType.STRING,
                        p.getUniqueId().toString()
                );

                skull.setItemMeta(meta);
                if (slot > PLAYER_SLOT_END) break;
                inv.setItem(slot, skull);
                usedSlots.add(slot);
                slot++;
            }
        }

        // Page navigation buttons
        ConfigurationSection itemsSection = menu.getConfigurationSection("items");
        if (itemsSection != null) {
            // Invalidate cache when configuration changes
            if (lastMenuConfig != menuConfig) {
                staticItemCache.clear();
                lastMenuConfig = menuConfig;
            }

            // Previous page button
            if (totalPages > 1 && page > 0 && itemsSection.isConfigurationSection("page_prev")) {
                ConfigurationSection prevSec = itemsSection.getConfigurationSection("page_prev");
                int slot = prevSec.getInt("slot", 0);
                inv.setItem(slot, buildPageItem(prevSec, page + 1, totalPages));
                usedSlots.add(slot);
            }

            // Next page button
            if (totalPages > 1 && page < totalPages - 1 && itemsSection.isConfigurationSection("page_next")) {
                ConfigurationSection nextSec = itemsSection.getConfigurationSection("page_next");
                int slot = nextSec.getInt("slot", 8);
                inv.setItem(slot, buildPageItem(nextSec, page + 1, totalPages));
                usedSlots.add(slot);
            }

            // Page info button
            if (itemsSection.isConfigurationSection("page_info")) {
                ConfigurationSection infoSec = itemsSection.getConfigurationSection("page_info");
                int slot = infoSec.getInt("slot", 4);
                inv.setItem(slot, buildPageItem(infoSec, page + 1, totalPages));
                usedSlots.add(slot);
            }

            // Static items with caching
            for (String key : itemsSection.getKeys(false)) {
                if (key.equals("players") || key.startsWith("page_")) continue;

                ConfigurationSection itemSec = itemsSection.getConfigurationSection(key);
                if (itemSec == null) continue;

                int slot = itemSec.getInt("slot");
                if (usedSlots.contains(slot)) continue;

                // Use cached items when available
                ItemStack item = staticItemCache.computeIfAbsent(key, k -> {
                    Material material = Material.valueOf(itemSec.getString("material", "STONE"));
                    ItemStack stack = new ItemStack(material);
                    ItemMeta meta = stack.getItemMeta();
                    if (meta == null) return stack;

                    meta.setDisplayName(Colorizer.colorize(itemSec.getString("display", k)));
                    List<String> lore = itemSec.getStringList("lore").stream()
                            .map(Colorizer::colorize)
                            .collect(Collectors.toList());
                    meta.setLore(lore);
                    hideFlags(meta);
                    stack.setItemMeta(meta);
                    return stack;
                });

                inv.setItem(slot, item);
                usedSlots.add(slot);
            }
        }

        admin.openInventory(inv);
        openMenus.put(admin.getUniqueId(), title);
    }

    private ItemStack buildPageItem(ConfigurationSection sec, int page, int total) {
        Material mat = Material.valueOf(sec.getString("material", "PAPER"));
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return stack;

        String display = sec.getString("display", "")
                .replace("%page%", String.valueOf(page))
                .replace("%total%", String.valueOf(total));

        meta.setDisplayName(Colorizer.colorize(display));

        List<String> lore = sec.getStringList("lore").stream()
                .map(l -> l.replace("%page%", String.valueOf(page))
                        .replace("%total%", String.valueOf(total)))
                .map(Colorizer::colorize)
                .collect(Collectors.toList());

        meta.setLore(lore);
        hideFlags(meta);
        stack.setItemMeta(meta);

        return stack;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player admin)) return;

        UUID adminId = admin.getUniqueId();
        String invTitle = e.getView().getTitle();
        if (!invTitle.equals(openMenus.get(adminId))) return;

        if (actionCooldown.contains(adminId)) {
            configManager.sendMessage(admin, "cooldown");
            e.setCancelled(true);
            return;
        }

        e.setCancelled(true);
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        int slot = e.getRawSlot();
        FileConfiguration menuConfig = menuManager.getMenuConfig();
        ConfigurationSection menu = menuConfig.getConfigurationSection("menu");
        if (menu == null) return;

        ConfigurationSection itemsSection = menu.getConfigurationSection("items");
        if (itemsSection == null) return;

        int page = adminPages.getOrDefault(adminId, 0);
        int playerCount = Bukkit.getOnlinePlayers().size();
        int totalPages = Math.max(1, (playerCount + PLAYERS_PER_PAGE - 1) / PLAYERS_PER_PAGE);

        // Handle page navigation buttons
        if (itemsSection.isConfigurationSection("page_prev") &&
                slot == itemsSection.getConfigurationSection("page_prev").getInt("slot", 45)) {
            if (page > 0) {
                scheduleOpen(admin, page - 1);
            }

            return;
        }

        if (itemsSection.isConfigurationSection("page_next") &&
                slot == itemsSection.getConfigurationSection("page_next").getInt("slot", 53)) {
            if (page < totalPages - 1) {
                scheduleOpen(admin, page + 1);
            }

            return;
        }

        if (itemsSection.isConfigurationSection("page_info") &&
                slot == itemsSection.getConfigurationSection("page_info").getInt("slot", 49)) {
            setCooldown(adminId);
            return;
        }

        // Handle player head clicks
        if (slot >= PLAYER_SLOT_START && slot <= PLAYER_SLOT_END &&
                clicked.getType() == Material.PLAYER_HEAD) {

            ItemMeta meta = clicked.getItemMeta();
            String uuidString = meta.getPersistentDataContainer().get(playerUuidKey, PersistentDataType.STRING);
            if (uuidString == null) return;

            UUID targetUuid = UUID.fromString(uuidString);
            Player target = Bukkit.getPlayer(targetUuid);
            handlePlayerAction(e.getClick(), admin, target, itemsSection, page);
            return;
        }

        // Handle static item clicks
        for (String key : itemsSection.getKeys(false)) {
            if (key.equals("players") || key.startsWith("page_")) continue;

            ConfigurationSection itemSec = itemsSection.getConfigurationSection(key);
            if (itemSec == null || itemSec.getInt("slot") != slot) continue;

            handleStaticItemAction(e.getClick(), admin, itemSec, page);
            break;
        }
    }

    private void handlePlayerAction(ClickType click, Player admin, Player target,
                                    ConfigurationSection itemsSection, int page) {
        if (target == null || !target.isOnline()) {
            configManager.sendMessage(admin, "player_not_found");
            setCooldown(admin.getUniqueId());
            return;
        }

        ConfigurationSection playersSection = itemsSection.getConfigurationSection("players");
        if (playersSection == null) return;

        ConfigurationSection actions = playersSection.getConfigurationSection("actions");
        if (actions == null) return;

        String action = getActionForClick(click, actions);
        if (action == null) return;

        executeAction(action, admin, target, page);
    }

    private void handleStaticItemAction(ClickType click, Player admin,
                                        ConfigurationSection itemSec, int page) {
        ConfigurationSection actions = itemSec.getConfigurationSection("actions");
        if (actions == null) return;

        String action = getActionForClick(click, actions);
        if (action == null) return;

        executeAction(action, admin, null, page);
    }

    private String getActionForClick(ClickType click, ConfigurationSection actions) {
        return switch (click) {
            case LEFT -> actions.getString("left");
            case RIGHT -> actions.getString("right");
            case SHIFT_LEFT -> actions.getString("shift_left");
            case SHIFT_RIGHT -> actions.getString("shift_right");
            case MIDDLE -> actions.getString("middle");
            default -> null;
        };
    }

    private void executeAction(String action, Player admin, Player target, int page) {
        UUID adminId = admin.getUniqueId();
        setCooldown(adminId);

        switch (action) {
            case "toggle_lag" -> handleToggleLag(admin, target, page);
            case "toggle_unlag" -> handleToggleUnlag(admin, target, page);
            case "unlag_all" -> handleUnlagAll(admin, page);
            case "reload_config" -> handleReloadConfig(admin, page);
            case "close" -> handleClose(admin);
            case "crash_entity" -> handleCrash(admin, target, CrashManager.CrashType.ENTITY);
            case "crash_sign" -> handleCrash(admin, target, CrashManager.CrashType.SIGN);
            case "crash_payload" -> handleCrash(admin, target, CrashManager.CrashType.PAYLOAD);
            default -> admin.sendMessage("§e[INFO] Unknown action: " + action);
        }
    }

    private void handleToggleLag(Player admin, Player target, int page) {
        if (lagEffectManager.hasActiveLagEffect(target)) {
            configManager.sendMessage(admin, "lag_already_applied", target.getName());
        } else {
            lagEffectManager.applyLagEffect(target);
            configManager.sendMessage(admin, "lag_applied", target.getName());
            scheduleOpen(admin, page);
        }
    }

    private void handleToggleUnlag(Player admin, Player target, int page) {
        if (!lagEffectManager.hasActiveLagEffect(target)) {
            configManager.sendMessage(admin, "lag_already_removed", target.getName());
        } else {
            lagEffectManager.removeLagEffect(target);
            configManager.sendMessage(admin, "lag_removed", target.getName());
        }

        scheduleOpen(admin, page, 3L);
    }

    private void handleUnlagAll(Player admin, int page) {
        lagEffectManager.clearAllLagEffects();
        configManager.sendMessage(admin, "lag_removed_all");
        scheduleOpen(admin, page);
    }

    private void handleReloadConfig(Player admin, int page) {
        configManager.reloadConfig();
        menuManager.reloadMenu();
        staticItemCache.clear();
        configManager.sendMessage(admin, "reload_success");
        scheduleOpen(admin, page);
    }

    private void handleClose(Player admin) {
        admin.closeInventory();
    }

    private void handleCrash(Player admin, Player target, CrashManager.CrashType crashType) {
        if (target == null || !target.isOnline()) {
            configManager.sendMessage(admin, "player_not_found");
            return;
        }

        crashManager.crashPlayer(target, crashType);

        String messageKey = switch (crashType) {
            case ENTITY -> "crashed_entity";
            case SIGN -> "crashed_sign";
            case PAYLOAD -> "crashed_payload";
            default -> "crashed";
        };

        configManager.sendMessage(admin, messageKey, target.getName());
    }

    private void scheduleOpen(Player admin, int page) {
        scheduleOpen(admin, page, 1L);
    }

    private void scheduleOpen(Player admin, int page, long delay) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> open(admin, page), delay);
    }

    private void setCooldown(UUID playerId) {
        actionCooldown.add(playerId);
        Bukkit.getScheduler().runTaskLater(plugin,
                () -> actionCooldown.remove(playerId),
                COOLDOWN_TICKS
        );
    }
}