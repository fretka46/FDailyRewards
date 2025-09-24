package com.fretka46.fDailyRewards.Storage;

import com.fretka46.fDailyRewards.Utils.Log;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
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
        int monthNumber = LocalDateTime.now().getMonthValue();
        plugin.saveResource("rewardsTemplate.yml", false);
        File rewardsFile = new File(plugin.getDataFolder(), monthNumber + "_rewards.yml");
        if (!rewardsFile.exists()) {
            Log.severe("Rewards config for month " + monthNumber + "_rewards.yml not found");
            Log.severe("Please make sure that the file exists in the FDailyRewards plugin folder.");
            Log.severe("Falling back to rewardsTemplate.yml");

            // Save default template
            rewardsFile = new File(plugin.getDataFolder(), "rewardsTemplate.yml");
        }
        FileConfiguration cfg = YamlConfiguration.loadConfiguration(rewardsFile);
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
    }

    private static Integer parseDay(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    /**
     * Reads a returns a DailyRewardItem from the given configuration section
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
}
