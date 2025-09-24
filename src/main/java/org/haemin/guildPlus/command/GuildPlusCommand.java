package org.haemin.guildPlus.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.haemin.guildPlus.GuildPlus;
import org.haemin.guildPlus.command.sub.BuffCommand;
import org.haemin.guildPlus.command.sub.ChestCommand;
import org.haemin.guildPlus.command.sub.ReloadCommand;

import java.util.*;

public class GuildPlusCommand implements CommandExecutor, TabCompleter {
    private final Map<String, SubCommand> subs = new LinkedHashMap<>();

    public GuildPlusCommand(GuildPlus plugin) {
        add(new ReloadCommand(plugin));
        add(new BuffCommand(plugin));
        add(new ChestCommand(plugin));
    }

    private void add(SubCommand s) { subs.put(s.name(), s); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("/" + label + " reload|buff|chest");
            return true;
        }
        SubCommand s = subs.get(args[0].toLowerCase(Locale.ROOT));
        if (s == null) {
            sender.sendMessage("/" + label + " reload|buff|chest");
            return true;
        }
        String[] rest = Arrays.copyOfRange(args, 1, args.length);
        return s.execute(sender, rest);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> list = new ArrayList<>(subs.keySet());
            list.sort(String.CASE_INSENSITIVE_ORDER);
            return filter(list, args[0]);
        }
        SubCommand s = subs.get(args[0].toLowerCase(Locale.ROOT));
        if (s == null) return Collections.emptyList();
        return s.tab(sender, Arrays.copyOfRange(args, 1, args.length));
    }

    private List<String> filter(List<String> list, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        List<String> out = new ArrayList<>();
        for (String s : list) if (s.toLowerCase(Locale.ROOT).startsWith(p)) out.add(s);
        return out;
    }

    public interface SubCommand {
        String name();
        default String permission() { return ""; }
        boolean execute(CommandSender sender, String[] args);
        default List<String> tab(CommandSender sender, String[] args) { return Collections.emptyList(); }
    }
}
