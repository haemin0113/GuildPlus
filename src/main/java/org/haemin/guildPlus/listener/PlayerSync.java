package org.haemin.guildPlus.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.haemin.guildPlus.GuildPlus;

public class PlayerSync implements Listener {
    private final GuildPlus plugin;
    public PlayerSync(GuildPlus plugin) { this.plugin = plugin; }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.buffs().reapplyFor(e.getPlayer()), 1L);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.buffs().reapplyFor(e.getPlayer()), 1L);
    }
}
