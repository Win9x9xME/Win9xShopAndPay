package com.win9x.shopandpay.gui;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.LotteryPrize;
import com.win9x.shopandpay.manager.LotteryManager;
import com.win9x.shopandpay.util.ColorCodeConverter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;

public class LotteryGUI implements Listener {

    private final Win9xShopAndPay plugin;
    private final LotteryManager lotteryManager;
    
    private final NamespacedKey lotteryDrawKey;
    private final NamespacedKey lotteryCloseKey;
    
    private static final int INVENTORY_SIZE = 27;
    private static final int[] DISPLAY_SLOTS = {11, 12, 13, 14, 15};
    
    private final Map<UUID, AtomicBoolean> spinningPlayers = new ConcurrentHashMap<>();
    private final Map<UUID, Long> drawCooldowns = new ConcurrentHashMap<>();
    
    private long getDrawCooldownMs() {
        return plugin.getConfig().getLong("lottery.draw-cooldown", 5) * 1000L;
    }
    
    public LotteryGUI(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.lotteryManager = plugin.getLotteryManager();
        this.lotteryDrawKey = new NamespacedKey(plugin, "lottery_draw");
        this.lotteryCloseKey = new NamespacedKey(plugin, "lottery_close");
    }

    public void openLottery(Player player) {
        Inventory inventory = Bukkit.createInventory(null, INVENTORY_SIZE, "§6抽奖机");
        
        fillBorder(inventory);
        updateDisplaySlots(inventory);
        addDrawButton(inventory);
        addCloseButton(inventory);
        
        player.openInventory(inventory);
    }

    private void fillBorder(Inventory inventory) {
        ItemStack border = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName("");
        border.setItemMeta(meta);
        
        int[] borderSlots = {0, 1, 2, 3, 4, 5, 6, 7, 8, 
                            9, 17, 
                            18, 19, 20, 21, 22, 23, 24, 25, 26};
        for (int slot : borderSlots) {
            inventory.setItem(slot, border);
        }
    }

    private void updateDisplaySlots(Inventory inventory) {
        List<LotteryPrize> prizes = lotteryManager.getPrizes();
        if (prizes.isEmpty()) {
            for (int slot : DISPLAY_SLOTS) {
                ItemStack empty = new ItemStack(Material.BARRIER);
                ItemMeta meta = empty.getItemMeta();
                meta.setDisplayName("§c暂无奖品");
                empty.setItemMeta(meta);
                inventory.setItem(slot, empty);
            }
            return;
        }
        
        for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
            int prizeIndex = ThreadLocalRandom.current().nextInt(prizes.size());
            LotteryPrize prize = prizes.get(prizeIndex);
            ItemStack item = prize.getItem().clone();
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                lore.add("§7稀有度: " + getRarityColor(prize.getRarity()) + prize.getRarity());
                lore.add("§7权重: " + prize.getWeight());
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inventory.setItem(DISPLAY_SLOTS[i], item);
        }
    }

    private void addDrawButton(Inventory inventory) {
        ItemStack drawButton = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = drawButton.getItemMeta();
        if (meta != null) {
            String currencySymbol = plugin.getCurrencyManager().getCurrency(lotteryManager.getCostCurrencyId()) != null ?
                    plugin.getCurrencyManager().getCurrency(lotteryManager.getCostCurrencyId()).getSymbol() : "";
            meta.setDisplayName("§e开始抽奖");
            List<String> lore = new ArrayList<>();
            lore.add("§7消耗: " + lotteryManager.getCostAmount() + " " + currencySymbol);
            meta.setLore(lore);
            
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(lotteryDrawKey, PersistentDataType.BOOLEAN, true);
            drawButton.setItemMeta(meta);
        }
        inventory.setItem(22, drawButton);
    }

    private void addCloseButton(Inventory inventory) {
        ItemStack closeButton = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = closeButton.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c关闭");
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(lotteryCloseKey, PersistentDataType.BOOLEAN, true);
            closeButton.setItemMeta(meta);
        }
        inventory.setItem(21, closeButton);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        if (!"§6抽奖机".equals(event.getView().getTitle())) return;
        
        event.setCancelled(true);
        
        Player player = (Player) event.getWhoClicked();
        int slot = event.getSlot();
        
        if (slot < 0 || slot >= INVENTORY_SIZE) return;
        
        ItemStack clickedItem = event.getCurrentItem();
        
        if (clickedItem == null || clickedItem.getType() == Material.AIR) return;
        
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null) return;
        
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        
        if (pdc.has(lotteryDrawKey, PersistentDataType.BOOLEAN)) {
            handleDrawClick(player);
        } else if (pdc.has(lotteryCloseKey, PersistentDataType.BOOLEAN)) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        if (!"§6抽奖机".equals(event.getView().getTitle())) return;
        
        Player player = (Player) event.getPlayer();
        spinningPlayers.remove(player.getUniqueId());
    }

    private void handleDrawClick(Player player) {
        UUID playerId = player.getUniqueId();
        
        long now = System.currentTimeMillis();
        Long lastDraw = drawCooldowns.get(playerId);
        if (lastDraw != null && now - lastDraw < getDrawCooldownMs()) {
            return;
        }
        
        AtomicBoolean spinning = spinningPlayers.computeIfAbsent(playerId, k -> new AtomicBoolean(false));
        if (!spinning.compareAndSet(false, true)) {
            ColorCodeConverter.sendMessage(player, "lottery-spinning");
            return;
        }
        
        try {
            if (!lotteryManager.canAfford(player)) {
                ColorCodeConverter.sendMessage(player, "lottery-not-enough-currency");
                return;
            }
            
            boolean success = lotteryManager.deductCost(player);
            if (!success) {
                ColorCodeConverter.sendMessage(player, "lottery-failed-deduct");
                return;
            }
            
            drawCooldowns.put(playerId, now);
            
            LotteryPrize winningPrize = lotteryManager.drawPrize();
            
            animateSpin(player, winningPrize);
        } finally {
            spinning.compareAndSet(true, false);
        }
    }

    private void animateSpin(Player player, LotteryPrize winningPrize) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (inventory == null) {
            spinningPlayers.remove(player.getUniqueId());
            return;
        }
        
        List<LotteryPrize> prizes = lotteryManager.getPrizes();
        if (prizes.isEmpty()) {
            spinningPlayers.remove(player.getUniqueId());
            return;
        }
        
        final UUID playerId = player.getUniqueId();
        final int spinDuration = 1500;
        final int frameInterval = 100;
        final int totalFrames = spinDuration / frameInterval;
        final int[] currentFrame = {0};
        
        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!player.isOnline()) {
                task.cancel();
                spinningPlayers.remove(playerId);
                return;
            }
            
            AtomicBoolean spinning = spinningPlayers.get(playerId);
            if (spinning == null) {
                task.cancel();
                return;
            }
            
            if (currentFrame[0] >= totalFrames) {
                task.cancel();
                showResult(player, winningPrize);
                return;
            }
            
            for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
                int prizeIndex = (ThreadLocalRandom.current().nextInt(prizes.size()) + currentFrame[0]) % prizes.size();
                LotteryPrize prize = prizes.get(prizeIndex);
                ItemStack item = prize.getItem().clone();
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    List<String> lore = new ArrayList<>();
                    lore.add("§7稀有度: " + getRarityColor(prize.getRarity()) + prize.getRarity());
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inventory.setItem(DISPLAY_SLOTS[i], item);
            }
            
            currentFrame[0]++;
        }, 0, Math.max(1, (int) (frameInterval / 50.0)));
    }

    private void showResult(Player player, LotteryPrize winningPrize) {
        Inventory inventory = player.getOpenInventory().getTopInventory();
        if (inventory == null) return;
        
        for (int i = 0; i < DISPLAY_SLOTS.length; i++) {
            if (i == 2) {
                ItemStack item = winningPrize.getItem().clone();
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName("§6§l中奖! " + (winningPrize.getDisplayName() != null ? winningPrize.getDisplayName().replace("&", "§") : ""));
                    List<String> lore = new ArrayList<>();
                    lore.add("§7稀有度: " + getRarityColor(winningPrize.getRarity()) + winningPrize.getRarity());
                    lore.add("");
                    lore.add("§a恭喜获得此奖品!");
                    meta.setLore(lore);
                    item.setItemMeta(meta);
                }
                inventory.setItem(DISPLAY_SLOTS[i], item);
            } else {
                ItemStack empty = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                ItemMeta meta = empty.getItemMeta();
                meta.setDisplayName("");
                empty.setItemMeta(meta);
                inventory.setItem(DISPLAY_SLOTS[i], empty);
            }
        }
        
        lotteryManager.givePrize(player, winningPrize);
        
        String message = "§6恭喜你在抽奖中获得了: " + getRarityColor(winningPrize.getRarity()) + 
                (winningPrize.getDisplayName() != null ? winningPrize.getDisplayName().replace("&", "§") : winningPrize.getItem().getType().name());
        ColorCodeConverter.sendMessage(player, message);
    }

    private String getRarityColor(String rarity) {
        if (rarity == null) {
            return "§f";
        }
        switch (rarity.toLowerCase()) {
            case "legendary":
                return "§d";
            case "epic":
                return "§5";
            case "rare":
                return "§b";
            case "uncommon":
                return "§a";
            case "common":
            default:
                return "§f";
        }
    }
}