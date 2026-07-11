package com.win9x.shopandpay.gui;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.Currency;
import com.win9x.shopandpay.data.ShopItem;
import com.win9x.shopandpay.manager.CurrencyManager;
import com.win9x.shopandpay.manager.LanguageManager;
import com.win9x.shopandpay.manager.ShopManager;
import com.win9x.shopandpay.util.ColorCodeConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopGUI implements Listener {

    private final Win9xShopAndPay plugin;
    private final CurrencyManager currencyManager;
    private final ShopManager shopManager;
    private final LanguageManager languageManager;
    
    private final NamespacedKey buyCurrencyKey;
    private final NamespacedKey currencyKey;
    private final NamespacedKey currencyPageKey;
    private final NamespacedKey itemPageKey;
    private final NamespacedKey searchKey;
    private final NamespacedKey lotteryKey;
    private final NamespacedKey cdkeyKey;
    
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    
    private static final int INVENTORY_SIZE = 54;
    private static final int CURRENCY_SLOTS_PER_PAGE = 7;
    private static final int ITEM_SLOTS_PER_PAGE = 36;
    private static final int SEARCH_SLOTS_PER_PAGE = 7;
    
    private final Map<String, Integer> playerCurrencyPages = new HashMap<>();
    private final Map<String, Integer> playerItemPages = new HashMap<>();
    private final Map<String, String> playerSearchTerms = new HashMap<>();

    public ShopGUI(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.currencyManager = plugin.getCurrencyManager();
        this.shopManager = plugin.getShopManager();
        this.languageManager = plugin.getLanguageManager();
        this.buyCurrencyKey = new NamespacedKey(plugin, "buy_currency");
        this.currencyKey = new NamespacedKey(plugin, "currency_id");
        this.currencyPageKey = new NamespacedKey(plugin, "currency_page");
        this.itemPageKey = new NamespacedKey(plugin, "item_page");
        this.searchKey = new NamespacedKey(plugin, "search_term");
        this.lotteryKey = new NamespacedKey(plugin, "lottery");
        this.cdkeyKey = new NamespacedKey(plugin, "cdkey");
    }

    public void openShop(Player player) {
        openShop(player, currencyManager.getDefaultCurrency().getId(), 0, 0, "");
    }

    public void openShop(Player player, String currencyId) {
        openShop(player, currencyId, 0, 0, "");
    }

    private void openShop(Player player, String currencyId, int currencyPage, int itemPage, String searchTerm) {
        Currency currency = currencyManager.getCurrency(currencyId);
        if (currency == null) {
            currency = currencyManager.getDefaultCurrency();
            currencyId = currency.getId();
        }

        playerCurrencyPages.put(player.getUniqueId().toString(), currencyPage);
        playerItemPages.put(player.getUniqueId().toString(), itemPage);
        playerSearchTerms.put(player.getUniqueId().toString(), searchTerm);

        Component title = languageManager.getMessageComponent("shop-title", player, 
                currency.getSymbol() + currency.getName());
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, SERIALIZER.serialize(title));

        fillCurrencyBar(inventory, player, currencyId, currencyPage);
        fillItemArea(inventory, player, currencyId, itemPage, searchTerm);
        fillSearchBar(inventory, player, searchTerm, itemPage);

        player.openInventory(inventory);
    }

    private void fillCurrencyBar(Inventory inventory, Player player, String selectedCurrencyId, int page) {
        List<Currency> currencies = new ArrayList<>(currencyManager.getAllCurrencies().values());
        
        int totalPages = (int) Math.ceil((double) currencies.size() / CURRENCY_SLOTS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        ItemStack prevPage = createPageButton(player, "prev-currency", languageManager.getMessage("prev-page", player), Material.ARROW);
        PersistentDataContainer prevContainer = prevPage.getItemMeta().getPersistentDataContainer();
        prevContainer.set(currencyPageKey, PersistentDataType.INTEGER, page);
        prevPage.getItemMeta().setDisplayName("§e<");
        prevPage.setItemMeta(prevPage.getItemMeta());
        inventory.setItem(0, prevPage);

        int startIndex = page * CURRENCY_SLOTS_PER_PAGE;
        int endIndex = Math.min(startIndex + CURRENCY_SLOTS_PER_PAGE, currencies.size());

        for (int i = startIndex; i < endIndex; i++) {
            Currency currency = currencies.get(i);
            int slot = i - startIndex + 1;
            
            ItemStack currencyItem = new ItemStack(currency.getId().equals(selectedCurrencyId) ? Material.GOLD_INGOT : Material.IRON_INGOT);
            ItemMeta meta = currencyItem.getItemMeta();
            
            if (meta != null) {
                String displayName = currency.getSymbol() + currency.getName();
                if (currency.getId().equals(selectedCurrencyId)) {
                    meta.setDisplayName("§a" + displayName);
                } else {
                    meta.setDisplayName(displayName);
                }
                
                List<String> lore = new ArrayList<>();
                double balance = currencyManager.getBalance(player, currency.getId());
                lore.add(languageManager.getMessage("shop-balance", player, currency.getSymbol(), balance).replace("&", "§"));
                meta.setLore(lore);
                
                PersistentDataContainer container = meta.getPersistentDataContainer();
                container.set(currencyKey, PersistentDataType.STRING, currency.getId());
                
                currencyItem.setItemMeta(meta);
            }
            
            inventory.setItem(slot, currencyItem);
        }

        for (int i = endIndex - startIndex + 1; i <= CURRENCY_SLOTS_PER_PAGE; i++) {
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = filler.getItemMeta();
            meta.setDisplayName("");
            filler.setItemMeta(meta);
            inventory.setItem(i, filler);
        }

        ItemStack nextPage = createPageButton(player, "next-currency", languageManager.getMessage("next-page", player), Material.ARROW);
        PersistentDataContainer nextContainer = nextPage.getItemMeta().getPersistentDataContainer();
        nextContainer.set(currencyPageKey, PersistentDataType.INTEGER, page);
        nextPage.getItemMeta().setDisplayName("§e>");
        nextPage.setItemMeta(nextPage.getItemMeta());
        inventory.setItem(8, nextPage);
    }

    private void fillItemArea(Inventory inventory, Player player, String currencyId, int page, String searchTerm) {
        List<ShopItem> allItems = shopManager.getShopItems();
        
        if (searchTerm != null && !searchTerm.isEmpty()) {
            List<ShopItem> filtered = new ArrayList<>();
            String lowerSearch = searchTerm.toLowerCase();
            for (ShopItem item : allItems) {
                String itemName = item.getItem().getType().name().toLowerCase();
                if (itemName.contains(lowerSearch)) {
                    filtered.add(item);
                }
            }
            allItems = filtered;
        }

        int totalPages = (int) Math.ceil((double) allItems.size() / ITEM_SLOTS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        int startIndex = page * ITEM_SLOTS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEM_SLOTS_PER_PAGE, allItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            ShopItem shopItem = allItems.get(i);
            int slot = (i - startIndex) + 9;
            
            ItemStack displayItem = shopItem.getItem().clone();
            ItemMeta meta = displayItem.getItemMeta();
            
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                double price = shopItem.getPrice(currencyId);
                
                if (price > 0) {
                    Currency currency = currencyManager.getCurrency(currencyId);
                    lore.add(languageManager.getMessage("shop-price", player, currency.getSymbol(), price).replace("&", "§"));
                    lore.add(languageManager.getMessage("shop-click-to-buy", player).replace("&", "§"));
                } else {
                    lore.add(languageManager.getMessage("shop-currency-not-available", player).replace("&", "§"));
                }
                
                meta.setLore(lore);
                displayItem.setItemMeta(meta);
            }
            
            inventory.setItem(slot, displayItem);
        }

        for (int i = endIndex - startIndex + 9; i < 45; i++) {
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = filler.getItemMeta();
            meta.setDisplayName("");
            filler.setItemMeta(meta);
            inventory.setItem(i, filler);
        }
    }

    private void fillSearchBar(Inventory inventory, Player player, String searchTerm, int itemPage) {
        List<ShopItem> allItems = shopManager.getShopItems();
        
        if (searchTerm != null && !searchTerm.isEmpty()) {
            List<ShopItem> filtered = new ArrayList<>();
            String lowerSearch = searchTerm.toLowerCase();
            for (ShopItem item : allItems) {
                String itemName = item.getItem().getType().name().toLowerCase();
                if (itemName.contains(lowerSearch)) {
                    filtered.add(item);
                }
            }
            allItems = filtered;
        }

        int totalPages = (int) Math.ceil((double) allItems.size() / ITEM_SLOTS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        ItemStack prevPage = createPageButton(player, "prev-item", languageManager.getMessage("prev-page", player), Material.ARROW);
        PersistentDataContainer prevContainer = prevPage.getItemMeta().getPersistentDataContainer();
        prevContainer.set(itemPageKey, PersistentDataType.INTEGER, itemPage);
        prevPage.getItemMeta().setDisplayName("§e<");
        prevPage.setItemMeta(prevPage.getItemMeta());
        inventory.setItem(45, prevPage);

        ItemStack searchBox;
        if (searchTerm != null && !searchTerm.isEmpty()) {
            searchBox = new ItemStack(Material.COMPASS);
        } else {
            searchBox = new ItemStack(Material.PAPER);
        }
        
        ItemMeta searchMeta = searchBox.getItemMeta();
        if (searchMeta != null) {
            String displayName = searchTerm != null && !searchTerm.isEmpty() 
                    ? "§e搜索: " + searchTerm 
                    : languageManager.getMessage("search-hint", player).replace("&", "§");
            searchMeta.setDisplayName(displayName);
            
            List<String> lore = new ArrayList<>();
            lore.add(languageManager.getMessage("search-click", player).replace("&", "§"));
            searchMeta.setLore(lore);
            
            PersistentDataContainer searchContainer = searchMeta.getPersistentDataContainer();
            searchContainer.set(searchKey, PersistentDataType.STRING, searchTerm != null ? searchTerm : "");
            
            searchBox.setItemMeta(searchMeta);
        }
        inventory.setItem(46, searchBox);

        ItemStack cdkey = new ItemStack(Material.TRIPWIRE_HOOK);
        ItemMeta cdkeyMeta = cdkey.getItemMeta();
        if (cdkeyMeta != null) {
            cdkeyMeta.setDisplayName(languageManager.getMessage("cdkey-button", player).replace("&", "§"));
            List<String> lore = new ArrayList<>();
            lore.add(languageManager.getMessage("cdkey-hint", player).replace("&", "§"));
            cdkeyMeta.setLore(lore);
            PersistentDataContainer cdkeyContainer = cdkeyMeta.getPersistentDataContainer();
            cdkeyContainer.set(cdkeyKey, PersistentDataType.BOOLEAN, true);
            cdkey.setItemMeta(cdkeyMeta);
        }
        inventory.setItem(50, cdkey);

        ItemStack lottery = new ItemStack(Material.HOPPER);
        ItemMeta lotteryMeta = lottery.getItemMeta();
        if (lotteryMeta != null) {
            lotteryMeta.setDisplayName(languageManager.getMessage("lottery-button", player).replace("&", "§"));
            List<String> lore = new ArrayList<>();
            lore.add(languageManager.getMessage("lottery-hint", player).replace("&", "§"));
            lotteryMeta.setLore(lore);
            PersistentDataContainer lotteryContainer = lotteryMeta.getPersistentDataContainer();
            lotteryContainer.set(lotteryKey, PersistentDataType.BOOLEAN, true);
            lottery.setItemMeta(lotteryMeta);
        }
        inventory.setItem(51, lottery);

        ItemStack buyCurrency = new ItemStack(Material.EMERALD);
        ItemMeta buyMeta = buyCurrency.getItemMeta();
        if (buyMeta != null) {
            buyMeta.setDisplayName(languageManager.getMessage("buy-currency-button", player).replace("&", "§"));
            List<String> lore = new ArrayList<>();
            lore.add(languageManager.getMessage("buy-currency-hint", player).replace("&", "§"));
            buyMeta.setLore(lore);
            PersistentDataContainer container = buyMeta.getPersistentDataContainer();
            container.set(buyCurrencyKey, PersistentDataType.BOOLEAN, true);
            buyCurrency.setItemMeta(buyMeta);
        }
        inventory.setItem(52, buyCurrency);

        ItemStack nextPage = createPageButton(player, "next-item", languageManager.getMessage("next-page", player), Material.ARROW);
        PersistentDataContainer nextContainer = nextPage.getItemMeta().getPersistentDataContainer();
        nextContainer.set(itemPageKey, PersistentDataType.INTEGER, itemPage);
        nextPage.getItemMeta().setDisplayName("§e>");
        nextPage.setItemMeta(nextPage.getItemMeta());
        inventory.setItem(53, nextPage);

        for (int i = 47; i <= 49; i++) {
            ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
            ItemMeta meta = filler.getItemMeta();
            meta.setDisplayName("");
            filler.setItemMeta(meta);
            inventory.setItem(i, filler);
        }
    }

    private ItemStack createPageButton(Player player, String type, String name, Material material) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name.replace("&", "§"));
            button.setItemMeta(meta);
        }
        return button;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        
        if (event.getClickedInventory() == null || event.getClickedInventory().equals(player.getInventory())) {
            return;
        }
        
        int slot = event.getSlot();
        
        if (slot < 0 || slot >= INVENTORY_SIZE) return;

        event.setCancelled(true);

        String playerId = player.getUniqueId().toString();
        String title = event.getView().getTitle();
        String currentCurrencyId = extractCurrencyIdFromTitle(title);
        int currentCurrencyPage = playerCurrencyPages.getOrDefault(playerId, 0);
        int currentItemPage = playerItemPages.getOrDefault(playerId, 0);
        String currentSearchTerm = playerSearchTerms.getOrDefault(playerId, "");

        if (slot == 0) {
            handleCurrencyPageChange(player, currentCurrencyId, currentCurrencyPage - 1, currentItemPage, currentSearchTerm);
            return;
        }

        if (slot == 8) {
            handleCurrencyPageChange(player, currentCurrencyId, currentCurrencyPage + 1, currentItemPage, currentSearchTerm);
            return;
        }

        if (slot >= 1 && slot <= 7) {
            handleCurrencyClick(event.getInventory().getItem(slot), player, currentItemPage, currentSearchTerm);
            return;
        }

        if (slot >= 9 && slot <= 44) {
            handleItemClick(player, slot - 9, currentCurrencyId, currentItemPage, currentSearchTerm);
            return;
        }

        if (slot == 45) {
            handleItemPageChange(player, currentCurrencyId, currentItemPage - 1, currentSearchTerm);
            return;
        }

        if (slot == 53) {
            handleItemPageChange(player, currentCurrencyId, currentItemPage + 1, currentSearchTerm);
            return;
        }

        if (slot == 46) {
            handleSearchClick(player, currentCurrencyId, currentItemPage);
            return;
        }

        if (slot == 50) {
            handleCdkeyButtonClick(player);
            return;
        }

        if (slot == 51) {
            handleLotteryButtonClick(player);
            return;
        }

        if (slot == 52) {
            handleBuyCurrencyButtonClick(player);
            return;
        }
    }

    private void handleCurrencyPageChange(Player player, String currentCurrencyId, int newPage, int itemPage, String searchTerm) {
        List<Currency> currencies = new ArrayList<>(currencyManager.getAllCurrencies().values());
        int totalPages = (int) Math.ceil((double) currencies.size() / CURRENCY_SLOTS_PER_PAGE);
        
        if (totalPages == 0) totalPages = 1;
        
        if (newPage < 0) newPage = totalPages - 1;
        if (newPage >= totalPages) newPage = 0;
        
        openShop(player, currentCurrencyId, newPage, itemPage, searchTerm);
    }

    private void handleItemPageChange(Player player, String currencyId, int newPage, String searchTerm) {
        List<ShopItem> allItems = shopManager.getShopItems();
        
        if (searchTerm != null && !searchTerm.isEmpty()) {
            List<ShopItem> filtered = new ArrayList<>();
            String lowerSearch = searchTerm.toLowerCase();
            for (ShopItem item : allItems) {
                String itemName = item.getItem().getType().name().toLowerCase();
                if (itemName.contains(lowerSearch)) {
                    filtered.add(item);
                }
            }
            allItems = filtered;
        }

        int totalPages = (int) Math.ceil((double) allItems.size() / ITEM_SLOTS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        
        if (newPage < 0) newPage = totalPages - 1;
        if (newPage >= totalPages) newPage = 0;
        
        openShop(player, currencyId, playerCurrencyPages.getOrDefault(player.getUniqueId().toString(), 0), newPage, searchTerm);
    }

    private void handleCurrencyClick(ItemStack item, Player player, int itemPage, String searchTerm) {
        if (item == null || item.getItemMeta() == null) return;
        
        PersistentDataContainer container = item.getItemMeta().getPersistentDataContainer();
        if (!container.has(currencyKey, PersistentDataType.STRING)) return;
        
        String currencyId = container.get(currencyKey, PersistentDataType.STRING);
        if (currencyId != null) {
            openShop(player, currencyId, playerCurrencyPages.getOrDefault(player.getUniqueId().toString(), 0), itemPage, searchTerm);
        }
    }

    private void handleItemClick(Player player, int relativeSlot, String currencyId, int itemPage, String searchTerm) {
        List<ShopItem> allItems = shopManager.getShopItems();
        
        if (searchTerm != null && !searchTerm.isEmpty()) {
            List<ShopItem> filtered = new ArrayList<>();
            String lowerSearch = searchTerm.toLowerCase();
            for (ShopItem item : allItems) {
                String itemName = item.getItem().getType().name().toLowerCase();
                if (itemName.contains(lowerSearch)) {
                    filtered.add(item);
                }
            }
            allItems = filtered;
        }

        int itemIndex = itemPage * ITEM_SLOTS_PER_PAGE + relativeSlot;
        if (itemIndex >= allItems.size()) return;
        
        ShopItem shopItem = allItems.get(itemIndex);
        
        Currency currency = currencyManager.getCurrency(currencyId);
        if (currency == null) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("currency-invalid", player));
            return;
        }

        double price = shopItem.getPrice(currencyId);
        
        if (price <= 0) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("shop-currency-not-available", player));
            return;
        }

        if (!currencyManager.withdraw(player, currencyId, price)) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("shop-insufficient-funds", player, 
                    currency.getSymbol(), price));
            return;
        }

        player.getInventory().addItem(shopItem.getItem());
        ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("shop-buy-success", player, 
                shopItem.getItem().getAmount(), shopItem.getItem().getType().name()));
        
        openShop(player, currencyId, playerCurrencyPages.getOrDefault(player.getUniqueId().toString(), 0), itemPage, searchTerm);
    }

    private void handleSearchClick(Player player, String currencyId, int itemPage) {
        player.closeInventory();
        ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("search-input", player));
    }

    private void handleBuyCurrencyButtonClick(Player player) {
        player.closeInventory();
        String mode = plugin.getConfig().getString("gui.buy-currency-mode", "url");
        String customMessage = plugin.getConfig().getString("gui.buy-currency-message", "请跳转到该网页以购买其他币种！");
        
        String url;
        if ("server".equalsIgnoreCase(mode)) {
            plugin.startHttpServerOnDemand();
            com.win9x.shopandpay.server.SimpleHttpServer httpServer = plugin.getHttpServer();
            if (httpServer != null && httpServer.isRunning()) {
                url = "http://" + getServerAddress() + ":" + httpServer.getPort() + "/buy-currency";
            } else {
                url = "http://localhost:" + plugin.getConfig().getInt("gui.buy-currency-server-port", 8080) + "/buy-currency";
            }
        } else {
            url = plugin.getConfig().getString("gui.buy-currency-url", "https://example.com/buy-currency");
        }
        
        Component msg = Component.text(customMessage, NamedTextColor.GREEN);
        ColorCodeConverter.sendMessage(player, msg);
        ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("buy-currency-message", player));
        
        Component message = Component.text(languageManager.getMessage("buy-currency-click-here", player), NamedTextColor.AQUA)
                .clickEvent(ClickEvent.openUrl(url));
        ColorCodeConverter.sendMessage(player, message);
    }

    private void handleLotteryButtonClick(Player player) {
        player.closeInventory();
        plugin.getLotteryGUI().openLottery(player);
    }

    private void handleCdkeyButtonClick(Player player) {
        player.closeInventory();
        ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("cdkey-enter-prompt", player));
    }
    
    private String getServerAddress() {
        String customAddress = plugin.getConfig().getString("gui.buy-currency-server-address", "");
        if (!customAddress.isEmpty()) {
            return customAddress;
        }
        String ip = plugin.getServer().getIp();
        if (ip == null || ip.isEmpty() || "0.0.0.0".equals(ip) || "127.0.0.1".equals(ip)) {
            return "localhost";
        }
        return ip;
    }

    private String extractCurrencyIdFromTitle(String title) {
        title = title.replace("§", "");
        for (Currency currency : currencyManager.getAllCurrencies().values()) {
            if (title.contains(currency.getName()) || title.contains(currency.getSymbol())) {
                return currency.getId();
            }
        }
        Currency defaultCurrency = currencyManager.getDefaultCurrency();
        return defaultCurrency != null ? defaultCurrency.getId() : "coins";
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        String playerId = player.getUniqueId().toString();
        
        if (!playerCurrencyPages.containsKey(playerId)) return;
        
        event.setCancelled(true);
    }
}