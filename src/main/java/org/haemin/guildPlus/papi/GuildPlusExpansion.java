package org.haemin.guildPlus.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.haemin.guildPlus.GuildPlus;

public class GuildPlusExpansion extends PlaceholderExpansion {
    private final GuildPlus plugin;
    public GuildPlusExpansion(GuildPlus plugin) { this.plugin = plugin; }

    @Override public String getIdentifier() { return "guildplus"; }
    @Override public String getAuthor() { return "haemin"; }
    @Override public String getVersion() { return "1.0.0"; }

    @Override
    public String onPlaceholderRequest(Player p, String params) {
        if (p == null) return "";
        String gid = plugin.hook().getGuildId(p);
        if (gid == null) return "";
        if (params.equalsIgnoreCase("temp_count")) {
            return String.valueOf(plugin.buffs().activeTempCount(gid));
        }
        return "";
    }
}
