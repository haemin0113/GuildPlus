package org.haemin.guildPlus.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.haemin.guildPlus.GuildPlus;
import org.haemin.guildPlus.buff.BuffService;

public class BuffReapplyListener implements Listener {
    private final GuildPlus plugin;
    private final BuffService buffs;

    public BuffReapplyListener(GuildPlus plugin, BuffService buffs) {
        this.plugin = plugin;
        this.buffs = buffs;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> buffs.reapplyFor(e.getPlayer()));
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> buffs.reapplyFor(e.getPlayer()));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Bukkit.getScheduler().runTask(plugin, () -> buffs.reapplyFor(e.getPlayer()));
    }
}
