package com.win9x.shopandpay.listener;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.gui.ShopItemListener;
import com.win9x.shopandpay.manager.LanguageManager;
import com.win9x.shopandpay.util.ColorCodeConverter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class PlayerJoinListener implements Listener {

    private final Win9xShopAndPay plugin;
    private final LanguageManager languageManager;
    private final Set<String> joinedPlayers = new HashSet<>();
    private File playersFile;

    public PlayerJoinListener(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        loadJoinedPlayers();
    }

    private void loadJoinedPlayers() {
        playersFile = new File(plugin.getDataFolder(), "players.txt");
        
        if (playersFile.exists()) {
            try {
                Files.readAllLines(playersFile.toPath()).forEach(line -> {
                    if (!line.trim().isEmpty()) {
                        joinedPlayers.add(line.trim());
                    }
                });
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to load players.txt");
            }
        }
    }

    private synchronized void savePlayer(String playerId) {
        if (joinedPlayers.contains(playerId)) {
            return;
        }
        
        joinedPlayers.add(playerId);
        
        try {
            Files.write(playersFile.toPath(), joinedPlayers);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player to players.txt");
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerId = player.getUniqueId().toString();
        
        if (!joinedPlayers.contains(playerId)) {
            ItemStack compass = ShopItemListener.createShopCompass(plugin, player);
            player.getInventory().addItem(compass);
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("first-join-gift", player));
            savePlayer(playerId);
        }
    }
}