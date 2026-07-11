package com.win9x.shopandpay.manager;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.util.ColorCodeConverter;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final Win9xShopAndPay plugin;
    private final Map<String, FileConfiguration> languages = new HashMap<>();
    private String defaultLanguage = "zh-CN";

    private static final String[] SUPPORTED_LANGUAGES = {
        "zh-CN", "zh-TW", "en-US", "en-GB", "ja-JP", "ko-KR"
    };

    public LanguageManager(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        loadDefaultLanguage();
        loadLanguages();
    }

    private void loadDefaultLanguage() {
        FileConfiguration config = plugin.getConfig();
        String lang = config.getString("language.default", "zh-CN");
        
        if (isValidLanguage(lang)) {
            defaultLanguage = lang;
        } else {
            plugin.getLogger().warning("Invalid default language: " + lang + ", using zh-CN instead");
        }
    }

    private void loadLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "languages");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        for (String lang : SUPPORTED_LANGUAGES) {
            File langFile = new File(langFolder, lang + ".yml");
            
            if (!langFile.exists()) {
                try (InputStream is = plugin.getResource("languages/" + lang + ".yml")) {
                    if (is != null) {
                        Files.copy(is, langFile.toPath());
                    } else {
                        langFile.createNewFile();
                    }
                } catch (IOException e) {
                    plugin.getLogger().severe("Failed to create language file: " + lang + ".yml");
                    continue;
                }
            }
            
            FileConfiguration config = YamlConfiguration.loadConfiguration(langFile);
            languages.put(lang, config);
        }
    }

    public void reloadLanguages() {
        languages.clear();
        loadDefaultLanguage();
        loadLanguages();
        plugin.getLogger().info("Languages reloaded successfully");
    }

    public String getMessage(String key, String language) {
        FileConfiguration langConfig = languages.get(language);
        if (langConfig == null) {
            langConfig = languages.get(defaultLanguage);
        }
        
        String message = langConfig.getString(key);
        if (message == null) {
            langConfig = languages.get(defaultLanguage);
            message = langConfig.getString(key);
        }
        
        return message != null ? message : key;
    }

    public String getMessage(String key, Player player) {
        String language = plugin.getConfig().getString("language.default", "zh-CN");
        if (!isValidLanguage(language)) {
            language = defaultLanguage;
        }
        return getMessage(key, language);
    }

    public String getMessage(String key, Player player, Object... replacements) {
        String message = getMessage(key, player);
        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
        }
        return message;
    }

    public Component getMessageComponent(String key, Player player) {
        String message = getMessage(key, player);
        return ColorCodeConverter.convert(message);
    }

    public Component getMessageComponent(String key, Player player, Object... replacements) {
        String message = getMessage(key, player, replacements);
        return ColorCodeConverter.convert(message);
    }

    public Component getMessageComponent(String key, String language) {
        String message = getMessage(key, language);
        return ColorCodeConverter.convert(message);
    }

    public Component getMessageComponent(String key, String language, Object... replacements) {
        String message = getMessage(key, language, replacements);
        return ColorCodeConverter.convert(message);
    }

    public String getMessage(String key, String language, Object... replacements) {
        String message = getMessage(key, language);
        for (int i = 0; i < replacements.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(replacements[i]));
        }
        return message;
    }

    private String getPlayerLanguage(Player player) {
        String locale = player.getLocale();
        
        if (locale.startsWith("zh")) {
            if (locale.equals("zh_TW") || locale.equals("zh_HK") || locale.equals("zh_MO")) {
                return "zh-TW";
            }
            return "zh-CN";
        } else if (locale.startsWith("ja")) {
            return "ja-JP";
        } else if (locale.startsWith("ko")) {
            return "ko-KR";
        } else if (locale.startsWith("en")) {
            if (locale.equals("en_GB") || locale.equals("en_AU") || locale.equals("en_CA") || locale.equals("en_NZ")) {
                return "en-GB";
            }
            return "en-US";
        }
        
        return defaultLanguage;
    }

    public String[] getSupportedLanguages() {
        return SUPPORTED_LANGUAGES;
    }

    public boolean isValidLanguage(String language) {
        for (String lang : SUPPORTED_LANGUAGES) {
            if (lang.equalsIgnoreCase(language)) {
                return true;
            }
        }
        return false;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }
}