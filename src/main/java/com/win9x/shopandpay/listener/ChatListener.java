package com.win9x.shopandpay.listener;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.gui.ShopGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

/**
 * Captures chat input for one-shot prompts (shop search) and AI conversation mode.
 */
public class ChatListener implements Listener {

    private final Win9xShopAndPay plugin;

    public ChatListener(Win9xShopAndPay plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (plugin.getAiManager() != null && plugin.getAiManager().isInAiMode(id)) {
            event.setCancelled(true);
            String message = event.getMessage().trim();
            if (message.equalsIgnoreCase("exit") || message.equalsIgnoreCase("退出")
                    || message.equalsIgnoreCase("q") || message.equalsIgnoreCase("quit")) {
                plugin.getAiManager().setAiMode(id, false);
                return;
            }
            plugin.getAiManager().askAsync(player, message);
            return;
        }

        ShopGUI shopGUI = plugin.getShopGUI();
        if (shopGUI != null && shopGUI.isAwaitingSearch(id)) {
            event.setCancelled(true);
            String currencyId = shopGUI.consumeSearchCurrency(id);
            String term = event.getMessage();
            Bukkit.getScheduler().runTask(plugin, () -> shopGUI.openShop(player, currencyId, term));
        }
    }
}
