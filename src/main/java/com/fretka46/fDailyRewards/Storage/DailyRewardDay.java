package com.fretka46.fDailyRewards.Storage;

public class DailyRewardDay {
    /**
     * Day of the month (1-31)
     */
    public final int day;
    /**
     * Is this reward only for VIP players
     */
    public final boolean vip;
    /**
     * Item which will be displayed in GUI
     */
    public final DailyRewardItem item;
    /**
     * List of commands which will be executed when player claims the reward
     */
    public final String[] commands;

    public DailyRewardDay(int day, boolean vip, DailyRewardItem item, String[] commands) {
        this.day = day;
        this.vip = vip;
        this.item = item;
        this.commands = commands;
    }
}
