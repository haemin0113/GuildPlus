package org.haemin.guildPlus.view;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.haemin.guildPlus.GuildPlus;
import org.haemin.guildPlus.util.Configs;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DisplayService implements org.bukkit.event.Listener {
    private final GuildPlus plugin;
    private BukkitTask task;
    private final Map<UUID, org.bukkit.scoreboard.Scoreboard> boards = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bars = new ConcurrentHashMap<>();

    public DisplayService(GuildPlus plugin) { this.plugin = plugin; }

    public void start() {
        stop();
        int period = Math.max(1, Configs.cfg().getInt("display.refresh-ticks", 20));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, period);
    }

    public void stop() {
        if (task != null) task.cancel();
        task = null;
        for (Player p : Bukkit.getOnlinePlayers()) clearAll(p);
        boards.clear();
        bars.values().forEach(BossBar::removeAll);
        bars.clear();
    }

    private void tick() {
        for (Player p : Bukkit.getOnlinePlayers()) renderFor(p);
    }

    public void refreshGuild(String guildId) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            String gid = plugin.hook().getGuildId(p);
            if (guildId == null || (gid != null && gid.equals(guildId))) renderFor(p);
        }
    }

    public void refreshAll() { refreshGuild(null); }

    public void refreshFor(Player p) { renderFor(p); }

    private void renderFor(Player p) {
        String mode = String.valueOf(Configs.cfg().getString("display.mode", "scoreboard")).toLowerCase(Locale.ROOT);
        String gid = plugin.hook().getGuildId(p);
        Map<String, Long> active = getActiveTemp(p, gid);
        if (active.isEmpty()) { clearAll(p); return; }
        List<String> lines = buildLines(active);
        if (mode.equals("actionbar")) showActionbar(p, lines);
        else if (mode.equals("bossbar")) showBossbar(p, lines);
        else showScoreboard(p, lines);
    }

    private Map<String, Long> getActiveTemp(Player p, String gid) {
        if (gid == null) return Collections.emptyMap();
        try {
            Map<String, Long> m = plugin.buffs().activeTempForPlayer(p);
            return m == null ? Collections.emptyMap() : m;
        } catch (Throwable ignored) {}
        return Collections.emptyMap();
    }

    private List<String> buildLines(Map<String, Long> active) {
        List<Map.Entry<String, Long>> list = new ArrayList<>(active.entrySet());
        list.sort(Comparator.comparingLong(Map.Entry::getValue));
        List<String> out = new ArrayList<>();
        int idx = 1;
        for (Map.Entry<String, Long> e : list) {
            long remain = Math.max(0, e.getValue() - System.currentTimeMillis());
            String name = Optional.ofNullable(plugin.buffs().getTempNodes().get(e.getKey())).map(n -> n.name).orElse(e.getKey());
            String line = Configs.msgs().getString("display-temp-line", "{index}. {name} {remain}")
                    .replace("{index}", String.valueOf(idx++))
                    .replace("{name}", name)
                    .replace("{remain}", format(remain));
            out.add(org.haemin.guildPlus.util.Chat.color(line));
        }
        return out;
    }

    private void showScoreboard(Player p, List<String> lines) {
        org.bukkit.scoreboard.Scoreboard sb = boards.computeIfAbsent(p.getUniqueId(),
                u -> Bukkit.getScoreboardManager().getNewScoreboard());
        String title = org.haemin.guildPlus.util.Chat.color(Configs.cfg().getString("display.scoreboard.title", "&b활성 임시 버프"));
        org.bukkit.scoreboard.Objective obj = sb.getObjective("gp_temp");
        if (obj == null) obj = sb.registerNewObjective("gp_temp", "dummy", title);
        obj.setDisplayName(title);
        obj.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);
        for (String e : new ArrayList<>(sb.getEntries())) sb.resetScores(e);
        int score = lines.size();
        int z = 0x200B;
        int i = 0;
        for (String line : lines) {
            String unique = line + (char)(z + (i++));
            obj.getScore(unique).setScore(score--);
        }
        p.setScoreboard(sb);
    }

    private void showActionbar(Player p, List<String> lines) {
        String joined = String.join("  ", lines);
        p.sendActionBar(Component.text(joined));
        BossBar bar = bars.remove(p.getUniqueId());
        if (bar != null) bar.removeAll();
    }

    private void showBossbar(Player p, List<String> lines) {
        BossBar bar = bars.computeIfAbsent(p.getUniqueId(), u -> {
            BarColor color = BarColor.valueOf(Configs.cfg().getString("display.bossbar.color", "BLUE").toUpperCase(Locale.ROOT));
            BarStyle style = BarStyle.valueOf(Configs.cfg().getString("display.bossbar.style", "SOLID").toUpperCase(Locale.ROOT));
            BossBar b = Bukkit.createBossBar("", color, style);
            b.addPlayer(p);
            return b;
        });
        String title = String.join("  ", lines);
        bar.setTitle(org.haemin.guildPlus.util.Chat.color(title));
        bar.setProgress(1.0);
    }

    private void clearAll(Player p) {
        if (boards.containsKey(p.getUniqueId()))
            p.setScoreboard(Bukkit.getScoreboardManager().getNewScoreboard());
        BossBar bar = bars.remove(p.getUniqueId());
        if (bar != null) bar.removeAll();
    }

    private String format(long ms) {
        long s = ms / 1000;
        long h = s / 3600; long m = (s % 3600) / 60; long sec = s % 60;
        if (h > 0) return h+"시간 "+m+"분 "+sec+"초";
        if (m > 0) return m+"분 "+sec+"초";
        return sec+"초";
    }
}
