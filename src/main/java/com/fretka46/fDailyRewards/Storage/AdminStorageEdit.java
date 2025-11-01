package com.fretka46.fDailyRewards.Storage;

import com.fretka46.fDailyRewards.Utils.Log;
import org.bukkit.Bukkit;

import java.sql.SQLException;
import java.util.UUID;

public class AdminStorageEdit {

    public static int resetDay(String nickname, Integer day) {
        var uuid = Bukkit.getOfflinePlayer(UUID.fromString(nickname)).getUniqueId();

        try (var ps = DatabaseManager.Connection.prepareStatement("DELETE FROM rewards_claimed WHERE uuid = ? AND day = ?;")) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, day);

            return ps.executeUpdate();
        } catch (SQLException ex) {
            Log.severe("Database error while setting day: " + ex.getMessage());
            return 0;
        }
    }

    public static int getLastClaimedDay(String uuid) {

        try (var ps = DatabaseManager.Connection.prepareStatement("SELECT day FROM rewards_claimed WHERE uuid = ? ORDER BY day DESC LIMIT 1;")) {
            ps.setString(1, uuid);

            try (var rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("day");
                } else {
                    return 0; // No days claimed
                }
            }
        } catch (SQLException ex) {
            Log.severe("Database error while getting last claimed day: " + ex.getMessage());
            return 0;
        }
    }
}
