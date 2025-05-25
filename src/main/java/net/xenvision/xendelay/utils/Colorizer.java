package net.xenvision.xendelay.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.ChatColor;

public class Colorizer {
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([a-fA-F\\d]{6})");

    public static String colorize(String str) {
        if (str == null || str.isEmpty()) return "";

        Matcher matcher = HEX_PATTERN.matcher(str);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String hexCode = matcher.group(1);
            String formattedHex = "§x" + hexCode.chars()
                    .mapToObj(c -> "§" + (char) c)
                    .reduce("", String::concat);
            matcher.appendReplacement(result, formattedHex);
        }

        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(result).toString());
    }
}

