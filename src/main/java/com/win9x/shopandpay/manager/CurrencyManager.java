package com.win9x.shopandpay.manager;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.Currency;
import net.milkbowl.vault.economy.Economy;
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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CurrencyManager {

    private final Win9xShopAndPay plugin;
    private final Map<String, Currency> currencies = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Double>> playerBalances = new ConcurrentHashMap<>();
    private final ReadWriteLock saveLock = new ReentrantReadWriteLock();
    private File balanceFile;
    private FileConfiguration balanceConfig;
    private File currencyFile;
    private FileConfiguration currencyConfig;

    public CurrencyManager(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        loadCurrencies();
        loadPlayerBalances();
    }

    private void loadCurrencies() {
        currencyFile = new File(plugin.getDataFolder(), "currencies.yml");
        
        if (!currencyFile.exists()) {
            try (InputStream is = plugin.getResource("currencies.yml")) {
                if (is != null) {
                    Files.copy(is, currencyFile.toPath());
                } else {
                    currencyFile.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create currencies.yml");
                return;
            }
        }
        
        currencyConfig = YamlConfiguration.loadConfiguration(currencyFile);
        ConfigurationSection currenciesSection = currencyConfig.getConfigurationSection("currencies");
        
        if (currenciesSection == null) {
            return;
        }

        for (String key : currenciesSection.getKeys(false)) {
            ConfigurationSection currencySection = currenciesSection.getConfigurationSection(key);
            if (currencySection == null) continue;

            String name = currencySection.getString("name", key);
            String symbol = currencySection.getString("symbol", "");
            boolean isDefault = currencySection.getBoolean("default", false);
            String storage = currencySection.getString("storage", "config");

            currencies.put(key, new Currency(key, name, symbol, isDefault, storage));
        }
    }

    private void loadPlayerBalances() {
        balanceFile = new File(plugin.getDataFolder(), "balances.yml");
        if (!balanceFile.exists()) {
            try {
                balanceFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create balances.yml");
            }
        }
        balanceConfig = YamlConfiguration.loadConfiguration(balanceFile);
        
        for (String playerId : balanceConfig.getKeys(false)) {
            Map<String, Double> balances = new HashMap<>();
            ConfigurationSection playerSection = balanceConfig.getConfigurationSection(playerId);
            if (playerSection != null) {
                for (String currencyId : playerSection.getKeys(false)) {
                    balances.put(currencyId, playerSection.getDouble(currencyId, 0));
                }
            }
            playerBalances.put(playerId, balances);
        }
    }

    public void savePlayerBalances() {
        saveLock.writeLock().lock();
        try {
            balanceConfig.set("", null);
            
            for (Map.Entry<String, Map<String, Double>> entry : playerBalances.entrySet()) {
                String playerId = entry.getKey();
                for (Map.Entry<String, Double> balanceEntry : entry.getValue().entrySet()) {
                    balanceConfig.set(playerId + "." + balanceEntry.getKey(), balanceEntry.getValue());
                }
            }

            try {
                balanceConfig.save(balanceFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save balances.yml");
            }
        } finally {
            saveLock.writeLock().unlock();
        }
    }

    public double getBalance(Player player, String currencyId) {
        Currency currency = currencies.get(currencyId);
        if (currency == null) {
            return 0;
        }

        if ("vault".equalsIgnoreCase(currency.getStorage())) {
            Economy economy = plugin.getEconomy();
            if (economy != null) {
                return economy.getBalance(player);
            }
            return 0;
        }

        String playerId = player.getUniqueId().toString();
        return playerBalances.computeIfAbsent(playerId, k -> new HashMap<>())
                .getOrDefault(currencyId, 0.0);
    }

    public boolean withdraw(Player player, String currencyId, double amount) {
        Currency currency = currencies.get(currencyId);
        if (currency == null || amount <= 0) {
            return false;
        }

        if ("vault".equalsIgnoreCase(currency.getStorage())) {
            Economy economy = plugin.getEconomy();
            if (economy != null && economy.has(player, amount)) {
                economy.withdrawPlayer(player, amount);
                return true;
            }
            return false;
        }

        String playerId = player.getUniqueId().toString();
        Map<String, Double> balances = playerBalances.computeIfAbsent(playerId, k -> new HashMap<>());
        double currentBalance = balances.getOrDefault(currencyId, 0.0);
        
        if (currentBalance >= amount) {
            balances.put(currencyId, currentBalance - amount);
            savePlayerBalances();
            return true;
        }
        return false;
    }

    public void deposit(Player player, String currencyId, double amount) {
        Currency currency = currencies.get(currencyId);
        if (currency == null || amount <= 0) {
            return;
        }

        if ("vault".equalsIgnoreCase(currency.getStorage())) {
            Economy economy = plugin.getEconomy();
            if (economy != null) {
                economy.depositPlayer(player, amount);
            }
            return;
        }

        String playerId = player.getUniqueId().toString();
        Map<String, Double> balances = playerBalances.computeIfAbsent(playerId, k -> new HashMap<>());
        double currentBalance = balances.getOrDefault(currencyId, 0.0);
        balances.put(currencyId, currentBalance + amount);
        savePlayerBalances();
    }

    public Currency getCurrency(String currencyId) {
        return currencies.get(currencyId);
    }

    public Currency getDefaultCurrency() {
        return currencies.values().stream()
                .filter(Currency::isDefault)
                .findFirst()
                .orElse(currencies.values().stream().findFirst().orElse(null));
    }

    public Map<String, Currency> getAllCurrencies() {
        return currencies;
    }

    public boolean isValidCurrency(String currencyId) {
        return currencies.containsKey(currencyId);
    }

    public void setBalance(Player player, String currencyId, double amount) {
        Currency currency = currencies.get(currencyId);
        if (currency == null) {
            return;
        }

        if ("vault".equalsIgnoreCase(currency.getStorage())) {
            Economy economy = plugin.getEconomy();
            if (economy != null) {
                double currentBalance = economy.getBalance(player);
                if (currentBalance > amount) {
                    economy.withdrawPlayer(player, currentBalance - amount);
                } else if (currentBalance < amount) {
                    economy.depositPlayer(player, amount - currentBalance);
                }
            }
            return;
        }

        String playerId = player.getUniqueId().toString();
        Map<String, Double> balances = playerBalances.computeIfAbsent(playerId, k -> new HashMap<>());
        balances.put(currencyId, amount);
        savePlayerBalances();
    }
}