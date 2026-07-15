package com.win9x.shopandpay.gui;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.Currency;
import com.win9x.shopandpay.data.MarketListing;
import com.win9x.shopandpay.manager.LanguageManager;
import com.win9x.shopandpay.manager.MarketManager;
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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class MarketGUI implements Listener {

    private static final int INVENTORY_SIZE = 54;
    private static final int SLOTS_PER_PAGE = 45;

    private final Win9xShopAndPay plugin;
    private final MarketManager marketManager;
    private final LanguageManager languageManager;
    private final org.bukkit.NamespacedKey listingKey;

    public MarketGUI(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.marketManager = plugin.getMarketManager();
        this.languageManager = plugin.getLanguageManager();
        this.listingKey = new org.bukkit.NamespacedKey(plugin, "market_listing_id");
    }

    public void openBrowse(Player player, int page) {
        List<MarketListing> all = new ArrayList<>(marketManager.getAllListings());
        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) SLOTS_PER_PAGE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        MarketInventoryHolder holder = new MarketInventoryHolder(MarketInventoryHolder.Mode.BROWSE, page);
        Inventory inv = Bukkit.createInventory(holder, INVENTORY_SIZE,
                ColorCodeConverter.toLegacy(languageManager.getMessageComponent("market-title-browse", player)));
        holder.setInventory(inv);

        int start = page * SLOTS_PER_PAGE;
        int end = Math.min(start + SLOTS_PER_PAGE, all.size());
        for (int i = start; i < end; i++) {
            MarketListing listing = all.get(i);
            int slot = i - start;
            ItemStack display = listing.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                Currency currency = plugin.getCurrencyManager().getCurrency(listing.getCurrencyId());
                String symbol = currency != null ? currency.getSymbol() : "";
                lore.add(languageManager.getMessage("market-price", player, symbol, listing.getPrice()).replace("&", "§"));
                lore.add(languageManager.getMessage("market-seller", player, listing.getSellerName()).replace("&", "§"));
                lore.add(languageManager.getMessage("market-click-to-buy", player).replace("&", "§"));
                meta.setLore(lore);
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(listingKey, PersistentDataType.STRING, listing.getId());
                display.setItemMeta(meta);
            }
            inv.setItem(slot, display);
        }

        fillFillers(inv, end - start);
        addNavButtons(inv, page, totalPages, MarketInventoryHolder.Mode.BROWSE, player);
        inv.setItem(49, modeButton(player, "market-my-listings", Material.CHEST, MarketInventoryHolder.Mode.MY_LISTINGS, 0));
        player.openInventory(inv);
    }

    public void openMyListings(Player player, int page) {
        List<MarketListing> all = marketManager.getListingsBy(player.getUniqueId());
        int totalPages = Math.max(1, (int) Math.ceil(all.size() / (double) SLOTS_PER_PAGE));
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;

        MarketInventoryHolder holder = new MarketInventoryHolder(MarketInventoryHolder.Mode.MY_LISTINGS, page);
        Inventory inv = Bukkit.createInventory(holder, INVENTORY_SIZE,
                ColorCodeConverter.toLegacy(languageManager.getMessageComponent("market-title-my", player)));
        holder.setInventory(inv);

        int start = page * SLOTS_PER_PAGE;
        int end = Math.min(start + SLOTS_PER_PAGE, all.size());
        for (int i = start; i < end; i++) {
            MarketListing listing = all.get(i);
            int slot = i - start;
            ItemStack display = listing.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                Currency currency = plugin.getCurrencyManager().getCurrency(listing.getCurrencyId());
                String symbol = currency != null ? currency.getSymbol() : "";
                lore.add(languageManager.getMessage("market-price", player, symbol, listing.getPrice()).replace("&", "§"));
                lore.add(languageManager.getMessage("market-click-to-delist", player).replace("&", "§"));
                meta.setLore(lore);
                PersistentDataContainer pdc = meta.getPersistentDataContainer();
                pdc.set(listingKey, PersistentDataType.STRING, listing.getId());
                display.setItemMeta(meta);
            }
            inv.setItem(slot, display);
        }

        fillFillers(inv, end - start);
        addNavButtons(inv, page, totalPages, MarketInventoryHolder.Mode.MY_LISTINGS, player);
        inv.setItem(49, modeButton(player, "market-browse", Material.EMERALD, MarketInventoryHolder.Mode.BROWSE, 0));
        player.openInventory(inv);
    }

    private void fillFillers(Inventory inv, int used) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta m = filler.getItemMeta();
        m.setDisplayName("");
        filler.setItemMeta(m);
        for (int i = used; i < SLOTS_PER_PAGE; i++) {
            inv.setItem(i, filler);
        }
        for (int i = SLOTS_PER_PAGE; i < INVENTORY_SIZE; i++) {
            inv.setItem(i, filler);
        }
    }

    private ItemStack modeButton(Player player, String key, Material material, MarketInventoryHolder.Mode mode, int page) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(languageManager.getMessage(key, player).replace("&", "§"));
            button.setItemMeta(meta);
        }
        return button;
    }

    private void addNavButtons(Inventory inv, int page, int totalPages, MarketInventoryHolder.Mode mode, Player player) {
        ItemStack prev = new ItemStack(Material.ARROW);
        ItemMeta pm = prev.getItemMeta();
        if (pm != null) {
            pm.setDisplayName("§e<");
            prev.setItemMeta(pm);
        }
        inv.setItem(45, prev);

        ItemStack next = new ItemStack(Material.ARROW);
        ItemMeta nm = next.getItemMeta();
        if (nm != null) {
            nm.setDisplayName("§e>");
            next.setItemMeta(nm);
        }
        inv.setItem(53, next);

        ItemStack info = new ItemStack(Material.PAPER);
        ItemMeta im = info.getItemMeta();
        if (im != null) {
            im.setDisplayName("§7" + (page + 1) + " / " + totalPages);
            info.setItemMeta(im);
        }
        inv.setItem(47, info);

        ItemStack close = new ItemStack(Material.BARRIER);
        ItemMeta cm = close.getItemMeta();
        if (cm != null) {
            cm.setDisplayName("§c" + languageManager.getMessage("market-close", player).replace("&", "§"));
            close.setItemMeta(cm);
        }
        inv.setItem(51, close);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof MarketInventoryHolder)) {
            return;
        }
        event.setCancelled(true);

        Player player = (Player) event.getWhoClicked();
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(top)) {
            return;
        }
        int slot = event.getSlot();
        if (slot < 0 || slot >= INVENTORY_SIZE) {
            return;
        }

        MarketInventoryHolder holder = (MarketInventoryHolder) top.getHolder();
        int page = holder.getPage();

        if (slot == 45) {
            openMode(player, holder.getMode(), page - 1);
            return;
        }
        if (slot == 53) {
            openMode(player, holder.getMode(), page + 1);
            return;
        }
        if (slot == 49) {
            openMode(player, holder.getMode() == MarketInventoryHolder.Mode.BROWSE
                    ? MarketInventoryHolder.Mode.MY_LISTINGS : MarketInventoryHolder.Mode.BROWSE, 0);
            return;
        }
        if (slot == 51) {
            player.closeInventory();
            return;
        }

        if (slot < SLOTS_PER_PAGE) {
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType().isAir()) {
                return;
            }
            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) {
                return;
            }
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            if (!pdc.has(listingKey, PersistentDataType.STRING)) {
                return;
            }
            String id = pdc.get(listingKey, PersistentDataType.STRING);
            if (holder.getMode() == MarketInventoryHolder.Mode.BROWSE) {
                MarketManager.BuyResult result = marketManager.buyListing(player, id);
                switch (result) {
                    case SUCCESS:
                        ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("market-buy-success", player));
                        break;
                    case NOT_FOUND:
                        ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("market-not-found", player));
                        break;
                    case OWN_ITEM:
                        ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("market-own-item", player));
                        break;
                    case INSUFFICIENT_FUNDS:
                        ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("market-insufficient-funds", player));
                        break;
                }
                openBrowse(player, page);
            } else {
                MarketManager.DelistResult result = marketManager.delist(player, id);
                switch (result) {
                    case SUCCESS:
                        ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("market-delist-success", player));
                        break;
                    case NOT_FOUND:
                        ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("market-not-found", player));
                        break;
                    case NOT_OWNER:
                        ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("market-not-owner", player));
                        break;
                }
                openMyListings(player, page);
            }
        }
    }

    private void openMode(Player player, MarketInventoryHolder.Mode mode, int page) {
        if (mode == MarketInventoryHolder.Mode.BROWSE) {
            openBrowse(player, page);
        } else {
            openMyListings(player, page);
        }
    }
}
