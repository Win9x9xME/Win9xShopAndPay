package com.win9x.shopandpay.manager;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.LotteryTicketRound;
import com.win9x.shopandpay.util.ColorCodeConverter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Periodic lottery (彩票). A round is open for purchases for the whole cycle
 * (cycle-days). At the start of the next cycle the previous round's winner is
 * drawn (weighted by ticket amount) and announced, the prize is paid out, and a
 * new round opens. State is persisted to lottery-tickets.yml.
 */
public class LotteryTicketManager {

    private final Win9xShopAndPay plugin;
    private final Map<Integer, LotteryTicketRound> history = new ConcurrentHashMap<>();
    private LotteryTicketRound currentRound;
    private int lastRoundNumber;
    private File file;
    private FileConfiguration config;

    public LotteryTicketManager(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        load();
        if (currentRound == null) {
            openNewRound(System.currentTimeMillis());
        }
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("lottery-ticket.enabled", true);
    }

    public String getCurrencyId() {
        return plugin.getConfig().getString("lottery-ticket.currency-id", "coins");
    }

    public int getCycleDays() {
        return Math.max(1, plugin.getConfig().getInt("lottery-ticket.cycle-days", 1));
    }

    public double getMinAmount() {
        return Math.max(0.0, plugin.getConfig().getDouble("lottery-ticket.min-amount", 1.0));
    }

    public double getPayoutRatio() {
        double r = plugin.getConfig().getDouble("lottery-ticket.payout-ratio", 1.0);
        return Math.max(0.0, Math.min(1.0, r));
    }

    public int getHistoryKept() {
        return Math.max(0, plugin.getConfig().getInt("lottery-ticket.history-kept", 20));
    }

    public LotteryTicketRound getCurrentRound() {
        return currentRound;
    }

    public List<LotteryTicketRound> getHistory() {
        List<LotteryTicketRound> sorted = new ArrayList<>(history.values());
        sorted.sort((a, b) -> Integer.compare(b.getRoundNumber(), a.getRoundNumber()));
        return sorted;
    }

    public enum BuyResult {
        SUCCESS, DISABLED, INVALID_AMOUNT, BELOW_MIN, NOT_OPEN
    }

    public BuyResult buy(Player player, double amount) {
        if (!isEnabled()) {
            return BuyResult.DISABLED;
        }
        if (currentRound == null || currentRound.isClosed()) {
            return BuyResult.NOT_OPEN;
        }
        if (!Double.isFinite(amount) || amount <= 0) {
            return BuyResult.INVALID_AMOUNT;
        }
        if (amount < getMinAmount()) {
            return BuyResult.BELOW_MIN;
        }
        if (!plugin.getCurrencyManager().withdraw(player, getCurrencyId(), amount)) {
            return BuyResult.INVALID_AMOUNT;
        }
        currentRound.addPurchase(player.getUniqueId(), player.getName(), amount);
        save();
        return BuyResult.SUCCESS;
    }

    /**
     * Called periodically by the scheduler. If the current round's cycle has
     * elapsed, draws the winner, pays out, announces, and opens a new round.
     */
    public void tick() {
        if (currentRound == null) {
            openNewRound(System.currentTimeMillis());
            return;
        }
        long now = System.currentTimeMillis();
        if (now < currentRound.getCloseTime()) {
            return;
        }
        rollover(now);
    }

    private void rollover(long now) {
        LotteryTicketRound round = currentRound;
        round.setClosed(true);

        if (round.getTotalPool() > 0 && !round.getPurchases().isEmpty()) {
            UUID winner = drawWeightedWinner(round);
            LotteryTicketRound.TicketPurchase purchase = round.getPurchases().get(winner);
            double prize = round.getTotalPool() * getPayoutRatio();
            round.setWinnerUUID(winner);
            round.setWinnerName(purchase.getPlayerName());
            round.setPrize(prize);

            history.put(round.getRoundNumber(), round);
            trimHistory();
            lastRoundNumber = round.getRoundNumber();
            // Persist the result BEFORE paying out so a crash cannot cause a
            // duplicate payout on the next reload.
            save();

            OfflinePlayer winnerOffline = plugin.getServer().getOfflinePlayer(winner);
            plugin.getCurrencyManager().deposit(winnerOffline, getCurrencyId(), prize);
        } else {
            history.put(round.getRoundNumber(), round);
            trimHistory();
            lastRoundNumber = round.getRoundNumber();
            save();
        }

        announce(round);

        openNewRound(now);
        save();
    }

    private UUID drawWeightedWinner(LotteryTicketRound round) {
        double total = round.getTotalPool();
        double r = ThreadLocalRandom.current().nextDouble(total);
        double cumulative = 0.0;
        UUID winner = null;
        for (Map.Entry<UUID, LotteryTicketRound.TicketPurchase> entry : round.getPurchases().entrySet()) {
            cumulative += entry.getValue().getAmount();
            if (r < cumulative) {
                winner = entry.getKey();
                break;
            }
        }
        if (winner == null && !round.getPurchases().isEmpty()) {
            winner = round.getPurchases().keySet().iterator().next();
        }
        return winner;
    }

    private void openNewRound(long now) {
        int number = lastRoundNumber + 1;
        long close = now + (long) getCycleDays() * 24L * 60L * 60L * 1000L;
        currentRound = new LotteryTicketRound(number, now, close);
        lastRoundNumber = number;
    }

    private void announce(LotteryTicketRound round) {
        String line;
        if (round.getWinnerUUID() == null) {
            line = "§6[彩票] §7第 " + round.getRoundNumber() + " 期无人购买，奖池作废。新一期已开放购买！";
        } else {
            line = "§6[彩票] §7第 " + round.getRoundNumber() + " 期开奖！中奖者: §e"
                    + round.getWinnerName() + " §7奖金: §e" + round.getPrize()
                    + " §7。新一期已开放购买！";
        }
        net.kyori.adventure.text.Component component = ColorCodeConverter.convert(line);
        for (Player p : Bukkit.getOnlinePlayers()) {
            ColorCodeConverter.sendMessage(p, component);
        }
        plugin.getLogger().info(org.bukkit.ChatColor.stripColor(line));
    }

    private void trimHistory() {
        int kept = getHistoryKept();
        if (kept <= 0 || history.size() <= kept) {
            return;
        }
        List<Integer> keys = new ArrayList<>(history.keySet());
        Collections.sort(keys);
        while (history.size() > kept) {
            history.remove(keys.remove(0));
        }
    }

    public void load() {
        file = new File(plugin.getDataFolder(), "lottery-tickets.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create lottery-tickets.yml");
                return;
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        history.clear();

        lastRoundNumber = config.getInt("last-round", 0);
        ConfigurationSection histSec = config.getConfigurationSection("history");
        if (histSec != null) {
            for (String key : histSec.getKeys(false)) {
                ConfigurationSection rs = histSec.getConfigurationSection(key);
                if (rs == null) {
                    continue;
                }
                LotteryTicketRound round = deserializeRound(rs);
                if (round != null) {
                    history.put(round.getRoundNumber(), round);
                }
            }
        }

        ConfigurationSection curSec = config.getConfigurationSection("current");
        if (curSec != null) {
            currentRound = deserializeRound(curSec);
            if (currentRound != null) {
                lastRoundNumber = Math.max(lastRoundNumber, currentRound.getRoundNumber());
            }
        }
    }

    private LotteryTicketRound deserializeRound(ConfigurationSection s) {
        try {
            int number = s.getInt("round-number");
            long open = s.getLong("open-time");
            long close = s.getLong("close-time");
            LotteryTicketRound round = new LotteryTicketRound(number, open, close);
            round.setTotalPool(s.getDouble("total-pool", 0));
            round.setClosed(s.getBoolean("closed", false));
            if (s.isString("winner-uuid")) {
                round.setWinnerUUID(UUID.fromString(s.getString("winner-uuid")));
                round.setWinnerName(s.getString("winner-name", "?"));
                round.setPrize(s.getDouble("prize", 0));
            }
            ConfigurationSection pur = s.getConfigurationSection("purchases");
            if (pur != null) {
                for (String uuidStr : pur.getKeys(false)) {
                    ConfigurationSection ps = pur.getConfigurationSection(uuidStr);
                    if (ps == null) {
                        continue;
                    }
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        round.getPurchases().put(uuid,
                                new LotteryTicketRound.TicketPurchase(ps.getString("name", "?"), ps.getDouble("amount", 0)));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
            return round;
        } catch (Exception e) {
            return null;
        }
    }

    public void save() {
        if (config == null) {
            return;
        }
        config.set("last-round", lastRoundNumber);
        config.set("history", null);
        for (LotteryTicketRound round : history.values()) {
            serializeRound(config, "history." + round.getRoundNumber(), round);
        }
        config.set("current", null);
        if (currentRound != null) {
            serializeRound(config, "current", currentRound);
        }
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save lottery-tickets.yml");
        }
    }

    private void serializeRound(FileConfiguration config, String base, LotteryTicketRound round) {
        config.set(base + ".round-number", round.getRoundNumber());
        config.set(base + ".open-time", round.getOpenTime());
        config.set(base + ".close-time", round.getCloseTime());
        config.set(base + ".total-pool", round.getTotalPool());
        config.set(base + ".closed", round.isClosed());
        if (round.getWinnerUUID() != null) {
            config.set(base + ".winner-uuid", round.getWinnerUUID().toString());
            config.set(base + ".winner-name", round.getWinnerName());
            config.set(base + ".prize", round.getPrize());
        }
        for (Map.Entry<UUID, LotteryTicketRound.TicketPurchase> entry : round.getPurchases().entrySet()) {
            String p = base + ".purchases." + entry.getKey();
            config.set(p + ".name", entry.getValue().getPlayerName());
            config.set(p + ".amount", entry.getValue().getAmount());
        }
    }

    public void reload() {
        load();
        if (currentRound == null) {
            openNewRound(System.currentTimeMillis());
            save();
        }
    }
}
