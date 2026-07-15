package com.win9x.shopandpay.manager;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.MarketListing;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Free market / auction house. Players list items for sale (paying a listing
 * fee), other players buy them (the price goes to the seller), and the owner
 * can delist items to get them back. State is persisted to market.yml.
 */
public class MarketManager {

    private final Win9xShopAndPay plugin;
    private final Map<String, MarketListing> listings = new ConcurrentHashMap<>();
    private File file;
    private FileConfiguration config;

    public MarketManager(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        load();
    }

    public String getCurrencyId() {
        return plugin.getConfig().getString("market.currency-id", "coins");
    }

    public double getListFee() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("market.list-fee");
        if (sec != null) {
            return sec.getDouble(getCurrencyId(), 0.0);
        }
        return plugin.getConfig().getDouble("market.list-fee", 0.0);
    }

    public int getMaxListingsPerPlayer() {
        return plugin.getConfig().getInt("market.max-listings-per-player", 20);
    }

    public int getMaxListAmount() {
        return plugin.getConfig().getInt("market.max-list-amount", 64);
    }

    public enum ListResult {
        SUCCESS, NO_ITEM, INVALID_PRICE, AMOUNT_TOO_LARGE, TOO_MANY_LISTINGS, FEE_FAILED
    }

    public enum BuyResult {
        SUCCESS, NOT_FOUND, OWN_ITEM, INSUFFICIENT_FUNDS
    }

    public enum DelistResult {
        SUCCESS, NOT_FOUND, NOT_OWNER
    }

    public ListResult listItem(Player seller, double price) {
        ItemStack hand = seller.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) {
            return ListResult.NO_ITEM;
        }
        if (price <= 0) {
            return ListResult.INVALID_PRICE;
        }
        if (hand.getAmount() > getMaxListAmount()) {
            return ListResult.AMOUNT_TOO_LARGE;
        }
        if (countListingsBy(seller.getUniqueId()) >= getMaxListingsPerPlayer()) {
            return ListResult.TOO_MANY_LISTINGS;
        }

        double fee = getListFee();
        if (fee > 0 && !plugin.getCurrencyManager().withdraw(seller, getCurrencyId(), fee)) {
            return ListResult.FEE_FAILED;
        }

        ItemStack sold = hand.clone();
        String id = generateId();
        MarketListing listing = new MarketListing(id, seller.getUniqueId(),
                seller.getName(), sold, price, getCurrencyId(), System.currentTimeMillis());
        listings.put(id, listing);

        seller.getInventory().setItemInMainHand(null);
        save();
        return ListResult.SUCCESS;
    }

    public BuyResult buyListing(Player buyer, String id) {
        MarketListing listing = listings.get(id);
        if (listing == null) {
            return BuyResult.NOT_FOUND;
        }
        if (listing.getSellerId().equals(buyer.getUniqueId())) {
            return BuyResult.OWN_ITEM;
        }
        if (!plugin.getCurrencyManager().withdraw(buyer, listing.getCurrencyId(), listing.getPrice())) {
            return BuyResult.INSUFFICIENT_FUNDS;
        }

        OfflinePlayer seller = plugin.getServer().getOfflinePlayer(listing.getSellerId());
        plugin.getCurrencyManager().deposit(seller, listing.getCurrencyId(), listing.getPrice());

        Map<Integer, ItemStack> overflow = buyer.getInventory().addItem(listing.getItem().clone());
        for (ItemStack drop : overflow.values()) {
            buyer.getWorld().dropItem(buyer.getLocation(), drop);
        }

        listings.remove(id);
        save();
        return BuyResult.SUCCESS;
    }

    public DelistResult delist(Player owner, String id) {
        MarketListing listing = listings.get(id);
        if (listing == null) {
            return DelistResult.NOT_FOUND;
        }
        if (!listing.getSellerId().equals(owner.getUniqueId())) {
            return DelistResult.NOT_OWNER;
        }

        Map<Integer, ItemStack> overflow = owner.getInventory().addItem(listing.getItem().clone());
        for (ItemStack drop : overflow.values()) {
            owner.getWorld().dropItem(owner.getLocation(), drop);
        }

        listings.remove(id);
        save();
        return DelistResult.SUCCESS;
    }

    public MarketListing getListing(String id) {
        return listings.get(id);
    }

    public Collection<MarketListing> getAllListings() {
        return listings.values();
    }

    public List<MarketListing> getListingsBy(UUID sellerId) {
        List<MarketListing> result = new ArrayList<>();
        for (MarketListing listing : listings.values()) {
            if (listing.getSellerId().equals(sellerId)) {
                result.add(listing);
            }
        }
        return result;
    }

    public int countListingsBy(UUID sellerId) {
        int count = 0;
        for (MarketListing listing : listings.values()) {
            if (listing.getSellerId().equals(sellerId)) {
                count++;
            }
        }
        return count;
    }

    private String generateId() {
        String id;
        do {
            id = Integer.toHexString(ThreadLocalRandom.current().nextInt()).toUpperCase();
        } while (listings.containsKey(id));
        return id;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "market.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create market.yml");
                return;
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        listings.clear();

        ConfigurationSection root = config.getConfigurationSection("listings");
        if (root == null) {
            return;
        }
        for (String id : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            try {
                UUID sellerId = UUID.fromString(s.getString("seller-id"));
                String sellerName = s.getString("seller-name", "?");
                ItemStack item = s.getItemStack("item");
                double price = s.getDouble("price", 0);
                String currencyId = s.getString("currency-id", getCurrencyId());
                long listedAt = s.getLong("listed-at", System.currentTimeMillis());
                if (item == null) {
                    continue;
                }
                listings.put(id, new MarketListing(id, sellerId, sellerName, item, price, currencyId, listedAt));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Skipping invalid market listing " + id);
            }
        }
    }

    public void save() {
        if (config == null) {
            return;
        }
        config.set("listings", null);
        for (MarketListing listing : listings.values()) {
            String path = "listings." + listing.getId();
            config.set(path + ".seller-id", listing.getSellerId().toString());
            config.set(path + ".seller-name", listing.getSellerName());
            config.set(path + ".item", listing.getItem());
            config.set(path + ".price", listing.getPrice());
            config.set(path + ".currency-id", listing.getCurrencyId());
            config.set(path + ".listed-at", listing.getListedAt());
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save market.yml");
        }
    }

    public void reload() {
        load();
    }
}
