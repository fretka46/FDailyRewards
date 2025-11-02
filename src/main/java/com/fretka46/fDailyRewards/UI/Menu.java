package com.fretka46.fDailyRewards.UI;

import com.fretka46.fDailyRewards.FDailyRewards;
import com.fretka46.fDailyRewards.Storage.ConfigManager;
import com.fretka46.fDailyRewards.Storage.DailyRewardDay;
import com.fretka46.fDailyRewards.Storage.DailyRewardItem;
import com.fretka46.fDailyRewards.Storage.DatabaseManager;
import com.fretka46.fDailyRewards.Utils.Log;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;

import static com.fretka46.fDailyRewards.Storage.ConfigManager.parseDay;
import static com.fretka46.fDailyRewards.Storage.ConfigManager.readItem;

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
    private final Map<Integer, DailyRewardDay> slotToCustomItem = new HashMap<>(); // slot index -> custom item id
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

    public DailyRewardDay getCustomItemAt(int rawSlot) {
        return slotToCustomItem.get(rawSlot);
    }

    // ------------------------ rendering ------------------------ //

    private void fillDaysFromConfig(Player player) throws SQLException {
        var localTime = java.time.LocalDateTime.now();
        int nextDayToClaim = DatabaseManager.getNextDayToClaim(player.getUniqueId(), !player.hasPermission("survival.premium.dailylogin"));
        var config = FDailyRewards.getPlugin(FDailyRewards.class).getConfig();

        for (int day = 1; day <= 31; day++) {
            DailyRewardDay reward = ConfigManager.getRewardForDay(day);
            if (reward == null || reward.item == null) continue;


            boolean isVip = player.hasPermission("survival.premium.dailylogin");

            ItemStack stack;
            DailyRewardDay currentDayReward = ConfigManager.getRewardForDay(day);
            int slot = currentDayReward.inventorySlot;
            if (slot == -1) {
                Log.severe("Invalid slot " + slot + " in day " + day + " Skipping this reward.");
                continue;
            }


            // Already claimed
            if (DatabaseManager.hasClaimedDay(player.getUniqueId(), day)) {

                DailyRewardItem claimedItem;

                if (reward.vip) {
                    claimedItem = readItem(config.getConfigurationSection("reward_claimed_item_vip"));
                    assert claimedItem != null;
                    claimedItem.material = currentDayReward.item.material;
                }
                else
                    claimedItem = readItem(config.getConfigurationSection("reward_claimed_item"));

                assert claimedItem != null;
                claimedItem.name = currentDayReward.item.name;

                stack = toItemStack(claimedItem);
                inventory.setItem(slot, stack);
                slotToDay.put(slot, day);
                continue;
            }

            // VIP locked
            if (reward.vip && !isVip) {
                stack = toItemStack(reward.item);
                stack = appendLore(stack, config.getString("reward_vip_locked_loreline", "ERR: reward_vip_locked_loreline"));
                inventory.setItem(slot, stack);
                slotToDay.put(slot, day);
                continue;
            }

            // Skipped VIP but now VIP -> allow claim
            if (reward.vip && day < DatabaseManager.getNextDayToClaim(player.getUniqueId(), true)) {

                var claimAvailableSec = config.getConfigurationSection("reward_claim_available_item_vip");
                var claimAvailableItem = readItem(claimAvailableSec);

                assert claimAvailableItem != null;
                reward.item.material = claimAvailableItem.material;
                reward.item.customModelData = claimAvailableItem.customModelData;

                stack = appendLore(toItemStack(reward.item), config.getString("reward_vip_can_skip"));

                stack.addUnsafeEnchantment(Enchantment.UNBREAKING, 1);
                ItemMeta meta = stack.getItemMeta();
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
                stack.setItemMeta(meta);

                inventory.setItem(slot, stack);
                slotToDay.put(slot, day);
                continue;
            }

            // This day
            if (day == nextDayToClaim) {
                var yesterdayReward = ConfigManager.getRewardForDay(day - 1);
                if (!isVip && yesterdayReward != null && yesterdayReward.vip && DatabaseManager.hasClaimedRewardInTwoDays(player.getUniqueId(), localTime)) {
                    // Yesterday was VIP and player is not VIP -> available tomorrow

                    // Check if already one day passed
                    if (DatabaseManager.hasClaimedRewardInLastDay(player.getUniqueId(), localTime.minusDays(1)))
                        stack = appendLore(toItemStack(reward.item), config.getString("reward_claim_available_tommorow_loreline"));
                    else
                        stack = appendLore(toItemStack(reward.item), config.getString("reward_claim_available_2_days_loreline"));

                    inventory.setItem(slot, stack);
                    slotToDay.put(slot, day);
                    continue;

                } else if (DatabaseManager.hasClaimedRewardInLastDay(player.getUniqueId(), java.time.LocalDateTime.now())) {
                    // Today already claimed (24h cooldown) -> available tomorrow
                    stack = appendLore(toItemStack(reward.item), config.getString("reward_claim_available_tommorow_loreline"));
                    inventory.setItem(slot, stack);
                    slotToDay.put(slot, day);
                    continue;

                } else {
                    // Today is available to claim now - highlight it


                    ConfigurationSection claimAvailableSec;
                    if (reward.vip)
                        claimAvailableSec = config.getConfigurationSection("reward_claim_available_item_vip");
                    else
                        claimAvailableSec = config.getConfigurationSection("reward_claim_available_item");

                    var claimAvailableItem = readItem(claimAvailableSec);

                    assert claimAvailableItem != null;
                    reward.item.material = claimAvailableItem.material;
                    reward.item.customModelData = claimAvailableItem.customModelData;

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

        // Custom items
        var customItems = config.getConfigurationSection("non_functional_items");
        if (customItems != null) {
            for (String key : customItems.getKeys(false)) {
                var day = parseDay(key);
                if (day == null) continue;

                var sec = customItems.getConfigurationSection(key);
                if (sec == null) continue;

                int inventorySlot = sec.getInt("inventorySlot", -1);
                var isVip = sec.getBoolean("vip", false);
                var itemSec = sec.getConfigurationSection("item");
                DailyRewardItem item = readItem(itemSec);
                List<String> cmdList = Objects.requireNonNull(itemSec).getStringList("commands");
                String[] commands = cmdList.toArray(new String[0]);

                assert item != null;
                var stack = toItemStack(item);
                inventory.setItem(inventorySlot, stack);
                slotToCustomItem.put(inventorySlot, new DailyRewardDay(-1, isVip, item, commands, inventorySlot));
            }
        }



        // Fill border and empty slots with filler
        if (config.getBoolean("fill_empty_slots", true)) {
            Material fillerMat = Material.GRAY_STAINED_GLASS_PANE;
            ItemStack filler = new ItemStack(fillerMat);
            ItemMeta fillerMeta = filler.getItemMeta();
            fillerMeta.setHideTooltip(true);
            filler.setItemMeta(fillerMeta);
            for (int slot = 0; slot < SIZE; slot++) {
                if (inventory.getItem(slot) == null) {
                    inventory.setItem(slot, filler);
                }
            }
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
        if (cfg.name != null && !cfg.name.isEmpty())
            meta.displayName(MINI_MESSAGE.deserialize(cfg.name).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        if (cfg.lore != null && !cfg.lore.isEmpty()) meta.lore(splitLore(cfg.lore));
        // Apply tooltip style if present ...
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

    private static List<Component> splitLore(String text) {
        List<Component> list = new ArrayList<>();
        for (String line : text.split("\\r?\\n")) {
            list.add(MINI_MESSAGE.deserialize(line).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }
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
