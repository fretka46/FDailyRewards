package com.fretka46.fDailyRewards.Utils;

import com.fretka46.fDailyRewards.FDailyRewards;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class DatabaseManager {
    public static Connection Connection;

    public static Connection connect() throws SQLException, ClassNotFoundException {
        // Create directory if not exist
        java.io.File dir = new java.io.File("plugins/DailyRewards");
        if (!dir.exists()) {
            dir.mkdirs();
            Log.info("Creating database directory: " + dir.getAbsolutePath());
        }

        Class.forName("org.sqlite.JDBC");
        var connection = DriverManager.getConnection("jdbc:sqlite:plugins/DailyRewards/database.db");

        // Create default tables if not exist
        var ps = connection.prepareStatement("CREATE TABLE IF NOT EXISTS rewards_claimed (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uuid TEXT NOT NULL," +
                "date TEXT NOT NULL" +
                ");");
        ps.executeUpdate();
        ps.close();

        return connection;
    }


    public static void setRewardClaimed(UUID uuid, LocalDateTime time) throws SQLException {
        var date = getResetDateTime(time).toLocalDate();
        var ps = Connection.prepareStatement("INSERT INTO rewards_claimed (uuid, date) VALUES (?, ?)");
        ps.setString(1, uuid.toString());
        ps.setString(2, date.toString());
        ps.executeUpdate();

        ps.close();
    }

    public static boolean hasClaimedReward(UUID uuid, LocalDateTime time) throws SQLException {
        var date = getResetDateTime(time).toLocalDate();
        var ps = Connection.prepareStatement("SELECT COUNT(*) FROM rewards_claimed WHERE uuid = ? AND date = ?");
        ps.setString(1, uuid.toString());
        ps.setString(2, date.toString());
        var rs = ps.executeQuery();
        boolean claimed = false;
        if (rs.next()) {
            claimed = rs.getInt(1) > 0;
        }

        rs.close();
        ps.close();

        return claimed;
    }

    /**
     * Get the reset date time based on the configured reset time. (DRY)
     */
    private static LocalDateTime getResetDateTime(LocalDateTime time) {
        var config_reset_time = FDailyRewards.getPlugin(FDailyRewards.class).getConfig().getString("day_reset_time", "04:15");;

        // If check is before reset time, use previous day
        LocalDateTime resetDateTime = time.withHour(Integer.parseInt(config_reset_time.split(":")[0]))
                                           .withMinute(Integer.parseInt(config_reset_time.split(":")[1]))
                                           .withSecond(0)
                                           .withNano(0);
        if (time.isBefore(resetDateTime)) {
            resetDateTime = resetDateTime.minusDays(1);
        }
        return resetDateTime;
    }


}
