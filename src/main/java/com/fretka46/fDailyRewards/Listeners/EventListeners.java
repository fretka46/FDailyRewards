package com.fretka46.fDailyRewards.Listeners;

import com.fretka46.fDailyRewards.UI.Menu;
import com.fretka46.fDailyRewards.Utils.Log;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

public class EventListeners implements Listener {

    @EventHandler
    public void onPlayerInterract(PlayerInteractEvent ev) {
        // Open menu

        Log.info("Opening menu");
        Menu.openFor(ev.getPlayer());
    }
}
