package com.win9x.shopandpay.manager;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.LotteryPrize;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class LotteryManager {

    private final Win9xShopAndPay plugin;
    private final List<LotteryPrize> prizes = new CopyOnWriteArrayList<>();
    private volatile String costCurrencyId;
    private volatile double costAmount;
    private volatile double totalWeight;
    private File lotteryFile;
    private FileConfiguration lotteryConfig;

    public LotteryManager(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        loadLottery();
    }

    private void loadLottery() {
        lotteryFile = new File(plugin.getDataFolder(), "lottery.yml");
        
        if (!lotteryFile.exists()) {
            try (InputStream is = plugin.getResource("lottery.yml")) {
                if (is != null) {
                    Files.copy(is, lotteryFile.toPath());
                } else {
                    lotteryFile.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create lottery.yml");
                return;
            }
        }
        
        lotteryConfig = YamlConfiguration.loadConfiguration(lotteryFile);
        ConfigurationSection lotterySection = lotteryConfig.getConfigurationSection("lottery");
        
        if (lotterySection == null) {
            return;
        }

        ConfigurationSection costSection = lotterySection.getConfigurationSection("cost");
        if (costSection != null) {
            costCurrencyId = costSection.getString("currency-id", "coins");
            
            double amount = costSection.getDouble("amount", 100.0);
            costAmount = Math.max(0.0, amount);
            if (amount < 0) {
                plugin.getLogger().warning("Lottery cost amount is negative, clamped to 0");
            }
        }

        prizes.clear();
        totalWeight = 0.0;
        
        ConfigurationSection prizesSection = lotterySection.getConfigurationSection("prizes");
        if (prizesSection != null) {
            for (String key : prizesSection.getKeys(false)) {
                ConfigurationSection prizeSection = prizesSection.getConfigurationSection(key);
                if (prizeSection == null) continue;

                String itemType = prizeSection.getString("type", "DIAMOND");
                int amount = prizeSection.getInt("amount", 1);
                double weight = prizeSection.getDouble("weight", 1.0);
                String displayName = prizeSection.getString("display-name", "");
                String rarity = prizeSection.getString("rarity", "common");

                if (amount <= 0) {
                    plugin.getLogger().warning("Invalid amount for prize " + key + ", skipping");
                    continue;
                }
                
                if (weight <= 0) {
                    plugin.getLogger().warning("Invalid weight for prize " + key + ", skipping");
                    continue;
                }

                ItemStack item = createItemStack(itemType, amount);
                if (item.getType() == Material.DIAMOND && !itemType.equalsIgnoreCase("DIAMOND")) {
                    plugin.getLogger().warning("Invalid item type in lottery prize: " + itemType);
                    continue;
                }
                if (!displayName.isEmpty()) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null) {
                        meta.setDisplayName(displayName.replace("&", "§"));
                        item.setItemMeta(meta);
                    }
                }

                prizes.add(new LotteryPrize(key, item, weight, displayName, rarity));
                totalWeight += weight;
            }
        }
        
        plugin.getLogger().info("Loaded " + prizes.size() + " lottery prizes with total weight " + totalWeight);
    }

    public LotteryPrize drawPrize() {
        if (prizes.isEmpty() || totalWeight <= 0) {
            return null;
        }

        double random = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulative = 0.0;

        for (LotteryPrize prize : prizes) {
            cumulative += prize.getWeight();
            if (random < cumulative) {
                return prize;
            }
        }

        return prizes.get(prizes.size() - 1);
    }

    public boolean canAfford(Player player) {
        if (costAmount <= 0) {
            return true;
        }
        return plugin.getCurrencyManager().getBalance(player, costCurrencyId) >= costAmount;
    }

    public boolean deductCost(Player player) {
        if (costAmount <= 0) {
            return true;
        }
        return plugin.getCurrencyManager().withdraw(player, costCurrencyId, costAmount);
    }

    public boolean givePrize(Player player, LotteryPrize prize) {
        if (prize == null) {
            return false;
        }

        ItemStack original = prize.getItem();
        ItemStack item = new ItemStack(original.getType(), original.getAmount());
        
        if (prize.getDisplayName() != null && !prize.getDisplayName().isEmpty()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(prize.getDisplayName().replace("&", "§"));
                item.setItemMeta(meta);
            }
        }
        
        java.util.Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
        for (ItemStack drop : overflow.values()) {
            player.getWorld().dropItem(player.getLocation(), drop);
        }
        return true;
    }

    public String getCostCurrencyId() {
        return costCurrencyId;
    }

    public double getCostAmount() {
        return costAmount;
    }

    public List<LotteryPrize> getPrizes() {
        return new ArrayList<>(prizes);
    }

    public void reload() {
        loadLottery();
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
        
        return new ItemStack(Material.DIAMOND, amount);
    }
}