package com.fretka46.fDailyRewards.Commands;

import com.fretka46.fDailyRewards.Storage.AdminStorageEdit;
import com.fretka46.fDailyRewards.Storage.ConfigManager;
import com.fretka46.fDailyRewards.Storage.DatabaseManager;
import com.fretka46.fDailyRewards.UI.MenuListener;
import com.fretka46.fDailyRewards.Utils.Messages;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;

public class Admin {

    public static int ResetDayExecutor(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();

        // Check permission
        if (!sender.hasPermission("fdailyrewards.admin.resetday")) {
            sender.sendMessage("You do not have permission to use this command.");
            return 0;
        }

        var targetPlayer = ctx.getArgument("player", String.class);
        var day = ctx.getArgument("day", Integer.class);

        // Get uuid
        var uuid = Bukkit.getOfflinePlayer(targetPlayer).getUniqueId();

        // Reset day in database
        var editedRows = AdminStorageEdit.resetDay(String.valueOf(uuid), day);

        Messages.sendMessage(sender, "Reset day " + day + " for player " + targetPlayer);
        Messages.sendMessage(sender, "Records edited: " + editedRows);
        return 1;
    }

    public static int SetDayExecutor(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        // Check permission
        if (!sender.hasPermission("fdailyrewards.admin.setday")) {
            sender.sendMessage("You do not have permission to use this command.");
            return 0;
        }

        var targetPlayer = ctx.getArgument("player", String.class);
        var day = ctx.getArgument("day", Integer.class);
        var uuid = Bukkit.getOfflinePlayer(targetPlayer).getUniqueId();

        if (DatabaseManager.hasClaimedDay(uuid, day)) {
            Messages.sendMessage(sender, "Player " + targetPlayer + " has already claimed day " + day + " reward this month.");
            return 0;
        }

        DatabaseManager.setRewardClaimed(uuid, java.time.LocalDateTime.now(), day);

        Messages.sendMessage(sender,"Set day " + day + " for player " + targetPlayer);
        return 1;
    }

    public static int GiftDayExecutor(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        // Check permission
        if (!sender.hasPermission("fdailyrewards.admin.giftday")) {
            sender.sendMessage("You do not have permission to use this command.");
            return 0;
        }

        var targetPlayer = ctx.getArgument("player", String.class);
        var day = ctx.getArgument("day", Integer.class);
        var uuid = Bukkit.getOfflinePlayer(targetPlayer).getUniqueId();

        // Get reward for the day
        var reward = ConfigManager.getRewardForDay(day);

        if (reward == null) {
            Messages.sendMessage(sender, "No reward configured for day " + day + ".");
            return 0;
        }

        // Give reward to player
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            Messages.sendMessage(sender, "Player " + targetPlayer + " is not online.");
            return 0;
        }


        MenuListener.executeCommands(reward, player);
        DatabaseManager.setRewardClaimed(player.getUniqueId(), LocalDateTime.now(), reward.day);
        player.playSound(player.getLocation(), "thecivia:thecivia.sound.34", 1.0f, 1.0f);
        Messages.sendMessage(player, "Byla ti věnována odměna za den " + day + ", yayy!");

        Messages.sendMessage(sender,"Gifted day " + day + " reward to player " + targetPlayer);

        return 1;
    }

    public static int CheckDayExecutor(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        // Check permission
        if (!sender.hasPermission("fdailyrewards.admin.checkday")) {
            sender.sendMessage("You do not have permission to use this command.");
            return 0;
        }

        var targetPlayer = ctx.getArgument("player", String.class);
        var uuid = Bukkit.getOfflinePlayer(targetPlayer).getUniqueId();

        var lastClaimedDay = AdminStorageEdit.getLastClaimedDay(uuid.toString());

        if (lastClaimedDay == 0)
            Messages.sendMessage(sender, "Player " + targetPlayer + " has not claimed any days yet.");
        else
            Messages.sendMessage(sender,"Last claimed day for " + targetPlayer + " is " + lastClaimedDay);

        return 1;
    }

    public static int SetVipExecutor(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        // Check permission
        if (!sender.hasPermission("fdailyrewards.admin.setvip")) {
            sender.sendMessage("You do not have permission to use this command.");
            return 0;
        }

        var targetPlayer = ctx.getArgument("player", String.class);
        var uuid = Bukkit.getOfflinePlayer(targetPlayer).getUniqueId();

        var extended = DatabaseManager.makePlayerVip(uuid);


        if (extended)
            Messages.sendMessage(sender, "Player " + targetPlayer + " is already VIP for the current month, VIP status has been extended.");
        else
            Messages.sendMessage(sender,"VIP Status set to true for " + targetPlayer + " in current month");

        return 1;
    }

    public static int RemoveVipExecutor(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        // Check permission
        if (!sender.hasPermission("fdailyrewards.admin.setvip")) {
            sender.sendMessage("You do not have permission to use this command.");
            return 0;
        }

        var targetPlayer = ctx.getArgument("player", String.class);
        var month = ctx.getArgument("month", Integer.class);
        var uuid = Bukkit.getOfflinePlayer(targetPlayer).getUniqueId();

        var removed = DatabaseManager.removePlayerVip(uuid, month);


        if (removed)
            Messages.sendMessage(sender, "Player " + targetPlayer + " VIP status has been removed for month " + month);
        else
            Messages.sendMessage(sender,"Player " + targetPlayer + " does not have VIP status for month " + month);

        return 1;
    }

    public static int UnlockMissedExecutor(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();
        // Check permission
        if (!sender.hasPermission("fdailyrewards.admin.unlockmissed")) {
            sender.sendMessage("You do not have permission to use this command.");
            return 0;
        }

        var targetPlayer = ctx.getArgument("player", String.class);
        var player = Bukkit.getPlayerExact(targetPlayer);
        // Check online status
        if (player == null) {
            Messages.sendMessage(sender, "Player " + targetPlayer + " is not currently online.");
            return 0;
        }

        // Get missed days
        var uuid = Bukkit.getOfflinePlayer(targetPlayer).getUniqueId();
        var missedDays = DatabaseManager.getMissedDays(uuid);

        // Unlock missed days, where allowed
        int unlockedDays = 0;
        var isVip = DatabaseManager.isVipPlayer(uuid);
        for (var day : missedDays) {
            if (day.vip && !isVip)
                continue; // skip VIP rewards for non-VIP players

            DatabaseManager.setRewardClaimed(player.getUniqueId(), LocalDateTime.now(), day.day);
            MenuListener.executeCommands(day, player);
            unlockedDays++;
        }

        Messages.sendMessage(sender,"Unlocked " + unlockedDays + " for player " + targetPlayer);
        Messages.sendMessage(player, "Odemčeno dní: " + unlockedDays);
        player.playSound(player.getLocation(), "thecivia:thecivia.sound.34", 1.0f, 1.0f);

        return 1;
    }
}
