package org.haemin.guildPlus.hook;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.haemin.guildPlus.GuildPlus;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Locale;
import java.util.UUID;

public class MmocoreHook {
    private final GuildPlus plugin;
    public MmocoreHook(GuildPlus plugin) { this.plugin = plugin; }

    public String getGuildId(Player p) {
        if (p == null) return null;

        Object guild = null;

        // 1) PlayerData 경로 (여러 시그니처 시도)
        Class<?> pdClz = tryLoad(
                "net.Indyuce.mmocore.api.player.PlayerData",
                "net.Indyuce.mmocore.player.PlayerData"
        );
        if (pdClz != null) {
            Object pd = tryInvokeStatic(pdClz, new String[]{"get", "getOrCreate", "getFrom"},
                    new Class[][]{{Player.class}, {Player.class}, {Player.class}},
                    new Object[][]{{p}, {p}, {p}});
            if (pd == null) {
                pd = tryInvokeStatic(pdClz, new String[]{"get", "getOrCreate"},
                        new Class[][]{{UUID.class}, {UUID.class}},
                        new Object[][]{{p.getUniqueId()}, {p.getUniqueId()}});
            }
            if (pd != null) {
                guild = tryInvoke(pd, new String[]{"getGuild", "guild"}, new Class[][]{{}, {}}, new Object[][]{{}, {}});
                if (guild == null) {
                    Object module = tryInvoke(pd, new String[]{"getGuildModule"}, new Class[][]{{}}, new Object[][]{{}});
                    if (module != null) {
                        guild = tryInvoke(module, new String[]{"getGuild"}, new Class[][]{{}}, new Object[][]{{}});
                    }
                }
            }
        }

        // 2) GuildManager 경로
        if (guild == null) {
            Object core = tryCoreInstance();
            if (core != null) {
                Object gm = tryInvoke(core, new String[]{"getGuildManager", "getGuilds"}, new Class[][]{{}, {}}, new Object[][]{{}, {}});
                if (gm != null) {
                    guild = tryInvoke(gm,
                            new String[]{"getFromPlayer", "getFrom", "get", "find", "findByPlayer"},
                            new Class[][]{
                                    {Player.class}, {Player.class}, {Player.class}, {Player.class}, {Player.class}},
                            new Object[][]{{p}, {p}, {p}, {p}, {p}});
                    if (guild == null) {
                        guild = tryInvoke(gm,
                                new String[]{"get", "find", "fromUUID"},
                                new Class[][]{{UUID.class}, {UUID.class}, {UUID.class}},
                                new Object[][]{{p.getUniqueId()}, {p.getUniqueId()}, {p.getUniqueId()}});
                    }
                }
            }
        }

        // 3) MythicLib 경로(구버전)
        if (guild == null) {
            try {
                Class<?> ml = Class.forName("io.lumine.mythic.lib.api.player.MMOPlayerData");
                Method get = ml.getMethod("get", Player.class);
                Object data = get.invoke(null, p);
                Method g = data.getClass().getMethod("getGuildID");
                Object id = g.invoke(data);
                if (id != null) return String.valueOf(id);
            } catch (Throwable ignored) {}
        }

        if (guild == null) {
            debug("MmocoreHook.getGuildId: could not resolve guild for " + p.getName());
            return null;
        }
        return extractId(guild);
    }

    public int countMembers(String guildId) {
        if (guildId == null) return 0;

        Object core = tryCoreInstance();
        if (core != null) {
            Object gm = tryInvoke(core, new String[]{"getGuildManager", "getGuilds"}, new Class[][]{{}, {}}, new Object[][]{{}, {}});
            if (gm != null) {
                Object guild = tryInvoke(gm,
                        new String[]{"get", "find", "getById", "getGuild"},
                        new Class[][]{{String.class}, {String.class}, {String.class}, {String.class}},
                        new Object[][]{{guildId}, {guildId}, {guildId}, {guildId}});
                if (guild != null) {
                    Object members = tryInvoke(guild,
                            new String[]{"getMembers", "getMemberList", "members"},
                            new Class[][]{{}, {}, {}},
                            new Object[][]{{}, {}, {}});
                    if (members instanceof Collection) return ((Collection<?>) members).size();
                }
            }
        }

        int online = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            String gid = getGuildId(p);
            if (gid != null && gid.equalsIgnoreCase(guildId)) online++;
        }
        return online;
    }

    private String extractId(Object guild) {
        Object id = tryInvoke(guild,
                new String[]{"getId", "getIdentifier", "getName", "getUniqueId", "getUniqueID", "getKey", "id", "identifier", "name"},
                new Class[][]{{}, {}, {}, {}, {}, {}, {}, {}, {}},
                new Object[][]{{}, {}, {}, {}, {}, {}, {}, {}, {}});
        if (id == null) {
            try {
                Field f = guild.getClass().getDeclaredField("id");
                f.setAccessible(true);
                id = f.get(guild);
            } catch (Throwable ignored) {}
        }
        return id == null ? String.valueOf(guild) : String.valueOf(id);
    }

    private Object tryCoreInstance() {
        try {
            Class<?> core = Class.forName("net.Indyuce.mmocore.MMOCore");
            try {
                Field f = core.getField("plugin");
                return f.get(null);
            } catch (NoSuchFieldException e) {
                Method get = core.getMethod("plugin");
                return get.invoke(null);
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private Class<?> tryLoad(String... names) {
        for (String n : names) {
            try { return Class.forName(n); } catch (Throwable ignored) {}
        }
        return null;
    }

    private Object tryInvoke(Object target, String[] names, Class<?>[][] sigs, Object[][] args) {
        for (int i = 0; i < names.length; i++) {
            try {
                Method m = target.getClass().getMethod(names[i], sigs[i]);
                return m.invoke(target, args[i]);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private Object tryInvokeStatic(Class<?> clz, String[] names, Class<?>[][] sigs, Object[][] args) {
        for (int i = 0; i < names.length; i++) {
            try {
                Method m = clz.getMethod(names[i], sigs[i]);
                return m.invoke(null, args[i]);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private void debug(String s) {
        if (plugin.getConfig().getBoolean("debug", false))
            plugin.getLogger().warning("[DEBUG] " + s);
    }
}
