package com.win9x.shopandpay.manager;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.Currency;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
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
            Map<String, Double> balances = new ConcurrentHashMap<>();
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
        Map<String, Double> balances = playerBalances.get(playerId);
        return balances != null ? balances.getOrDefault(currencyId, 0.0) : 0.0;
    }

    public boolean withdraw(Player player, String currencyId, double amount) {
        Currency currency = currencies.get(currencyId);
        if (currency == null || amount <= 0) {
            return false;
        }

        if ("vault".equalsIgnoreCase(currency.getStorage())) {
            Economy economy = plugin.getEconomy();
            if (economy == null || !economy.has(player, amount)) {
                return false;
            }
            net.milkbowl.vault.economy.EconomyResponse response = economy.withdrawPlayer(player, amount);
            return response != null && response.transactionSuccess();
        }

        String playerId = player.getUniqueId().toString();
        Map<String, Double> balances = playerBalances.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        AtomicBoolean success = new AtomicBoolean(false);
        balances.compute(currencyId, (k, current) -> {
            double c = (current == null) ? 0.0 : current;
            if (c >= amount) {
                success.set(true);
                return c - amount;
            }
            return c;
        });
        if (success.get()) {
            savePlayerBalances();
            return true;
        }
        return false;
    }

    /**
     * Force-withdraws an amount regardless of the player's balance, allowing the
     * balance to go negative. Used for system actions such as overdue loan
     * collection. For internal currencies this can produce a negative balance;
     * for Vault it relies on the economy plugin's overdraft behaviour and
     * returns whether the transaction actually succeeded.
     */
    public boolean withdrawForce(org.bukkit.OfflinePlayer offlinePlayer, String currencyId, double amount) {
        Currency currency = currencies.get(currencyId);
        if (currency == null || !Double.isFinite(amount) || amount <= 0) {
            return false;
        }

        if ("vault".equalsIgnoreCase(currency.getStorage())) {
            Economy economy = plugin.getEconomy();
            if (economy == null) {
                return false;
            }
            net.milkbowl.vault.economy.EconomyResponse response = economy.withdrawPlayer(offlinePlayer, amount);
            return response != null && response.transactionSuccess();
        }

        String playerId = offlinePlayer.getUniqueId().toString();
        Map<String, Double> balances = playerBalances.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        balances.compute(currencyId, (k, current) -> {
            double c = (current == null) ? 0.0 : current;
            return c - amount;
        });
        savePlayerBalances();
        return true;
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
            }            return;
        }

        String playerId = player.getUniqueId().toString();
        Map<String, Double> balances = playerBalances.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        balances.merge(currencyId, amount, Double::sum);
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
            if (!player.isOnline()) {
                String playerId = player.getUniqueId().toString();
                Map<String, Double> balances = playerBalances.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
                balances.put(currencyId, Math.max(0.0, amount));
                savePlayerBalances();
                return;
            }
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
        Map<String, Double> balances = playerBalances.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        balances.put(currencyId, Math.max(0.0, amount));
        savePlayerBalances();
    }

    public void deposit(OfflinePlayer offlinePlayer, String currencyId, double amount) {
        Currency currency = currencies.get(currencyId);
        if (currency == null || amount <= 0) {
            return;
        }

        if ("vault".equalsIgnoreCase(currency.getStorage())) {
            Economy economy = plugin.getEconomy();
            if (economy != null) {
                economy.depositPlayer(offlinePlayer, amount);
            }
            return;
        }

        String playerId = offlinePlayer.getUniqueId().toString();
        Map<String, Double> balances = playerBalances.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        balances.merge(currencyId, amount, Double::sum);
        savePlayerBalances();
    }

    public void setBalance(OfflinePlayer offlinePlayer, String currencyId, double amount) {
        Currency currency = currencies.get(currencyId);
        if (currency == null) {
            return;
        }

        if ("vault".equalsIgnoreCase(currency.getStorage())) {
            Player player = offlinePlayer.getPlayer();
            if (player != null && player.isOnline()) {
                Economy economy = plugin.getEconomy();
                if (economy != null) {
                    double currentBalance = economy.getBalance(player);
                    if (currentBalance > amount) {
                        economy.withdrawPlayer(player, currentBalance - amount);
                    } else if (currentBalance < amount) {
                        economy.depositPlayer(player, amount - currentBalance);
                    }
                }
            }
            return;
        }

        String playerId = offlinePlayer.getUniqueId().toString();
        Map<String, Double> balances = playerBalances.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        balances.put(currencyId, Math.max(0.0, amount));
        savePlayerBalances();
    }

    public void reload() {
        currencies.clear();
        loadCurrencies();
    }

    public double getBalance(OfflinePlayer offlinePlayer, String currencyId) {
        Currency currency = currencies.get(currencyId);
        if (currency == null) {
            return 0;
        }

        if ("vault".equalsIgnoreCase(currency.getStorage())) {
            Player player = offlinePlayer.getPlayer();
            if (player != null && player.isOnline()) {
                Economy economy = plugin.getEconomy();
                if (economy != null) {
                    return economy.getBalance(player);
                }
            }
            return 0;
        }

        String playerId = offlinePlayer.getUniqueId().toString();
        Map<String, Double> balances = playerBalances.get(playerId);
        return balances != null ? balances.getOrDefault(currencyId, 0.0) : 0.0;
    }
}