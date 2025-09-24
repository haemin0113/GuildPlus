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

public final class BuffGui {
    private static GuildPlus plugin;
    private static boolean registered = false;

    public static void openInfinite(GuildPlus pl, Player p) {
        ensure(pl);
        String gid = pl.hook().getGuildId(p);
        if (gid == null) { p.sendMessage(Configs.msg("no-guild")); return; }
        Inventory inv = build(pl, p, "infinite", gid);
        p.openInventory(inv);
        SoundUtil.play(p, "gui.buff.open");
    }

    public static void openTemp(GuildPlus pl, Player p) {
        ensure(pl);
        String gid = pl.hook().getGuildId(p);
        if (gid == null) { p.sendMessage(Configs.msg("no-guild")); return; }
        Inventory inv = build(pl, p, "temp", gid);
        p.openInventory(inv);
        SoundUtil.play(p, "gui.buff.open");
    }

    private static void ensure(GuildPlus pl) {
        if (plugin == null) plugin = pl;
        if (!registered) {
            Bukkit.getPluginManager().registerEvents(new Clicks(), plugin);
            registered = true;
        }
    }

    private static Inventory build(GuildPlus pl, Player p, String kind, String gid) {
        ConfigurationSection root = Configs.gui().getConfigurationSection(kind);
        String title = Chat.color(root.getString("title", "&8Guild Buffs"));
        int rows = Math.max(1, Math.min(6, root.getInt("rows", 6)));
        int size = rows * 9;
        Holder holder = new Holder(kind);
        Inventory inv = Bukkit.createInventory(holder, size, title);
        Map<Integer, String> placed = new HashMap<>();
        ConfigurationSection layout = root.getConfigurationSection("layout");
        ConfigurationSection def = root.getConfigurationSection("default");
        ConfigurationSection items = root.getConfigurationSection("items");
        Map<String, ConfigurationSection> nodes = loadBuffNodes(kind);
        if (layout != null) {
            for (String id : layout.getKeys(false)) {
                int slot = layout.getInt(id);
                if (slot < 0 || slot >= size) continue;
                if (!nodes.containsKey(id)) continue;
                inv.setItem(slot, buildItem(pl, p, kind, gid, id, nodes.get(id), def, items == null ? null : items.getConfigurationSection(id)));
                placed.put(slot, id);
                holder.map.put(slot, id);
            }
        }
        int cursor = 0;
        for (Map.Entry<String, ConfigurationSection> e : nodes.entrySet()) {
            String id = e.getKey();
            if (layout != null && layout.getKeys(false).contains(id)) continue;
            while (cursor < size && placed.containsKey(cursor)) cursor++;
            if (cursor >= size) break;
            inv.setItem(cursor, buildItem(pl, p, kind, gid, id, e.getValue(), def, items == null ? null : items.getConfigurationSection(id)));
            holder.map.put(cursor, id);
            cursor++;
        }
        return inv;
    }

    private static Map<String, ConfigurationSection> loadBuffNodes(String kind) {
        Map<String, ConfigurationSection> out = new LinkedHashMap<>();
        ConfigurationSection parent = null;
        if (Configs.buffs().isConfigurationSection(kind + ".nodes")) parent = Configs.buffs().getConfigurationSection(kind + ".nodes");
        else if (Configs.buffs().isConfigurationSection(kind)) parent = Configs.buffs().getConfigurationSection(kind);
        if (parent == null) return out;
        for (String id : parent.getKeys(false)) {
            ConfigurationSection c = parent.getConfigurationSection(id);
            if (c != null) out.put(id, c);
        }
        return out;
    }

    private static ItemStack buildItem(GuildPlus pl, Player p, String kind, String gid, String id, ConfigurationSection node, ConfigurationSection def, ConfigurationSection override) {
        String nameRaw = pick(def, override, "name");
        String nameLocked = pick(def, override, "name-locked");
        String nameUnlocked = pick(def, override, "name-unlocked");
        List<String> loreBase = list(def, override, "lore");
        List<String> loreLocked = list(def, override, "lore-locked");
        List<String> loreUnlocked = list(def, override, "lore-unlocked");
        String mat = pick(def, override, "material");
        int cmd = num(def, override, "custom-model-data");
        boolean infinite = kind.equalsIgnoreCase("infinite");
        boolean activeInf = infinite && pl.buffs().isInfiniteActive(gid, id);
        boolean activeTmp = !infinite && pl.buffs().isTempActive(gid, id);
        boolean unlocked = infinite ? activeInf : activeTmp;
        String statsStr = formatStats(node.getConfigurationSection("stats"));
        long remain = !infinite ? pl.buffs().tempRemainMillis(gid, id) : 0L;
        String duration = !infinite ? formatDuration(node) : "";
        String costStr = !infinite ? formatCost(node) : "";
        State state = computeState(pl, p, kind, gid, id, node);
        String stateStr = state.display;
        String displayName = nameRaw;
        if (state.locked) {
            if (nameLocked != null && !nameLocked.isEmpty()) displayName = nameLocked;
        } else {
            if (nameUnlocked != null && !nameUnlocked.isEmpty()) displayName = nameUnlocked;
        }
        Map<String,String> vars = new HashMap<>();
        vars.put("name", node.getString("name", id));
        vars.put("id", id);
        vars.put("stats", statsStr);
        vars.put("state", stateStr);
        vars.put("duration", !infinite ? millisToKorean(remain > 0 ? remain : node.getLong("duration-seconds", node.getLong("duration", 0)) * 1000L) : "");
        vars.put("cost", costStr);
        ItemStack is = new ItemStack(asMat(mat));
        ItemMeta im = is.getItemMeta();
        if (cmd > 0) im.setCustomModelData(cmd);
        im.setDisplayName(Chat.color(apply(displayName, vars)));
        List<String> lore = new ArrayList<>();
        for (String l : loreBase) lore.add(Chat.color(apply(l, vars)));
        if (state.locked) for (String l : loreLocked) lore.add(Chat.color(apply(l, vars)));
        else for (String l : loreUnlocked) lore.add(Chat.color(apply(l, vars)));
        List<String> reqLines = requireLines(pl, gid, node);
        if (!reqLines.isEmpty()) {
            lore.addAll(reqLines);
        }
        im.setLore(lore);
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        is.setItemMeta(im);
        return is;
    }

    private static Material asMat(String s) {
        if (s == null) return Material.PAPER;
        try { return Material.valueOf(s.toUpperCase(Locale.ROOT)); } catch (Throwable ignored) { return Material.PAPER; }
    }

    private static String pick(ConfigurationSection def, ConfigurationSection over, String key) {
        if (over != null && over.getString(key) != null) return over.getString(key);
        return def == null ? null : def.getString(key);
    }

    private static int num(ConfigurationSection def, ConfigurationSection over, String key) {
        if (over != null && over.isInt(key)) return over.getInt(key);
        return def != null ? def.getInt(key, 0) : 0;
    }

    private static List<String> list(ConfigurationSection def, ConfigurationSection over, String key) {
        List<String> a = over != null && over.isList(key) ? over.getStringList(key) : Collections.emptyList();
        List<String> b = def != null && def.isList(key) ? def.getStringList(key) : Collections.emptyList();
        if (!a.isEmpty()) return a;
        return b;
    }

    private static String apply(String s, Map<String,String> vars) {
        if (s == null) return "";
        String out = s;
        for (Map.Entry<String,String> e : vars.entrySet()) out = out.replace("{"+e.getKey()+"}", e.getValue());
        return out;
    }

    private static String formatStats(ConfigurationSection stats) {
        if (stats == null) return "-";
        List<String> parts = new ArrayList<>();
        for (String k : stats.getKeys(false)) {
            double v = stats.getDouble(k, 0D);
            String sign = v >= 0 ? "+" : "";
            parts.add(k.toUpperCase(Locale.ROOT) + " " + sign + (v % 1 == 0 ? String.valueOf((long)v) : String.valueOf(v)));
        }
        return String.join(", ", parts);
    }

    private static List<String> requireLines(GuildPlus pl, String gid, ConfigurationSection node) {
        List<String> out = new ArrayList<>();
        int minMembers = node.getInt("min-members", 0);
        if (minMembers > 0) {
            int cur = pl.hook().countMembers(gid);
            String c = cur >= minMembers ? "&a" : "&c";
            out.add(Chat.color("&8• &7최소 인원: " + c + cur + "&7/&f" + minMembers));
        }
        List<String> req = node.getStringList("requires");
        if (req != null && !req.isEmpty()) {
            for (String dep : req) {
                String nm = resolveBuffName(dep);
                boolean ok = pl.buffs().isInfiniteActive(gid, dep) || pl.buffs().isTempActive(gid, dep);
                String c = ok ? "&a" : "&c";
                out.add(Chat.color("&8• &7필요: " + c + nm));
            }
        }
        return out;
    }

    private static String resolveBuffName(String id) {
        ConfigurationSection s = null;
        if (Configs.buffs().isConfigurationSection("infinite.nodes."+id)) s = Configs.buffs().getConfigurationSection("infinite.nodes."+id);
        else if (Configs.buffs().isConfigurationSection("infinite."+id)) s = Configs.buffs().getConfigurationSection("infinite."+id);
        if (s == null) {
            if (Configs.buffs().isConfigurationSection("temp.nodes."+id)) s = Configs.buffs().getConfigurationSection("temp.nodes."+id);
            else if (Configs.buffs().isConfigurationSection("temp."+id)) s = Configs.buffs().getConfigurationSection("temp."+id);
        }
        if (s == null) return id;
        return s.getString("name", id);
    }

    private static State computeState(GuildPlus pl, Player p, String kind, String gid, String id, ConfigurationSection node) {
        boolean infinite = kind.equalsIgnoreCase("infinite");
        if (infinite) {
            if (pl.buffs().isInfiniteActive(gid, id)) return new State(false, Configs.msg("buff-state-unlocked"));
            int minMembers = node.getInt("min-members", 0);
            if (minMembers > 0 && pl.hook().countMembers(gid) < minMembers) return new State(true, Configs.msg("buff-state-locked-require"));
            List<String> req = node.getStringList("requires");
            if (req != null) {
                for (String dep : req) if (!pl.buffs().isInfiniteActive(gid, dep)) return new State(true, Configs.msg("buff-state-locked-require"));
            }
            return new State(true, Configs.msg("buff-state-locked"));
        } else {
            if (pl.buffs().isTempActive(gid, id)) {
                long remain = pl.buffs().tempRemainMillis(gid, id);
                return new State(false, Configs.msg("buff-state-temp-active", Collections.singletonMap("time", millisToKorean(remain))));
            }
            int maxAct = Configs.cfg().getInt("temp.max-active", 3);
            if (pl.buffs().activeTempCount(gid) >= maxAct) return new State(true, Configs.msg("buff-state-temp-max"));
            int minMembers = node.getInt("min-members", 0);
            if (minMembers > 0 && pl.hook().countMembers(gid) < minMembers) return new State(true, Configs.msg("buff-state-locked-require"));
            List<String> req = node.getStringList("requires");
            if (req != null) for (String dep : req) if (!pl.buffs().isInfiniteActive(gid, dep)) return new State(true, Configs.msg("buff-state-locked-require"));
            return new State(true, Configs.msg("buff-state-temp-ready"));
        }
    }

    private static String formatDuration(ConfigurationSection node) {
        long ms = Math.max(0L, node.getLong("duration-seconds", node.getLong("duration", 0L))) * 1000L;
        return millisToKorean(ms);
    }

    private static String formatCost(ConfigurationSection node) {
        double cost = node.getDouble("cost", 0D);
        if (cost <= 0) return "무료";
        return String.format(Locale.ROOT, "%.0f", cost);
    }

    private static String millisToKorean(long ms) {
        if (ms <= 0) return "0초";
        long s = ms / 1000L;
        long h = s / 3600L;
        long m = (s % 3600L) / 60L;
        long ss = s % 60L;
        StringBuilder sb = new StringBuilder();
        if (h > 0) sb.append(h).append("시간 ");
        if (m > 0) sb.append(m).append("분 ");
        if (ss > 0 || sb.length() == 0) sb.append(ss).append("초");
        return sb.toString().trim();
    }

    private static final class Holder implements InventoryHolder {
        final String kind;
        final Map<Integer,String> map = new HashMap<>();
        Holder(String k) { this.kind = k; }
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
            String id = h.map.getOrDefault(e.getRawSlot(), null);
            if (id == null) return;
            Player p = (Player) he;
            String gid = plugin.hook().getGuildId(p);
            if (gid == null) { p.closeInventory(); p.sendMessage(Configs.msg("no-guild")); SoundUtil.play(p, "gui.buff.fail"); return; }
            ClickType ct = e.getClick();
            boolean infinite = h.kind.equalsIgnoreCase("infinite");
            if (infinite) {
                boolean ok = plugin.buffs().unlockInfinite(p, id);
                if (ok) {
                    SoundUtil.play(p, "gui.buff.click");
                    SoundUtil.play(p, "buff.unlock");
                    Bukkit.getScheduler().runTask(plugin, () -> openInfinite(plugin, p));
                } else {
                    SoundUtil.play(p, "gui.buff.fail");
                }
                return;
            }
            if (ct.isRightClick()) {
                boolean ok = plugin.buffs().deactivateTemp(p, id);
                if (ok) {
                    SoundUtil.play(p, "gui.buff.click");
                    SoundUtil.play(p, "buff.deactivate");
                    Bukkit.getScheduler().runTask(plugin, () -> openTemp(plugin, p));
                } else {
                    SoundUtil.play(p, "gui.buff.fail");
                }
            } else {
                boolean ok = plugin.buffs().activateTemp(p, id);
                if (ok) {
                    SoundUtil.play(p, "gui.buff.click");
                    SoundUtil.play(p, "buff.activate");
                    Bukkit.getScheduler().runTask(plugin, () -> openTemp(plugin, p));
                } else {
                    SoundUtil.play(p, "gui.buff.fail");
                }
            }
        }
    }

    private static final class State {
        final boolean locked;
        final String display;
        State(boolean l, String d) { this.locked = l; this.display = d; }
    }
}