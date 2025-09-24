package org.haemin.guildPlus.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GuildChestAliasChangeEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player actor;
    private final String guildId;
    private final int pageIndex;
    private final String alias;
    public GuildChestAliasChangeEvent(Player actor, String guildId, int pageIndex, String alias) { this.actor=actor; this.guildId=guildId; this.pageIndex=pageIndex; this.alias=alias; }
    public Player getActor() { return actor; }
    public String getGuildId() { return guildId; }
    public int getPageIndex() { return pageIndex; }
    public String getAlias() { return alias; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
