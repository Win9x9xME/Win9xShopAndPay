package com.win9x.shopandpay.manager;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.CDKey;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CDKeyManager {

    private final Win9xShopAndPay plugin;
    private final Map<String, CDKey> cdKeys = new HashMap<>();
    private File cdKeyFile;
    private FileConfiguration cdKeyConfig;

    public CDKeyManager(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        loadCDKeys();
    }

    private void loadCDKeys() {
        cdKeyFile = new File(plugin.getDataFolder(), "cdkeys.yml");
        if (!cdKeyFile.exists()) {
            try {
                cdKeyFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create cdkeys.yml");
            }
        }
        cdKeyConfig = YamlConfiguration.loadConfiguration(cdKeyFile);
        
        ConfigurationSection section = cdKeyConfig.getConfigurationSection("cdkeys");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            ConfigurationSection cdKeySection = section.getConfigurationSection(key);
            if (cdKeySection == null) continue;

            String itemType = cdKeySection.getString("item-type", "DIAMOND");
            int amount = cdKeySection.getInt("amount", 1);
            String name = cdKeySection.getString("name", "");
            int uses = cdKeySection.getInt("uses", 0);
            int maxUses = cdKeySection.getInt("max-uses", 1);
            long expiryTime = cdKeySection.getLong("expiry-time", 0);
            
            Set<String> usedByPlayers = new HashSet<>();
            List<String> usedList = cdKeySection.getStringList("used-by");
            if (usedList != null) {
                usedByPlayers.addAll(usedList);
            }

            Material material;
            try {
                material = org.bukkit.Material.valueOf(itemType);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid material type '" + itemType + "' in CDKey: " + key + ", using DIAMOND instead");
                material = org.bukkit.Material.DIAMOND;
            }
            
            ItemStack item = new ItemStack(material, amount);
            if (!name.isEmpty()) {
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    meta.setDisplayName(name);
                    item.setItemMeta(meta);
                }
            }

            cdKeys.put(key, new CDKey(key, item, uses, maxUses, expiryTime, usedByPlayers));
        }
    }

    public void saveCDKeys() {
        cdKeyConfig.set("cdkeys", null);
        
        for (Map.Entry<String, CDKey> entry : cdKeys.entrySet()) {
            CDKey cdKey = entry.getValue();
            String path = "cdkeys." + entry.getKey();
            
            cdKeyConfig.set(path + ".item-type", cdKey.getItem().getType().name());
            cdKeyConfig.set(path + ".amount", cdKey.getItem().getAmount());
            cdKeyConfig.set(path + ".name", cdKey.getItem().getItemMeta() != null ? 
                    cdKey.getItem().getItemMeta().getDisplayName() : "");
            cdKeyConfig.set(path + ".uses", cdKey.getUses());
            cdKeyConfig.set(path + ".max-uses", cdKey.getMaxUses());
            cdKeyConfig.set(path + ".expiry-time", cdKey.getExpiryTime());
            cdKeyConfig.set(path + ".used-by", new ArrayList<>(cdKey.getUsedByPlayers()));
        }

        try {
            cdKeyConfig.save(cdKeyFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save cdkeys.yml");
        }
    }

    public String generateCDKey(ItemStack item, int maxUses, long expiryTime) {
        String key;
        do {
            key = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        } while (cdKeys.containsKey(key));

        cdKeys.put(key, new CDKey(key, item, 0, maxUses, expiryTime));
        saveCDKeys();
        return key;
    }

    public enum RedeemResult {
        SUCCESS,
        NOT_FOUND,
        EXPIRED,
        ALREADY_USED
    }

    public RedeemResult redeemCDKey(String key, org.bukkit.entity.Player player) {
        CDKey cdKey = cdKeys.get(key);
        if (cdKey == null) {
            return RedeemResult.NOT_FOUND;
        }

        if (cdKey.isExpired()) {
            return RedeemResult.EXPIRED;
        }

        String playerId = player.getUniqueId().toString();
        if (!cdKey.canBeUsedByPlayer(playerId)) {
            return RedeemResult.ALREADY_USED;
        }

        player.getInventory().addItem(cdKey.getItem());
        cdKey.use(playerId);
        saveCDKeys();
        return RedeemResult.SUCCESS;
    }

    public boolean deleteCDKey(String key) {
        if (cdKeys.containsKey(key)) {
            cdKeys.remove(key);
            saveCDKeys();
            return true;
        }
        return false;
    }

    public CDKey getCDKey(String key) {
        return cdKeys.get(key);
    }

    public Collection<CDKey> getAllCDKeys() {
        return cdKeys.values();
    }
}