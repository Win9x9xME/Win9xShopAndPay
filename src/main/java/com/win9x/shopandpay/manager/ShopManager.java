package com.win9x.shopandpay.manager;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.ShopItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ShopManager {

    private final Win9xShopAndPay plugin;
    private final List<ShopItem> shopItems = new CopyOnWriteArrayList<>();
    private File shopItemsFile;
    private FileConfiguration shopItemsConfig;

    public ShopManager(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        loadShopItems();
    }

    private void loadShopItems() {
        shopItemsFile = new File(plugin.getDataFolder(), "shop-items.yml");
        
        if (!shopItemsFile.exists()) {
            try (InputStream is = plugin.getResource("shop-items.yml")) {
                if (is != null) {
                    Files.copy(is, shopItemsFile.toPath());
                } else {
                    shopItemsFile.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create shop-items.yml");
                return;
            }
        }
        
        shopItemsConfig = YamlConfiguration.loadConfiguration(shopItemsFile);
        ConfigurationSection itemsSection = shopItemsConfig.getConfigurationSection("shop-items");
        
        if (itemsSection == null) {
            return;
        }

        for (String key : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(key);
            if (itemSection == null) continue;

            String itemType = itemSection.getString("type", "DIAMOND");
            int amount = itemSection.getInt("amount", 1);
            String name = itemSection.getString("name", "");
            int slot = itemSection.getInt("slot", 0);

            Map<String, Double> prices = new HashMap<>();
            ConfigurationSection pricesSection = itemSection.getConfigurationSection("prices");
            if (pricesSection != null) {
                for (String currencyId : pricesSection.getKeys(false)) {
                    prices.put(currencyId, pricesSection.getDouble(currencyId, 0));
                }
            }

            ItemStack item = createItemStack(itemType, amount);
            if (!name.isEmpty()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(name.replace("&", "§"));
                    item.setItemMeta(meta);
                }
            }

            shopItems.add(new ShopItem(item, prices, slot));
        }
    }

    public List<ShopItem> getShopItems() {
        return shopItems;
    }

    public ShopItem getShopItem(int slot) {
        return shopItems.stream()
                .filter(item -> item.getSlot() == slot)
                .findFirst()
                .orElse(null);
    }

    public void reload() {
        shopItems.clear();
        loadShopItems();
    }
    
    private ItemStack createItemStack(String itemType, int amount) {
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
        
        plugin.getLogger().warning("Invalid item type '" + itemType + "', using DIAMOND instead");
        return new ItemStack(Material.DIAMOND, amount);
    }
}