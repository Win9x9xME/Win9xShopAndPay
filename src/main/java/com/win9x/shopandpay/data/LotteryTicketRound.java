package com.win9x.shopandpay.data;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class LotteryTicketRound {

    public static class TicketPurchase {
        private final String playerName;
        private double amount;

        public TicketPurchase(String playerName, double amount) {
            this.playerName = playerName;
            this.amount = amount;
        }

        public String getPlayerName() {
            return playerName;
        }

        public double getAmount() {
            return amount;
        }

        public void addAmount(double extra) {
            this.amount += extra;
        }
    }

    private final int roundNumber;
    private final long openTime;
    private final long closeTime;
    private double totalPool;
    private final Map<UUID, TicketPurchase> purchases = new LinkedHashMap<>();
    private UUID winnerUUID;
    private String winnerName;
    private double prize;
    private boolean closed;

    public LotteryTicketRound(int roundNumber, long openTime, long closeTime) {
        this.roundNumber = roundNumber;
        this.openTime = openTime;
        this.closeTime = closeTime;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public long getOpenTime() {
        return openTime;
    }

    public long getCloseTime() {
        return closeTime;
    }

    public double getTotalPool() {
        return totalPool;
    }

    public void setTotalPool(double totalPool) {
        this.totalPool = totalPool;
    }

    public Map<UUID, TicketPurchase> getPurchases() {
        return purchases;
    }

    public UUID getWinnerUUID() {
        return winnerUUID;
    }

    public void setWinnerUUID(UUID winnerUUID) {
        this.winnerUUID = winnerUUID;
    }

    public String getWinnerName() {
        return winnerName;
    }

    public void setWinnerName(String winnerName) {
        this.winnerName = winnerName;
    }

    public double getPrize() {
        return prize;
    }

    public void setPrize(double prize) {
        this.prize = prize;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }

    public void addPurchase(UUID playerId, String playerName, double amount) {
        TicketPurchase existing = purchases.get(playerId);
        if (existing == null) {
            purchases.put(playerId, new TicketPurchase(playerName, amount));
        } else {
            existing.addAmount(amount);
        }
        totalPool += amount;
    }
}
