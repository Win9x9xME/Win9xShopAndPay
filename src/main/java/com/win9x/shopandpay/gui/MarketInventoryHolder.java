package com.win9x.shopandpay.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class MarketInventoryHolder implements InventoryHolder {

    public enum Mode {
        BROWSE,
        MY_LISTINGS
    }

    private final Mode mode;
    private final int page;
    private Inventory inventory;

    public MarketInventoryHolder(Mode mode, int page) {
        this.mode = mode;
        this.page = page;
    }

    public Mode getMode() {
        return mode;
    }

    public int getPage() {
        return page;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
