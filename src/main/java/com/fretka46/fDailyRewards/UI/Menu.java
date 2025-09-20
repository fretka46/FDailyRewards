package com.fretka46.fDailyRewards.UI;

import com.fretka46.fDailyRewards.FDailyRewards;
import com.fretka46.fDailyRewards.Storage.ConfigManager;
import com.fretka46.fDailyRewards.Storage.DailyRewardDay;
import com.fretka46.fDailyRewards.Storage.DailyRewardItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Menu {
    public static InventoryView getMenu(HumanEntity player) {
        var plugin = FDailyRewards.getPlugin(FDailyRewards.class);
        var title = plugin.getConfig().getString("menu.title", "Daily Rewards");

        InventoryView view = MenuType.GENERIC_9X6.builder()
                .title(Component.text(title))
                .build(player);

        Inventory inv = view.getTopInventory();

        // Rámeček: šedé sklo kolem, s výjimkou spodního středu (info)
        buildBorder(inv);
        placeInfo(inv, plugin.getConfig().getString("menu.infoTitle", "Informace"), plugin.getConfig().getString("menu.infoLore", "Klikni pro víc informací."));

        // Plnění odměn do vnitřní mřížky 7x4, zleva nahoře (XY 1:1)
        List<Integer> contentSlots = getInnerSlots(inv.getSize());
        int index = 0;
        for (int day = 1; day <= 31; day++) {
            DailyRewardDay reward = ConfigManager.getRewardForDay(day);
            if (reward == null || reward.item == null) continue;
            if (index >= contentSlots.size()) break; // zatím bez stránkování
            inv.setItem(contentSlots.get(index++), toItemStack(reward.item));
        }

        return view;
    }

    private static void buildBorder(Inventory inv) {
        int size = inv.getSize(); // očekáváme 54
        int width = 9;
        int height = size / width;
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        meta.displayName(Component.text(" "));
        pane.setItemMeta(meta);

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                boolean isEdge = row == 0 || row == height - 1 || col == 0 || col == width - 1;
                if (!isEdge) continue;
                // Info box
                if (row == height - 1 && col == width / 2) continue; // 9x6 -> slot 49
                int slot = row * width + col;
                inv.setItem(slot, pane);
            }
        }
    }

    private static void placeInfo(Inventory inv, String title, String lore) {
        int width = 9;
        int height = inv.getSize() / width;
        int infoSlot = (height - 1) * width + (width / 2);
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(Component.text(title == null ? "Info" : title));
        if (lore != null && !lore.isEmpty()) {
            List<Component> lines = new ArrayList<>();
            for (String line : splitLines(lore)) lines.add(Component.text(line));
            meta.lore(lines);
        }
        info.setItemMeta(meta);
        inv.setItem(infoSlot, info);
    }

    private static List<Integer> getInnerSlots(int size) {
        int width = 9;
        int height = size / width; // 6
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row < height - 1; row++) { // 1..4
            for (int col = 1; col < width - 1; col++) { // 1..7
                slots.add(row * width + col);
            }
        }
        return slots; // velikost 28
    }

    private static ItemStack toItemStack(DailyRewardItem cfg) {
        Material mat = cfg.material != null ? cfg.material : Material.PAPER;
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (cfg.customModelData > 0) meta.setCustomModelData(cfg.customModelData);
        if (cfg.name != null && !cfg.name.isEmpty()) meta.displayName(Component.text(cfg.name));
        if (cfg.lore != null && !cfg.lore.isEmpty()) {
            List<Component> lore = new ArrayList<>();
            for (String line : splitLines(cfg.lore)) lore.add(Component.text(line));
            meta.lore(lore);
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static List<String> splitLines(String text) {
        return Arrays.asList(text.split("\\r?\\n"));
    }
}
