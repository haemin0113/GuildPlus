package org.haemin.guildPlus.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GuildChestExpandEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player actor;
    private final String guildId;
    private final int pageIndex;
    private final int rowsAfter;
    public GuildChestExpandEvent(Player actor, String guildId, int pageIndex, int rowsAfter) { this.actor=actor; this.guildId=guildId; this.pageIndex=pageIndex; this.rowsAfter=rowsAfter; }
    public Player getActor() { return actor; }
    public String getGuildId() { return guildId; }
    public int getPageIndex() { return pageIndex; }
    public int getRowsAfter() { return rowsAfter; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
