package net.xenvision.xendelay.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.xenvision.xendelay.utils.LagEffectManager;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class XenDelayExpansion extends PlaceholderExpansion {

    private static final String LAGGED_KEY = "lagged";
    private static final String LAGGED_DISPLAY = "§c✔ Lagged";
    private static final String NORMAL_DISPLAY = "§a✘ Normal";

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
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) return "";

        String normalized = params.toLowerCase();
        if (LAGGED_KEY.equals(normalized)) {
            return lagEffectManager.hasActiveLagEffect(player) ? LAGGED_DISPLAY : NORMAL_DISPLAY;
        }

        return null;
    }
}