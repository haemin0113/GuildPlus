package org.haemin.guildPlus.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.haemin.guildPlus.GuildPlus;
import org.haemin.guildPlus.chest.ChestService;
import org.haemin.guildPlus.command.GuildPlusCommand;
import org.haemin.guildPlus.util.Chat;
import org.haemin.guildPlus.util.Configs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ChestCommand implements GuildPlusCommand.SubCommand {
    private final GuildPlus plugin;
    private final ChestService chests;

    public ChestCommand(GuildPlus plugin) {
        this.plugin = plugin;
        this.chests = plugin.chests();
    }

    @Override public String name() { return "chest"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Player only.");
            return true;
        }
        String gid = plugin.hook().getGuildId(p);
        if (gid == null) {
            p.sendMessage(Chat.color(Configs.msg("no-guild")));
            return true;
        }

        if (args.length == 0 || args[0].equalsIgnoreCase("open")) {
            if (args.length >= 2) {
                int page = parseInt(args[1], 1);
                if (!chests.canOpen(p, gid, page)) {
                    p.sendMessage(Chat.color(Configs.msg("chest-locked")));
                    return true;
                }
                chests.openPage(p, gid, page);
            } else {
                chests.openSelector(p);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("unlock")) {
            if (args.length < 2) { p.sendMessage("/guildplus chest unlock <page>"); return true; }
            int page = parseInt(args[1], 1);
            double cost = Configs.cfg().getDouble("chest.cost.unlock-page", 0D);
            if (cost > 0 && plugin.econ().isReady()) {
                if (!plugin.econ().has(p, cost)) {
                    p.sendMessage(Chat.color(Configs.msg("chest-unlock-page-need-money").replace("{cost}", String.valueOf(cost))));
                    return true;
                }
                if (!plugin.econ().withdraw(p, cost)) {
                    p.sendMessage(Chat.color(Configs.msg("withdraw-failed")));
                    return true;
                }
            }
            chests.getOrCreate(gid).unlockPage(page);
            if (plugin.display() != null) plugin.display().refreshGuild(gid);
            plugin.chests().saveAll();
            p.sendMessage(Chat.color(Configs.msg("chest-unlocked-page").replace("{page}", String.valueOf(page))));
            return true;
        }

        if (args[0].equalsIgnoreCase("expand")) {
            if (args.length < 2) { p.sendMessage("/guildplus chest expand <page> [rows]"); return true; }
            int page = parseInt(args[1], 1);
            int now = chests.getOrCreate(gid).getRows(page);
            int target = args.length >= 3 ? parseInt(args[2], now + 1) : now + 1;
            if (target > 6) target = 6;
            if (target <= now) { p.sendMessage(Chat.color("&e이미 해당 줄 수 이상입니다.")); return true; }

            int add = target - now;
            double unit = Configs.cfg().getDouble("chest.cost.expand-rows", 0D);
            double cost = unit * add;
            if (cost > 0 && plugin.econ().isReady()) {
                if (!plugin.econ().has(p, cost)) {
                    p.sendMessage(Chat.color(Configs.msg("chest-expand-need-money").replace("{cost}", String.valueOf(cost))));
                    return true;
                }
                if (!plugin.econ().withdraw(p, cost)) {
                    p.sendMessage(Chat.color(Configs.msg("withdraw-failed")));
                    return true;
                }
            }
            chests.getOrCreate(gid).setRows(page, target);
            if (plugin.display() != null) plugin.display().refreshGuild(gid);
            plugin.chests().saveAll();
            p.sendMessage(Chat.color(Configs.msg("chest-expanded-rows").replace("{rows}", String.valueOf(target))));
            return true;
        }

        p.sendMessage("/guildplus chest open|unlock|expand");
        return true;
    }

    @Override
    public List<String> tab(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.add("open"); out.add("unlock"); out.add("expand");
        } else if (args.length == 2 && (args[0].equalsIgnoreCase("unlock") || args[0].equalsIgnoreCase("expand"))) {
            for (int i = 1; i <= 9; i++) out.add(String.valueOf(i));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("expand")) {
            for (int i = 1; i <= 6; i++) out.add(String.valueOf(i));
        }
        return out;
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}
