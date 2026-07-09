package com.win9x.shopandpay.gui;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.Currency;
import com.win9x.shopandpay.data.ShopItem;
import com.win9x.shopandpay.manager.CurrencyManager;
import com.win9x.shopandpay.manager.LanguageManager;
import com.win9x.shopandpay.manager.ShopManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class ShopGUI implements Listener {

    private final Win9xShopAndPay plugin;
    private final CurrencyManager currencyManager;
    private final ShopManager shopManager;
    private final LanguageManager languageManager;
    private final NamespacedKey buyCurrencyKey;

    public ShopGUI(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.currencyManager = plugin.getCurrencyManager();
        this.shopManager = plugin.getShopManager();
        this.languageManager = plugin.getLanguageManager();
        this.buyCurrencyKey = new NamespacedKey(plugin, "buy_currency");
    }

    public void openShop(Player player) {
        openShop(player, currencyManager.getDefaultCurrency().getId());
    }

    public void openShop(Player player, String currencyId) {
        Currency currency = currencyManager.getCurrency(currencyId);
        if (currency == null) {
            currency = currencyManager.getDefaultCurrency();
        }

        String title = ChatColor.DARK_PURPLE + languageManager.getMessage("shop-title", player, 
                currency.getSymbol() + currency.getName());
        Inventory inventory = Bukkit.createInventory(null, 27, title);

        for (ShopItem shopItem : shopManager.getShopItems()) {
            ItemStack displayItem = shopItem.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                double price = shopItem.getPrice(currencyId);
                
                if (price > 0) {
                    lore.add(languageManager.getMessage("shop-price", player, currency.getSymbol(), price));
                    lore.add(languageManager.getMessage("shop-click-to-buy", player));
                } else {
                    lore.add(languageManager.getMessage("shop-currency-not-available", player));
                }
                
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            
            inventory.setItem(shopItem.getSlot(), displayItem);
        }

        addBuyCurrencyButton(inventory, player);

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null) {
                ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = filler.getItemMeta();
                meta.setDisplayName("");
                filler.setItemMeta(meta);
                inventory.setItem(i, filler);
            }
        }

        player.openInventory(inventory);
    }

    private void addBuyCurrencyButton(Inventory inventory, Player player) {
        ItemStack button = new ItemStack(Material.EMERALD);
        ItemMeta meta = button.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ChatColor.GREEN + languageManager.getMessage("buy-currency-button", player));
            
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + languageManager.getMessage("buy-currency-hint", player));
            meta.setLore(lore);
            
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(buyCurrencyKey, PersistentDataType.BOOLEAN, true);
            
            button.setItemMeta(meta);
        }
        
        inventory.setItem(26, button);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        String title = event.getView().getTitle();
        if (!title.startsWith(ChatColor.DARK_PURPLE.toString())) return;

        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        if (slot == 26) {
            handleBuyCurrencyButtonClick(player);
            return;
        }

        ShopItem shopItem = shopManager.getShopItem(slot);
        if (shopItem == null) return;

        String currencyId = extractCurrencyIdFromTitle(title);
        Currency currency = currencyManager.getCurrency(currencyId);
        
        if (currency == null) {
            player.sendMessage(languageManager.getMessage("currency-invalid", player));
            return;
        }

        double price = shopItem.getPrice(currencyId);
        
        if (price <= 0) {
            player.sendMessage(languageManager.getMessage("shop-currency-not-available", player));
            return;
        }

        if (!currencyManager.withdraw(player, currencyId, price)) {
            player.sendMessage(languageManager.getMessage("shop-insufficient-funds", player, 
                    currency.getSymbol(), price));
            return;
        }

        player.getInventory().addItem(shopItem.getItem());
        player.sendMessage(languageManager.getMessage("shop-buy-success", player, 
                shopItem.getItem().getAmount(), shopItem.getItem().getType().name()));
    }

    private void handleBuyCurrencyButtonClick(Player player) {
        String url = plugin.getConfig().getString("gui.buy-currency-url", "https://example.com/buy-currency");
        String customMessage = plugin.getConfig().getString("gui.buy-currency-message", "请跳转到该网页以购买其他币种！");
        
        player.sendMessage(ChatColor.GREEN + customMessage);
        player.sendMessage(languageManager.getMessage("buy-currency-message", player));
        
        TextComponent message = new TextComponent(ChatColor.AQUA + languageManager.getMessage("buy-currency-click-here", player));
        message.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
        player.spigot().sendMessage(message);
    }

    private String extractCurrencyIdFromTitle(String title) {
        for (Currency currency : currencyManager.getAllCurrencies().values()) {
            if (title.contains(currency.getSymbol())) {
                return currency.getId();
            }
        }
        return currencyManager.getDefaultCurrency().getId();
    }
}