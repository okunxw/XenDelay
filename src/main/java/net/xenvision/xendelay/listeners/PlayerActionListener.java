package net.xenvision.xendelay.listeners;

import net.xenvision.xendelay.utils.LagEffectManager;
import net.xenvision.xendelay.utils.ConfigManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.entity.Player;

public class PlayerActionListener implements Listener {
    private final ConfigManager configManager;

    public PlayerActionListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();

        // Проверяем, активированы ли лаги у игрока
        if (LagEffectManager.isLagged(player)) {
            event.setCancelled(true);

            boolean enableLagMessages = configManager.getConfig().getBoolean("lag_settings.enable_lag_messages", true);
            if (enableLagMessages) {
                configManager.sendMessage(player, "error_interact_blocked", player.getName());
            }
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player player = (Player) event.getDamager();

            // Проверяем, активированы ли лаги у игрока
            if (LagEffectManager.isLagged(player)) {
                event.setCancelled(true);

                boolean enableLagMessages = configManager.getConfig().getBoolean("lag_settings.enable_lag_messages", true);
                if (enableLagMessages) {
                    configManager.sendMessage(player, "error_attack_blocked", player.getName());
                }
            }
        }
    }
}