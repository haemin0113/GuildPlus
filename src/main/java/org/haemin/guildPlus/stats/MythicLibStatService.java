package org.haemin.guildPlus.stats;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.haemin.guildPlus.GuildPlus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;

public class MythicLibStatService {
    public static final String SOURCE_PREFIX = "guildplus";
    private final GuildPlus plugin;

    private Class<?> cMMOPlayerData;
    private Method mGetData;
    private int getMode; // 0: Player, 1: UUID, 2: OfflinePlayer
    private Method mGetStatMap;

    private Class<?> cStatMap;
    private Method mGetInstanceStr;
    private Method mGetInstanceStrBool;
    private Method mUpdateStats; // updateStats / update / recalc 등 가변

    private Class<?> cStatInstance;
    private Method mInstRemove; // remove / removeModifier
    private Method mInstAddModifier; // addModifier(StatModifier)
    private Method mInstGetTotal; // optional

    private Class<?> cStatModifier;
    private Constructor<?> ctorStatModifier;

    private Class<?> cModifierType;
    private Object modifierTypeFlat;

    private boolean ready;

    private final Map<UUID, Map<String, Set<String>>> applied = new HashMap<>();

    public MythicLibStatService(GuildPlus plugin) {
        this.plugin = plugin;
        try {
            cMMOPlayerData = Class.forName("io.lumine.mythic.lib.api.player.MMOPlayerData");
            try { mGetData = cMMOPlayerData.getMethod("get", Player.class); getMode = 0; }
            catch (NoSuchMethodException ex1) {
                try { mGetData = cMMOPlayerData.getMethod("get", java.util.UUID.class); getMode = 1; }
                catch (NoSuchMethodException ex2) { mGetData = cMMOPlayerData.getMethod("get", OfflinePlayer.class); getMode = 2; }
            }
            mGetStatMap = cMMOPlayerData.getMethod("getStatMap");

            cStatMap = Class.forName("io.lumine.mythic.lib.api.stat.StatMap");
            try { mGetInstanceStr = cStatMap.getMethod("getInstance", String.class); } catch (Throwable ignored) {}
            try { mGetInstanceStrBool = cStatMap.getMethod("getInstance", String.class, boolean.class); } catch (Throwable ignored) {}
            mUpdateStats = findZeroArg(cStatMap, "updateStats", "update", "recalculateStats", "refreshStats", "recalculate", "cache", "apply");

            cStatInstance = Class.forName("io.lumine.mythic.lib.api.stat.StatInstance");
            try { mInstRemove = cStatInstance.getMethod("remove", String.class); }
            catch (NoSuchMethodException e) { mInstRemove = cStatInstance.getMethod("removeModifier", String.class); }
            mInstAddModifier = cStatInstance.getMethod("addModifier", Class.forName("io.lumine.mythic.lib.api.stat.modifier.StatModifier"));
            try { mInstGetTotal = cStatInstance.getMethod("getTotal"); } catch (Throwable ignored) {}

            cStatModifier = Class.forName("io.lumine.mythic.lib.api.stat.modifier.StatModifier");
            cModifierType = Class.forName("io.lumine.mythic.lib.player.modifier.ModifierType");
            modifierTypeFlat = Enum.valueOf((Class<Enum>) cModifierType.asSubclass(Enum.class), "FLAT");
            ctorStatModifier = cStatModifier.getConstructor(String.class, String.class, double.class, cModifierType);

            ready = mInstRemove != null && (mGetInstanceStr != null || mGetInstanceStrBool != null);
            dbg("MythicLib reflection ready=" + ready + " getMode=" + getMode + " remove=" + (mInstRemove==null?"?":mInstRemove.getName()) + " update=" + (mUpdateStats==null?"(none)":mUpdateStats.getName()));
        } catch (Throwable t) {
            ready = false;
            dbg("MythicLib reflection init failed: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    public boolean isReady() { return ready; }

    // GuildPlus에서 흔히 쓰는 단축형
    public void apply(Player player, Map<String, Double> stats) {
        apply(player, SOURCE_PREFIX, stats);
    }

    public void apply(Player player, String sourceKey, Map<String, Double> stats) {
        if (!ready || player == null || stats == null || stats.isEmpty()) return;
        try {
            Object data = resolveData(player);
            Object map = data == null ? null : mGetStatMap.invoke(data);
            dbg("apply player=" + player.getName() + " source=" + sourceKey + " stats=" + stats + " map=" + (map!=null));
            if (map == null) return;

            for (Map.Entry<String, Double> e : stats.entrySet()) {
                String statId = normalize(e.getKey());
                double val = e.getValue() == null ? 0D : e.getValue();
                Object inst = getInstance(map, statId);
                if (inst == null) { dbg("apply skip: inst null for " + statId); continue; }
                mInstRemove.invoke(inst, sourceKey);
                Object mod = ctorStatModifier.newInstance(sourceKey, statId, val, modifierTypeFlat);
                mInstAddModifier.invoke(inst, mod);
                remember(player.getUniqueId(), sourceKey, statId);
            }
            if (mUpdateStats != null) mUpdateStats.invoke(map);

            for (String statId0 : stats.keySet()) {
                String statId = normalize(statId0);
                Object inst2 = getInstance(map, statId);
                Double total = (inst2 != null && mInstGetTotal != null) ? ((Number) mInstGetTotal.invoke(inst2)).doubleValue() : null;
                dbg("after-update " + player.getName() + " " + statId + " total=" + total);
            }
        } catch (Throwable t) {
            dbg("apply exception: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    public void remove(Player player, String sourceKey, Set<String> statIds) {
        if (!ready || player == null || sourceKey == null || statIds == null || statIds.isEmpty()) return;
        try {
            Object data = resolveData(player);
            Object map = data == null ? null : mGetStatMap.invoke(data);
            dbg("remove player=" + player.getName() + " source=" + sourceKey + " statIds=" + statIds + " map=" + (map!=null));
            if (map == null) return;

            for (String statId0 : statIds) {
                String statId = normalize(statId0);
                Object inst = getInstance(map, statId);
                if (inst == null) continue;
                mInstRemove.invoke(inst, sourceKey);
                forget(player.getUniqueId(), sourceKey, statId);
            }
            if (mUpdateStats != null) mUpdateStats.invoke(map);
        } catch (Throwable t) {
            dbg("remove exception: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    public void removeAllByPrefix(Player player, String prefix) {
        if (!ready || player == null || prefix == null) return;
        UUID u = player.getUniqueId();
        Map<String, Set<String>> bySource = applied.getOrDefault(u, Collections.emptyMap());
        List<String> toRemove = new ArrayList<>();
        for (String src : bySource.keySet()) if (src.startsWith(prefix)) toRemove.add(src);
        for (String src : toRemove) remove(player, src, new HashSet<>(bySource.getOrDefault(src, Collections.emptySet())));
    }

    private Object resolveData(Player p) {
        try {
            if (getMode == 0) return mGetData.invoke(null, p);
            if (getMode == 1) return mGetData.invoke(null, p.getUniqueId());
            return mGetData.invoke(null, (OfflinePlayer) p);
        } catch (Throwable t) { return null; }
    }

    private Object getInstance(Object statMap, String statId) {
        try {
            if (mGetInstanceStr != null) {
                Object inst = mGetInstanceStr.invoke(statMap, statId);
                if (inst != null) return inst;
            }
            if (mGetInstanceStrBool != null) {
                return mGetInstanceStrBool.invoke(statMap, statId, true);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private Method findZeroArg(Class<?> c, String... names) {
        for (String n : names) {
            try { return c.getMethod(n); } catch (Throwable ignored) {}
        }
        return null;
    }

    private String normalize(String s) {
        return s == null ? "" : s.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private boolean isDebug() {
        try { return plugin.getConfig().getBoolean("debug", false); } catch (Throwable ignored) { return false; }
    }

    private void dbg(String msg) {
        if (isDebug()) plugin.getLogger().info("[DEBUG] " + msg);
    }

    private void remember(UUID u, String source, String stat) {
        applied.computeIfAbsent(u, k -> new HashMap<>()).computeIfAbsent(source, k -> new HashSet<>()).add(stat);
    }

    private void forget(UUID u, String source, String stat) {
        Map<String, Set<String>> m = applied.get(u);
        if (m == null) return;
        Set<String> s = m.get(source);
        if (s == null) return;
        s.remove(stat);
        if (s.isEmpty()) m.remove(source);
        if (m.isEmpty()) applied.remove(u);
    }
}
