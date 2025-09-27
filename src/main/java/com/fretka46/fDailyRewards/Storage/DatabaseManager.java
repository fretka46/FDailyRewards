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
        if (dir.mkdirs())
            Log.info("Creating database directory: " + dir.getAbsolutePath());

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

        ps = connection.prepareStatement("CREATE TABLE IF NOT EXISTS daily_logins (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "uuid TEXT NOT NULL," +
                "date TEXT NOT NULL" +
                ");");

        ps.executeUpdate();
        ps.close();

        // Remove old entries (older than 60 days)
        ps = connection.prepareStatement("DELETE FROM rewards_claimed WHERE date < date('now', '-30 days');");
        int deleted = ps.executeUpdate();
        if (deleted > 0) {
            Log.info("Cleaned up " + deleted + " old reward claim records.");
        }

        ps = connection.prepareStatement("DELETE FROM daily_logins WHERE date < date('now', '-30 days');");
        deleted = ps.executeUpdate();
        if (deleted > 0) {
            Log.info("Cleaned up " + deleted + " old daily login records.");
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

    public static boolean hasClaimedRewardInTwoDays(UUID uuid, LocalDateTime time) {
        try {
            var date = getResetDateTime(time).toLocalDate();
            var ps = Connection.prepareStatement("SELECT COUNT(*) FROM rewards_claimed WHERE uuid = ? AND date >= ?");
            ps.setString(1, uuid.toString());
            ps.setString(2, date.minusDays(1).toString());

            var rs = ps.executeQuery();
            boolean claimed = false;
            if (rs.next()) {
                claimed = rs.getInt(1) > 0;
            }
            rs.close();
            ps.close();
            return claimed;
        } catch (SQLException ex) {
            Log.severe("Database error while checking reward claim: " + ex.getMessage());
            return true;
        }
    }

    /**
     * Get the next day the player should claim, skipping VIP days if needed
     * @param uuid UUID of the player
     * @param skipVipDays whether to skip VIP days
     * @return The next day to claim (1-based). If all days are claimed, returns maxDay + 1
     */
    public static int getNextDayToClaim(UUID uuid, boolean skipVipDays) {
        try {

            var now = LocalDateTime.now();
            var monthStr = String.format("%04d-%02d", now.getYear(), now.getMonthValue());

            // Get all claimed days
            var ps = Connection.prepareStatement(
                    "SELECT day FROM rewards_claimed WHERE uuid = ? AND strftime('%Y-%m', date) = ?"
            );
            ps.setString(1, uuid.toString());
            ps.setString(2, monthStr);
            var rs = ps.executeQuery();

            java.util.Set<Integer> claimedDays = new java.util.HashSet<>();
            while (rs.next()) {
                claimedDays.add(rs.getInt(1));
            }
            rs.close();
            ps.close();

            int maxDay = ConfigManager.getAllRewards().size();

            for (int day = 1; day <= maxDay; day++) {
                var reward = ConfigManager.getRewardForDay(day);
                if (claimedDays.contains(day)) continue;
                if (skipVipDays && reward != null && reward.vip) continue;

                return day;
            }

            // If all days are claimed, return maxDay + 1 (indicating all claimed)
            return maxDay + 1;

        } catch (SQLException ex) {
            Log.severe("Database error while getting next day to claim: " + ex.getMessage());
            return 1;
        }
    }

   public static boolean hasClaimedDay(UUID uuid, int day) {
       try {
           var now = LocalDateTime.now();
           var monthStr = String.format("%04d-%02d", now.getYear(), now.getMonthValue());

           var ps = Connection.prepareStatement(
               "SELECT COUNT(*) FROM rewards_claimed WHERE uuid = ? AND day = ? AND strftime('%Y-%m', date) = ?"
           );
           ps.setString(1, uuid.toString());
           ps.setInt(2, day);
           ps.setString(3, monthStr);

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

    public static boolean hasLoggedInToday(UUID uuid, LocalDateTime time) {
        try {
            var date = getResetDateTime(time).toLocalDate();
            var ps = Connection.prepareStatement("SELECT COUNT(*) FROM daily_logins WHERE uuid = ? AND date = ?");
            ps.setString(1, uuid.toString());
            ps.setString(2, date.toString());

            var rs = ps.executeQuery();
            boolean loggedIn = false;
            if (rs.next())
                loggedIn = rs.getInt(1) > 0;

            rs.close();
            ps.close();
            return loggedIn;
        } catch (SQLException ex) {
            Log.severe("Database error while checking login: " + ex.getMessage());
            return true;
        }
    }

    public static void setLoggedInToday(UUID uuid, LocalDateTime time) {
        try {
            var date = getResetDateTime(time).toLocalDate();
            var ps = Connection.prepareStatement("INSERT INTO daily_logins (uuid, date) VALUES (?, ?)");
            ps.setString(1, uuid.toString());
            ps.setString(2, date.toString());
            ps.executeUpdate();

            ps.close();
        } catch (SQLException ex) {
            Log.severe("Database error while setting login: " + ex.getMessage());
        }
    }
}