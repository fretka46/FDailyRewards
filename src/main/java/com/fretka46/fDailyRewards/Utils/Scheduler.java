package com.fretka46.fDailyRewards.Utils;

import com.fretka46.fDailyRewards.FDailyRewards;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Dictionary;

public class Scheduler {


    public static final Dictionary<Player, BukkitTask> scheduledTasks = new java.util.Hashtable<>();

    public static void scheduleRewardMessage(Player player) {
        // Send message to the player every 30 mins
        var plugin = FDailyRewards.getPlugin(FDailyRewards.class);

        var interval = plugin.getConfig().getInt("reward_claim_available_interval");

        // Run reminder from now every 30 mins
        // Cancelable with cancelRewardMessage
        var task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Messages.sendTranslatedMessageTo(player, "reward_claim_available_message");
        }, 0L, 10 * 20L); // 20 ticks

        scheduledTasks.put(player, task);
    }

    // Cancels the already running task
    public static void cancelRewardMessage(Player player) {
        var task = scheduledTasks.get(player);
        if (task != null) {
            task.cancel();
            scheduledTasks.remove(player);
        }
    }
}
