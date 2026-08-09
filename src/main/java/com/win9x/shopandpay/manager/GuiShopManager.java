package com.win9x.shopandpay.manager;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.GuiShopItem;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Loads GUI shop items (both buy and sell lists) from gui-shop-items.yml.
 * The prices / names / descriptions defined here are UI-only and do not
 * modify the underlying item's data values.
 */
public class GuiShopManager {

    private final Win9xShopAndPay plugin;
    private final List<GuiShopItem> buyItems = new CopyOnWriteArrayList<>();
    private final List<GuiShopItem> sellItems = new CopyOnWriteArrayList<>();
    private File file;
    private FileConfiguration config;

    public GuiShopManager(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "gui-shop-items.yml");
        if (!file.exists()) {
            try (InputStream is = plugin.getResource("gui-shop-items.yml")) {
                if (is != null) {
                    Files.copy(is, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create gui-shop-items.yml: " + e.getMessage());
                return;
            }
        }
        config = YamlConfiguration.loadConfiguration(file);

        buyItems.clear();
        sellItems.clear();

        loadItems(config.getConfigurationSection("buy"), buyItems, true);
        loadItems(config.getConfigurationSection("sell"), sellItems, false);
    }

    public void reload() {
        load();
    }

    public List<GuiShopItem> getBuyItems() {
        return Collections.unmodifiableList(buyItems);
    }

    public List<GuiShopItem> getSellItems() {
        return Collections.unmodifiableList(sellItems);
    }

    private void loadItems(ConfigurationSection section, List<GuiShopItem> target, boolean buyList) {
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSection = section.getConfigurationSection(key);
            if (itemSection == null) continue;

            String type = itemSection.getString("type", "DIAMOND");
            int amount = Math.max(1, itemSection.getInt("amount", 1));

            ItemStack item = createItemStack(type, amount);
            if (item == null) {
                plugin.getLogger().warning("Invalid GUI shop item type '" + type + "' for entry '" + key + "'");
                continue;
            }

            String name = itemSection.getString("name", "");
            if (name == null) name = "";
            name = name.replace("&", "§");

            List<String> descriptionLines = new ArrayList<>();
            Object descObj = itemSection.get("description");
            if (descObj instanceof List) {
                for (Object line : (List<?>) descObj) {
                    if (line != null) {
                        descriptionLines.add(line.toString().replace("&", "§"));
                    }
                }
            } else if (descObj instanceof String) {
                descriptionLines.add(((String) descObj).replace("&", "§"));
            }

            double price;
            if (buyList) {
                price = itemSection.getDouble("buy_price", 0.0);
            } else {
                price = itemSection.getDouble("sell_price", 0.0);
            }

            GuiShopItem guiItem = new GuiShopItem(key, item, name, descriptionLines, price);
            target.add(guiItem);
        }
    }

    private ItemStack createItemStack(String itemType, int amount) {
        if (itemType == null || itemType.isEmpty()) return null;

        Material material = Material.matchMaterial(itemType);
        if (material != null && material != Material.AIR) {
            return new ItemStack(material, amount);
        }

        if (itemType.contains(":")) {
            String[] parts = itemType.split(":", 2);
            String namespace = parts[0].toLowerCase();
            String key = parts[1].toLowerCase();
            material = Material.matchMaterial(namespace + ":" + key);
            if (material != null && material != Material.AIR) {
                return new ItemStack(material, amount);
            }
        }

        return null;
    }
}
