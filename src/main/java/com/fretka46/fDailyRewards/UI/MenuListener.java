package com.fretka46.fDailyRewards.UI;

import com.fretka46.fDailyRewards.Storage.ConfigManager;
import com.fretka46.fDailyRewards.Storage.DailyRewardDay;
import com.fretka46.fDailyRewards.Storage.DatabaseManager;
import com.fretka46.fDailyRewards.Utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Listener for the Daily Rewards custom inventory holder.
 * - Denies all interactions inside this menu (click/drag).
 * - Detects clicks on specific day items via slot->day mapping held by Menu holder.
 */
public class MenuListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Menu holder)) return;

        // Always deny any interaction inside this menu (top or bottom)
        e.setCancelled(true);

        // Only react to clicks inside the top inventory area
        int raw = e.getRawSlot();
        if (raw < 0 || raw >= e.getView().getTopInventory().getSize()) return;

        Integer day = holder.getDayAt(raw);
        if (day == null) return; // clicked border/info/empty slot

        if (!(e.getWhoClicked() instanceof Player player)) return;

        DailyRewardDay rewardDay = ConfigManager.getRewardForDay(day);
        boolean isPlayerVip = player.hasPermission("survival.premium.dailylogin");
        int claimedDays = DatabaseManager.getTotalClaims(player.getUniqueId());
        var localTime = java.time.LocalDateTime.now();

        // TODO: Improve the checks as they are not working perfectly now

        // Check if this reward is already claimed
        if (DatabaseManager.hasClaimedDay(player.getUniqueId(), day)) {
            Messages.sendTranslatedMessageTo(player, "reward_already_claimed");
            // Play a custom sound by its namespaced ID
            player.playSound(player.getLocation(), "thecivia:thecivia.sound.27", 1.0f, 1.0f);
            return;
        }

        // Check if the player can claim this specific day's reward (e.g. not a future day)
        if (day > localTime.getDayOfMonth()) {
            Messages.sendTranslatedMessageTo(player, "reward_not_yet_available");
            player.playSound(player.getLocation(), "thecivia:thecivia.sound.28", 1.0f, 1.0f);
            return;
        }

        // Check for VIP
        if (rewardDay.vip) {
            if (!isPlayerVip) {
                Messages.sendTranslatedMessageTo(player, "reward_vip_only");
                player.playSound(player.getLocation(), "thecivia:thecivia.sound.28", 1.0f, 1.0f);
                return;
            }

            claimReward(rewardDay, player);
            return;
        }



        // Check if the player can claim this specific day's reward (e.g. not skipping days)
        if (day > claimedDays + 1) {
            Messages.sendTranslatedMessageTo(player, "reward_previous_not_claimed");
            player.playSound(player.getLocation(), "thecivia:thecivia.sound.28", 1.0f, 1.0f);
            return;
        }

        // Check if player already claimed last day (24h cooldown)
        if (DatabaseManager.hasClaimedRewardInLastDay(player.getUniqueId(), localTime)) {
            Messages.sendTranslatedMessageTo(player, "reward_already_claimed_today");
            // Play a custom sound by its namespaced ID
            player.playSound(player.getLocation(), "thecivia:thecivia.sound.27", 1.0f, 1.0f);
            return;
        }

        // -- REWARD ALLOWED -- //

        // Execute commands for the clicked day as console
        claimReward(rewardDay, player);
    }

    private void claimReward(DailyRewardDay rewardDay, Player player) {
        // Mark reward as claimed in DB
        var localTime = java.time.LocalDateTime.now();
        DatabaseManager.setRewardClaimed(player.getUniqueId(), localTime, rewardDay.day);

        player.playSound(player.getLocation(), "thecivia:thecivia.sound.34", 1.0f, 1.0f);

        if (rewardDay.commands == null) return;
        for (String cmd : rewardDay.commands) {
            if (cmd == null || cmd.isBlank()) continue;
            String resolved = cmd
                    .replace("%player_name%", player.getName())
                    .replace("{player}", player.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), resolved);
        }

        // Refresh menu
        var inventory = player.getOpenInventory();
        if (inventory.getTopInventory().getHolder() instanceof Menu) {
            inventory.close();
            Menu.openFor(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        e.getView().getTopInventory();
        if (!(e.getView().getTopInventory().getHolder() instanceof Menu)) return;
        // Deny any drag operations inside this menu
        e.setCancelled(true);
    }
}
