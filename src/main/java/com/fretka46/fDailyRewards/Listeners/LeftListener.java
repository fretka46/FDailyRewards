package com.fretka46.fDailyRewards.Listeners;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import com.fretka46.fDailyRewards.Utils.Scheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LeftListener implements Listener {
    @EventHandler
    public void onPlayerLeft(PlayerConnectionCloseEvent ev) {
        Scheduler.cancelRewardMessage(ev.getPlayerUniqueId());
    }
}
