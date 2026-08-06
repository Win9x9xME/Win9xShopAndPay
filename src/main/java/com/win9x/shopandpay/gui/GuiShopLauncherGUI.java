package com.win9x.shopandpay.gui;

import com.win9x.shopandpay.Win9xShopAndPay;
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

/**
 * The entry GUI for /wsap gui shop. A 9-slot chest with only two usable
 * slots: one to open the buy store, one to open the sell store.
 */
public class GuiShopLauncherGUI implements Listener {

    private static final int INVENTORY_SIZE = 9;
    private static final int BUY_SLOT = 3;
    private static final int SELL_SLOT = 5;

    private final Win9xShopAndPay plugin;
    private final LanguageManager languageManager;

    public GuiShopLauncherGUI(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    public void open(Player player) {
        GuiShopInventoryHolder holder = new GuiShopInventoryHolder(GuiShopInventoryHolder.Mode.LAUNCHER, 0);
        Inventory inv = Bukkit.createInventory(holder, INVENTORY_SIZE,
                ColorCodeConverter.toLegacy(languageManager.getMessageComponent("gui-shop-launcher-title", player)));
        holder.setInventory(inv);

        ItemStack buy = new ItemStack(Material.EMERALD);
        ItemMeta bm = buy.getItemMeta();
        if (bm != null) {
            bm.setDisplayName("§a" + languageManager.getMessage("gui-shop-buy-title", player).replace("&", "§"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(languageManager.getMessage("gui-shop-buy-desc", player).replace("&", "§"));
            bm.setLore(lore);
            buy.setItemMeta(bm);
        }
        inv.setItem(BUY_SLOT, buy);

        ItemStack sell = new ItemStack(Material.REDSTONE);
        ItemMeta sm = sell.getItemMeta();
        if (sm != null) {
            sm.setDisplayName("§c" + languageManager.getMessage("gui-shop-sell-title", player).replace("&", "§"));
            java.util.List<String> lore = new java.util.ArrayList<>();
            lore.add(languageManager.getMessage("gui-shop-sell-desc", player).replace("&", "§"));
            sm.setLore(lore);
            sell.setItemMeta(sm);
        }
        inv.setItem(SELL_SLOT, sell);

        // Fill the rest with gray panes for visual polish.
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        if (fm != null) {
            fm.setDisplayName("");
            filler.setItemMeta(fm);
        }
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            if (i != BUY_SLOT && i != SELL_SLOT) {
                inv.setItem(i, filler);
            }
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof GuiShopInventoryHolder)) return;

        GuiShopInventoryHolder holder = (GuiShopInventoryHolder) top.getHolder();
        if (holder.getMode() != GuiShopInventoryHolder.Mode.LAUNCHER) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) {
            return;
        }

        int slot = event.getSlot();
        if (slot < 0 || slot >= INVENTORY_SIZE) return;

        Player player = (Player) event.getWhoClicked();
        if (slot == BUY_SLOT) {
            plugin.getGuiShopPanelGUI().open(player, 0);
        } else if (slot == SELL_SLOT) {
            plugin.getGuiSellPanelGUI().open(player, 0);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof GuiShopInventoryHolder)) return;
        event.setCancelled(true);
    }
}
