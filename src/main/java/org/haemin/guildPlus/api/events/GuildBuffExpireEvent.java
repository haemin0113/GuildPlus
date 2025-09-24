package org.haemin.guildPlus.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GuildBuffExpireEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String guildId;
    private final String nodeId;
    public GuildBuffExpireEvent(String guildId, String nodeId) { this.guildId=guildId; this.nodeId=nodeId; }
    public String getGuildId() { return guildId; }
    public String getNodeId() { return nodeId; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
