package net.xenvision.xendelay.listeners;

import net.xenvision.xendelay.utils.LagEffectManager;
import net.xenvision.xendelay.utils.ConfigManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Handles action blocking (interactions/attacks) for lag-affected players
 * with message throttling to prevent spam.
 */
public class PlayerActionListener implements Listener {

    private static final long MESSAGE_THROTTLE_MS = TimeUnit.SECONDS.toMillis(3);
    
    private final ConfigManager configManager;
    private final LagEffectManager lagEffectManager;
    private final ConcurrentHashMap<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();
    private volatile boolean enableLagMessages;

    public PlayerActionListener(ConfigManager configManager, LagEffectManager lagEffectManager) {
        this.configManager = configManager;
        this.lagEffectManager = lagEffectManager;
        this.enableLagMessages = configManager.getConfig().getBoolean("lag_settings.enable_lag_messages", true);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (lagEffectManager.isLagged(player)) {
            event.setCancelled(true);
            notifyPlayer(player, "error_interact_blocked");
        }
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        
        if (lagEffectManager.isLagged(player)) {
            event.setCancelled(true);
            notifyPlayer(player, "error_attack_blocked");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastMessageTime.remove(event.getPlayer().getUniqueId());
    }

    private void notifyPlayer(Player player, String messageKey) {
        if (!enableLagMessages) return;

        UUID uuid = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        Long lastTime = lastMessageTime.get(uuid);

        if (lastTime == null || currentTime - lastTime >= MESSAGE_THROTTLE_MS) {
            configManager.sendMessage(player, messageKey);
            lastMessageTime.put(uuid, currentTime);
        }
    }

    // Updates settings when the configuration is reloaded
    public void reloadConfig() {
        this.enableLagMessages = configManager.getConfig().getBoolean("lag_settings.enable_lag_messages", true);
    }
}
