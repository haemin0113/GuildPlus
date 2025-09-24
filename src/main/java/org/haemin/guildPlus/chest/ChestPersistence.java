package org.haemin.guildPlus.chest;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.haemin.guildPlus.GuildPlus;

import java.io.File;
import java.io.IOException;
import java.util.*;

public final class ChestPersistence {

    public static void loadAll(GuildPlus plugin, ChestService service) {
        File dir = new File(plugin.getDataFolder(), "chests");
        if (!dir.exists()) dir.mkdirs();
        File[] files = dir.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;

        for (File f : files) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            String gid = y.getString("guild");
            if (gid == null || gid.isEmpty()) continue;
            ChestService.GuildChest gc = service.getOrCreate(gid);

            List<Integer> unlocked = y.getIntegerList("unlocked");
            for (int p : unlocked) gc.unlockPage(p);

            for (String key : y.getConfigurationSection("rows").getKeys(false)) {
                int page = parseInt(key, 1);
                gc.setRows(page, y.getInt("rows." + key, 1));
            }

            if (y.isConfigurationSection("names")) {
                for (String key : y.getConfigurationSection("names").getKeys(false)) {
                    int page = parseInt(key, 1);
                    gc.setName(page, y.getString("names." + key, "길드 창고 " + page));
                }
            }

            if (y.isConfigurationSection("pages")) {
                for (String key : y.getConfigurationSection("pages").getKeys(false)) {
                    int page = parseInt(key, 1);
                    int size = Math.max(9, Math.min(54, y.getInt("pages." + key + ".size", gc.getRows(page) * 9)));
                    ItemStack[] items = ((List<ItemStack>) y.getList("pages." + key + ".contents", Collections.emptyList()))
                            .toArray(new ItemStack[0]);
                    Inventory inv = gc.getPageInventory(page);
                    if (inv.getSize() != size) inv = gc.getPageInventory(page); // 재생성
                    inv.setContents(pad(items, inv.getSize()));
                }
            }
        }
    }

    public static void saveAll(GuildPlus plugin, ChestService service, Map<String, ChestService.GuildChest> snapshot) {
        File dir = new File(plugin.getDataFolder(), "chests");
        if (!dir.exists()) dir.mkdirs();
        for (Map.Entry<String, ChestService.GuildChest> e : snapshot.entrySet()) {
            String gid = e.getKey();
            ChestService.GuildChest gc = e.getValue();
            File f = new File(dir, gid + ".yml");
            YamlConfiguration y = new YamlConfiguration();

            y.set("guild", gid);
            y.set("unlocked", new ArrayList<>(getUnlocked(gc)));
            Map<String, Integer> rows = new LinkedHashMap<>();
            for (int p : getUnlocked(gc)) rows.put(String.valueOf(p), gc.getRows(p));
            y.createSection("rows", rows);

            Map<String, String> names = new LinkedHashMap<>();
            for (int p : getUnlocked(gc)) names.put(String.valueOf(p), gc.getName(p));
            y.createSection("names", names);

            Map<String, Object> pages = new LinkedHashMap<>();
            for (int p : getUnlocked(gc)) {
                Inventory inv = gc.getPageInventory(p);
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("size", inv.getSize());
                node.put("contents", Arrays.asList(inv.getContents()));
                pages.put(String.valueOf(p), node);
            }
            y.createSection("pages", pages);

            try { y.save(f); } catch (IOException ignored) {}
        }
    }

    private static int parseInt(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    private static ItemStack[] pad(ItemStack[] arr, int size) {
        ItemStack[] out = new ItemStack[size];
        for (int i = 0; i < size; i++) out[i] = i < arr.length ? arr[i] : null;
        return out;
    }

    private static List<Integer> getUnlocked(ChestService.GuildChest gc) {
        List<Integer> out = new ArrayList<>();
        for (int p = 1; p <= 9; p++) if (gc.isUnlocked(p)) out.add(p);
        return out;
    }
}
