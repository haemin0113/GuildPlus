package org.haemin.guildPlus.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Chat {
    private static final Pattern HEX1 = Pattern.compile("(?i)&#([0-9a-f]{6})");
    private static final Pattern HEX2 = Pattern.compile("(?i)<#([0-9a-f]{6})>");

    public static String color(String s) {
        if (s == null) return null;
        s = applyHex(s, HEX1);
        s = applyHex(s, HEX2);
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    private static String applyHex(String s, Pattern p) {
        Matcher m = p.matcher(s);
        StringBuffer out = new StringBuffer();
        while (m.find()) m.appendReplacement(out, toSectionHex(m.group(1)));
        m.appendTail(out);
        return out.toString();
    }

    private static String toSectionHex(String hex) {
        StringBuilder sb = new StringBuilder("§x");
        for (char c : hex.toCharArray()) sb.append('§').append(c);
        return sb.toString();
    }

    public static Component component(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(color(legacy));
    }

    public static String formatDurationKR(long millis) {
        if (millis < 0) millis = 0;
        long s = millis / 1000;
        long d = s / 86400; s %= 86400;
        long h = s / 3600; s %= 3600;
        long m = s / 60; s %= 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("일 ");
        if (h > 0) sb.append(h).append("시간 ");
        if (m > 0) sb.append(m).append("분 ");
        sb.append(s).append("초");
        return sb.toString().trim();
    }
}
