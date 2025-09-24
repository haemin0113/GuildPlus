package org.haemin.guildPlus.api.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class GuildBuffActivateEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final String guildId;
    private final String nodeId;
    private final boolean temporary;
    private final long durationMillis;
    public GuildBuffActivateEvent(String guildId, String nodeId, boolean temporary, long durationMillis) { this.guildId=guildId; this.nodeId=nodeId; this.temporary=temporary; this.durationMillis=durationMillis; }
    public String getGuildId() { return guildId; }
    public String getNodeId() { return nodeId; }
    public boolean isTemporary() { return temporary; }
    public long getDurationMillis() { return durationMillis; }
    public HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
}
