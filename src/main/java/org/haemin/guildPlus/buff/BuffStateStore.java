package org.haemin.guildPlus.buff;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

class BuffStateStore {

    private final File infiniteFile;
    private final File tempFile;

    BuffStateStore(File infiniteFile, File tempFile) {
        this.infiniteFile = infiniteFile;
        this.tempFile = tempFile;
    }

    void save(Map<String, Set<String>> activeInfinite, Map<String, Map<String, Long>> activeTemp) throws IOException {
        YamlConfiguration y1 = new YamlConfiguration();
        activeInfinite.forEach((g,set)-> y1.set(g, new ArrayList<>(set)));
        y1.save(infiniteFile);
        YamlConfiguration y2 = new YamlConfiguration();
        activeTemp.forEach((g,map)-> map.forEach((n,exp)-> y2.set(g+"."+n, exp)));
        y2.save(tempFile);
    }

    void loadInto(Map<String, Set<String>> activeInfinite, Map<String, Map<String, Long>> activeTemp) {
        activeInfinite.clear(); activeTemp.clear();
        try {
            YamlConfiguration y1 = YamlConfiguration.loadConfiguration(infiniteFile);
            for (String g: y1.getKeys(false)) activeInfinite.put(g, new LinkedHashSet<>(y1.getStringList(g)));
        } catch (Exception ignored) {}
        try {
            YamlConfiguration y2 = YamlConfiguration.loadConfiguration(tempFile);
            for (String g: y2.getKeys(false)) {
                Map<String, Long> m = new HashMap<>();
                for (String node: y2.getConfigurationSection(g).getKeys(false)) m.put(node, y2.getLong(g+"."+node));
                activeTemp.put(g, m);
            }
        } catch (Exception ignored) {}
    }
}
