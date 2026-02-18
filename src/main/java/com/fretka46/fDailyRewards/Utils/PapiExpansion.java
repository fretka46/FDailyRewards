package com.fretka46.fDailyRewards.Utils;

import com.fretka46.fDailyRewards.FDailyRewards;
import com.fretka46.fDailyRewards.Storage.DatabaseManager;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class PapiExpansion extends PlaceholderExpansion {

    private final FDailyRewards plugin;

    public PapiExpansion(FDailyRewards plugin) {
        this.plugin = plugin;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return String.join(", ", plugin.getDescription().getAuthors()); //
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "fdr";
    }

    @Override
    @NotNull
    public String getVersion() {
        return plugin.getDescription().getVersion(); //
    }

    @Override
    public boolean persist() {
        return true; //
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        // Currently claimed day: %fdr_claimed_day%
        if (params.matches("claimed_day")) {
            int day = DatabaseManager.getLastClaimedDay(player.getUniqueId());
            return String.valueOf(day);
        }
        return null;
    }
}