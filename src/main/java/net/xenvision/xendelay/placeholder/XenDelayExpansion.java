package net.xenvision.xendelay.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.xenvision.xendelay.utils.LagEffectManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class XenDelayExpansion extends PlaceholderExpansion {
    
    private final LagEffectManager lagEffectManager;

    public XenDelayExpansion(LagEffectManager lagEffectManager) {
        this.lagEffectManager = lagEffectManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "xendelay";
    }

    @Override
    public @NotNull String getAuthor() {
        return "XenVision";
    }

    @Override
    public @NotNull String getVersion() {
        return "2.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null) return "";
        if (params.equalsIgnoreCase("lagged")) {
            return lagEffectManager.isLagged(player) ? "§c✔ Lagged" : "§a✘ Normal";
        }
        
        return null;
    }
}
