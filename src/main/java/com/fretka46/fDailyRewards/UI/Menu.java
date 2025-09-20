package com.fretka46.fDailyRewards.UI;

import com.fretka46.fDailyRewards.FDailyRewards;
import com.fretka46.fDailyRewards.Storage.ConfigManager;
import com.fretka46.fDailyRewards.Storage.DailyRewardDay;
import com.fretka46.fDailyRewards.Storage.DailyRewardItem;
import com.fretka46.fDailyRewards.Storage.DatabaseManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.util.*;

/**
 * Custom Inventory Holder for the Daily Rewards menu.
 * - Keeps an internal slot->day mapping so listeners can easily resolve clicks.
 * - Renders a gray border with a bottom-center info slot.
 * - Fills the inner 7x4 grid from the config (XY 1:1 = top-left inner corner).
 * - Built per-player to allow dynamic, personalized content per open.
 */
public class Menu implements InventoryHolder {
    // Inventory constants
    private static final int SIZE = 54;      // 9x6
    private static final int WIDTH = 9;
    public static final MiniMessage MINI_MESSAGE = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage();

    private final Inventory inventory;
    private final Map<Integer, Integer> slotToDay = new HashMap<>(); // slot index -> day (1..31)
    private final Player viewer; // player for whom this menu was generated

    /**
     * Always create a fresh instance per open to ensure per-player dynamic content.
     */
    public Menu(Player viewer) throws SQLException {
        this.viewer = viewer;
        var plugin = FDailyRewards.getPlugin(FDailyRewards.class);
        String title = plugin.getConfig().getString("menu_title", "Daily Rewards");
        // Create inventory with a component title (Paper API)
        this.inventory = Bukkit.createInventory(this, SIZE, Component.text(title));
        // Render static frame and info slot
        buildBorder();
        placeInfo(plugin.getConfig().getString("info_title", "Info"), plugin.getConfig().getString("info_lore", "There you can get rewards"));
        // Fill content from config for this viewer
        fillDaysFromConfig(viewer);
    }

    @Override
    public Inventory getInventory() {
        return this.inventory;
    }

    /**
     * Convenience: open a fresh menu for the given player (always re-generated).
     */
    public static void openFor(Player player) {
        try {
            new Menu(player).open();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Open this already-built menu for its viewer.
     */
    public void open() {
        viewer.openInventory(this.inventory);
    }

    /**
     * Get day number (1..31) at a given raw slot or null if none.
     */
    public Integer getDayAt(int rawSlot) {
        return slotToDay.get(rawSlot);
    }

    // ------------------------ rendering ------------------------ //

    private void buildBorder() {
        ItemStack pane = namedItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        int height = SIZE / WIDTH;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < WIDTH; col++) {
                boolean edge = row == 0 || row == height - 1 || col == 0 || col == WIDTH - 1;
                if (!edge) continue;
                // Skip bottom center (info slot)
                if (row == height - 1 && col == WIDTH / 2) continue; // slot 49 for 9x6
                int slot = row * WIDTH + col;
                inventory.setItem(slot, pane);
            }
        }
    }

    private void placeInfo(String title, String lore) {
        int height = SIZE / WIDTH;
        int infoSlot = (height - 1) * WIDTH + (WIDTH / 2); // 49 in 9x6
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(Component.text(title == null ? "Info" : title));
        if (lore != null && !lore.isEmpty()) {
            List<Component> lines = splitLore(lore);
            meta.lore(lines);
        }
        info.setItemMeta(meta);
        inventory.setItem(infoSlot, info);
    }

    private void fillDaysFromConfig(Player player) throws SQLException {
        // Compute inner 7x4 grid slots once
        List<Integer> contentSlots = getInnerSlots(SIZE);
        int claimedDays = DatabaseManager.getTotalClaims(player.getUniqueId());
        int idx = 0;
        for (int day = 1; day <= 31; day++) {
            DailyRewardDay reward = ConfigManager.getRewardForDay(day);
            if (reward == null || reward.item == null) continue;
            if (idx >= contentSlots.size()) break; // No paging yet
            int slot = contentSlots.get(idx++);

            var config = FDailyRewards.getPlugin(FDailyRewards.class).getConfig();
            if (day <= claimedDays) {
                // Already claimed
                var claimeeItem = ConfigManager.readItem(config.getConfigurationSection("reward_claimed_item"));
                if (claimeeItem != null) reward = new DailyRewardDay(day, reward.vip, claimeeItem, reward.commands);
            } else if (day == claimedDays + 1) {
                // Next available
                // Add enchant glow + lore line
                reward.item.lore = (reward.item.lore == null ? "" : reward.item.lore + "\n") + config.getString("reward_claim_available_loreline", "ERR: reward_claim_available_loreline");
                var stack = toItemStack(reward.item);
                stack.addUnsafeEnchantment(Enchantment.UNBREAKING, 1); // just to get the glow effect
                inventory.setItem(slot, stack);
                slotToDay.put(slot, day);
                continue;
            } else if (reward.vip && !player.hasPermission("survival.premium.dailylogin")) {
                // VIP reward but player lacks permission
                // Add lore line
                reward.item.lore = (reward.item.lore == null ? "" : reward.item.lore + "\n") + config.getString("reward_vip_locked_loreline", "ERR: reward_vip_locked_loreline");
            }

            inventory.setItem(slot, toItemStack(reward.item));
            slotToDay.put(slot, day);
        }
    }

    // ------------------------ helpers ------------------------ //

    private static ItemStack toItemStack(DailyRewardItem cfg) {
        Material mat = cfg.material != null ? cfg.material : Material.PAPER;
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        if (cfg.customModelData > 0) meta.setCustomModelData(cfg.customModelData);
        if (cfg.name != null && !cfg.name.isEmpty()) meta.displayName(MINI_MESSAGE.deserialize(cfg.name));
        if (cfg.lore != null && !cfg.lore.isEmpty()) meta.lore(splitLore(cfg.lore));
        // Apply tooltip style if present (value like "minecraft:thecivia/legendary")
        if (cfg.tooltipStyle != null && !cfg.tooltipStyle.isBlank()) {
            NamespacedKey key = NamespacedKey.fromString(cfg.tooltipStyle);
            if (key != null) {
                try {
                    meta.setTooltipStyle(key);
                } catch (NoSuchMethodError ignored) {
                    // Running on an older API without tooltipStyle support
                }
            }
        }
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack namedItem(Material material, String name) {
        ItemStack it = new ItemStack(material);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(MINI_MESSAGE.deserialize(name));
        it.setItemMeta(meta);
        return it;
    }

    private static List<Component> splitLore(String text) {
        List<Component> list = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) list.add(MINI_MESSAGE.deserialize(line));
        return list;
    }

    /**
     * Returns the slot indices for the inner area (excluding the outer border).
     * For a 9x6 inventory this yields 7x4 = 28 slots in row-major order.
     */
    private static List<Integer> getInnerSlots(int size) {
        int width = WIDTH;
        int height = size / width;
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row < height - 1; row++) {
            for (int col = 1; col < width - 1; col++) {
                slots.add(row * width + col);
            }
        }
        return slots;
    }
}
