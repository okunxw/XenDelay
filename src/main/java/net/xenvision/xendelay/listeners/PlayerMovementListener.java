package net.xenvision.xendelay.listeners;

import net.xenvision.xendelay.utils.LagEffectManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;

/**
 * Handles blocking movement for lagged players.
 */
public class PlayerMovementListener implements Listener {
    
    private final LagEffectManager lagEffectManager;

    public PlayerMovementListener(LagEffectManager lagEffectManager) {
        this.lagEffectManager = lagEffectManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        
        if (!lagEffectManager.isLagged(player)) return;
        
        event.setCancelled(true);
        // Optional: Visual feedback, particles, sounds here.
    }
}
