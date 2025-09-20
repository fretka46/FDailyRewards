package com.fretka46.fDailyRewards.Utils;

import com.fretka46.fDailyRewards.FDailyRewards;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public class Messages {

    private static final String PREFIX = FDailyRewards.getPlugin(FDailyRewards.class).getConfig().getString("message_prefix");
    public static final MiniMessage MINI_MESSAGE = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

    public static void sendTranslatedMessageTo(Player player, String path) {
        String message = FDailyRewards.getPlugin(
                FDailyRewards.class).getConfig().getString(path, "ERR: " + path + " not found in config.");

        player.sendMessage(MINI_MESSAGE.deserialize(PREFIX + " " + message));
    }
}
