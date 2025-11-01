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
}
