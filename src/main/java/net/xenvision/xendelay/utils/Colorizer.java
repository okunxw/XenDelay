package net.xenvision.xendelay.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for colorizing strings with Bukkit color codes and hex support.
 */
public class Colorizer {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F\\d]{6})");

    /**
     * Colorizes a string, supporting both &-codes and hex color codes (&#RRGGBB).
     * For legacy versions of Minecraft, hex codes will be ignored.
     *
     * @param str Input string
     * @return Colorized string
     */
    public static String colorize(String str) {
        if (str == null || str.isEmpty()) return "";

        String result = str;
        // Detect server version for hex support (1.16+)
        boolean supportsHex = isHexSupported();

        if (supportsHex) {
            Matcher matcher = HEX_PATTERN.matcher(result);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String hexCode = matcher.group(1);
                StringBuilder replacement = new StringBuilder("§x");
                for (char c : hexCode.toCharArray()) {
                    replacement.append('§').append(c);
                }
                matcher.appendReplacement(buffer, replacement.toString());
            }
            matcher.appendTail(buffer);
            result = buffer.toString();
        }
        return ChatColor.translateAlternateColorCodes('&', result);
    }

    private static boolean isHexSupported() {
        // 1.16+ supports hex; you may want to cache this result
        String version = Bukkit.getBukkitVersion().split("-")[0];
        String[] parts = version.split("\\.");
        try {
            int major = Integer.parseInt(parts[1]);
            return major >= 16;
        } catch (Exception e) {
            return false;
        }
    }
}