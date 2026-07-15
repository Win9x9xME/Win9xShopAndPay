package com.win9x.shopandpay.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marks an inventory as the Win9xShopAndPay lottery GUI so click/drag handlers
 * can reliably tell it apart from any other inventory that happens to share a
 * similar title.
 */
public final class LotteryInventoryHolder implements InventoryHolder {

    private Inventory inventory;

    void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
