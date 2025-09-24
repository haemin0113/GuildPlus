package org.haemin.guildPlus.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.haemin.guildPlus.GuildPlus;
import org.haemin.guildPlus.command.GuildPlusCommand;
import org.haemin.guildPlus.gui.BuffGui;

import java.util.*;

public class BuffCommand implements GuildPlusCommand.SubCommand {
    private final GuildPlus plugin;
    public BuffCommand(GuildPlus plugin) { this.plugin = plugin; }

    @Override public String name() { return "buff"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Player only.");
            return true;
        }
        Player p = (Player) sender;

        String gid = plugin.hook().getGuildId(p);
        if (gid == null) {
            p.sendMessage(org.haemin.guildPlus.util.Configs.msg("no-guild"));
            return true;
        }

        if (args.length == 0) {
            p.sendMessage("/guildplus buff <infinite|temp>");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("infinite")) { BuffGui.openInfinite(plugin, p); return true; }
        if (sub.equals("temp"))     { BuffGui.openTemp(plugin, p);     return true; }

        p.sendMessage("/guildplus buff <infinite|temp>");
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        if (args.length == 1) return filter(Arrays.asList("infinite", "temp"), args[0]);
        return Collections.emptyList();
    }
    private List<String> filter(List<String> list, String p) {
        String s = p.toLowerCase(Locale.ROOT); List<String> out = new ArrayList<>();
        for (String e : list) if (e.toLowerCase(Locale.ROOT).startsWith(s)) out.add(e);
        return out;
    }
}
