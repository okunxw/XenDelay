package net.xenvision.xendelay.gui;

import net.xenvision.xendelay.utils.LagEffectManager;
import net.xenvision.xendelay.utils.ConfigManager;
import net.xenvision.xendelay.utils.MenuManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class MenuBuilder implements Listener {
    private final LagEffectManager lagEffectManager;
    private final ConfigManager configManager;
    private final MenuManager menuManager;
    private final Plugin plugin;

    public MenuBuilder(Plugin plugin, LagEffectManager lagEffectManager, ConfigManager configManager, MenuManager menuManager) {
        this.plugin = plugin;
        this.lagEffectManager = lagEffectManager;
        this.configManager = configManager;
        this.menuManager = menuManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player admin) {
        FileConfiguration menuConfig = menuManager.getMenuConfig();
        ConfigurationSection menu = menuConfig.getConfigurationSection("menu");
        if (menu == null) {
            admin.sendMessage("§c[Ошибка] menu.yml не содержит секцию menu!");
            return;
        }
        String title = menu.getString("title", "XenDelay Menu");
        int size = menu.getInt("size", 36);
        Inventory inv = Bukkit.createInventory(null, size, title);

        // Dynamic players section
        ConfigurationSection playersSection = menu.getConfigurationSection("items.players");
        if (playersSection != null) {
            int start = playersSection.getInt("slot_start", 0);
            int end = playersSection.getInt("slot_end", 8);
            Material mat = Material.valueOf(playersSection.getString("material", "PLAYER_HEAD"));
            for (Player p : Bukkit.getOnlinePlayers()) {
                ItemStack skull = new ItemStack(mat);
                SkullMeta meta = (SkullMeta) skull.getItemMeta();
                meta.setOwningPlayer(p);
                String status = lagEffectManager.isLagged(p) ? "§cLagged" : "§aNormal";
                String display = playersSection.getString("display", "%player_colored%").replace("%player_colored%", (lagEffectManager.isLagged(p) ? "§c" : "§a") + p.getName());
                meta.setDisplayName(display);
                List<String> lore = new ArrayList<>();
                List<String> loreConfig = playersSection.getStringList("lore");
                if (loreConfig != null && !loreConfig.isEmpty()) {
                    for (String l : loreConfig) {
                        lore.add(l.replace("%status%", status));
                    }
                }
                meta.setLore(lore);
                skull.setItemMeta(meta);
                int slot = start++;
                if (slot > end) break;
                inv.setItem(slot, skull);
            }
        }

        // Static items (unlagall, reload, etc.)
        ConfigurationSection itemsSection = menu.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                if (key.equals("players")) continue;
                ConfigurationSection item = itemsSection.getConfigurationSection(key);
                if (item == null) continue;
                int slot = item.getInt("slot");
                Material material = Material.valueOf(item.getString("material", "STONE"));
                ItemStack stack = new ItemStack(material);
                ItemMeta meta = stack.getItemMeta();
                meta.setDisplayName(item.getString("display", key));
                meta.setLore(item.getStringList("lore"));
                stack.setItemMeta(meta);
                inv.setItem(slot, stack);
            }
        }

        admin.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTitle().contains("XenDelay"))) return;
        e.setCancelled(true);
        Player admin = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName();
        // Player head logic
        if (clicked.getType() == Material.PLAYER_HEAD) {
            String playerName = org.bukkit.ChatColor.stripColor(name).replace(" (Lagged)", "").replace(" (Normal)", "");
            Player target = Bukkit.getPlayer(playerName);
            if (target == null) {
                configManager.sendMessage(admin, "player_not_found", playerName);
                return;
            }
            if (lagEffectManager.isLagged(target)) {
                lagEffectManager.removeLag(target);
                configManager.sendMessage(admin, "lag_removed", target.getName());
                configManager.sendMessage(target, "you_are_unlagged");
            } else {
                lagEffectManager.applyLag(target);
                configManager.sendMessage(admin, "lag_applied", target.getName());
                configManager.sendMessage(target, "you_are_lagged");
            }
            open(admin); // refresh
        }
        // Unlag All
        else if (clicked.getType() == Material.BARRIER && name.contains("Unlag All")) {
            lagEffectManager.removeLagFromAll();
            configManager.sendMessage(admin, "lag_removed_all");
            open(admin);
        }
        // Reload Config
        else if (clicked.getType() == Material.COMMAND_BLOCK && name.contains("Reload Config")) {
            configManager.reloadConfig();
            menuManager.reloadMenu();
            configManager.sendMessage(admin, "reload_success");
            open(admin);
        }
    }
}