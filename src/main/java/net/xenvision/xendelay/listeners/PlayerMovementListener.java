package net.xenvision.xendelay.listeners;

import net.xenvision.xendelay.utils.LagEffectManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.entity.Player;

public class PlayerMovementListener implements Listener {

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();

        // Проверяем, включены ли лаги у игрока
        if (LagEffectManager.isLagged(player)) {
            // Останавливаем движение игрока на несколько миллисекунд
            event.setCancelled(true);

            // Имитация нестабильного соединения - небольшой телепорт
            player.teleport(player.getLocation().add(
                    Math.random() * 0.3 - 0.15, 0, Math.random() * 0.3 - 0.15));
        }
    }
}