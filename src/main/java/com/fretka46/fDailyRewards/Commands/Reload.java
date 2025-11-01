package com.fretka46.fDailyRewards.Commands;

import com.fretka46.fDailyRewards.FDailyRewards;
import com.fretka46.fDailyRewards.Storage.ConfigManager;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class Reload {

    public static int executor(CommandContext<CommandSourceStack> ctx) {
        var sender = ctx.getSource().getSender();

        // Check permissions
        if (!sender.hasPermission("fdailyrewards.reload")) {
            sender.sendMessage("You do not have permission to use this command.");
            return 0;
        }

        // Reload the config

        var plugin = com.fretka46.fDailyRewards.FDailyRewards.getPlugin(FDailyRewards.class);
        plugin.reloadConfig();
        ConfigManager.reload(plugin);

        // Send confirmation message
        sender.sendMessage("Configuration reloaded successfully");
        return 1;
    }
}
