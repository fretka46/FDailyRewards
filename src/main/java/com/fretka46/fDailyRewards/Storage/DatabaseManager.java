package com.fretka46.fDailyRewards.Storage;

import com.fretka46.fDailyRewards.FDailyRewards;
import com.fretka46.fDailyRewards.Utils.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class DatabaseManager {
    public static Connection Connection;

    public static Connection connect() throws SQLException, ClassNotFoundException {
        // Create directory if not exist
        java.io.File dir = new java.io.File("plugins/FDailyRewards");
        if (!dir.exists()) {
            dir.mkdirs();
            Log.info("Creating database directory: " + dir.getAbsolutePath());
        }

        Class.forName("org.sqlite.JDBC");
        var connection = DriverManager.getConnection("jdbc:sqlite:plugins/FDailyRewards/database.db");

        // Create default tables if not exist
        var ps = connection.prepareStatement("CREATE TABLE IF NOT EXISTS rewards_claimed (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uuid TEXT NOT NULL," +
                "date TEXT NOT NULL," +
                "day INTEGER NOT NULL" +
                ");");
        ps.executeUpdate();
        ps.close();

        // Remove old entries (older than 60 days)
        ps = connection.prepareStatement("DELETE FROM rewards_claimed WHERE date < date('now', '-60 days');");
        int deleted = ps.executeUpdate();
        if (deleted > 0) {
            Log.info("Cleaned up " + deleted + " old reward claim records.");
        }
        ps.close();

        return connection;
    }

    /**
     * Get total number of claimed rewards by the player this month
     */
    public static int getTotalClaims(UUID uuid) {
        try {
            var ps = Connection.prepareStatement("SELECT COUNT(*) FROM rewards_claimed WHERE uuid = ? AND strftime('%Y-%m', date) = strftime('%Y-%m', ?);");
            ps.setString(1, uuid.toString());
            ps.setString(2, LocalDateTime.now().toLocalDate().toString());
            var rs = ps.executeQuery();
            int count = 0;
            if (rs.next()) {
                count = rs.getInt(1);
            }

            rs.close();
            ps.close();

            return count;
        }
        catch (SQLException ex) {
            Log.severe("Database error while getting total claims: " + ex.getMessage());
            return 31;
        }
    }


    public static void setRewardClaimed(UUID uuid, LocalDateTime time, int day) {
        try {
            var date = getResetDateTime(time).toLocalDate();
            var ps = Connection.prepareStatement("INSERT INTO rewards_claimed (uuid, date, day) VALUES (?, ?, ?)");
            ps.setString(1, uuid.toString());
            ps.setString(2, date.toString());
            ps.setInt(3, day);
            ps.executeUpdate();

            ps.close();
        }
        catch (SQLException ex) {
            Log.severe("Database error while setting reward claim: " + ex.getMessage());
        }
    }

    /**
     * Checks if player already claimed reward to specific date
     */
    public static boolean hasClaimedRewardInLastDay(UUID uuid, LocalDateTime time) {

        try {
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
        catch (SQLException ex) {
            Log.severe("Database error while checking reward claim: " + ex.getMessage());
            return true;
        }
    }

    public static int getNextDayToClaim(UUID uuid) {
        try {
            var ps = Connection.prepareStatement(
                    "SELECT day FROM rewards_claimed WHERE uuid = ? ORDER BY day ASC"
            );
            ps.setString(1, uuid.toString());
            var rs = ps.executeQuery();

            int expectedDay = 1;
            while (rs.next()) {
                int claimedDay = rs.getInt(1);

                if (claimedDay < expectedDay) {
                    continue; // duplicate or out of order entry, ignore
                }
                if (claimedDay == expectedDay) {
                    expectedDay++;
                    continue;
                }
                break; // found a gap
            }

            rs.close();
            ps.close();
            return expectedDay;
        } catch (SQLException ex) {
            Log.severe("Database error while getting next day to claim: " + ex.getMessage());
            return 1; // fallback
        }
    }

    public static boolean hasClaimedDay(UUID uuid, int day) {

        try {
            var ps = Connection.prepareStatement("SELECT COUNT(*) FROM rewards_claimed WHERE uuid = ? AND day = ?");
            ps.setString(1, uuid.toString());
            ps.setInt(2, day);

            var rs = ps.executeQuery();
            boolean claimed = false;
            if (rs.next()) {
                claimed = rs.getInt(1) > 0;
            }
            rs.close();
            ps.close();
            return claimed;
        }
        catch (SQLException ex) {
            Log.severe("Database error while checking day claim: " + ex.getMessage());
            return true;
        }
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
