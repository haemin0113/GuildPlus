package org.haemin.guildPlus.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.haemin.guildPlus.GuildPlus;
import org.haemin.guildPlus.chest.ChestService;
import org.haemin.guildPlus.util.Chat;
import org.haemin.guildPlus.util.Configs;

public class ChestListener implements Listener {
    private final GuildPlus plugin;
    private final ChestService chests;

    public ChestListener(GuildPlus plugin, ChestService chests) {
        this.plugin = plugin;
        this.chests = chests;
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player p)) return;
        String gid = plugin.hook().getGuildId(p);
        Integer page = chests.peekOpeningPage(p);
        if (page == null) return;
        if (!chests.canOpen(p, gid, page)) {
            p.sendMessage(Chat.color(Configs.msg("chest-locked")));
            e.setCancelled(true);
        }
    }
}
