package com.win9x.shopandpay.manager;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.Loan;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * System loans. Players borrow principal, owe principal*(1+rate) at the end of
 * the term. Overdue loans are force-collected (balance may go negative).
 */
public class LoanManager {

    private final Win9xShopAndPay plugin;
    private final Map<String, Loan> loans = new ConcurrentHashMap<>();
    private final Object saveLock = new Object();
    private File file;
    private FileConfiguration config;

    public LoanManager(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        load();
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("loan.enabled", true);
    }

    public String getCurrencyId() {
        return plugin.getConfig().getString("loan.currency-id", "coins");
    }

    public double getInterestRate() {
        return plugin.getConfig().getDouble("loan.interest-rate", 0.1);
    }

    public int getTermDays() {
        return Math.max(1, plugin.getConfig().getInt("loan.term-days", 7));
    }

    public double getMaxAmount() {
        return plugin.getConfig().getDouble("loan.max-amount", 10000.0);
    }

    public int getMaxActiveLoans() {
        return Math.max(1, plugin.getConfig().getInt("loan.max-active-loans", 3));
    }

    public enum BorrowResult {
        SUCCESS, DISABLED, INVALID_AMOUNT, TOO_LARGE, TOO_MANY_LOANS
    }

    public enum RepayResult {
        SUCCESS, FULLY_PAID, NO_ACTIVE_LOAN, INVALID_AMOUNT, INSUFFICIENT_FUNDS
    }

    public BorrowResult borrow(Player player, double amount) {
        if (!isEnabled()) {
            return BorrowResult.DISABLED;
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            return BorrowResult.INVALID_AMOUNT;
        }
        if (amount > getMaxAmount()) {
            return BorrowResult.TOO_LARGE;
        }
        synchronized (saveLock) {
            if (countActiveLoans(player.getUniqueId()) >= getMaxActiveLoans()) {
                return BorrowResult.TOO_MANY_LOANS;
            }

            String id = generateId();
            long now = System.currentTimeMillis();
            long due = now + (long) getTermDays() * 24L * 60L * 60L * 1000L;
            Loan loan = new Loan(id, player.getUniqueId(), amount, getInterestRate(),
                    getTermDays(), now, due, 0.0, Loan.Status.ACTIVE);
            loans.put(id, loan);
            // Persist the debt before paying out the principal so a crash cannot
            // leave the player with funds but no recorded loan.
            save();
        }
        plugin.getCurrencyManager().deposit(player, getCurrencyId(), amount);
        return BorrowResult.SUCCESS;
    }

    public RepayResult repay(Player player, double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            return RepayResult.INVALID_AMOUNT;
        }
        synchronized (saveLock) {
            List<Loan> active = getActiveLoans(player.getUniqueId());
            if (active.isEmpty()) {
                return RepayResult.NO_ACTIVE_LOAN;
            }

            double toPay = Math.min(amount, totalRemaining(active));
            if (!plugin.getCurrencyManager().withdraw(player, getCurrencyId(), toPay)) {
                return RepayResult.INSUFFICIENT_FUNDS;
            }

            applyPayment(active, toPay);
            save();

            boolean anyRemaining = active.stream().anyMatch(l -> l.getRemaining() > 0.0);
            return anyRemaining ? RepayResult.SUCCESS : RepayResult.FULLY_PAID;
        }
    }

    private double totalRemaining(List<Loan> active) {
        double sum = 0.0;
        for (Loan l : active) {
            sum += l.getRemaining();
        }
        return sum;
    }

    private void applyPayment(List<Loan> active, double payment) {
        for (Loan loan : active) {
            if (payment <= 0) {
                break;
            }
            double remaining = loan.getRemaining();
            double applied = Math.min(remaining, payment);
            loan.setPaidAmount(loan.getPaidAmount() + applied);
            if (loan.getRemaining() <= 0.0) {
                loan.setStatus(Loan.Status.PAID);
            }
            payment -= applied;
        }
    }

    public List<Loan> getActiveLoans(UUID playerId) {
        List<Loan> result = new ArrayList<>();
        for (Loan loan : loans.values()) {
            if (loan.getStatus() == Loan.Status.ACTIVE && loan.getPlayerId().equals(playerId)) {
                result.add(loan);
            }
        }
        return result;
    }

    public int countActiveLoans(UUID playerId) {
        return getActiveLoans(playerId).size();
    }

    public Collection<Loan> getAllLoans() {
        return loans.values();
    }

    /**
     * Called periodically by the scheduler. Force-collects overdue loans,
     * allowing the player's balance to go negative.
     */
    public void tickOverdue() {
        synchronized (saveLock) {
            long now = System.currentTimeMillis();
            boolean dirty = false;
            for (Loan loan : loans.values()) {
                if (loan.isOverdue(now)) {
                    double remaining = loan.getRemaining();
                    OfflinePlayer owner = plugin.getServer().getOfflinePlayer(loan.getPlayerId());
                    boolean collected = plugin.getCurrencyManager().withdrawForce(owner, getCurrencyId(), remaining);
                    if (collected) {
                        loan.setPaidAmount(loan.getTotalOwed());
                        loan.setStatus(Loan.Status.OVERDUE_COLLECTED);
                        dirty = true;
                        Player online = owner.getPlayer();
                        if (online != null) {
                            online.sendMessage("§c你的一笔贷款已逾期，系统已强制扣款 " + remaining
                                    + "（余额可能为负，请尽快补足）。");
                        }
                    }
                    // If collection failed (e.g. Vault rejected the overdraft), leave
                    // the loan ACTIVE so the next tick retries it.
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
            id = "L" + Long.toHexString(ThreadLocalRandom.current().nextLong() & 0xffffffL).toUpperCase();
        } while (loans.containsKey(id));
        return id;
    }

    public void load() {
        synchronized (saveLock) {
            file = new File(plugin.getDataFolder(), "loans.yml");
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to create loans.yml");
                    return;
                }
            }
            config = YamlConfiguration.loadConfiguration(file);
            loans.clear();

            ConfigurationSection root = config.getConfigurationSection("loans");
            if (root == null) {
                return;
            }
            for (String id : root.getKeys(false)) {
                ConfigurationSection s = root.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                try {
                    UUID playerId = UUID.fromString(s.getString("player-id"));
                    double principal = s.getDouble("principal", 0);
                    double rate = s.getDouble("interest-rate", 0);
                    int termDays = s.getInt("term-days", 7);
                    long startTime = s.getLong("start-time", 0);
                    long dueTime = s.getLong("due-time", 0);
                    double paid = s.getDouble("paid-amount", 0);
                    Loan.Status status = Loan.Status.valueOf(s.getString("status", "ACTIVE"));
                    loans.put(id, new Loan(id, playerId, principal, rate, termDays, startTime, dueTime, paid, status));
                } catch (Exception e) {
                    plugin.getLogger().warning("Skipping invalid loan " + id);
                }
            }
        }
    }

    public void save() {
        synchronized (saveLock) {
            if (config == null) {
                return;
            }
            config.set("loans", null);
            for (Loan loan : loans.values()) {
                String path = "loans." + loan.getId();
                config.set(path + ".player-id", loan.getPlayerId().toString());
                config.set(path + ".principal", loan.getPrincipal());
                config.set(path + ".interest-rate", loan.getInterestRate());
                config.set(path + ".term-days", loan.getTermDays());
                config.set(path + ".start-time", loan.getStartTime());
                config.set(path + ".due-time", loan.getDueTime());
                config.set(path + ".paid-amount", loan.getPaidAmount());
                config.set(path + ".status", loan.getStatus().name());
            }
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save loans.yml");
            }
        }
    }

    public void reload() {
        load();
    }
}
