package com.win9x.shopandpay.data;

import org.bukkit.inventory.ItemStack;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CDKey {

    private final String key;
    private final ItemStack item;
    private final AtomicInteger uses;
    private final int maxUses;
    private final long expiryTime;
    private final Set<String> usedByPlayers = ConcurrentHashMap.newKeySet();

    public CDKey(String key, ItemStack item, int uses, int maxUses, long expiryTime) {
        this.key = key;
        this.item = item;
        this.uses = new AtomicInteger(uses);
        this.maxUses = maxUses;
        this.expiryTime = expiryTime;
    }

    public CDKey(String key, ItemStack item, int uses, int maxUses, long expiryTime, Set<String> usedByPlayers) {
        this.key = key;
        this.item = item;
        this.uses = new AtomicInteger(uses);
        this.maxUses = maxUses;
        this.expiryTime = expiryTime;
        if (usedByPlayers != null) {
            this.usedByPlayers.addAll(usedByPlayers);
        }
    }

    public String getKey() {
        return key;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getUses() {
        return uses.get();
    }

    public int getMaxUses() {
        return maxUses;
    }

    public long getExpiryTime() {
        return expiryTime;
    }

    public Set<String> getUsedByPlayers() {
        return usedByPlayers;
    }

    public boolean hasExpiryDate() {
        return expiryTime > 0;
    }

    public boolean isExpired() {
        if (uses.get() >= maxUses) {
            return true;
        }
        
        if (expiryTime > 0 && System.currentTimeMillis() > expiryTime) {
            return true;
        }
        
        return false;
    }

    public boolean canBeUsedByPlayer(String playerId) {
        return !usedByPlayers.contains(playerId);
    }

    public boolean use(String playerId) {
        if (!usedByPlayers.add(playerId)) {
            return false;
        }
        uses.incrementAndGet();
        return true;
    }
}