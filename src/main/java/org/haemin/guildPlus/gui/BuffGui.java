package org.haemin.guildPlus.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
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
import org.haemin.guildPlus.buff.BuffService;
import org.haemin.guildPlus.util.Chat;
import org.haemin.guildPlus.util.Configs;
import org.haemin.guildPlus.util.SoundUtil;

import java.util.*;

public class BuffGui implements Listener {
    private final GuildPlus plugin;
    public BuffGui(GuildPlus plugin) { this.plugin = plugin; }

    public static void openInfinite(GuildPlus plugin, Player p) { open(plugin, p, true); }
    public static void openTemp(GuildPlus plugin, Player p) { open(plugin, p, false); }

    private static void open(GuildPlus plugin, Player p, boolean infinite) {
        FileConfiguration g = Configs.gui();
        String base = infinite ? "infinite" : "temp";
        String title = Chat.color(g.getString(base + ".title", infinite ? "&8길드 버프 &7(영구)" : "&8길드 버프 &7(기간제)"));
        int rows = Math.max(1, Math.min(6, g.getInt(base + ".rows", 6)));
        int size = rows * 9;

        Map<String, Integer> layout = new LinkedHashMap<>();
        if (g.isConfigurationSection(base + ".layout")) {
            for (String id : g.getConfigurationSection(base + ".layout").getKeys(false)) {
                layout.put(id, g.getInt(base + ".layout." + id));
            }
        }

        List<BuffService.Node> nodes = new ArrayList<>(infinite
                ? plugin.buffs().getInfiniteNodes().values()
                : plugin.buffs().getTempNodes().values());

        Inventory inv = Bukkit.createInventory(p, size, title);
        boolean[] used = new boolean[size];
        Map<Integer, BuffService.Node> slotMap = new HashMap<>();

        for (BuffService.Node n : nodes) {
            Integer fixed = layout.get(n.id);
            if (fixed != null && fixed >= 0 && fixed < size && !used[fixed]) {
                inv.setItem(fixed, buildItem(plugin, n, infinite, p));
                used[fixed] = true;
                slotMap.put(fixed, n);
            }
        }
        int cursor = 0;
        for (BuffService.Node n : nodes) {
            if (slotMap.containsValue(n)) continue;
            while (cursor < size && used[cursor]) cursor++;
            if (cursor >= size) break;
            inv.setItem(cursor, buildItem(plugin, n, infinite, p));
            used[cursor] = true;
            slotMap.put(cursor, n);
        }

        p.setMetadata("gp_buff_slots_" + (infinite ? "inf" : "tmp"), new org.bukkit.metadata.FixedMetadataValue(plugin, slotMap));
        p.openInventory(inv);
        SoundUtil.play(p, g.getString(base + ".sound-open", ""));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        FileConfiguration g = Configs.gui();
        String ti = Chat.color(g.getString("infinite.title", "&8길드 버프 &7(영구)"));
        String tt = Chat.color(g.getString("temp.title", "&8길드 버프 &7(기간제)"));
        String title = e.getView().getTitle();
        boolean inf = ti.equals(title);
        boolean tmp = tt.equals(title);
        if (!inf && !tmp) return;

        e.setCancelled(true);
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getInventory().getSize()) return;

        String key = "gp_buff_slots_" + (inf ? "inf" : "tmp");
        if (!p.hasMetadata(key)) return;
        @SuppressWarnings("unchecked")
        Map<Integer, BuffService.Node> map = (Map<Integer, BuffService.Node>) p.getMetadata(key).get(0).value();
        BuffService.Node n = map.get(slot);
        if (n == null) return;

        String gid = plugin.hook().getGuildId(p);
        if (gid == null) { p.closeInventory(); p.sendMessage(Chat.color(Configs.msg("no-guild"))); return; }

        if (inf) {
            if (plugin.buffs().isInfiniteActive(gid, n.id)) return;
            if (!plugin.buffs().canUnlockInfinite(gid, n.id)) { p.sendMessage(Chat.color(Configs.msg("require-fail"))); return; }
            plugin.buffs().unlockInfinite(gid, n.id);
            SoundUtil.play(p, g.getString("infinite.sound-click", ""));
            p.sendMessage(Chat.color(Configs.msg("infinite-unlocked").replace("{name}", n.name)));
            openInfinite(plugin, p);
            return;
        }

        if (e.getClick() == ClickType.RIGHT) {
            if (!plugin.buffs().isTempActive(gid, n.id)) return;
            plugin.buffs().deactivateTemp(gid, n.id);
            SoundUtil.play(p, g.getString("temp.sound-click", ""));
            p.sendMessage(Chat.color(Configs.msg("temp-removed")));
            openTemp(plugin, p);
            return;
        }

        BuffService.TempBlock chk = plugin.buffs().checkTempActivate(gid, n.id);
        if (chk == BuffService.TempBlock.LIMIT) {
            p.sendMessage(Chat.color(Configs.msg("temp-limit-reached")
                    .replace("{max}", String.valueOf(Configs.cfg().getInt("temp.max-active-per-guild", 99)))));
            return;
        }
        if (chk == BuffService.TempBlock.REQUIREMENTS) {
            p.sendMessage(Chat.color(Configs.msg("require-fail")));
            return;
        }
        double cost = n.cost;
        if (cost > 0 && plugin.econ().isReady()) {
            if (!plugin.econ().has(p, cost)) { p.sendMessage(Chat.color(Configs.msg("need-money").replace("{cost}", String.valueOf(cost)))); return; }
            if (!plugin.econ().withdraw(p, cost)) { p.sendMessage(Chat.color(Configs.msg("withdraw-failed"))); return; }
        }
        plugin.buffs().activateTemp(gid, n.id);
        SoundUtil.play(p, g.getString("temp.sound-click", ""));
        p.sendMessage(Chat.color(Configs.msg("temp-activated").replace("{name}", n.name)));
        openTemp(plugin, p);
    }

    private static ItemStack buildItem(GuildPlus plugin, BuffService.Node n, boolean infinite, Player p) {
        boolean unlocked = infinite ? plugin.buffs().isInfiniteActive(plugin.hook().getGuildId(p), n.id)
                : plugin.buffs().isTempActive(plugin.hook().getGuildId(p), n.id);
        String state = infinite ? (unlocked ? "&a해금됨" : "&c미해금")
                : (unlocked ? "&a활성" : "&c비활성");

        FileConfiguration g = Configs.gui();
        String base = infinite ? "infinite" : "temp";
        ConfigurationSection def = g.getConfigurationSection(base + ".default");
        ConfigurationSection items = g.getConfigurationSection(base + ".items");
        String idPath = items != null && items.isConfigurationSection(n.id) ? base + ".items." + n.id : null;

        Material mat = mat(firstNonNull(
                g.getString((idPath != null ? idPath + "." : "") + "material-" + (unlocked ? "unlocked" : "locked")),
                g.getString((idPath != null ? idPath + "." : "") + "material"),
                def != null ? def.getString("material-" + (unlocked ? "unlocked" : "locked")) : null,
                def != null ? def.getString("material") : null,
                infinite ? "NETHER_STAR" : "CLOCK"
        ));

        Integer cmd = firstNonNull(
                intObj(g.getString((idPath != null ? idPath + "." : "") + "custom-model-data-" + (unlocked ? "unlocked" : "locked"))),
                intObj(g.getString((idPath != null ? idPath + "." : "") + "custom-model-data")),
                def != null ? intObj(def.getString("custom-model-data-" + (unlocked ? "unlocked" : "locked"))) : null,
                def != null ? intObj(def.getString("custom-model-data")) : null
        );

        String name = firstNonNull(
                g.getString((idPath != null ? idPath + "." : "") + "name-" + (unlocked ? "unlocked" : "locked")),
                g.getString((idPath != null ? idPath + "." : "") + "name"),
                def != null ? def.getString("name-" + (unlocked ? "unlocked" : "locked")) : null,
                def != null ? def.getString("name") : null,
                "{name}"
        );

        List<String> lore = firstListNonEmpty(
                g.getStringList((idPath != null ? idPath + "." : "") + "lore-" + (unlocked ? "unlocked" : "locked")),
                g.getStringList((idPath != null ? idPath + "." : "") + "lore"),
                def != null ? def.getStringList("lore-" + (unlocked ? "unlocked" : "locked")) : Collections.emptyList(),
                def != null ? def.getStringList("lore") : Collections.emptyList()
        );

        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.setDisplayName(Chat.color(applyPlaceholders(name, n, state)));
        List<String> outLore = new ArrayList<>();
        for (String line : lore) outLore.add(Chat.color(applyPlaceholders(line, n, state)));
        String req = requiresText(plugin, n);
        if (!req.isEmpty()) {
            outLore.add(Chat.color("&8────────────────"));
            outLore.add(Chat.color("&7요구: &f" + req));
        }
        m.setLore(outLore);
        if (cmd != null) {
            try { m.setCustomModelData(cmd); } catch (Throwable ignored) {}
        }
        it.setItemMeta(m);
        return it;
    }

    private static String requiresText(GuildPlus plugin, BuffService.Node n) {
        if (n.requires == null || n.requires.isEmpty()) return "";
        List<String> parts = new ArrayList<>();
        for (String raw : n.requires) {
            if (raw == null || raw.isEmpty()) continue;
            String s = raw.toLowerCase(Locale.ROOT).replace(" ", "");
            if (s.startsWith("members>=") || s.startsWith("guild-members>=") || s.startsWith("membercount>=")) {
                String num = s.substring(s.indexOf(">=") + 2);
                parts.add("길드 최소 인원 " + num + "명");
                continue;
            }
            String id = s;
            if (id.startsWith("infinite:")) id = id.substring("infinite:".length());
            if (id.startsWith("temp:")) id = id.substring("temp:".length());
            String name = resolveBuffName(plugin, id);
            if (!name.equals(id)) parts.add("버프 " + name);
            else parts.add(raw);
        }
        return String.join("&7, &f", parts);
    }

    private static String resolveBuffName(GuildPlus plugin, String id) {
        BuffService.Node n = plugin.buffs().getInfiniteNodes().get(id);
        if (n != null) return n.name;
        n = plugin.buffs().getTempNodes().get(id);
        if (n != null) return n.name;
        return id;
    }

    private static String applyPlaceholders(String line, BuffService.Node n, String stateText) {
        String remain = formatRemain(n.durationMillis);
        String stats = statLine(n.stats);
        return line.replace("{name}", n.name)
                .replace("{stats}", stats)
                .replace("{state}", stateText)
                .replace("{duration}", remain)
                .replace("{cost}", n.cost <= 0 ? "0" : String.valueOf(n.cost));
    }

    private static String statLine(Map<String, Double> m) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, Double> e : m.entrySet()) {
            double v = e.getValue() == null ? 0D : e.getValue();
            String vs = (Math.floor(v) == v) ? String.valueOf((long) v) : String.valueOf(v);
            parts.add(e.getKey() + "+" + vs);
        }
        return String.join(", ", parts);
    }

    private static String formatRemain(long ms) {
        long s = ms / 1000;
        long h = s / 3600; long m = (s % 3600) / 60; long sec = s % 60;
        if (h > 0) return h+"시간 "+m+"분 "+sec+"초";
        if (m > 0) return m+"분 "+sec+"초";
        return sec+"초";
    }

    private static void play(Player p, String soundKey) {
        if (soundKey == null || soundKey.isEmpty()) return;
        try { p.playSound(p.getLocation(), Sound.valueOf(soundKey.toUpperCase()), 1f, 1f); } catch (Exception ignored) {}
    }

    private static Material mat(String s) {
        Material m = s == null ? null : Material.matchMaterial(s);
        return m != null ? m : Material.BARRIER;
    }

    private static <T> T firstNonNull(T... arr) { for (T t : arr) if (t != null) return t; return null; }
    private static Integer intObj(String s) { if (s == null) return null; try { return Integer.parseInt(s); } catch (Exception e) { return null; } }
    @SafeVarargs private static List<String> firstListNonEmpty(List<String>... lists) { for (List<String> l : lists) if (l != null && !l.isEmpty()) return l; return Collections.emptyList(); }
}
