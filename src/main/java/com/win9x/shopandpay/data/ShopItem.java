package com.win9x.shopandpay.data;

import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ShopItem {

    private final ItemStack item;
    private final Map<String, Double> prices = new HashMap<>();
    private final int slot;

    public ShopItem(ItemStack item, Map<String, Double> prices, int slot) {
        this.item = item;
        this.prices.putAll(prices);
        this.slot = slot;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice(String currencyId) {
        return prices.getOrDefault(currencyId, 0.0);
    }

    public Map<String, Double> getPrices() {
        return prices;
    }

    public int getSlot() {
        return slot;
    }

    public boolean hasPriceForCurrency(String currencyId) {
        return prices.containsKey(currencyId);
    }
}