package com.win9x.shopandpay.manager;

import com.win9x.shopandpay.Win9xShopAndPay;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * System buyback: the server buys items from players at configured per-unit
 * prices, giving them currency. Lets players one-click recycle their loot.
 */
public class BuybackManager {

    private final Win9xShopAndPay plugin;
    private final Map<Material, Double> prices = new ConcurrentHashMap<>();
    private File file;
    private FileConfiguration config;

    public BuybackManager(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        load();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("buyback.enabled", true);
    }

    public String getCurrencyId() {
        return plugin.getConfig().getString("buyback.currency-id", "coins");
    }

    public double getDefaultPrice() {
        return plugin.getConfig().getDouble("buyback.default-price", 1.0);
    }

    public boolean isOnlyConfigured() {
        return plugin.getConfig().getBoolean("buyback.only-configured", false);
    }

    public double getPrice(Material material) {
        Double configured = prices.get(material);
        if (configured != null) {
            return configured;
        }
        return isOnlyConfigured() ? 0.0 : getDefaultPrice();
    }

    public boolean isSellable(Material material) {
        return getPrice(material) > 0.0;
    }

    public double sell(Player player, Material material, int amount) {
        double unit = getPrice(material);
        if (unit <= 0 || amount <= 0) {
            return 0.0;
        }
        double total = unit * amount;
        plugin.getCurrencyManager().deposit(player, getCurrencyId(), total);
        return total;
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "buyback.yml");
        if (!file.exists()) {
            try (InputStream is = plugin.getResource("buyback.yml")) {
                if (is != null) {
                    Files.copy(is, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create buyback.yml");
                return;
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        prices.clear();

        ConfigurationSection pricesSection = config.getConfigurationSection("prices");
        if (pricesSection == null) {
            return;
        }
        for (String key : pricesSection.getKeys(false)) {
            try {
                Material material = Material.valueOf(key.toUpperCase());
                prices.put(material, pricesSection.getDouble(key, 0.0));
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Unknown material in buyback.yml: " + key);
            }
        }
    }

    public void reload() {
        load();
    }
}
