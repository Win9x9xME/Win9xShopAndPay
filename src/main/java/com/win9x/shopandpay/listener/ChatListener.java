package com.win9x.shopandpay.listener;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.manager.AIAssistantManager;
import com.win9x.shopandpay.manager.LanguageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {

    private final Win9xShopAndPay plugin;
    private final AIAssistantManager aiAssistantManager;
    private final LanguageManager languageManager;

    public ChatListener(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.aiAssistantManager = plugin.getAIAssistantManager();
        this.languageManager = plugin.getLanguageManager();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        String message = event.getMessage();
        String triggerPrefix = plugin.getTriggerPrefix();

        if (message.startsWith(triggerPrefix)) {
            String query = message.substring(triggerPrefix.length()).trim();
            
            if (query.isEmpty()) {
                return;
            }

            if (!aiAssistantManager.isEnabled()) {
                player.sendMessage(languageManager.getMessage("ai-not-enabled", player));
                return;
            }

            if (!player.hasPermission("win9xshopandpay.ai.use")) {
                player.sendMessage(languageManager.getMessage("ai-no-permission", player));
                return;
            }

            event.setCancelled(true);
            aiAssistantManager.processMessage(player, query);
        }
    }
}