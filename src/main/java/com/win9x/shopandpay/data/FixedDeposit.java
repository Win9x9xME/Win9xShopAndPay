package com.win9x.shopandpay.data;

import java.util.UUID;

public class FixedDeposit {

    public enum Status {
        ACTIVE,
        MATURED,
        CLAIMED,
        EARLY_WITHDRAWN
    }

    private final String id;
    private final UUID playerId;
    private final double principal;
    private final int termDays;
    private final double rate;
    private final long startTime;
    private final long matureTime;
    private Status status;

    public FixedDeposit(String id, UUID playerId, double principal, int termDays,
                        double rate, long startTime, long matureTime, Status status) {
        this.id = id;
        this.playerId = playerId;
        this.principal = principal;
        this.termDays = termDays;
        this.rate = rate;
        this.startTime = startTime;
        this.matureTime = matureTime;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public double getPrincipal() {
        return principal;
    }

    public int getTermDays() {
        return termDays;
    }

    public double getRate() {
        return rate;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getMatureTime() {
        return matureTime;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public double getInterest() {
        return principal * rate;
    }

    public double getTotal() {
        return principal + getInterest();
    }

    public boolean isMatured(long now) {
        return status == Status.ACTIVE && now >= matureTime;
    }
}
