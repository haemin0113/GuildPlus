package org.haemin.guildPlus.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.haemin.guildPlus.GuildPlus;

import java.io.File;

public final class Configs {
    private static GuildPlus plugin;
    private static FileConfiguration cfg, buffs, messages, sounds, gui;

    public static void init(GuildPlus pl) {
        plugin = pl;
        plugin.saveDefaultConfig();
        reloadAll();
    }

    public static void reloadAll() {
        plugin.reloadConfig();
        cfg = plugin.getConfig();
        buffs = load("buffs.yml");
        messages = load("messages.yml");
        sounds = load("sounds.yml");
        gui = load("gui.yml");     // ← 반드시 로드
    }

    public static void reloadGui() { gui = load("gui.yml"); }
    public static void reloadBuffs() { buffs = load("buffs.yml"); }

    public static FileConfiguration cfg() { return cfg; }
    public static FileConfiguration buffs() { return buffs; }
    public static FileConfiguration msgs() { return messages; }
    public static FileConfiguration sounds() { return sounds; }
    public static FileConfiguration gui() { if (gui == null) reloadGui(); return gui; }

    private static FileConfiguration load(String name) {
        File f = new File(plugin.getDataFolder(), name);
        if (!f.exists()) plugin.saveResource(name, false);
        return YamlConfiguration.loadConfiguration(f);
    }

    public static String msg(String key) {
        String raw = msgs().getString(key, key);
        String prefix = msgs().getString("prefix", "");
        return org.haemin.guildPlus.util.Chat.color(raw.replace("{prefix}", prefix));
    }

    public static String msg(String key, java.util.Map<String, String> vars) {
        String out = msgs().getString(key, key);
        out = out.replace("{prefix}", msgs().getString("prefix", ""));
        for (java.util.Map.Entry<String,String> e : vars.entrySet())
            out = out.replace("{"+e.getKey()+"}", e.getValue());
        return org.haemin.guildPlus.util.Chat.color(out);
    }

}
