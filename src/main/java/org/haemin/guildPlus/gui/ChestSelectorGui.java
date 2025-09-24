package org.haemin.guildPlus.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.haemin.guildPlus.GuildPlus;
import org.haemin.guildPlus.util.Chat;
import org.haemin.guildPlus.util.Configs;
import org.haemin.guildPlus.util.SoundUtil;

import java.util.*;

public final class ChestSelectorGui {
    private static GuildPlus plugin;
    private static boolean registered = false;

    public static void open(GuildPlus pl, Player p) {
        ensure(pl);
        String gid = pl.hook().getGuildId(p);
        if (gid == null) { p.sendMessage(Configs.msg("no-guild")); return; }
        ConfigurationSection sel = Configs.gui().getConfigurationSection("chest.selector");
        String title = Chat.color(sel.getString("title","&8길드 창고 선택"));
        int size = Math.max(9, Math.min(54, sel.getInt("size", 9)));
        Holder holder = new Holder();
        Inventory inv = Bukkit.createInventory(holder, size, title);
        ConfigurationSection unlocked = sel.getConfigurationSection("item-unlocked");
        ConfigurationSection locked = sel.getConfigurationSection("item-locked");
        double unlockCost = Configs.cfg().getDouble("chest.cost.unlock-page", 0D);
        for (int page = 1; page <= 9 && page <= size; page++) {
            int slot = page - 1;
            boolean isUnlocked = plugin.chest().isPageUnlocked(gid, page);
            ItemStack it = isUnlocked ? make(unlocked, page, unlockCost) : make(locked, page, unlockCost);
            inv.setItem(slot, it);
            holder.pages.put(slot, page);
        }
        p.openInventory(inv);
        SoundUtil.play(p, "gui.chest.open");
    }

    private static void ensure(GuildPlus pl) {
        if (plugin == null) plugin = pl;
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(new Clicks(), plugin);
            registered = true;
        }
    }

    private static ItemStack make(ConfigurationSection sec, int page, double cost) {
        String mat = sec.getString("material","CHEST");
        String name = sec.getString("name","&a{PAGE}번 창고");
        List<String> lore = sec.getStringList("lore");
        ItemStack is = new ItemStack(asMat(mat));
        ItemMeta im = is.getItemMeta();
        Map<String,String> vars = new HashMap<>();
        vars.put("PAGE", String.valueOf(page));
        vars.put("COST", cost <= 0 ? "무료" : String.format(Locale.ROOT,"%.0f", cost));
        im.setDisplayName(Chat.color(apply(name, vars)));
        List<String> out = new ArrayList<>();
        for (String l : lore) out.add(Chat.color(apply(l, vars)));
        im.setLore(out);
        im.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        is.setItemMeta(im);
        return is;
    }

    private static Material asMat(String s) {
        try { return Material.valueOf(s.toUpperCase(Locale.ROOT)); } catch (Throwable t) { return Material.CHEST; }
    }

    private static String apply(String s, Map<String,String> vars) {
        String out = s;
        for (Map.Entry<String,String> e : vars.entrySet()) out = out.replace("{"+e.getKey()+"}", e.getValue());
        return out;
    }

    private static final class Holder implements InventoryHolder {
        final Map<Integer,Integer> pages = new HashMap<>();
        @Override public Inventory getInventory() { return null; }
    }

    private static final class Clicks implements Listener {
        @EventHandler
        public void onClick(InventoryClickEvent e) {
            HumanEntity he = e.getWhoClicked();
            if (!(he instanceof Player)) return;
            if (!(e.getInventory().getHolder() instanceof Holder)) return;
            Holder h = (Holder) e.getInventory().getHolder();
            e.setCancelled(true);
            Integer page = h.pages.get(e.getRawSlot());
            if (page == null) return;
            Player p = (Player) he;
            String gid = plugin.hook().getGuildId(p);
            if (gid == null) { p.closeInventory(); p.sendMessage(Configs.msg("no-guild")); SoundUtil.play(p,"gui.chest.deny"); return; }
            boolean unlocked = plugin.chest().isPageUnlocked(gid, page);
            ClickType ct = e.getClick();
            if (unlocked) {
                if (ct.isShiftClick() && ct.isLeftClick()) {
                    boolean ok = plugin.chest().tryExpandRows(p, page);
                    if (ok) SoundUtil.play(p,"gui.chest.click"); else SoundUtil.play(p,"gui.chest.deny");
                    Bukkit.getScheduler().runTask(plugin, () -> open(plugin, p));
                } else if (ct.isLeftClick()) {
                    SoundUtil.play(p,"gui.chest.click");
                    plugin.chest().open(p, page);
                } else {
                    SoundUtil.play(p,"gui.chest.click");
                }
            } else {
                if (ct.isRightClick()) {
                    int prevRows = page == 1 ? 6 : plugin.chest().getRows(gid, page - 1);
                    if (page > 1 && prevRows < 6) {
                        p.sendMessage(Configs.msg("chest-unlock-need-prev-max"));
                        SoundUtil.play(p,"gui.chest.deny");
                        return;
                    }
                    boolean ok = plugin.chest().tryUnlockPage(p, page);
                    if (ok) {
                        SoundUtil.play(p,"gui.chest.unlock");
                        Bukkit.getScheduler().runTask(plugin, () -> open(plugin, p));
                    } else {
                        SoundUtil.play(p,"gui.chest.deny");
                    }
                } else {
                    SoundUtil.play(p,"gui.chest.deny");
                }
            }
        }
    }
}