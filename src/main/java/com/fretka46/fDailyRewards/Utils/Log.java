package com.fretka46.fDailyRewards.Utils;

import com.fretka46.fDailyRewards.FDailyRewards;
import org.bukkit.command.CommandSender;
import java.util.logging.Logger;

public class Log {
    private static final Logger logger = FDailyRewards.getPlugin(FDailyRewards.class).getLogger();

    public static void info(String message) {
        logger.info(message);
    }

    public static void warning(String message) {
        logger.warning(message);
    }

    public static void severe(String message) {
        logger.severe(message);
    }

    public static void debug(String message) {
        if (FDailyRewards.getPlugin(FDailyRewards.class).getConfig().getBoolean("debug", false)) {
            logger.info("[DEBUG] " + message);
        }
    }
}