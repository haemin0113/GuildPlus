package org.haemin.guildPlus;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.haemin.guildPlus.buff.BuffService;
import org.haemin.guildPlus.command.GuildPlusCommand;
import org.haemin.guildPlus.gui.BuffGui;
import org.haemin.guildPlus.gui.ChestSelectorGui;
import org.haemin.guildPlus.hook.EconHook;
import org.haemin.guildPlus.hook.MmocoreHook;
import org.haemin.guildPlus.listener.ChestListener;
import org.haemin.guildPlus.stats.MythicLibStatService;
import org.haemin.guildPlus.util.Configs;
import org.haemin.guildPlus.view.DisplayService;
import org.haemin.guildPlus.chest.ChestService;
import org.haemin.guildPlus.papi.GuildPlusExpansion;

public final class GuildPlus extends JavaPlugin {

    private static GuildPlus instance;
    private MmocoreHook hook;
    private EconHook econ;
    private MythicLibStatService mythic;
    private BuffService buffs;
    private ChestService chests;
    private DisplayService display;

    public static GuildPlus get() { return instance; }
    public MmocoreHook hook() { return hook; }
    public EconHook econ() { return econ; }
    public MythicLibStatService mythic() { return mythic; }
    public BuffService buffs() { return buffs; }
    public ChestService chests() { return chests; }
    public DisplayService display() { return display; }

    @Override
    public void onEnable() {
        instance = this;

        if (getServer().getPluginManager().getPlugin("MythicLib") == null) {
            getLogger().severe("MythicLib not found. Install MythicLib.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();
        Configs.init(this);
        if (getConfig().getBoolean("debug", false)) getLogger().info("[DEBUG] Debug mode is ON");

        mythic = new MythicLibStatService(this);
        hook = new MmocoreHook(this);
        econ = new EconHook(this);

        buffs = new BuffService(this, hook);
        buffs.loadAndSchedule();

        chests = new ChestService(this, hook);
        chests.loadAll();

        getCommand("guildplus").setExecutor(new GuildPlusCommand(this));

        BuffGui buffGui = new BuffGui(this);
        Bukkit.getPluginManager().registerEvents(buffGui, this);

        ChestSelectorGui chestGui = new ChestSelectorGui(this, chests);
        Bukkit.getPluginManager().registerEvents(chestGui, this);

        Bukkit.getPluginManager().registerEvents(new ChestListener(this, chests), this);

        display = new DisplayService(this);
        Bukkit.getPluginManager().registerEvents(display, this);
        display.start();

        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new GuildPlusExpansion(this).register();
        }
    }

    @Override
    public void onDisable() {
        try { if (display != null) display.stop(); } catch (Exception ignored) {}
        try { if (chests != null) chests.saveAll(); } catch (Exception ignored) {}
    }
}
