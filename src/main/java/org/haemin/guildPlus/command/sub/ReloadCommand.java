package org.haemin.guildPlus.command.sub;

import org.bukkit.command.CommandSender;
import org.haemin.guildPlus.GuildPlus;
import org.haemin.guildPlus.command.GuildPlusCommand;
import org.haemin.guildPlus.util.Chat;
import org.haemin.guildPlus.util.Configs;

public class ReloadCommand implements GuildPlusCommand.SubCommand {
    private final GuildPlus plugin;
    public ReloadCommand(GuildPlus plugin) { this.plugin = plugin; }

    @Override public String name() { return "reload"; }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Configs.reloadAll();
        plugin.buffs().reload();
        plugin.chests().loadAll();
        if (plugin.display() != null) plugin.display().start();
        sender.sendMessage(Chat.color(Configs.msg("reloaded")));
        return true;
    }
}
