package com.fretka46.fDailyRewards;

import com.fretka46.fDailyRewards.Listeners.EventListeners;
import com.fretka46.fDailyRewards.Storage.DatabaseManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.fretka46.fDailyRewards.Storage.ConfigManager;
import com.fretka46.fDailyRewards.UI.MenuListener;

import java.sql.SQLException;

public final class FDailyRewards extends JavaPlugin {

    @Override
    public void onEnable() {
        // Connect to database
        try {
            DatabaseManager.Connection = DatabaseManager.connect();
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        // Plugin startup logic
        ConfigManager.init(this);

        getServer().getPluginManager().registerEvents(new EventListeners(), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);

        /*
        Daily Rewards
        - Každý den v 4.15 se hráči mohou vyzvednout odměnu / command
        - survival.premium.dailylogin - VIP pass
        - Configu nastavení každý den,
         - VIP
         - Typ itemu
         - CustomModelData - itemtooltip

        - Odměny jsou formou příkazů

        - Notifikace chvilku po připojení že nemá vyzvednutou odměnu
         - Poté každou půlhodinu

         - Když to nestihne, jde to postupne takze jakoby nestihne posledni odmeny
         - kdyz si zakoupi VIP na konci mesice, muze si vybrat vsechny VIP odmeny pred tim


            - Možnost dělat config na další měsíc
         */
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
