package org.haemin.guildPlus.chest.model;

import org.bukkit.inventory.Inventory;

public class ChestPage {
    public boolean unlocked = false;
    public boolean locked = false;
    public int rows = 1;
    public String alias = null;
    public Inventory inv;
}
