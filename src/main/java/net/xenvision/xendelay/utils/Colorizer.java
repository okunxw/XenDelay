package net.xenvision.xendelay.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Colorizer {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f\\d]{6})");
    private static final boolean SUPPORTS_HEX = isHexSupported();

    private static boolean isHexSupported() {
        String version = Bukkit.getBukkitVersion();
        int majorVersion = parseMajorVersion(version);
        return majorVersion >= 16;
    }

    private static int parseMajorVersion(String version) {
        try {
            String[] parts = version.split("[-.]");

            if (parts.length > 1) {
                return Integer.parseInt(parts[1]);
            }

        } catch (NumberFormatException ignored) {
        }

        return 0;
    }

    public static String colorize(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        String processed = str;
        if (SUPPORTS_HEX && str.contains("&#")) {
            processed = processHexCodes(str);
        }

        return ChatColor.translateAlternateColorCodes('&', processed);
    }

    private static String processHexCodes(String str) {
        Matcher matcher = HEX_PATTERN.matcher(str);
        StringBuilder builder = new StringBuilder(str.length() + 32);

        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder(14); // §x§R§R§G§G§B§B
            replacement.append("§x");

            for (int i = 0; i < hex.length(); i++) {
                replacement.append('§').append(hex.charAt(i));
            }

            matcher.appendReplacement(builder, replacement.toString());
        }

        matcher.appendTail(builder);

        return builder.toString();
    }
}