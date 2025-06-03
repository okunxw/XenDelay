package net.xenvision.xendelay.listeners;

import net.xenvision.xendelay.utils.LagEffectManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimized handler for blocking movement of lagged players.
 */
public class PlayerMovementListener implements Listener {
    
    private final LagEffectManager lagEffectManager;
    private final ConcurrentHashMap<UUID, Boolean> lagCache = new ConcurrentHashMap<>();
    private static final double EPSILON = 0.001; // Tolerance for coordinate comparison

    public PlayerMovementListener(LagEffectManager lagEffectManager) {
        this.lagEffectManager = lagEffectManager;
    }

    // Updates the cache when a player's lag status changes
    public void updateLagCache(Player player, boolean isLagged) {
        lagCache.put(player.getUniqueId(), isLagged);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        // Ignore micro-movements and head rotations
        if (isSameLocation(event.getFrom(), event.getTo(), EPSILON)) {
            return;
        }

        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Check cache before querying manager
        Boolean isLagged = lagCache.get(playerId);
        if (isLagged == null) {
            isLagged = lagEffectManager.isLagged(player);
            lagCache.put(playerId, isLagged);
        }
        
        if (isLagged) {
            event.setCancelled(true);
        }
    }

    // Compares locations with specified precision
    private boolean isSameLocation(Location loc1, Location loc2, double epsilon) {
        return Math.abs(loc1.getX() - loc2.getX()) < epsilon
            && Math.abs(loc1.getY() - loc2.getY()) < epsilon
            && Math.abs(loc1.getZ() - loc2.getZ()) < epsilon;
    }
}
