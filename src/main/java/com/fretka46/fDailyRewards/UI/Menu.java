package com.fretka46.fDailyRewards.UI;

import com.fretka46.fDailyRewards.FDailyRewards;
import com.fretka46.fDailyRewards.Storage.ConfigManager;
import com.fretka46.fDailyRewards.Storage.DailyRewardDay;
import com.fretka46.fDailyRewards.Storage.DailyRewardItem;
import com.fretka46.fDailyRewards.Storage.DatabaseManager;
import com.fretka46.fDailyRewards.Utils.Log;
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
import org.jetbrains.annotations.NotNull;

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
        this.inventory = Bukkit.createInventory(this, SIZE, MINI_MESSAGE.deserialize(title));

        // Fill content from config for this viewer
        fillDaysFromConfig(viewer);
    }

    @Override
    public @NotNull Inventory getInventory() {
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
                // Skip bottom center (info slot) !!!!
                if (row == height - 1 && col == WIDTH / 2) continue; // slot 49 for 9x6
                int slot = row * WIDTH + col;
                inventory.setItem(slot, pane);
            }
        }
    }

    private void fillDaysFromConfig(Player player) throws SQLException {
        List<Integer> contentSlots = getInnerSlots(SIZE);
        int claimedDays = DatabaseManager.getTotalClaims(player.getUniqueId());
        var localTime = java.time.LocalDateTime.now();
        int idx = 0;

        for (int day = 1; day <= 31; day++) {
            DailyRewardDay reward = ConfigManager.getRewardForDay(day);
            if (reward == null || reward.item == null) continue;

            int slot;

            // 48 49 50
            switch (day){
                case 29:
                    if (ConfigManager.getRewardForDay(31) != null) slot = 48;
                    else if (ConfigManager.getRewardForDay(30) != null) slot = 48;
                    else slot = 49;
                    break;
                case 30:
                    if (ConfigManager.getRewardForDay(31) == null) slot = 50;
                    else slot = 49;
                    break;
                case 31:
                    slot = 50;
                    break;
                default:
                    if (idx >= contentSlots.size()) {
                        Log.warning("Not enough slots in the menu to display all rewards! Skipping day " + day);
                        continue;
                    }
                    slot = contentSlots.get(idx++);
            }
            var config = FDailyRewards.getPlugin(FDailyRewards.class).getConfig();

            ItemStack stack;

            // Already claimed
            if (DatabaseManager.hasClaimedDay(player.getUniqueId(), day)) {
                var claimedItem = ConfigManager.readItem(config.getConfigurationSection("reward_claimed_item"));
                stack = toItemStack(claimedItem != null ? claimedItem : reward.item);
                inventory.setItem(slot, stack);
                slotToDay.put(slot, day);
                continue;
            }

            // VIP locked
            if (reward.vip && !player.hasPermission("survival.premium.dailylogin")) {
                stack = toItemStack(reward.item);
                stack = appendLore(stack, config.getString("reward_vip_locked_loreline", "ERR: reward_vip_locked_loreline"));
                inventory.setItem(slot, stack);
                slotToDay.put(slot, day);
                continue;
            }

            // VIP skip
            if (day <= localTime.getDayOfMonth() && reward.vip) {
                stack = toItemStack(reward.item);
                stack = appendLore(stack, config.getString("reward_vip_can_skip", "ERR: reward_vip_can_skip"));
                stack.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
                ItemMeta meta = stack.getItemMeta();
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                stack.setItemMeta(meta);
                inventory.setItem(slot, stack);
                slotToDay.put(slot, day);
                continue;
            }

            // This day
            if (day == DatabaseManager.getNextDayToClaim(player.getUniqueId())) {
                if (DatabaseManager.hasClaimedRewardInLastDay(player.getUniqueId(), java.time.LocalDateTime.now())) {
                    // Today already claimed (24h cooldown) -> available tomorrow
                    stack = overrideLore(toItemStack(reward.item), config.getString("reward_claim_available_tommorow_loreline"));
                    inventory.setItem(slot, stack);
                    slotToDay.put(slot, day);
                    continue;
                } else {
                    // Today is available to claim now - highlight it
                    stack = appendLore(toItemStack(reward.item), config.getString("reward_claim_available_loreline", "ERR: reward_claim_available_loreline"));
                    stack.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
                    ItemMeta meta = stack.getItemMeta();
                    meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                    stack.setItemMeta(meta);
                    inventory.setItem(slot, stack);
                    slotToDay.put(slot, day);
                    continue;
                }
            }

            stack = toItemStack(reward.item);
            inventory.setItem(slot, stack);
            slotToDay.put(slot, day);
        }
    }

    private static ItemStack appendLore(ItemStack stack, String extra) {
        if (extra == null || extra.isBlank()) return stack;
        ItemMeta meta = stack.getItemMeta();
        List<Component> lore = meta.lore() != null ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.addAll(splitLore(extra));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack overrideLore(ItemStack stack, String newLore) {
        ItemMeta meta = stack.getItemMeta();
        meta.lore(newLore == null || newLore.isBlank() ? Collections.emptyList() : splitLore(newLore));
        stack.setItemMeta(meta);
        return stack;
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
