package com.win9x.shopandpay.data;

import java.util.UUID;

public class Loan {

    public enum Status {
        ACTIVE,
        PAID,
        OVERDUE_COLLECTED
    }

    private final String id;
    private final UUID playerId;
    private final double principal;
    private final double interestRate;
    private final int termDays;
    private final long startTime;
    private final long dueTime;
    private double paidAmount;
    private Status status;

    public Loan(String id, UUID playerId, double principal, double interestRate,
                int termDays, long startTime, long dueTime, double paidAmount, Status status) {
        this.id = id;
        this.playerId = playerId;
        this.principal = principal;
        this.interestRate = interestRate;
        this.termDays = termDays;
        this.startTime = startTime;
        this.dueTime = dueTime;
        this.paidAmount = paidAmount;
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

    public double getInterestRate() {
        return interestRate;
    }

    public int getTermDays() {
        return termDays;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getDueTime() {
        return dueTime;
    }

    public double getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(double paidAmount) {
        this.paidAmount = paidAmount;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public double getTotalOwed() {
        return principal * (1.0 + interestRate);
    }

    public double getRemaining() {
        return Math.max(0.0, getTotalOwed() - paidAmount);
    }

    public boolean isOverdue(long now) {
        return status == Status.ACTIVE && now > dueTime && getRemaining() > 0.0;
    }
}
