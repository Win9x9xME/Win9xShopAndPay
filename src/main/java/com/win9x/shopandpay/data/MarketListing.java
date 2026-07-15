package com.win9x.shopandpay.data;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class MarketListing {

    private final String id;
    private final UUID sellerId;
    private final String sellerName;
    private final ItemStack item;
    private final double price;
    private final String currencyId;
    private final long listedAt;

    public MarketListing(String id, UUID sellerId, String sellerName, ItemStack item,
                         double price, String currencyId, long listedAt) {
        this.id = id;
        this.sellerId = sellerId;
        this.sellerName = sellerName;
        this.item = item;
        this.price = price;
        this.currencyId = currencyId;
        this.listedAt = listedAt;
    }

    public String getId() {
        return id;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public String getSellerName() {
        return sellerName;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return price;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public long getListedAt() {
        return listedAt;
    }
}
