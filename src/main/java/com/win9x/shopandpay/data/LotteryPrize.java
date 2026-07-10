package com.win9x.shopandpay.data;

import org.bukkit.inventory.ItemStack;

public class LotteryPrize {

    private final String id;
    private final ItemStack item;
    private final double weight;
    private final String displayName;
    private final String rarity;

    public LotteryPrize(String id, ItemStack item, double weight, String displayName, String rarity) {
        this.id = id;
        this.item = item;
        this.weight = weight;
        this.displayName = displayName;
        this.rarity = rarity;
    }

    public String getId() {
        return id;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getWeight() {
        return weight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRarity() {
        return rarity;
    }
}