package com.win9x.shopandpay.gui;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.Currency;
import com.win9x.shopandpay.data.GuiShopItem;
import com.win9x.shopandpay.manager.CurrencyManager;
import com.win9x.shopandpay.manager.GuiShopManager;
import com.win9x.shopandpay.manager.LanguageManager;
import com.win9x.shopandpay.util.ColorCodeConverter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Sell panel of the GUI shop.
 *
 * Click semantics:
 *  - Left click           -> sell 1 unit from player's inventory
 *  - Shift + Left click   -> sell 1 full stack (player's stack of that type)
 *  - Shift + Right click  -> sell all matching stacks up to number of empty inventory slots
 *
 * Selling removes items from the player's inventory and credits currency at
 * the UI-only sell_price defined in gui-shop-items.yml.
 */
public class GuiSellPanelGUI implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 28;
    private static final int ITEM_START_SLOT = 10;
    private static final int ITEM_END_SLOT = 43;

    private static final int BACK_SLOT = 4;
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 48;
    private static final int NEXT_SLOT = 53;

    private final Win9xShopAndPay plugin;
    private final GuiShopManager guiShopManager;
    private final CurrencyManager currencyManager;
    private final LanguageManager languageManager;

    public GuiSellPanelGUI(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.guiShopManager = plugin.getGuiShopManager();
        this.currencyManager = plugin.getCurrencyManager();
        this.languageManager = plugin.getLanguageManager();
    }

    public void open(Player player, int page) {
        List<GuiShopItem> items = guiShopManager.getSellItems();
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        GuiShopInventoryHolder holder = new GuiShopInventoryHolder(GuiShopInventoryHolder.Mode.SELL, page);
        Inventory inv = Bukkit.createInventory(holder, INVENTORY_SIZE,
                ColorCodeConverter.toLegacy(languageManager.getMessageComponent("gui-shop-sell-title", player)));
        holder.setInventory(inv);

        String currencyId = plugin.getConfig().getString("gui-shop.currency-id", "coins");
        Currency currency = currencyManager.getCurrency(currencyId);
        String symbol = currency != null ? currency.getSymbol() : "";

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, items.size());
        for (int i = start; i < end; i++) {
            GuiShopItem gItem = items.get(i);
            int slot = ITEM_START_SLOT + (i - start);
            inv.setItem(slot, buildDisplay(gItem, symbol));
        }

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) { fm.setDisplayName(""); filler.setItemMeta(fm); }
        for (int s = ITEM_START_SLOT; s <= ITEM_END_SLOT; s++) {
            if (inv.getItem(s) == null || inv.getItem(s).getType().isAir()) {
                inv.setItem(s, filler);
            }
        }

        // Back button
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) {
            bm.setDisplayName("§c" + languageManager.getMessage("gui-shop-close", player).replace("&", "§"));
            back.setItemMeta(bm);
        }
        inv.setItem(BACK_SLOT, back);
        for (int s = 0; s <= 8; s++) {
            if (s != BACK_SLOT) {
                inv.setItem(s, filler);
            }
        }

        // Prev
        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta pm = prev.getItemMeta();
        if (pm != null) { pm.setDisplayName("§e<"); prev.setItemMeta(pm); }
        inv.setItem(PREV_SLOT, prev);

        // Info
        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        if (im != null) {
            im.setDisplayName("§7" + (page + 1) + " / " + totalPages);
            List<String> lore = new ArrayList<>();
            lore.add(languageManager.getMessage("gui-shop-left-click", player).replace("&", "§"));
            lore.add(languageManager.getMessage("gui-shop-shift-left", player).replace("&", "§"));
            lore.add(languageManager.getMessage("gui-shop-shift-right", player).replace("&", "§"));
            im.setLore(lore);
            info.setItemMeta(im);
        }
        inv.setItem(INFO_SLOT, info);

        // Next
        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nm = next.getItemMeta();
        if (nm != null) { nm.setDisplayName("§e>"); next.setItemMeta(nm); }
        inv.setItem(NEXT_SLOT, next);

        for (int s = 45; s <= 53; s++) {
            if (s != PREV_SLOT && s != INFO_SLOT && s != NEXT_SLOT) {
                inv.setItem(s, filler);
            }
        }

        player.openInventory(inv);
    }

    private ItemStack buildDisplay(GuiShopItem gItem, String symbol) {
        ItemStack display = gItem.getItem().clone();
        ItemMeta meta = display.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add("§f回收价: " + symbol + gItem.getPrice());
            lore.add("§f出售单位: " + gItem.getAmount());
            if (gItem.getDescription() != null && !gItem.getDescription().isEmpty()) {
                lore.add("§8---");
                lore.addAll(gItem.getDescription());
            }
            lore.add("§7" + languageManager.getMessage("gui-shop-left-click", "en-US").replace("&", "§"));
            lore.add("§7" + languageManager.getMessage("gui-shop-shift-left", "en-US").replace("&", "§"));
            lore.add("§7" + languageManager.getMessage("gui-shop-shift-right", "en-US").replace("&", "§"));
            meta.setLore(lore);
            if (gItem.getDisplayName() != null && !gItem.getDisplayName().isEmpty()) {
                meta.setDisplayName(gItem.getDisplayName());
            }
            display.setItemMeta(meta);
        }
        return display;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof GuiShopInventoryHolder)) return;

        GuiShopInventoryHolder holder = (GuiShopInventoryHolder) top.getHolder();
        if (holder.getMode() != GuiShopInventoryHolder.Mode.SELL) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) {
            return;
        }

        int slot = event.getSlot();
        if (slot < 0 || slot >= INVENTORY_SIZE) return;

        Player player = (Player) event.getWhoClicked();
        int page = holder.getPage();

        if (slot == BACK_SLOT) {
            plugin.getGuiShopLauncherGUI().open(player);
            return;
        }
        if (slot == PREV_SLOT) {
            open(player, page - 1);
            return;
        }
        if (slot == NEXT_SLOT) {
            open(player, page + 1);
            return;
        }

        if (slot >= ITEM_START_SLOT && slot <= ITEM_END_SLOT) {
            int relativeIndex = slot - ITEM_START_SLOT;
            int itemIndex = page * ITEMS_PER_PAGE + relativeIndex;
            List<GuiShopItem> items = guiShopManager.getSellItems();
            if (itemIndex < 0 || itemIndex >= items.size()) return;

            GuiShopItem gItem = items.get(itemIndex);
            handleSell(player, gItem, event.isLeftClick(), event.isRightClick(), event.isShiftClick(), page);
        }
    }

    private void handleSell(Player player, GuiShopItem gItem, boolean leftClick, boolean rightClick, boolean shiftClick, int page) {
        String currencyId = plugin.getConfig().getString("gui-shop.currency-id", "coins");
        Currency currency = currencyManager.getCurrency(currencyId);
        if (currency == null) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("currency-invalid", player));
            return;
        }

        Material material = gItem.getItem().getType();
        int unitAmount = gItem.getAmount();
        double unitPrice = gItem.getPrice();

        if (unitPrice <= 0 || unitAmount <= 0) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("gui-shop-price-invalid", player));
            return;
        }

        int unitsToSell;
        if (leftClick && !shiftClick) {
            unitsToSell = unitAmount;
        } else if (leftClick && shiftClick) {
            // Full stack (material max stack size)
            unitsToSell = material.getMaxStackSize();
        } else if (rightClick && shiftClick) {
            int emptySlots = countEmptyPlayerSlots(player);
            unitsToSell = Math.max(1, emptySlots) * unitAmount;
        } else {
            return;
        }

        int totalUnitsSold = 0;
        double totalReceived = 0.0;
        int remaining = unitsToSell;

        while (remaining > 0) {
            int canTake = Math.min(remaining, material.getMaxStackSize());
            int actuallySold = removeFromPlayerInventory(player, material, canTake);
            if (actuallySold <= 0) break;
            double salePrice = (actuallySold / (double) unitAmount) * unitPrice;
            totalReceived += salePrice;
            totalUnitsSold += actuallySold;
            remaining -= actuallySold;
        }

        if (totalUnitsSold <= 0) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("gui-shop-nothing-to-sell", player));
            return;
        }

        currencyManager.deposit(player, currencyId, totalReceived);
        ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("gui-shop-sell-success", player,
                totalUnitsSold, material.name(), currency.getSymbol(), totalReceived));

        open(player, page);
    }

    /**
     * Removes up to {@code maxAmount} items of the given material from the
     * player's inventory (first matching stacks first). Returns the actual
     * number of items removed.
     */
    private int removeFromPlayerInventory(Player player, Material material, int maxAmount) {
        int removed = 0;
        int target = maxAmount;
        for (int i = 0; i < 36 && removed < target; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot == null || slot.getType().isAir()) continue;
            if (!slot.getType().equals(material)) continue;

            int available = slot.getAmount();
            int canTake = Math.min(available, target - removed);
            if (canTake <= 0) continue;

            if (canTake >= available) {
                player.getInventory().setItem(i, null);
            } else {
                slot.setAmount(available - canTake);
                player.getInventory().setItem(i, slot);
            }
            removed += canTake;
        }
        return removed;
    }

    private int countEmptyPlayerSlots(Player player) {
        int empty = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack slot = player.getInventory().getItem(i);
            if (slot == null || slot.getType().isAir()) {
                empty++;
            }
        }
        return empty;
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof GuiShopInventoryHolder)) return;
        GuiShopInventoryHolder holder = (GuiShopInventoryHolder) top.getHolder();
        if (holder.getMode() != GuiShopInventoryHolder.Mode.SELL) return;
        event.setCancelled(true);
    }
}
