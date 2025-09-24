package org.haemin.guildPlus.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GuildChestLockEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player actor;
    private final String guildId;
    private final int pageIndex;
    private final boolean locked;
    public GuildChestLockEvent(Player actor, String guildId, int pageIndex, boolean locked) { this.actor=actor; this.guildId=guildId; this.pageIndex=pageIndex; this.locked=locked; }
    public Player getActor() { return actor; }
    public String getGuildId() { return guildId; }
    public int getPageIndex() { return pageIndex; }
    public boolean isLocked() { return locked; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
