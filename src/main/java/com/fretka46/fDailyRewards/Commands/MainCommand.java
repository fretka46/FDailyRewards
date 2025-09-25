package com.fretka46.fDailyRewards.Commands;

import com.fretka46.fDailyRewards.UI.Menu;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class MainCommand implements BasicCommand {

    @Override
    public void execute(CommandSourceStack command, String[] args) {

        var sender = command.getSender();

        // Check if sender is player
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return;
        }

        // Open menu for the player
        Menu.openFor(player);
    }
}
