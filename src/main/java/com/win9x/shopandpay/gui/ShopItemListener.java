package com.win9x.shopandpay.gui;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.manager.CurrencyManager;
import com.win9x.shopandpay.manager.LanguageManager;
import com.win9x.shopandpay.util.ColorCodeConverter;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ShopItemListener implements Listener {

    private final Win9xShopAndPay plugin;
    private final CurrencyManager currencyManager;
    private final LanguageManager languageManager;
    private final NamespacedKey shopCompassKey;
    private static final LegacyComponentSerializer SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public ShopItemListener(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.currencyManager = plugin.getCurrencyManager();
        this.languageManager = plugin.getLanguageManager();
        this.shopCompassKey = new NamespacedKey(plugin, "shop_compass");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.COMPASS) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer container = meta.getPersistentDataContainer();
        if (!container.has(shopCompassKey, PersistentDataType.BOOLEAN) || 
            !container.get(shopCompassKey, PersistentDataType.BOOLEAN)) {
            return;
        }

        event.setCancelled(true);
        
        ShopGUI shopGUI = new ShopGUI(plugin);
        shopGUI.openShop(player, currencyManager.getDefaultCurrency().getId());
    }

    public static ItemStack createShopCompass(Win9xShopAndPay plugin, Player player) {
        LanguageManager languageManager = plugin.getLanguageManager();
        NamespacedKey key = new NamespacedKey(plugin, "shop_compass");
        
        ItemStack compass = new ItemStack(Material.COMPASS);
        ItemMeta meta = compass.getItemMeta();
        
        if (meta != null) {
            String displayName = languageManager.getMessage("shop-item-name", player);
            meta.setDisplayName(displayName.replace("&", "§"));
            meta.setCustomModelData(1);
            
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(key, PersistentDataType.BOOLEAN, true);
            
            compass.setItemMeta(meta);
        }
        
        return compass;
    }
}