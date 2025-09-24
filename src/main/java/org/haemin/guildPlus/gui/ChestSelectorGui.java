package org.haemin.guildPlus.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.haemin.guildPlus.GuildPlus;
import org.haemin.guildPlus.chest.ChestService;
import org.haemin.guildPlus.util.Chat;
import org.haemin.guildPlus.util.Configs;
import org.haemin.guildPlus.util.SoundUtil;

import java.util.ArrayList;
import java.util.List;

public class ChestSelectorGui implements Listener {
    private final GuildPlus plugin;
    private final ChestService chests;

    public ChestSelectorGui(GuildPlus plugin, ChestService chests) {
        this.plugin = plugin; this.chests = chests;
    }

    public void open(Player p) {
        FileConfiguration g = Configs.gui();
        String gid = plugin.hook().getGuildId(p);
        if (gid == null) { p.sendMessage(Chat.color(Configs.msg("no-guild"))); return; }

        String title = Chat.color(g.getString("chest.selector.title", "&8길드 창고 선택"));
        int size = Math.max(9, Math.min(54, g.getInt("chest.selector.size", 9)));

        Material mUnlocked = mat(g.getString("chest.selector.item-unlocked.material", "CHEST"));
        String nUnlocked = g.getString("chest.selector.item-unlocked.name", "&a{PAGE}번 창고");
        List<String> lUnlocked = g.getStringList("chest.selector.item-unlocked.lore");

        Material mLocked = mat(g.getString("chest.selector.item-locked.material", "BARREL"));
        String nLocked = g.getString("chest.selector.item-locked.name", "&c{PAGE}번 창고");
        List<String> lLocked = g.getStringList("chest.selector.item-locked.lore");

        Inventory inv = Bukkit.createInventory(p, size, title);
        for (int i = 1; i <= 9 && i <= size; i++) {
            boolean unlocked = chests.getOrCreate(gid).isUnlocked(i);
            Material mat = unlocked ? mUnlocked : mLocked;
            String name = Chat.color((unlocked ? nUnlocked : nLocked).replace("{PAGE}", String.valueOf(i)));
            List<String> loreSrc = unlocked ? lUnlocked : lLocked;
            List<String> lore = new ArrayList<>();
            if (loreSrc.isEmpty()) {
                if (unlocked) {
                    lore.add(Chat.color("&7좌클릭: 열기"));
                    lore.add(Chat.color("&7쉬프트+좌클릭: 행 확장"));
                } else {
                    double cost = Configs.cfg().getDouble("chest.cost.unlock-page", 0D);
                    lore.add(Chat.color("&7잠김"));
                    lore.add(Chat.color("&7우클릭: 해금 &f(" + cost + ")"));
                }
            } else {
                for (String line : loreSrc) {
                    double cost = Configs.cfg().getDouble("chest.cost.unlock-page", 0D);
                    lore.add(Chat.color(line.replace("{PAGE}", String.valueOf(i)).replace("{COST}", String.valueOf(cost))));
                }
            }
            ItemStack it = new ItemStack(mat);
            ItemMeta m = it.getItemMeta();
            m.setDisplayName(name);
            m.setLore(lore);
            it.setItemMeta(m);
            inv.setItem(i - 1, it);
        }
        p.openInventory(inv);
        play(p, g.getString("chest.selector.sound-open", ""));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        FileConfiguration g = Configs.gui();
        String title = Chat.color(g.getString("chest.selector.title", "&8길드 창고 선택"));
        if (!title.equals(e.getView().getTitle())) return;

        e.setCancelled(true);
        String gid = plugin.hook().getGuildId(p);
        if (gid == null) { p.closeInventory(); p.sendMessage(Chat.color(Configs.msg("no-guild"))); return; }

        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getInventory().getSize()) return;
        int page = slot + 1;
        boolean unlocked = chests.getOrCreate(gid).isUnlocked(page);

        if (e.getClick() == ClickType.RIGHT && !unlocked) {
            if (page > 1) {
                int prevRows = chests.getOrCreate(gid).getRows(page - 1);
                boolean prevUnlocked = chests.getOrCreate(gid).isUnlocked(page - 1);
                if (!prevUnlocked || prevRows < 6) {
                    p.sendMessage(org.haemin.guildPlus.util.Chat.color(
                            Configs.msg("chest-unlock-require-prev-full").replace("{prev}", String.valueOf(page-1))
                    ));
                    SoundUtil.play(p, Configs.gui().getString("chest.selector.sound-deny", ""));
                    return;
                }
            }
            double cost = Configs.cfg().getDouble("chest.cost.unlock-page", 0D);
            if (cost > 0 && GuildPlus.get().econ().isReady()) {
                if (!GuildPlus.get().econ().has(p, cost)) {
                    p.sendMessage(org.haemin.guildPlus.util.Chat.color(Configs.msg("chest-unlock-page-need-money").replace("{cost}", String.valueOf(cost))));
                    SoundUtil.play(p, Configs.gui().getString("chest.selector.sound-deny", ""));
                    return;
                }
                if (!GuildPlus.get().econ().withdraw(p, cost)) {
                    p.sendMessage(org.haemin.guildPlus.util.Chat.color(Configs.msg("withdraw-failed")));
                    SoundUtil.play(p, Configs.gui().getString("chest.selector.sound-deny", ""));
                    return;
                }
            }
            chests.getOrCreate(gid).unlockPage(page);
            p.sendMessage(org.haemin.guildPlus.util.Chat.color(Configs.msg("chest-unlocked-page").replace("{page}", String.valueOf(page))));
            SoundUtil.play(p, Configs.gui().getString("chest.selector.sound-click", ""));
            open(p);
            return;
        }

        if (e.getClick().isLeftClick() && unlocked) {
            if (!chests.canOpen(p, gid, page)) { p.sendMessage(Chat.color(Configs.msg("chest-locked"))); return; }
            play(p, g.getString("chest.selector.sound-click", ""));
            chests.openPage(p, gid, page);
        }

        if (e.getClick().isLeftClick() && e.isShiftClick() && unlocked) {
            int now = chests.getOrCreate(gid).getRows(page);
            if (now >= 6) { p.sendMessage(Chat.color("&e이미 최대(6줄)입니다.")); return; }
            double unit = Configs.cfg().getDouble("chest.cost.expand-rows", 0D);
            double cost = unit;
            if (cost > 0 && plugin.econ().isReady()) {
                if (!plugin.econ().has(p, cost)) { p.sendMessage(Chat.color(Configs.msg("chest-expand-need-money").replace("{cost}", String.valueOf(cost)))); return; }
                if (!plugin.econ().withdraw(p, cost)) { p.sendMessage(Chat.color(Configs.msg("withdraw-failed"))); return; }
            }
            chests.getOrCreate(gid).setRows(page, now + 1);
            p.sendMessage(Chat.color(Configs.msg("chest-expanded-rows").replace("{rows}", String.valueOf(now+1))));
            play(p, g.getString("chest.selector.sound-click", ""));
            open(p);
        }
    }

    private static Material mat(String s) {
        Material m = Material.matchMaterial(s);
        return m != null ? m : Material.BARRIER;
    }

    private static void play(Player p, String soundKey) {
        if (soundKey == null || soundKey.isEmpty()) return;
        try { p.playSound(p.getLocation(), Sound.valueOf(soundKey.toUpperCase()), 1f, 1f); } catch (Exception ignored) {}
    }
}
