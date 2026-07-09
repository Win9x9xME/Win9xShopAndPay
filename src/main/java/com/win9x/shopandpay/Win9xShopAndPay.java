package com.win9x.shopandpay;

import com.win9x.shopandpay.commands.Win9xShopAndPayCommand;
import com.win9x.shopandpay.gui.ShopGUI;
import com.win9x.shopandpay.gui.ShopItemListener;
import com.win9x.shopandpay.listener.ChatListener;
import com.win9x.shopandpay.listener.PlayerJoinListener;
import com.win9x.shopandpay.manager.AIAssistantManager;
import com.win9x.shopandpay.manager.CDKeyManager;
import com.win9x.shopandpay.manager.CurrencyManager;
import com.win9x.shopandpay.manager.LanguageManager;
import com.win9x.shopandpay.manager.ShopManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Win9xShopAndPay extends JavaPlugin {

    private static Win9xShopAndPay instance;
    private Economy economy = null;
    private ShopManager shopManager;
    private CDKeyManager cdKeyManager;
    private CurrencyManager currencyManager;
    private LanguageManager languageManager;
    private AIAssistantManager aiAssistantManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        secureDataFolder();
        
        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found! Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        currencyManager = new CurrencyManager(this);
        shopManager = new ShopManager(this);
        cdKeyManager = new CDKeyManager(this);
        languageManager = new LanguageManager(this);
        aiAssistantManager = new AIAssistantManager(this);
        
        registerCommands();
        registerEvents();
        
        getLogger().info("Win9xShopAndPay has been enabled!");
    }

    private void secureDataFolder() {
        try {
            File dataFolder = getDataFolder();
            if (dataFolder.exists() && dataFolder.isDirectory()) {
                dataFolder.setReadable(true, false);
                dataFolder.setWritable(true, false);
                dataFolder.setExecutable(true, false);
                
                for (File file : dataFolder.listFiles()) {
                    if (file.isFile()) {
                        file.setReadable(true, false);
                        file.setWritable(true, false);
                    }
                }
                
                File configFile = new File(dataFolder, "config.yml");
                if (configFile.exists()) {
                    configFile.setReadable(true, false);
                    configFile.setWritable(true, false);
                }
            }
        } catch (SecurityException e) {
            getLogger().warning("Could not set secure permissions on data folder: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        cdKeyManager.saveCDKeys();
        currencyManager.savePlayerBalances();
        if (aiAssistantManager != null) {
            aiAssistantManager.shutdown();
        }
        getLogger().info("Win9xShopAndPay has been disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    private void registerCommands() {
        getCommand("win9xshopandpay").setExecutor(new Win9xShopAndPayCommand(this));
    }

    private void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new ShopGUI(this), this);
        pm.registerEvents(new ShopItemListener(this), this);
        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new ChatListener(this), this);
    }

    public static Win9xShopAndPay getInstance() {
        return instance;
    }

    public Economy getEconomy() {
        return economy;
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public CDKeyManager getCDKeyManager() {
        return cdKeyManager;
    }

    public CurrencyManager getCurrencyManager() {
        return currencyManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public AIAssistantManager getAIAssistantManager() {
        return aiAssistantManager;
    }

    public String getTriggerPrefix() {
        return getConfig().getString("ai-assistant.trigger-prefix", "#");
    }

    public void reloadAIConfig() {
        if (aiAssistantManager != null) {
            aiAssistantManager.loadConfig();
        }
    }
}