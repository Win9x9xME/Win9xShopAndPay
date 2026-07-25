package com.win9x.shopandpay.manager;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.FixedDeposit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bank: demand (活期) deposits that accrue interest each period, and
 * fixed-term (定期) deposits that pay a configured rate at maturity. State is
 * persisted to bank.yml.
 */
public class BankManager {

    private final Win9xShopAndPay plugin;
    private final Map<UUID, Double> demandBalances = new ConcurrentHashMap<>();
    private final Map<UUID, Long> demandLastAccrual = new ConcurrentHashMap<>();
    private final Map<String, FixedDeposit> fixedDeposits = new ConcurrentHashMap<>();
    private final Object saveLock = new Object();
    private File file;
    private FileConfiguration config;

    public BankManager(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        load();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("bank.enabled", true);
    }

    public String getCurrencyId() {
        return plugin.getConfig().getString("bank.currency-id", "coins");
    }

    public double getDemandRate() {
        return plugin.getConfig().getDouble("bank.demand-rate", 0.001);
    }

    public long getPeriodMillis() {
        int hours = Math.max(1, plugin.getConfig().getInt("bank.period-hours", 24));
        return (long) hours * 60L * 60L * 1000L;
    }

    public boolean isEarlyWithdrawPenalty() {
        return plugin.getConfig().getBoolean("bank.fixed-early-withdraw-penalty", true);
    }

    public Map<Integer, Double> getFixedTerms() {
        Map<Integer, Double> terms = new LinkedHashMap<>();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("bank.fixed-terms");
        if (sec != null) {
            for (String key : sec.getKeys(false)) {
                try {
                    int days = Integer.parseInt(key);
                    terms.put(days, sec.getDouble(key, 0.0));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return terms;
    }

    public double getDemandBalance(UUID playerId) {
        return demandBalances.getOrDefault(playerId, 0.0);
    }

    public List<FixedDeposit> getDepositsBy(UUID playerId) {
        List<FixedDeposit> result = new ArrayList<>();
        for (FixedDeposit d : fixedDeposits.values()) {
            if (d.getPlayerId().equals(playerId) && d.getStatus() != FixedDeposit.Status.CLAIMED
                    && d.getStatus() != FixedDeposit.Status.EARLY_WITHDRAWN) {
                result.add(d);
            }
        }
        return result;
    }

    public enum DemandResult {
        SUCCESS, DISABLED, INVALID_AMOUNT, INSUFFICIENT_FUNDS
    }

    public DemandResult depositDemand(Player player, double amount) {
        if (!isEnabled()) {
            return DemandResult.DISABLED;
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            return DemandResult.INVALID_AMOUNT;
        }
        if (!plugin.getCurrencyManager().withdraw(player, getCurrencyId(), amount)) {
            return DemandResult.INSUFFICIENT_FUNDS;
        }
        synchronized (saveLock) {
            UUID id = player.getUniqueId();
            demandBalances.merge(id, amount, Double::sum);
            demandLastAccrual.putIfAbsent(id, System.currentTimeMillis());
            save();
        }
        return DemandResult.SUCCESS;
    }

    public DemandResult withdrawDemand(Player player, double amount) {
        if (!isEnabled()) {
            return DemandResult.DISABLED;
        }
        double toWithdraw;
        synchronized (saveLock) {
            UUID id = player.getUniqueId();
            double balance = demandBalances.getOrDefault(id, 0.0);
            toWithdraw = (!Double.isFinite(amount) || amount <= 0) ? balance : Math.min(amount, balance);
            if (toWithdraw <= 0) {
                return DemandResult.INVALID_AMOUNT;
            }
            demandBalances.put(id, balance - toWithdraw);
            save();
        }
        plugin.getCurrencyManager().deposit(player, getCurrencyId(), toWithdraw);
        return DemandResult.SUCCESS;
    }

    public enum FixedResult {
        SUCCESS, DISABLED, INVALID_AMOUNT, INSUFFICIENT_FUNDS, INVALID_TERM
    }

    public FixedResult depositFixed(Player player, int termDays, double amount) {
        if (!isEnabled()) {
            return FixedResult.DISABLED;
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            return FixedResult.INVALID_AMOUNT;
        }
        Map<Integer, Double> terms = getFixedTerms();
        Double rate = terms.get(termDays);
        if (rate == null) {
            return FixedResult.INVALID_TERM;
        }
        if (!plugin.getCurrencyManager().withdraw(player, getCurrencyId(), amount)) {
            return FixedResult.INSUFFICIENT_FUNDS;
        }
        synchronized (saveLock) {
            long now = System.currentTimeMillis();
            long mature = now + (long) termDays * 24L * 60L * 60L * 1000L;
            String id = generateId();
            FixedDeposit deposit = new FixedDeposit(id, player.getUniqueId(), amount, termDays, rate, now, mature,
                    FixedDeposit.Status.ACTIVE);
            fixedDeposits.put(id, deposit);
            save();
        }
        return FixedResult.SUCCESS;
    }

    public enum ClaimResult {
        SUCCESS, NOT_FOUND, NOT_MATURED, NOT_OWNER, ALREADY_CLAIMED
    }

    public ClaimResult claim(Player player, String depositId) {
        FixedDeposit d;
        synchronized (saveLock) {
            d = fixedDeposits.get(depositId);
            if (d == null) {
                return ClaimResult.NOT_FOUND;
            }
            if (!d.getPlayerId().equals(player.getUniqueId())) {
                return ClaimResult.NOT_OWNER;
            }
            if (d.getStatus() == FixedDeposit.Status.CLAIMED || d.getStatus() == FixedDeposit.Status.EARLY_WITHDRAWN) {
                return ClaimResult.ALREADY_CLAIMED;
            }
            long now = System.currentTimeMillis();
            boolean claimable = d.getStatus() == FixedDeposit.Status.MATURED
                    || (d.getStatus() == FixedDeposit.Status.ACTIVE && now >= d.getMatureTime());
            if (!claimable) {
                return ClaimResult.NOT_MATURED;
            }
            d.setStatus(FixedDeposit.Status.CLAIMED);
            save();
        }
        plugin.getCurrencyManager().deposit(player, getCurrencyId(), d.getTotal());
        return ClaimResult.SUCCESS;
    }

    public enum EarlyResult {
        SUCCESS, NOT_FOUND, NOT_OWNER, ALREADY_CLAIMED
    }

    public EarlyResult earlyWithdraw(Player player, String depositId) {
        FixedDeposit d;
        double returned;
        synchronized (saveLock) {
            d = fixedDeposits.get(depositId);
            if (d == null) {
                return EarlyResult.NOT_FOUND;
            }
            if (!d.getPlayerId().equals(player.getUniqueId())) {
                return EarlyResult.NOT_OWNER;
            }
            if (d.getStatus() == FixedDeposit.Status.CLAIMED || d.getStatus() == FixedDeposit.Status.EARLY_WITHDRAWN) {
                return EarlyResult.ALREADY_CLAIMED;
            }
            returned = isEarlyWithdrawPenalty() ? d.getPrincipal() : d.getTotal();
            d.setStatus(FixedDeposit.Status.EARLY_WITHDRAWN);
            save();
        }
        plugin.getCurrencyManager().deposit(player, getCurrencyId(), returned);
        return EarlyResult.SUCCESS;
    }

    /**
     * Called periodically by the scheduler. Accrues demand interest for elapsed
     * periods and marks matured fixed deposits.
     */
    public void tick() {
        long now = System.currentTimeMillis();
        long period = getPeriodMillis();
        double rate = getDemandRate();

        synchronized (saveLock) {
            boolean dirty = false;
            for (Map.Entry<UUID, Long> entry : demandLastAccrual.entrySet()) {
                UUID id = entry.getKey();
                long last = entry.getValue();
                double balance = demandBalances.getOrDefault(id, 0.0);
                if (balance <= 0 || now < last + period) {
                    continue;
                }
                long elapsedPeriods = (now - last) / period;
                double factor = Math.pow(1.0 + rate, elapsedPeriods);
                double newBalance = balance * factor;
                demandBalances.put(id, newBalance);
                demandLastAccrual.put(id, last + elapsedPeriods * period);
                dirty = true;
            }

            for (FixedDeposit d : fixedDeposits.values()) {
                if (d.isMatured(now)) {
                    d.setStatus(FixedDeposit.Status.MATURED);
                    dirty = true;
                }
            }

            if (dirty) {
                save();
            }
        }
    }

    private String generateId() {
        String id;
        do {
            id = "D" + Long.toHexString(ThreadLocalRandom.current().nextLong() & 0xffffffL).toUpperCase();
        } while (fixedDeposits.containsKey(id));
        return id;
    }

    public void load() {
        synchronized (saveLock) {
            file = new File(plugin.getDataFolder(), "bank.yml");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to create bank.yml");
                    return;
                }
            }
            config = YamlConfiguration.loadConfiguration(file);
            demandBalances.clear();
            demandLastAccrual.clear();
            fixedDeposits.clear();

            ConfigurationSection demand = config.getConfigurationSection("demand");
            if (demand != null) {
                for (String uuidStr : demand.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        demandBalances.put(uuid, demand.getDouble(uuidStr, 0.0));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            ConfigurationSection demandTime = config.getConfigurationSection("demand-accrual-time");
            if (demandTime != null) {
                for (String uuidStr : demandTime.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        demandLastAccrual.put(uuid, demandTime.getLong(uuidStr, System.currentTimeMillis()));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }

            ConfigurationSection fixed = config.getConfigurationSection("fixed");
            if (fixed != null) {
                for (String id : fixed.getKeys(false)) {
                    ConfigurationSection s = fixed.getConfigurationSection(id);
                    if (s == null) {
                        continue;
                    }
                    try {
                        UUID playerId = UUID.fromString(s.getString("player-id"));
                        double principal = s.getDouble("principal", 0);
                        int termDays = s.getInt("term-days", 0);
                        double rate = s.getDouble("rate", 0);
                        long start = s.getLong("start-time", 0);
                        long mature = s.getLong("mature-time", 0);
                        FixedDeposit.Status status = FixedDeposit.Status.valueOf(s.getString("status", "ACTIVE"));
                        fixedDeposits.put(id, new FixedDeposit(id, playerId, principal, termDays, rate, start, mature, status));
                    } catch (Exception e) {
                        plugin.getLogger().warning("Skipping invalid fixed deposit " + id);
                    }
                }
            }
        }
    }

    public void save() {
        synchronized (saveLock) {
            if (config == null) {
                return;
            }
            config.set("demand", null);
            for (Map.Entry<UUID, Double> e : demandBalances.entrySet()) {
                config.set("demand." + e.getKey(), e.getValue());
            }
            config.set("demand-accrual-time", null);
            for (Map.Entry<UUID, Long> e : demandLastAccrual.entrySet()) {
                config.set("demand-accrual-time." + e.getKey(), e.getValue());
            }
            config.set("fixed", null);
            for (FixedDeposit d : fixedDeposits.values()) {
                String p = "fixed." + d.getId();
                config.set(p + ".player-id", d.getPlayerId().toString());
                config.set(p + ".principal", d.getPrincipal());
                config.set(p + ".term-days", d.getTermDays());
                config.set(p + ".rate", d.getRate());
                config.set(p + ".start-time", d.getStartTime());
                config.set(p + ".mature-time", d.getMatureTime());
                config.set(p + ".status", d.getStatus().name());
            }
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save bank.yml");
            }
        }
    }

    public void reload() {
        load();
    }
}
