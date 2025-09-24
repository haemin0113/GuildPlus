package org.haemin.guildPlus.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.haemin.guildPlus.GuildPlus;

import java.util.Locale;

public final class SoundUtil {
    public static void play(Player p, String spec) {
        if (spec == null) return;
        spec = spec.trim();
        if (spec.isEmpty() || spec.equalsIgnoreCase("none")) return;

        float vol = 1f, pit = 1f;
        String name = spec;

        if (spec.contains(",")) {
            String[] arr = spec.split("\\s*,\\s*");
            name = arr[0];
            if (arr.length >= 2) try { vol = Float.parseFloat(arr[1]); } catch (Exception ignored) {}
            if (arr.length >= 3) try { pit = Float.parseFloat(arr[2]); } catch (Exception ignored) {}
        }

        String[] candidates = name.split("\\|");
        for (String cand : candidates) {
            String enumName = cand.trim();
            if (enumName.contains(":"))
                enumName = enumName.substring(enumName.indexOf(':') + 1).replace('.', '_');
            enumName = enumName.toUpperCase(Locale.ROOT);
            try {
                Sound s = Sound.valueOf(enumName);
                p.playSound(p.getLocation(), s, vol, pit);
                debug("Played sound " + enumName + " v=" + vol + " p=" + pit);
                return;
            } catch (IllegalArgumentException ignored) {}
        }
        debug("Sound not found for spec: " + spec);
    }

    private static void debug(String msg) {
        if (GuildPlus.get().getConfig().getBoolean("debug", false))
            GuildPlus.get().getLogger().info("[DEBUG] " + msg);
    }
}
