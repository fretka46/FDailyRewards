package com.fretka46.fDailyRewards.Utils;

import com.fretka46.fDailyRewards.FDailyRewards;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Dictionary;
import java.util.UUID;

public class Scheduler {


    public static final Dictionary<UUID, BukkitTask> scheduledTasks = new java.util.Hashtable<>();

    public static void scheduleRewardMessage(Player player) {
        // Send message to the player every 30 mins
        var plugin = FDailyRewards.getPlugin(FDailyRewards.class);

        var interval = plugin.getConfig().getInt("reward_claim_available_interval");

        // Task already scheduled
        if (scheduledTasks.get(player.getUniqueId()) != null) {
            return;
        }

        // Run reminder from now every 30 mins
        var task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {

            if (!player.isOnline()) {
                cancelRewardMessage(player.getUniqueId());
                return;
            }

            // Execute command
            var cmd = plugin.getConfig().getString("reward_claim_available_command", "tell %player_name% FDR - Claim message not configured!").replace("%player_name%", player.getName());
            plugin.getServer().dispatchCommand(FDailyRewards.getPlugin(FDailyRewards.class).getServer().getConsoleSender(), cmd);

        }, 0L,  interval * 60 * 20L);

        scheduledTasks.put(player.getUniqueId(), task);
    }

    // Cancels the already running task
    public static void cancelRewardMessage(UUID uuid) {
        var task = scheduledTasks.get(uuid);
        if (task != null) {
            task.cancel();
            scheduledTasks.remove(uuid);
        }
    }
}
