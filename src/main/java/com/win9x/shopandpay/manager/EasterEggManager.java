package com.win9x.shopandpay.manager;

import com.win9x.shopandpay.Win9xShopAndPay;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class EasterEggManager {

    private final Win9xShopAndPay plugin;
    private File eggFile;
    private FileConfiguration eggConfig;
    private boolean lengshangRscEnabled;

    public EasterEggManager(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        eggFile = new File(plugin.getDataFolder(), "easter-egg.yml");
        
        if (!eggFile.exists()) {
            try (InputStream is = plugin.getResource("easter-egg.yml")) {
                if (is != null) {
                    Files.copy(is, eggFile.toPath());
                } else {
                    eggFile.createNewFile();
                }
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create easter-egg.yml");
                return;
            }
        }
        
        eggConfig = YamlConfiguration.loadConfiguration(eggFile);
        this.lengshangRscEnabled = eggConfig.getBoolean("lengshang-rsc.enabled", false);
    }

    public boolean isLengshangRscEnabled() {
        return lengshangRscEnabled;
    }

    public void reload() {
        loadConfig();
    }
}