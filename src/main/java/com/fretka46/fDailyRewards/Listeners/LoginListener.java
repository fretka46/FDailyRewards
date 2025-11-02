package com.fretka46.fDailyRewards.Listeners;

import com.fretka46.fDailyRewards.FDailyRewards;
import com.fretka46.fDailyRewards.Storage.ConfigManager;
import com.fretka46.fDailyRewards.Storage.DatabaseManager;
import com.fretka46.fDailyRewards.Utils.Log;
import com.fretka46.fDailyRewards.Utils.Messages;
import com.fretka46.fDailyRewards.Utils.Scheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class LoginListener implements Listener {

    @EventHandler
    public void onLogin(PlayerJoinEvent event)
    {
        var player = event.getPlayer();
        var localtime = java.time.LocalDateTime.now();
        var config = FDailyRewards.getPlugin(FDailyRewards.class).getConfig();

        // Check player daily rewards
        if (player.hasPermission("survival.login.tokeny") && !DatabaseManager.hasLoggedInToday(player.getUniqueId(), localtime))
        {
            // Set daily login and reward the player
            DatabaseManager.setLoggedInToday(player.getUniqueId(), localtime);

            // Execute commands
            var commands = config.getStringList("login_commands");
            for (var command : commands)
            {
                var cmd = command.replace("%player_name%", player.getName());
                FDailyRewards.getPlugin(FDailyRewards.class)
                        .getServer().dispatchCommand(FDailyRewards.getPlugin(FDailyRewards.class).getServer().getConsoleSender(), cmd);
            }

            // Send message
            Messages.sendTranslatedMessageTo(player, "login_rewarded");
        }

        if (config.getBoolean("show_reward_claim_available_message")) {
            if (!DatabaseManager.hasClaimedRewardInLastDay(player.getUniqueId(), localtime)) {

                // Check if player current day reward is vip and player is not vip
                var isVip = player.hasPermission("survival.premium.dailylogin");
                var nextDayClaim = DatabaseManager.getNextDayToClaim(player.getUniqueId(), !isVip);

                // safety: ensure index is valid
                if (nextDayClaim <= 0) {
                    Log.info("Invalid nextDayClaim for player " + player.getName() + ": " + nextDayClaim);
                    return;
                }

                var reward = ConfigManager.getRewardForDay(nextDayClaim - 1);

                // pokud je odměna VIP a hráč není VIP, není dostupná -> nezobrazovat zprávu
                if (reward != null && reward.vip && !isVip) {
                    Log.info("Player " + player.getName() + " has a VIP reward available, but is not VIP. Not scheduling claim message.");
                } else {
                    Scheduler.scheduleRewardMessage(player);
                }
            }
        }
    }
}
