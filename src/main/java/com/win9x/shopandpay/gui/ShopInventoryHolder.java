package com.win9x.shopandpay.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marks an inventory as belonging to the Win9xShopAndPay shop GUI and carries
 * the per-view state so we never have to parse it back out of the title.
 */
public final class ShopInventoryHolder implements InventoryHolder {

    private final String currencyId;
    private final int currencyPage;
    private final int itemPage;
    private final String searchTerm;
    private Inventory inventory;

    public ShopInventoryHolder(String currencyId, int currencyPage, int itemPage, String searchTerm) {
        this.currencyId = currencyId;
        this.currencyPage = currencyPage;
        this.itemPage = itemPage;
        this.searchTerm = searchTerm == null ? "" : searchTerm;
    }

    public String getCurrencyId() {
        return currencyId;
    }

    public int getCurrencyPage() {
        return currencyPage;
    }

    public int getItemPage() {
        return itemPage;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
