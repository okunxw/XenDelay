package net.xenvision.xendelay.listeners;

import net.xenvision.xendelay.utils.LagEffectManager;
import net.xenvision.xendelay.utils.ConfigManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Player;

/**
 * Handles blocking actions (interact/attack) for lagged players, with messaging throttle.
 */
public class PlayerActionListener implements Listener {
    private final ConfigManager configManager;
    private final LagEffectManager lagEffectManager;

    public PlayerActionListener(ConfigManager configManager, LagEffectManager lagEffectManager) {
        this.configManager = configManager;
        this.lagEffectManager = lagEffectManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (lagEffectManager.isLagged(player)) {
            event.setCancelled(true);
            boolean enableLagMessages = configManager.getConfig().getBoolean("lag_settings.enable_lag_messages", true);
            if (enableLagMessages) {
                configManager.sendMessage(player, "error_interact_blocked");
            }
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();
        if (lagEffectManager.isLagged(player)) {
            event.setCancelled(true);
            boolean enableLagMessages = configManager.getConfig().getBoolean("lag_settings.enable_lag_messages", true);
            if (enableLagMessages) {
                configManager.sendMessage(player, "error_attack_blocked");
            }
        }
    }
}