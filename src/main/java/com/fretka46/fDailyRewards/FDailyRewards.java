package com.fretka46.fDailyRewards;

import com.fretka46.fDailyRewards.Commands.MainCommand;
import com.fretka46.fDailyRewards.Listeners.LoginListener;
import com.fretka46.fDailyRewards.Storage.DatabaseManager;
import com.fretka46.fDailyRewards.Utils.Scheduler;
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

        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        registerCommand("fdailyrewards", new MainCommand());
        getServer().getPluginManager().registerEvents(new  LoginListener(), this);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        var tasksEnum = Scheduler.scheduledTasks.elements();
        while (tasksEnum.hasMoreElements()) {
            var task = tasksEnum.nextElement();
            task.cancel();
        }
    }
}
