package com.win9x.shopandpay.data;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single GUI shop entry. The item's type / amount is used for
 * the actual transaction; {@code displayName} / {@code description} /
 * {@code price} are UI-only values.
 */
public class GuiShopItem {

    private final String key;
    private final ItemStack item;
    private final String displayName;
    private final List<String> description;
    private final double price;

    public GuiShopItem(String key, ItemStack item, String displayName, List<String> description, double price) {
        this.key = key;
        this.item = item;
        this.displayName = displayName;
        this.description = description == null ? new ArrayList<>() : new ArrayList<>(description);
        this.price = price;
    }

    public String getKey() {
        return key;
    }

    /** The actual item used to execute the trade (may be further customized with a display name). */
    public ItemStack getItem() {
        return item;
    }

    /** UI-only display name, already color-code translated to §. */
    public String getDisplayName() {
        return displayName;
    }

    /** UI-only description lines, already color-code translated. */
    public List<String> getDescription() {
        return Collections.unmodifiableList(description);
    }

    /** UI-only price value. */
    public double getPrice() {
        return price;
    }

    public int getAmount() {
        return item == null ? 1 : item.getAmount();
    }
}
