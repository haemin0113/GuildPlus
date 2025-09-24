package org.haemin.guildPlus.buff;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.haemin.guildPlus.GuildPlus;
import org.haemin.guildPlus.hook.MmocoreHook;
import org.haemin.guildPlus.util.Configs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BuffService {

    public static class Node {
        public final String id;
        public final String name;
        public final Map<String, Double> stats;
        public final List<String> requires;
        public final long durationMillis;
        public final double cost;
        public Node(String id, String name, Map<String, Double> stats, List<String> requires, long durationMillis, double cost) {
            this.id = id;
            this.name = name == null ? id : name;
            this.stats = Collections.unmodifiableMap(new LinkedHashMap<>(stats));
            this.requires = requires == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(requires));
            this.durationMillis = durationMillis;
            this.cost = cost;
        }
    }

    public enum TempBlock { OK, LIMIT, REQUIREMENTS }

    private final GuildPlus plugin;
    private final MmocoreHook hook;

    private final Map<String, Node> infiniteNodes = new LinkedHashMap<>();
    private final Map<String, Node> tempNodes = new LinkedHashMap<>();

    private final Map<String, Set<String>> infiniteActive = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Long>> tempActive = new ConcurrentHashMap<>();

    private BukkitTask expiryTask;

    public BuffService(GuildPlus plugin, MmocoreHook hook) {
        this.plugin = plugin;
        this.hook = hook;
    }

    public void loadAndSchedule() {
        loadTrees();
        if (expiryTask != null) expiryTask.cancel();
        expiryTask = Bukkit.getScheduler().runTaskTimer(plugin, this::expireTick, 20L, 20L);
    }

    public void loadTrees() {
        infiniteNodes.clear();
        tempNodes.clear();

        FileConfiguration b = Configs.buffs();

        ConfigurationSection infParent =
                b.isConfigurationSection("infinite.nodes")
                        ? b.getConfigurationSection("infinite.nodes")
                        : b.getConfigurationSection("infinite");
        if (infParent != null) {
            for (String id : infParent.getKeys(false)) {
                ConfigurationSection c = infParent.getConfigurationSection(id);
                if (c == null) continue;
                String name = c.getString("name", id);
                Map<String, Double> stats = readStats(c.getConfigurationSection("stats"));
                List<String> req = c.getStringList("requires");
                Node n = new Node(id, name, stats, req, 0L, 0D);
                infiniteNodes.put(id, n);
            }
        }

        ConfigurationSection tmpParent =
                b.isConfigurationSection("temp.nodes")
                        ? b.getConfigurationSection("temp.nodes")
                        : b.getConfigurationSection("temp");
        if (tmpParent != null) {
            for (String id : tmpParent.getKeys(false)) {
                ConfigurationSection c = tmpParent.getConfigurationSection(id);
                if (c == null) continue;
                String name = c.getString("name", id);
                Map<String, Double> stats = readStats(c.getConfigurationSection("stats"));
                List<String> req = c.getStringList("requires");
                long dur = Math.max(0L, c.getLong("duration-seconds", 0L)) * 1000L;
                if (dur == 0L) dur = Math.max(0L, c.getLong("duration", 0L));
                double cost = c.getDouble("cost", 0D);
                Node n = new Node(id, name, stats, req, dur, cost);
                tempNodes.put(id, n);
            }
        }
    }

    private Map<String, Double> readStats(ConfigurationSection sec) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (sec != null) for (String k : sec.getKeys(false)) out.put(k, sec.getDouble(k, 0D));
        return out;
    }
    public Map<String, Double> totalsForPlayer(org.bukkit.entity.Player p) {
        String gid = hook.getGuildId(p);
        if (gid == null) return java.util.Collections.emptyMap();
        return new java.util.LinkedHashMap<>(aggregateForGuild(gid)); // 내부 합산 메서드 사용
    }




    private void expireTick() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Map<String, Long>> e : tempActive.entrySet()) {
            String gid = e.getKey();
            Map<String, Long> map = e.getValue();
            boolean changed = false;
            Iterator<Map.Entry<String, Long>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> en = it.next();
                if (en.getValue() <= now) { it.remove(); changed = true; }
            }
            if (changed) refreshGuild(gid);
        }
    }

    public Map<String, Node> getInfiniteNodes() { return infiniteNodes; }
    public Map<String, Node> getTempNodes() { return tempNodes; }

    public boolean isInfiniteActive(String guildId, String nodeId) {
        if (guildId == null) return false;
        return infiniteActive.getOrDefault(guildId, Collections.emptySet()).contains(nodeId);
    }

    public boolean canUnlockInfinite(String guildId, String nodeId) {
        Node n = infiniteNodes.get(nodeId);
        if (n == null) return false;
        if (isInfiniteActive(guildId, nodeId)) return false;
        return meetsRequirements(guildId, n.requires, true);
    }

    public void unlockInfinite(String guildId, String nodeId) {
        infiniteActive.computeIfAbsent(guildId, g -> ConcurrentHashMap.newKeySet()).add(nodeId);
        refreshGuild(guildId);
    }

    public boolean isTempActive(String guildId, String nodeId) {
        if (guildId == null) return false;
        Long end = tempActive.getOrDefault(guildId, Collections.emptyMap()).get(nodeId);
        return end != null && end > System.currentTimeMillis();
    }

    public long tempRemainMillis(String guildId, String nodeId) {
        if (guildId == null) return 0L;
        Long end = tempActive.getOrDefault(guildId, Collections.emptyMap()).get(nodeId);
        return end == null ? 0L : Math.max(0L, end - System.currentTimeMillis());
    }

    public TempBlock checkTempActivate(String guildId, String nodeId) {
        Node n = tempNodes.get(nodeId);
        if (n == null) return TempBlock.REQUIREMENTS;
        if (!meetsRequirements(guildId, n.requires, false)) return TempBlock.REQUIREMENTS;
        int max = Configs.cfg().getInt("temp.max-active-per-guild", 999);
        if (activeTempCount(guildId) >= max) return TempBlock.LIMIT;
        return TempBlock.OK;
    }

    public void activateTemp(String guildId, String nodeId) {
        Node n = tempNodes.get(nodeId);
        if (n == null) return;
        long end = System.currentTimeMillis() + n.durationMillis;
        tempActive.computeIfAbsent(guildId, g -> new ConcurrentHashMap<>()).put(nodeId, end);
        refreshGuild(guildId);
    }

    public void deactivateTemp(String guildId, String nodeId) {
        Map<String, Long> map = tempActive.get(guildId);
        if (map != null) {
            map.remove(nodeId);
            refreshGuild(guildId);
        }
    }

    public Map<String, Long> activeTempForGuild(String guildId) {
        if (guildId == null) return Collections.emptyMap();
        Map<String, Long> src = tempActive.getOrDefault(guildId, Collections.emptyMap());
        Map<String, Long> out = new LinkedHashMap<>();
        long now = System.currentTimeMillis();
        for (Map.Entry<String, Long> e : src.entrySet())
            if (e.getValue() != null && e.getValue() > now) out.put(e.getKey(), e.getValue());
        return out;
    }

    public Map<String, Long> activeTempForPlayer(Player p) {
        String gid = hook.getGuildId(p);
        if (gid == null) return Collections.emptyMap();
        return activeTempForGuild(gid);
    }

    public int activeTempCount(String guildId) {
        if (guildId == null) return 0;
        long now = System.currentTimeMillis();
        int c = 0;
        for (Long v : tempActive.getOrDefault(guildId, Collections.emptyMap()).values())
            if (v != null && v > now) c++;
        return c;
    }

    public void reload() {
        Configs.reloadBuffs();
        loadTrees();
    }

    public void refreshGuild(String guildId) {
        Map<String, Double> totals = aggregateForGuild(guildId);
        for (Player p : Bukkit.getOnlinePlayers()) {
            String gid = hook.getGuildId(p);
            if (gid != null && gid.equals(guildId)) plugin.mythic().apply(p, totals);
        }
    }

    public void refreshAll() {
        Set<String> seen = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            String gid = hook.getGuildId(p);
            if (gid == null || !seen.add(gid)) continue;
            refreshGuild(gid);
        }
    }

    public void reapplyFor(Player p) {
        String gid = hook.getGuildId(p);
        if (gid == null) return;
        Map<String, Double> totals = aggregateForGuild(gid);
        plugin.mythic().apply(p, totals);
    }

    private Map<String, Double> aggregateForGuild(String guildId) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (guildId == null) return out;

        Set<String> inf = infiniteActive.getOrDefault(guildId, Collections.emptySet());
        for (String id : inf) {
            Node n = infiniteNodes.get(id);
            if (n == null) continue;
            for (Map.Entry<String, Double> e : n.stats.entrySet()) out.merge(e.getKey(), e.getValue(), Double::sum);
        }

        long now = System.currentTimeMillis();
        Map<String, Long> tmp = tempActive.getOrDefault(guildId, Collections.emptyMap());
        for (Map.Entry<String, Long> e : tmp.entrySet()) {
            if (e.getValue() == null || e.getValue() <= now) continue;
            Node n = tempNodes.get(e.getKey());
            if (n == null) continue;
            for (Map.Entry<String, Double> s : n.stats.entrySet()) out.merge(s.getKey(), s.getValue(), Double::sum);
        }
        return out;
    }

    private boolean meetsRequirements(String guildId, List<String> req, boolean infiniteTree) {
        if (req == null || req.isEmpty()) return true;
        if (guildId == null) return false;

        for (String raw : req) {
            if (raw == null || raw.isEmpty()) continue;
            String s = raw.toLowerCase(Locale.ROOT).replace(" ", "");

            if (s.startsWith("members>=") || s.startsWith("guild-members>=") || s.startsWith("membercount>=")) {
                String num = s.substring(s.indexOf(">=") + 2);
                int need;
                try { need = Integer.parseInt(num); } catch (Exception e) { return false; }
                int have = hook.countMembers(guildId);
                if (have < need) return false;
                continue;
            }

            String id = raw;
            if (id.startsWith("infinite:")) id = id.substring("infinite:".length());
            if (id.startsWith("temp:")) id = id.substring("temp:".length());

            if (infiniteTree) {
                if (!isInfiniteActive(guildId, id)) return false;
            } else {
                if (!isTempActive(guildId, id) && !isInfiniteActive(guildId, id)) return false;
            }
        }
        return true;
    }
}
