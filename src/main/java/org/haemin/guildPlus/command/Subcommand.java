package org.haemin.guildPlus.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface Subcommand {
    String name();
    boolean execute(CommandSender sender, String[] args);
    List<String> tabComplete(CommandSender sender, String[] args);
    default boolean hasPermissionForTab(CommandSender sender) { return true; }
}
