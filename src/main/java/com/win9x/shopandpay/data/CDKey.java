package com.win9x.shopandpay.data;

import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class CDKey {

    private final String key;
    private final ItemStack item;
    private int uses;
    private final int maxUses;
    private final long expiryTime;
    private final Set<String> usedByPlayers;

    public CDKey(String key, ItemStack item, int uses, int maxUses, long expiryTime) {
        this.key = key;
        this.item = item;
        this.uses = uses;
        this.maxUses = maxUses;
        this.expiryTime = expiryTime;
        this.usedByPlayers = new HashSet<>();
    }

    public CDKey(String key, ItemStack item, int uses, int maxUses, long expiryTime, Set<String> usedByPlayers) {
        this.key = key;
        this.item = item;
        this.uses = uses;
        this.maxUses = maxUses;
        this.expiryTime = expiryTime;
        this.usedByPlayers = usedByPlayers != null ? usedByPlayers : new HashSet<>();
    }

    public String getKey() {
        return key;
    }

    public ItemStack getItem() {
        return item;
    }

    public int getUses() {
        return uses;
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
        if (uses >= maxUses) {
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

    public void use(String playerId) {
        uses++;
        usedByPlayers.add(playerId);
    }
}