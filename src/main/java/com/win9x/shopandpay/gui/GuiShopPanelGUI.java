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
 * Buy panel of the GUI shop.
 *  - 28-item page (slots 10..43)
 *  - Top row (0..8): back (to launcher), title
 *  - Bottom row (45..53): previous, current-page info, return, next
 *
 * Click semantics:
 *  - Left click           -> buy 1 unit
 *  - Shift + Left click   -> buy 1 full stack (material max stack)
 *  - Shift + Right click  -> buy stacks up to the number of empty inventory slots
 */
public class GuiShopPanelGUI implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int ITEMS_PER_PAGE = 28;
    private static final int ITEM_START_SLOT = 10;
    private static final int ITEM_END_SLOT = 43;

    private static final int BACK_SLOT = 4;      // return to launcher (top row center)
    private static final int PREV_SLOT = 45;
    private static final int INFO_SLOT = 48;
    private static final int NEXT_SLOT = 53;

    private final Win9xShopAndPay plugin;
    private final GuiShopManager guiShopManager;
    private final CurrencyManager currencyManager;
    private final LanguageManager languageManager;

    public GuiShopPanelGUI(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.guiShopManager = plugin.getGuiShopManager();
        this.currencyManager = plugin.getCurrencyManager();
        this.languageManager = plugin.getLanguageManager();
    }

    public void open(Player player, int page) {
        List<GuiShopItem> items = guiShopManager.getBuyItems();
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / ITEMS_PER_PAGE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        GuiShopInventoryHolder holder = new GuiShopInventoryHolder(GuiShopInventoryHolder.Mode.BUY, page);
        Inventory inv = Bukkit.createInventory(holder, INVENTORY_SIZE,
                ColorCodeConverter.toLegacy(languageManager.getMessageComponent("gui-shop-buy-title", player)));
        holder.setInventory(inv);

        String currencyId = plugin.getConfig().getString("gui-shop.currency-id", "coins");
        Currency currency = currencyManager.getCurrency(currencyId);
        String symbol = currency != null ? currency.getSymbol() : "";

        // Items
        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, items.size());
        for (int i = start; i < end; i++) {
            GuiShopItem gItem = items.get(i);
            int slot = ITEM_START_SLOT + (i - start);
            inv.setItem(slot, buildDisplay(gItem, symbol));
        }

        // Fillers for empty slots
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) { fm.setDisplayName(""); filler.setItemMeta(fm); }
        for (int s = ITEM_START_SLOT; s <= ITEM_END_SLOT; s++) {
            if (inv.getItem(s) == null || inv.getItem(s).getType().isAir()) {
                inv.setItem(s, filler);
            }
        }

        // Top row - center: back button to launcher
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta bm = back.getItemMeta();
        if (bm != null) {
            bm.setDisplayName("§c" + languageManager.getMessage("gui-shop-close", player).replace("&", "§"));
            back.setItemMeta(bm);
        }
        inv.setItem(BACK_SLOT, back);

        // Top row fillers
        for (int s = 0; s <= 8; s++) {
            if (s != BACK_SLOT) {
                inv.setItem(s, filler);
            }
        }

        // Nav row - prev
        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta pm = prev.getItemMeta();
        if (pm != null) {
            pm.setDisplayName("§e<");
            prev.setItemMeta(pm);
        }
        inv.setItem(PREV_SLOT, prev);

        // Info label
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
        if (nm != null) {
            nm.setDisplayName("§e>");
            next.setItemMeta(nm);
        }
        inv.setItem(NEXT_SLOT, next);

        // Bottom row fillers
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
            lore.add("§f单价: " + symbol + gItem.getPrice());
            lore.add("§f数量: " + gItem.getAmount());
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
        if (holder.getMode() != GuiShopInventoryHolder.Mode.BUY) return;

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
            List<GuiShopItem> items = guiShopManager.getBuyItems();
            if (itemIndex < 0 || itemIndex >= items.size()) return;

            GuiShopItem gItem = items.get(itemIndex);
            handleBuy(player, gItem, event.isLeftClick(), event.isRightClick(), event.isShiftClick(), page);
        }
    }

    private void handleBuy(Player player, GuiShopItem gItem, boolean leftClick, boolean rightClick, boolean shiftClick, int page) {
        String currencyId = plugin.getConfig().getString("gui-shop.currency-id", "coins");
        Currency currency = currencyManager.getCurrency(currencyId);
        if (currency == null) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("currency-invalid", player));
            return;
        }

        Material material = gItem.getItem().getType();
        int unitAmount = gItem.getAmount();
        double unitPrice = gItem.getPrice();
        int perStackAmount = material.getMaxStackSize();

        int unitsToBuy;
        if (leftClick && !shiftClick) {
            // 1 unit
            unitsToBuy = unitAmount;
        } else if (leftClick && shiftClick) {
            // 1 full stack (material max stack size)
            unitsToBuy = perStackAmount;
        } else if (rightClick && shiftClick) {
            // Buy stacks equal to number of empty inventory slots, each stack = unitAmount
            int emptySlots = countEmptyPlayerSlots(player);
            unitsToBuy = Math.max(1, emptySlots) * unitAmount;
        } else {
            return;
        }

        double totalPrice = unitPrice * (unitsToBuy / (double) unitAmount);

        if (totalPrice <= 0) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("gui-shop-price-invalid", player));
            return;
        }

        double balance = currencyManager.getBalance(player, currencyId);
        if (balance < totalPrice) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("gui-shop-insufficient", player,
                    currency.getSymbol(), totalPrice));
            return;
        }

        if (!currencyManager.withdraw(player, currencyId, totalPrice)) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("gui-shop-insufficient", player,
                    currency.getSymbol(), totalPrice));
            return;
        }

        int given;
        if (leftClick && shiftClick) {
            // 1 full stack
            ItemStack stack = new ItemStack(material, perStackAmount);
            given = giveSafely(player, stack);
        } else if (rightClick && shiftClick) {
            int stacks = Math.max(1, countEmptyPlayerSlots(player));
            int totalGiven = 0;
            for (int i = 0; i < stacks; i++) {
                ItemStack stack = new ItemStack(material, unitAmount);
                int actuallyGiven = giveSafely(player, stack);
                totalGiven += actuallyGiven;
                if (actuallyGiven < unitAmount) break;
            }
            given = totalGiven;
        } else {
            ItemStack stack = new ItemStack(material, unitAmount);
            given = giveSafely(player, stack);
        }

        ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("gui-shop-buy-success", player,
                material.name(), given, currency.getSymbol(), totalPrice));

        open(player, page);
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

    private int giveSafely(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return 0;
        java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        int given = item.getAmount();
        if (!overflow.isEmpty()) {
            for (ItemStack drop : overflow.values()) {
                player.getWorld().dropItem(player.getLocation(), drop);
                given -= (item.getAmount() - drop.getAmount());
            }
            // Recount: given = item amount - what remained (dropped)
            // Since addItem returns what could not fit, we can compute properly:
            int remained = 0;
            for (ItemStack drop : overflow.values()) {
                remained += drop.getAmount();
            }
            given = item.getAmount() - remained;
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("shop-inventory-full-dropped", player));
        }
        return Math.max(0, given);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof GuiShopInventoryHolder)) return;
        GuiShopInventoryHolder holder = (GuiShopInventoryHolder) top.getHolder();
        if (holder.getMode() != GuiShopInventoryHolder.Mode.BUY) return;
        event.setCancelled(true);
    }
}
