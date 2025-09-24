package org.haemin.guildPlus.chest;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.haemin.guildPlus.GuildPlus;
import org.haemin.guildPlus.hook.MmocoreHook;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChestService {

    private final GuildPlus plugin;
    private final MmocoreHook hook;

    private final Map<String, GuildChest> data = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> openingPage = new ConcurrentHashMap<>();

    public ChestService(GuildPlus plugin, MmocoreHook hook) {
        this.plugin = plugin;
        this.hook = hook;
    }

    public void loadAll() {
        ChestPersistence.loadAll(plugin, this);
    }

    public void saveAll() {
        ChestPersistence.saveAll(plugin, this, new LinkedHashMap<>(data));
    }

    public void openSelector(Player p) {
        new org.haemin.guildPlus.gui.ChestSelectorGui(plugin, this).open(p);
    }

    public void openPage(Player p, String guildId, int page) {
        openingPage.put(p.getUniqueId(), page);
        Inventory inv = getOrCreate(guildId).getPageInventory(page);
        p.openInventory(inv);
    }

    public Integer peekOpeningPage(Player p) { return openingPage.get(p.getUniqueId()); }

    public boolean canOpen(Player p, String guildId, int page) {
        if (guildId == null) return false;
        String g = hook.getGuildId(p);
        if (g == null || !g.equals(guildId)) return false;
        GuildChest gc = getOrCreate(guildId);
        if (!gc.isUnlocked(page)) return false;
        if (gc.isLocked(page)) return false;
        return true;
    }

    public boolean canEdit(Player p, String guildId, int page) { return canOpen(p, guildId, page); }

    public boolean canExpandRows(Player p, String guildId, int page) { return canOpen(p, guildId, page); }

    public boolean canUnlockPage(Player p, String guildId, int page) {
        if (guildId == null) return false;
        String g = hook.getGuildId(p);
        if (g == null || !g.equals(guildId)) return false;
        GuildChest gc = getOrCreate(guildId);
        return !gc.isUnlocked(page) && !gc.isLocked(page);
    }

    public GuildChest getOrCreate(String guildId) {
        return data.computeIfAbsent(guildId, k -> new GuildChest(plugin, guildId));
    }

    public static class GuildChest {
        private final GuildPlus plugin;
        private final String guildId;
        private final Map<Integer, Inventory> pages = new HashMap<>();
        private final Set<Integer> unlocked = new HashSet<>();
        private final Set<Integer> locked = new HashSet<>();
        private final Map<Integer, Integer> rows = new HashMap<>();
        private final Map<Integer, String> names = new HashMap<>();

        public GuildChest(GuildPlus plugin, String guildId) {
            this.plugin = plugin;
            this.guildId = guildId;
            unlocked.add(1);
            rows.put(1, 1);
            names.put(1, "길드 창고 1");
        }

        public boolean isUnlocked(int page) { return unlocked.contains(page); }
        public boolean isLocked(int page) { return locked.contains(page); }

        public void unlockPage(int page) {
            unlocked.add(page);
            rows.putIfAbsent(page, 1);
            names.putIfAbsent(page, "길드 창고 " + page);
        }

        public void lockPage(int page) { locked.add(page); }
        public void unlockLock(int page) { locked.remove(page); }

        public int getRows(int page) { return Math.max(1, rows.getOrDefault(page, 1)); }
        public void setRows(int page, int rows) { this.rows.put(page, Math.max(1, Math.min(6, rows))); }

        public String getName(int page) { return names.getOrDefault(page, "길드 창고 " + page); }
        public void setName(int page, String name) { names.put(page, name); }

        public Inventory getPageInventory(int page) {
            int r = getRows(page);
            String title = getName(page);
            Inventory inv = pages.get(page);
            if (inv == null || inv.getSize() != r * 9) {
                inv = Bukkit.createInventory(null, r * 9, title);
                pages.put(page, inv);
            }
            return inv;
        }
    }
}
