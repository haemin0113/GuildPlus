package org.haemin.guildPlus.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GuildChestOpenEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String guildId;
    private final int pageIndex;
    public GuildChestOpenEvent(Player player, String guildId, int pageIndex) { this.player=player; this.guildId=guildId; this.pageIndex=pageIndex; }
    public Player getPlayer() { return player; }
    public String getGuildId() { return guildId; }
    public int getPageIndex() { return pageIndex; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
