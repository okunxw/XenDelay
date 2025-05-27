package net.xenvision.xendelay.gui;

import net.xenvision.xendelay.utils.LagEffectManager;
import net.xenvision.xendelay.utils.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;

import java.util.stream.Collectors;

public class LagGui implements Listener {
    private final LagEffectManager lagEffectManager;
    private final ConfigManager configManager;
    private final Plugin plugin;

    public LagGui(Plugin plugin, LagEffectManager lagEffectManager, ConfigManager configManager) {
        this.plugin = plugin;
        this.lagEffectManager = lagEffectManager;
        this.configManager = configManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void open(Player admin) {
        int size = 9 * 4;
        Inventory inv = Bukkit.createInventory(null, size, "§bXenDelay Lag Manager");

        int i = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();
            meta.setOwningPlayer(p);
            boolean lagged = lagEffectManager.isLagged(p);
            meta.setDisplayName((lagged ? "§c" : "§a") + p.getName() + (lagged ? " §c(Lagged)" : " §a(Normal)"));
            meta.setLore(java.util.Collections.singletonList(lagged ? "§7ЛКМ: Снять лаги" : "§7ЛКМ: Выдать лаги"));
            skull.setItemMeta(meta);
            inv.setItem(i++, skull);
        }
        // Unlag All
        ItemStack unlagAll = new ItemStack(Material.BARRIER);
        org.bukkit.inventory.meta.ItemMeta uMeta = unlagAll.getItemMeta();
        uMeta.setDisplayName("§cUnlag All");
        unlagAll.setItemMeta(uMeta);
        inv.setItem(size - 2, unlagAll);

        // Reload
        ItemStack reload = new ItemStack(Material.COMMAND_BLOCK);
        org.bukkit.inventory.meta.ItemMeta rMeta = reload.getItemMeta();
        rMeta.setDisplayName("§eReload Config");
        reload.setItemMeta(rMeta);
        inv.setItem(size - 1, reload);

        admin.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTitle().contains("XenDelay Lag Manager"))) return;
        e.setCancelled(true);
        Player admin = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        String name = clicked.getItemMeta().getDisplayName();
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
            open(admin); // refresh menu
        } else if (clicked.getType() == Material.BARRIER && "§cUnlag All".equals(name)) {
            lagEffectManager.removeLagFromAll();
            configManager.sendMessage(admin, "lag_removed_all");
            open(admin); // refresh menu
        } else if (clicked.getType() == Material.COMMAND_BLOCK && "§eReload Config".equals(name)) {
            configManager.reloadConfig();
            configManager.sendMessage(admin, "reload_success");
            open(admin); // refresh menu
        }
    }
}
