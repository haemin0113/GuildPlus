package org.haemin.guildPlus.buff;

import org.bukkit.configuration.ConfigurationSection;
import org.haemin.guildPlus.util.Configs;

import java.util.*;

class BuffRepository {

    static class Node {
        final String id, name;
        final Map<String, Double> stats;
        final List<String> requires;
        final boolean temp;
        final long duration;
        final double cost;
        final int membersMin;
        Node(String id,String name,Map<String,Double> stats,List<String> req,boolean temp,long duration,double cost,int membersMin){
            this.id=id; this.name=name; this.stats=stats; this.requires=req; this.temp=temp; this.duration=duration; this.cost=cost; this.membersMin=membersMin;
        }
    }

    private final Map<String, Node> infinite = new LinkedHashMap<>();
    private final Map<String, Node> temp = new LinkedHashMap<>();

    void reload() {
        infinite.clear(); temp.clear();
        ConfigurationSection inf = Configs.buffs().getConfigurationSection("infinite.nodes");
        if (inf != null) for (String id: inf.getKeys(false)) {
            String base = id;
            String name = inf.getString(base+".name", id);
            Map<String,Double> stats = parseStats(inf.getConfigurationSection(base));
            List<String> req = inf.getStringList(base+".requires");
            int membersMin = inf.getInt(base+".conditions.members-min", 0);
            infinite.put(id, new Node(id,name,stats,req,false,0L,0D,membersMin));
        }
        ConfigurationSection tmp = Configs.buffs().getConfigurationSection("temp.nodes");
        if (tmp != null) for (String id: tmp.getKeys(false)) {
            String base = id;
            String name = tmp.getString(base+".name", id);
            Map<String,Double> stats = parseStats(tmp.getConfigurationSection(base));
            List<String> req = tmp.getStringList(base+".requires");
            long dur = parseDuration(tmp.getString(base+".duration","1h"));
            double cost = tmp.getDouble(base+".cost",0D);
            int membersMin = tmp.getInt(base+".conditions.members-min", 0);
            temp.put(id, new Node(id,name,stats,req,true,dur,cost,membersMin));
        }
    }

    Map<String, Node> infinite() { return infinite; }
    Map<String, Node> temp() { return temp; }

    private Map<String,Double> parseStats(ConfigurationSection sec) {
        Map<String,Double> map = new LinkedHashMap<>();
        if (sec == null) return map;
        if (sec.isConfigurationSection("stats")) {
            ConfigurationSection s = sec.getConfigurationSection("stats");
            for (String k : s.getKeys(false)) map.put(k.trim().toUpperCase(Locale.ROOT), s.getDouble(k));
        }
        if (map.isEmpty() && sec.isString("stat")) {
            String k = sec.getString("stat","ATTACK_DAMAGE").trim().toUpperCase(Locale.ROOT);
            double v = sec.getDouble("amount",1.0);
            map.put(k, v);
        }
        return map;
    }

    private long parseDuration(String s) {
        try {
            s = s.trim().toLowerCase(Locale.ROOT);
            long mul = 1000L;
            if (s.endsWith("ms")) { mul = 1L; s = s.substring(0, s.length()-2); }
            else if (s.endsWith("s")) { mul = 1000L; s = s.substring(0, s.length()-1); }
            else if (s.endsWith("m")) { mul = 60_000L; s = s.substring(0, s.length()-1); }
            else if (s.endsWith("h")) { mul = 3_600_000L; s = s.substring(0, s.length()-1); }
            else if (s.endsWith("d")) { mul = 86_400_000L; s = s.substring(0, s.length()-1); }
            return (long)(Double.parseDouble(s)*mul);
        } catch (Exception e) { return 3_600_000L; }
    }
}
