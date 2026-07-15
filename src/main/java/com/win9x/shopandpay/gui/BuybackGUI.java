package com.win9x.shopandpay.gui;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.manager.BuybackManager;
import com.win9x.shopandpay.manager.LanguageManager;
import com.win9x.shopandpay.util.ColorCodeConverter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

/**
 * System buyback GUI. While open, clicking an item in your own inventory sells
 * that stack to the server at the configured price; the "sell all" button sells
 * every sellable stack at once.
 */
public class BuybackGUI implements Listener {

    private static final int INVENTORY_SIZE = 27;
    private static final int SELL_ALL_SLOT = 11;
    private static final int INFO_SLOT = 13;
    private static final int CLOSE_SLOT = 15;

    private final Win9xShopAndPay plugin;
    private final BuybackManager buybackManager;
    private final LanguageManager languageManager;

    public BuybackGUI(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.buybackManager = plugin.getBuybackManager();
        this.languageManager = plugin.getLanguageManager();
    }

    public void openBuyback(Player player) {
        BuybackInventoryHolder holder = new BuybackInventoryHolder();
        Inventory inv = Bukkit.createInventory(holder, INVENTORY_SIZE,
                ColorCodeConverter.toLegacy(languageManager.getMessageComponent("buyback-title", player)));
        holder.setInventory(inv);

        ItemStack sellAll = new ItemStack(Material.HOPPER);
        ItemMeta sam = sellAll.getItemMeta();
        if (sam != null) {
            sam.setDisplayName(languageManager.getMessage("buyback-sell-all", player).replace("&", "§"));
            sam.setLore(java.util.Arrays.asList(
                    languageManager.getMessage("buyback-sell-all-hint", player).replace("&", "§")));
            sellAll.setItemMeta(sam);
        }
        inv.setItem(SELL_ALL_SLOT, sellAll);

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        if (im != null) {
            im.setDisplayName(languageManager.getMessage("buyback-info", player).replace("&", "§"));
            im.setLore(java.util.Arrays.asList(
                    languageManager.getMessage("buyback-info-hint", player).replace("&", "§")));
            info.setItemMeta(im);
        }
        inv.setItem(INFO_SLOT, info);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        if (cm != null) {
            cm.setDisplayName("§c" + languageManager.getMessage("buyback-close", player).replace("&", "§"));
            close.setItemMeta(cm);
        }
        inv.setItem(CLOSE_SLOT, close);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof BuybackInventoryHolder)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();

        Inventory clicked = event.getClickedInventory();

        if (clicked != null && clicked.equals(player.getInventory())) {
            event.setCancelled(true);
            ItemStack stack = event.getCurrentItem();
            if (stack == null || stack.getType().isAir()) {
                return;
            }
            sellStack(player, stack, event.getSlot());
            return;
        }

        event.setCancelled(true);

        if (clicked == null || !clicked.equals(top)) {
            return;
        }
        int slot = event.getSlot();
        if (slot == CLOSE_SLOT) {
            player.closeInventory();
        } else if (slot == SELL_ALL_SLOT) {
            sellAll(player);
        }
    }

    private void sellStack(Player player, ItemStack stack, int slot) {
        Material material = stack.getType();
        if (!buybackManager.isSellable(material)) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("buyback-not-sellable", player));
            return;
        }
        int amount = stack.getAmount();
        double total = buybackManager.sell(player, material, amount);
        player.getInventory().setItem(slot, null);
        ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("buyback-sold", player,
                amount, material.name(), total));
    }

    private void sellAll(Player player) {
        double grandTotal = 0.0;
        int stacks = 0;
        Map<Integer, ItemStack> contents = new HashMap<>();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack == null || stack.getType().isAir()) {
                continue;
            }
            Material material = stack.getType();
            if (!buybackManager.isSellable(material)) {
                continue;
            }
            int amount = stack.getAmount();
            grandTotal += buybackManager.sell(player, material, amount);
            player.getInventory().setItem(i, null);
            stacks++;
        }
        if (stacks == 0) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("buyback-nothing", player));
        } else {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("buyback-sell-all-success", player,
                    stacks, grandTotal));
        }
    }
}
