package com.win9x.shopandpay;

import com.win9x.shopandpay.commands.Win9xShopAndPayCommand;
import com.win9x.shopandpay.gui.LotteryGUI;
import com.win9x.shopandpay.gui.ShopGUI;
import com.win9x.shopandpay.gui.ShopItemListener;
import com.win9x.shopandpay.listener.EasterEggListener;
import com.win9x.shopandpay.listener.PlayerJoinListener;
import com.win9x.shopandpay.manager.CDKeyManager;
import com.win9x.shopandpay.manager.CurrencyManager;
import com.win9x.shopandpay.manager.EasterEggManager;
import com.win9x.shopandpay.manager.LanguageManager;
import com.win9x.shopandpay.manager.LotteryManager;
import com.win9x.shopandpay.manager.ShopManager;
import com.win9x.shopandpay.server.SimpleHttpServer;
import com.win9x.shopandpay.util.ColorCodeConverter;
import com.win9x.shopandpay.util.DebugLoggerHandler;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class Win9xShopAndPay extends JavaPlugin {

    private static Win9xShopAndPay instance;
    private Economy economy = null;
    private ShopManager shopManager;
    private CDKeyManager cdKeyManager;
    private CurrencyManager currencyManager;
    private LanguageManager languageManager;
    private LotteryManager lotteryManager;
    private LotteryGUI lotteryGUI;
    private ShopGUI shopGUI;
    private com.win9x.shopandpay.gui.MarketGUI marketGUI;
    private com.win9x.shopandpay.gui.BuybackGUI buybackGUI;
    private com.win9x.shopandpay.gui.GuiShopLauncherGUI guiShopLauncherGUI;
    private com.win9x.shopandpay.gui.GuiShopPanelGUI guiShopPanelGUI;
    private com.win9x.shopandpay.gui.GuiSellPanelGUI guiSellPanelGUI;
    private com.win9x.shopandpay.manager.GuiShopManager guiShopManager;
    private com.win9x.shopandpay.manager.AiManager aiManager;
    private com.win9x.shopandpay.manager.MarketManager marketManager;
    private com.win9x.shopandpay.manager.BuybackManager buybackManager;
    private com.win9x.shopandpay.manager.LoanManager loanManager;
    private com.win9x.shopandpay.manager.LotteryTicketManager lotteryTicketManager;
    private com.win9x.shopandpay.manager.BankManager bankManager;
    private EasterEggManager easterEggManager;
    private BukkitAudiences bukkitAudiences;
    private SimpleHttpServer httpServer;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        secureDataFolder();
        
        bukkitAudiences = BukkitAudiences.create(this);
        ColorCodeConverter.setBukkitAudiences(bukkitAudiences);
        
        if (!setupEconomy()) {
            getLogger().warning("Vault economy not found! Plugin will use internal currency system only.");
        }
        
        currencyManager = new CurrencyManager(this);
        shopManager = new ShopManager(this);
        cdKeyManager = new CDKeyManager(this);
        languageManager = new LanguageManager(this);
        lotteryManager = new LotteryManager(this);
        easterEggManager = new EasterEggManager(this);
        marketManager = new com.win9x.shopandpay.manager.MarketManager(this);
        buybackManager = new com.win9x.shopandpay.manager.BuybackManager(this);
        guiShopManager = new com.win9x.shopandpay.manager.GuiShopManager(this);
        aiManager = new com.win9x.shopandpay.manager.AiManager(this);
        loanManager = new com.win9x.shopandpay.manager.LoanManager(this);
        lotteryTicketManager = new com.win9x.shopandpay.manager.LotteryTicketManager(this);
        bankManager = new com.win9x.shopandpay.manager.BankManager(this);

        registerCommands();
        registerEvents();
        startSchedulers();
        
        generateDefaultBuyCurrencyHtml();
        
        getLogger().addHandler(new DebugLoggerHandler(this));
        
        getLogger().info("Win9xShopAndPay has been enabled!");
        getLogger().info("GitHub: https://github.com/Win9x9xME/Win9xShopAndPay");
    }
    
    private void generateDefaultBuyCurrencyHtml() {
        String fileName = getConfig().getString("gui.buy-currency-file", "buy-currency.html");
        File htmlFile = new File(getDataFolder(), fileName);
        
        if (htmlFile.exists()) {
            return;
        }
        
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(htmlFile), java.nio.charset.StandardCharsets.UTF_8))) {
            
            writer.write("<!DOCTYPE html>\n");
            writer.write("<html lang=\"zh-CN\">\n");
            writer.write("<head>\n");
            writer.write("    <meta charset=\"UTF-8\">\n");
            writer.write("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            writer.write("    <title>购买币种 - Win9xShopAndPay</title>\n");
            writer.write("    <style>\n");
            writer.write("        body { font-family: 'Microsoft YaHei', Arial, sans-serif; background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%); color: #fff; margin: 0; padding: 40px; min-height: 100vh; }\n");
            writer.write("        .container { max-width: 800px; margin: 0 auto; }\n");
            writer.write("        h1 { text-align: center; margin-bottom: 40px; color: #00ff88; }\n");
            writer.write("        .card { background: rgba(255,255,255,0.1); backdrop-filter: blur(10px); border-radius: 15px; padding: 30px; margin-bottom: 20px; border: 1px solid rgba(255,255,255,0.2); }\n");
            writer.write("        .card h2 { color: #00ff88; margin-top: 0; }\n");
            writer.write("        .price { font-size: 24px; color: #ffd700; font-weight: bold; }\n");
            writer.write("        .btn { display: inline-block; padding: 12px 30px; background: linear-gradient(135deg, #00ff88, #00cc6a); color: #000; text-decoration: none; border-radius: 8px; font-weight: bold; transition: transform 0.2s; }\n");
            writer.write("        .btn:hover { transform: scale(1.05); }\n");
            writer.write("        .note { background: rgba(255,215,0,0.1); border-left: 4px solid #ffd700; padding: 15px; margin-top: 20px; border-radius: 0 8px 8px 0; }\n");
            writer.write("        .footer { text-align: center; margin-top: 40px; color: #888; font-size: 14px; }\n");
            writer.write("    </style>\n");
            writer.write("</head>\n");
            writer.write("<body>\n");
            writer.write("    <div class=\"container\">\n");
            writer.write("        <h1>💰 购买币种</h1>\n");
            writer.write("        <div class=\"card\">\n");
            writer.write("            <h2>金币充值</h2>\n");
            writer.write("            <p>充值金币以购买商店物品</p>\n");
            writer.write("            <div class=\"price\">¥10 = 1000 金币</div>\n");
            writer.write("            <a href=\"#\" class=\"btn\">立即购买</a>\n");
            writer.write("        </div>\n");
            writer.write("        <div class=\"card\">\n");
            writer.write("            <h2>宝石充值</h2>\n");
            writer.write("            <p>充值宝石以购买稀有物品</p>\n");
            writer.write("            <div class=\"price\">¥20 = 100 宝石</div>\n");
            writer.write("            <a href=\"#\" class=\"btn\">立即购买</a>\n");
            writer.write("        </div>\n");
            writer.write("        <div class=\"note\">\n");
            writer.write("            <strong>注意:</strong><br>\n");
            writer.write("            - 充值后请返回游戏查看余额<br>\n");
            writer.write("            - 如果充值未到账，请联系管理员<br>\n");
            writer.write("            - 本页面由 Win9xShopAndPay 插件提供\n");
            writer.write("        </div>\n");
            writer.write("        <div class=\"footer\">\n");
            writer.write("            Powered by Win9xShopAndPay | GitHub: https://github.com/Win9x9xME/Win9xShopAndPay\n");
            writer.write("        </div>\n");
            writer.write("    </div>\n");
            writer.write("</body>\n");
            writer.write("</html>");
            
            getLogger().info("已生成默认购买币种页面: " + fileName);
        } catch (IOException e) {
            getLogger().warning("生成购买币种页面失败: " + e.getMessage());
        }
    }
    
    public void startHttpServerOnDemand() {
        String mode = getConfig().getString("gui.buy-currency-mode", "url");
        if ("server".equalsIgnoreCase(mode)) {
            if (httpServer == null) {
                httpServer = new SimpleHttpServer(this);
            }
            httpServer.start();
        }
    }

    private void secureDataFolder() {
        File dataFolder = getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    private void startSchedulers() {
        // Run periodic system tasks every 5 minutes (6000 ticks).
        long intervalTicks = 20L * 60L * 5L;
        getServer().getScheduler().runTaskTimer(this, () -> {
            try {
                if (loanManager != null) {
                    loanManager.tickOverdue();
                }
            } catch (Throwable t) {
                getLogger().warning("Loan tick failed: " + t.getMessage());
            }
            try {
                if (lotteryTicketManager != null) {
                    lotteryTicketManager.tick();
                }
            } catch (Throwable t) {
                getLogger().warning("Lottery ticket tick failed: " + t.getMessage());
            }
            try {
                if (bankManager != null) {
                    bankManager.tick();
                }
            } catch (Throwable t) {
                getLogger().warning("Bank tick failed: " + t.getMessage());
            }
        }, intervalTicks, intervalTicks);
    }

    @Override
    public void onDisable() {
        if (cdKeyManager != null) {
            cdKeyManager.saveCDKeys();
        }
        if (currencyManager != null) {
            currencyManager.savePlayerBalances();
        }
        if (marketManager != null) {
            marketManager.save();
        }
        if (loanManager != null) {
            loanManager.save();
        }
        if (lotteryTicketManager != null) {
            lotteryTicketManager.save();
        }
        if (bankManager != null) {
            bankManager.save();
        }
        if (httpServer != null) {
            httpServer.stop();
        }
        if (bukkitAudiences != null) {
            bukkitAudiences.close();
        }
        getLogger().info("Win9xShopAndPay has been disabled!");
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().severe("Vault plugin not found! Please install Vault in your server's plugins/ folder.");
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            getLogger().severe("No economy provider found! Vault is installed but no economy plugin (EssentialsX, CMI, etc.) is providing the Economy service.");
            return false;
        }
        economy = rsp.getProvider();
        if (economy == null) {
            getLogger().severe("Economy provider is null! Please check your economy plugin configuration.");
            return false;
        }
        getLogger().info("Successfully hooked into Vault economy: " + economy.getName());
        return true;
    }

    private void registerCommands() {
        Win9xShopAndPayCommand command = new Win9xShopAndPayCommand(this);
        getCommand("win9xshopandpay").setExecutor(command);
        getCommand("win9xshopandpay").setTabCompleter(command);
    }

    private void registerEvents() {
        PluginManager pm = getServer().getPluginManager();
        shopGUI = new ShopGUI(this);
        pm.registerEvents(shopGUI, this);
        pm.registerEvents(new ShopItemListener(this), this);
        pm.registerEvents(new PlayerJoinListener(this), this);
        pm.registerEvents(new EasterEggListener(easterEggManager), this);
        marketGUI = new com.win9x.shopandpay.gui.MarketGUI(this);
        pm.registerEvents(marketGUI, this);
        buybackGUI = new com.win9x.shopandpay.gui.BuybackGUI(this);
        pm.registerEvents(buybackGUI, this);

        guiShopLauncherGUI = new com.win9x.shopandpay.gui.GuiShopLauncherGUI(this);
        pm.registerEvents(guiShopLauncherGUI, this);
        guiShopPanelGUI = new com.win9x.shopandpay.gui.GuiShopPanelGUI(this);
        pm.registerEvents(guiShopPanelGUI, this);
        guiSellPanelGUI = new com.win9x.shopandpay.gui.GuiSellPanelGUI(this);
        pm.registerEvents(guiSellPanelGUI, this);

        pm.registerEvents(new com.win9x.shopandpay.listener.ChatListener(this), this);
        lotteryGUI = new LotteryGUI(this);
        pm.registerEvents(lotteryGUI, this);
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

    public BukkitAudiences getBukkitAudiences() {
        return bukkitAudiences;
    }
    
    public SimpleHttpServer getHttpServer() {
        return httpServer;
    }
    
    public LotteryManager getLotteryManager() {
        return lotteryManager;
    }
    
    public LotteryGUI getLotteryGUI() {
        return lotteryGUI;
    }
    
    public EasterEggManager getEasterEggManager() {
        return easterEggManager;
    }

    public ShopGUI getShopGUI() {
        return shopGUI;
    }

    public com.win9x.shopandpay.manager.AiManager getAiManager() {
        return aiManager;
    }

    public com.win9x.shopandpay.manager.MarketManager getMarketManager() {
        return marketManager;
    }

    public com.win9x.shopandpay.manager.BuybackManager getBuybackManager() {
        return buybackManager;
    }

    public com.win9x.shopandpay.gui.MarketGUI getMarketGUI() {
        return marketGUI;
    }

    public com.win9x.shopandpay.gui.BuybackGUI getBuybackGUI() {
        return buybackGUI;
    }

    public com.win9x.shopandpay.gui.GuiShopLauncherGUI getGuiShopLauncherGUI() {
        return guiShopLauncherGUI;
    }

    public com.win9x.shopandpay.gui.GuiShopPanelGUI getGuiShopPanelGUI() {
        return guiShopPanelGUI;
    }

    public com.win9x.shopandpay.gui.GuiSellPanelGUI getGuiSellPanelGUI() {
        return guiSellPanelGUI;
    }

    public com.win9x.shopandpay.manager.GuiShopManager getGuiShopManager() {
        return guiShopManager;
    }

    public com.win9x.shopandpay.manager.LoanManager getLoanManager() {
        return loanManager;
    }

    public com.win9x.shopandpay.manager.LotteryTicketManager getLotteryTicketManager() {
        return lotteryTicketManager;
    }

    public com.win9x.shopandpay.manager.BankManager getBankManager() {
        return bankManager;
    }
}