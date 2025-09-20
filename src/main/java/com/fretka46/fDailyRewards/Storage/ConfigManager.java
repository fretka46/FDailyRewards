package com.fretka46.fDailyRewards.Storage;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.LocalDateTime;
import java.util.*;

public class ConfigManager {
    private static final Map<Integer, DailyRewardDay> REWARDS_BY_DAY = new HashMap<>();

    /**
     * Načte konfiguraci a připraví cache odměn.
     */
    public static void init(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        reload(plugin);
    }

    public static DailyRewardDay getRewardForDay(int day) {
        return REWARDS_BY_DAY.get(day);
    }

    /**
     * Znovu načte konfiguraci odměn z config.yml
     */
    public static void reload(JavaPlugin plugin) {
        FileConfiguration cfg = plugin.getConfig();
        REWARDS_BY_DAY.clear();

        // Podporujeme strukturu: rewards.<den>.*
        ConfigurationSection rewardsSec = cfg.getConfigurationSection("rewards");
        if (rewardsSec != null) {
            for (String key : rewardsSec.getKeys(false)) {
                Integer day = parseDay(key);
                if (day == null) continue;

                ConfigurationSection sec = rewardsSec.getConfigurationSection(key);
                if (sec == null) continue;

                boolean vip = sec.getBoolean("vip", false);
                DailyRewardItem item = readItem(sec.getConfigurationSection("item"));
                List<String> cmdList = sec.getStringList("commands");
                String[] commands = cmdList.toArray(new String[0]);

                REWARDS_BY_DAY.put(day, new DailyRewardDay(day, vip, item, commands));
            }
        }

        // Alternativně: podpora listu v "rewardsList: - day: ..."
        if (cfg.isList("rewardsList")) {
            for (Map<?, ?> map : cfg.getMapList("rewardsList")) {
                Object dayObj = map.get("day");
                Integer day = null;
                if (dayObj instanceof Number n) day = n.intValue();
                else if (dayObj instanceof String s) day = parseDay(s);
                if (day == null) continue;

                boolean vip = asBoolean(map.get("vip"), false);

                DailyRewardItem item = new DailyRewardItem();
                Object itemObj = map.get("item");
                if (itemObj instanceof Map<?, ?> itemMap) {
                    item = readItemFromMap(itemMap);
                }

                List<String> commands = new ArrayList<>();
                Object cmdsObj = map.get("commands");
                if (cmdsObj instanceof List<?> list) {
                    for (Object o : list) if (o != null) commands.add(String.valueOf(o));
                }

                REWARDS_BY_DAY.put(day, new DailyRewardDay(day, vip, item, commands.toArray(new String[0])));
            }
        }
    }

    private static Integer parseDay(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static boolean asBoolean(Object o, boolean def) {
        if (o == null) return def;
        if (o instanceof Boolean b) return b;
        if (o instanceof Number n) return n.intValue() != 0;
        if (o instanceof String s) return Boolean.parseBoolean(s);
        return def;
    }

    /**
     * Reads a returns a DailyRewardItem from the given configuration section
     * @param sec
     * @return
     */
    public static DailyRewardItem readItem(ConfigurationSection sec) {
        if (sec == null) return null;
        DailyRewardItem item = new DailyRewardItem();
        // Material
        String matStr = sec.getString("material", "PAPER");
        Material mat = Material.matchMaterial(matStr == null ? "PAPER" : matStr);
        item.material = mat != null ? mat : Material.PAPER;
        // CustomModelData
        item.customModelData = sec.getInt("customModelData", sec.getInt("model_data", 0));
        // Tooltip style (namespaced key string like "minecraft:thecivia/legendary")
        item.tooltipStyle = sec.getString("tooltipStyle", sec.getString("tooltip_style"));
        // Název
        item.name = sec.getString("name", sec.getString("display_name", ""));
        // Lore: podporujeme jak string, tak list -> spojíme \n
        if (sec.isList("lore")) {
            List<String> loreList = sec.getStringList("lore");
            item.lore = String.join("\n", loreList);
        } else {
            item.lore = sec.getString("lore", "");
        }
        return item;
    }

    @SuppressWarnings("unchecked")
    private static DailyRewardItem readItemFromMap(Map<?, ?> map) {
        DailyRewardItem item = new DailyRewardItem();
        Object material = map.get("material");
        Material mat = Material.matchMaterial(material == null ? "PAPER" : String.valueOf(material));
        item.material = mat != null ? mat : Material.PAPER;
        Object cmd = map.get("customModelData");
        if (cmd instanceof Number n) item.customModelData = n.intValue();
        else if (cmd instanceof String s) try { item.customModelData = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        else {
            // fallback na model_data
            Object md = map.get("model_data");
            if (md instanceof Number n) item.customModelData = n.intValue();
            else if (md instanceof String s) try { item.customModelData = Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        }
        // Tooltip style
        Object style = map.get("tooltipStyle");
        if (style == null) style = map.get("tooltip_style");
        item.tooltipStyle = style == null ? null : String.valueOf(style);
        Object name = map.get("name");
        if (name == null) name = map.get("display_name");
        item.name = name == null ? "" : String.valueOf(name);
        Object lore = map.get("lore");
        if (lore instanceof List<?> list) {
            List<String> res = new ArrayList<>();
            for (Object o : list) if (o != null) res.add(String.valueOf(o));
            item.lore = String.join("\n", res);
        } else {
            item.lore = lore == null ? "" : String.valueOf(lore);
        }
        return item;
    }
}
