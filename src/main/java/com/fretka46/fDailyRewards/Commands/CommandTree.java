package com.fretka46.fDailyRewards.Commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Locale;

public final class CommandTree {

    private static final SuggestionProvider<CommandSourceStack> PLAYER_SUGGESTIONS = (ctx, builder) -> {
        String remaining = builder.getRemainingLowerCase();
        Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(remaining))
                .forEach(builder::suggest);
        return builder.buildFuture();
    };

    public static LiteralCommandNode<CommandSourceStack> buildRoot() {
        return Commands.literal("fdailyrewards")
                .executes(MainCommand::executor)
                .then(Commands.literal("reload")
                        .executes(Reload::executor))
                .then(Commands.literal("admin")
                        .then(Commands.literal("resetday")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests(PLAYER_SUGGESTIONS)
                                        .then(Commands.argument("day", IntegerArgumentType.integer())
                                                .executes(Admin::ResetDayExecutor))))

                        .then(Commands.literal("giftday")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests(PLAYER_SUGGESTIONS)
                                        .then(Commands.argument("day", IntegerArgumentType.integer())
                                                .executes(Admin::GiftDayExecutor))))

                        .then(Commands.literal("checkday")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests(PLAYER_SUGGESTIONS)
                                                .executes(Admin::CheckDayExecutor)))

                        .then(Commands.literal("setday")
                                .then(Commands.argument("player", StringArgumentType.word())
                                        .suggests(PLAYER_SUGGESTIONS)
                                        .then(Commands.argument("day", IntegerArgumentType.integer())
                                                .executes(Admin::SetDayExecutor)))))
                .build();
    }
}