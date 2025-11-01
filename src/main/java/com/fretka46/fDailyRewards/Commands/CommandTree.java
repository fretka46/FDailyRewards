package com.fretka46.fDailyRewards.Commands;

import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public final class CommandTree {
    public static LiteralCommandNode<CommandSourceStack> buildRoot() {
        return Commands.literal("fdailyrewards")
                .executes(MainCommand::executor)
                .then(Commands.literal("reload")
                        .executes(Reload::executor))


                .build();
    }
}