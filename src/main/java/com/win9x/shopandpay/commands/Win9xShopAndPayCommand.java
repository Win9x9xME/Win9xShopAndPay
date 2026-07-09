package com.win9x.shopandpay.commands;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.CDKey;
import com.win9x.shopandpay.data.Currency;
import com.win9x.shopandpay.gui.ShopGUI;
import com.win9x.shopandpay.gui.ShopItemListener;
import com.win9x.shopandpay.manager.CDKeyManager;
import com.win9x.shopandpay.manager.CurrencyManager;
import com.win9x.shopandpay.manager.LanguageManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class Win9xShopAndPayCommand implements CommandExecutor {

    private final Win9xShopAndPay plugin;
    private final CurrencyManager currencyManager;
    private final CDKeyManager cdKeyManager;
    private final LanguageManager languageManager;
    private final Map<String, Long> giveShopCooldowns = new HashMap<>();

    public Win9xShopAndPayCommand(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.currencyManager = plugin.getCurrencyManager();
        this.cdKeyManager = plugin.getCDKeyManager();
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "shop":
                return handleShopCommand(sender, args);
            case "cdkey":
                return handleCDKeyCommand(sender, args);
            case "currency":
                return handleCurrencyCommand(sender, args);
            case "reload":
                return handleReloadCommand(sender);
            case "give_shop":
                return handleGiveShopCommand(sender);
            default:
                sendHelp(sender);
                return true;
        }
    }

    private boolean handleShopCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("only-player", sender));
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("win9xshopandpay.shop")) {
            player.sendMessage(getMessage("no-permission", player));
            return true;
        }

        String currencyId = currencyManager.getDefaultCurrency().getId();
        if (args.length >= 2) {
            currencyId = args[1].toLowerCase();
        }

        Currency currency = currencyManager.getCurrency(currencyId);
        if (currency == null) {
            player.sendMessage(getMessage("currency-invalid", player));
            return true;
        }

        player.sendMessage(getMessage("text-shop-title", player));
        
        for (com.win9x.shopandpay.data.ShopItem shopItem : plugin.getShopManager().getShopItems()) {
            double price = shopItem.getPrice(currencyId);
            String priceStr;
            
            if (price > 0) {
                priceStr = currency.getSymbol() + price;
            } else {
                priceStr = getMessage("shop-currency-not-available", player);
            }
            
            String itemName = shopItem.getItem().getItemMeta() != null && 
                    shopItem.getItem().getItemMeta().hasDisplayName() ?
                    shopItem.getItem().getItemMeta().getDisplayName() :
                    shopItem.getItem().getType().name();
            
            player.sendMessage(getMessage("text-shop-item", player, 
                    itemName, shopItem.getItem().getAmount(), 
                    currency.getName(), priceStr));
        }
        
        return true;
    }

    private boolean handleCDKeyCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendCDKeyHelp(sender);
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "redeem":
                return redeemCDKey(sender, args);
            case "create":
                return createCDKey(sender, args);
            case "delete":
                return deleteCDKey(sender, args);
            case "list":
                return listCDKeys(sender);
            default:
                sendCDKeyHelp(sender);
                return true;
        }
    }

    private boolean redeemCDKey(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("only-player", sender));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(getMessage("error-usage-redeem-cdkey", sender));
            return true;
        }

        Player player = (Player) sender;
        String key = args[2].toUpperCase();

        CDKeyManager.RedeemResult result = cdKeyManager.redeemCDKey(key, player);
        if (result == CDKeyManager.RedeemResult.NOT_FOUND) {
            player.sendMessage(getMessage("cdkey-not-found", player));
            return true;
        } else if (result == CDKeyManager.RedeemResult.EXPIRED) {
            player.sendMessage(getMessage("cdkey-invalid-or-expired", player));
            return true;
        } else if (result == CDKeyManager.RedeemResult.ALREADY_USED) {
            player.sendMessage(getMessage("cdkey-already-used", player));
            return true;
        }

        player.sendMessage(getMessage("cdkey-redeem-success", player));
        return true;
    }

    private boolean createCDKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("win9xshopandpay.cdkey.create")) {
            sender.sendMessage(getMessage("no-permission", sender));
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(getMessage("error-usage-create-cdkey", sender));
            sender.sendMessage(getMessage("error-material-example", sender));
            sender.sendMessage(getMessage("error-expiry-format", sender));
            return true;
        }

        Material material;
        try {
            material = Material.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(getMessage("error-invalid-material", sender));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("error-invalid-number", sender));
            return true;
        }

        int maxUses = 1;
        if (args.length >= 5) {
            try {
                maxUses = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sender.sendMessage(getMessage("error-invalid-max-uses", sender));
                return true;
            }
        }

        long expiryTime = 0;
        if (args.length >= 6) {
            expiryTime = parseExpiryTime(args[5]);
            if (expiryTime < 0) {
                sender.sendMessage(getMessage("error-invalid-expiry", sender));
                sender.sendMessage(getMessage("error-expiry-format", sender));
                return true;
            }
        }

        ItemStack item = new ItemStack(material, amount);
        String key = cdKeyManager.generateCDKey(item, maxUses, expiryTime);

        sender.sendMessage(getMessage("cdkey-create-success", sender, key));
        sender.sendMessage(getMessage("cdkey-create-item", sender, material.name(), amount));
        sender.sendMessage(getMessage("cdkey-create-max-uses", sender, maxUses));
        if (expiryTime > 0) {
            sender.sendMessage(getMessage("cdkey-create-expiry", sender, formatExpiryTime(expiryTime)));
        } else {
            sender.sendMessage(getMessage("cdkey-create-permanent", sender));
        }
        return true;
    }

    private long parseExpiryTime(String input) {
        input = input.toLowerCase().trim();
        long multiplier = 0;
        
        if (input.endsWith("d")) {
            multiplier = 24L * 60 * 60 * 1000;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("h")) {
            multiplier = 60L * 60 * 1000;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("m")) {
            multiplier = 60L * 1000;
            input = input.substring(0, input.length() - 1);
        } else if (input.endsWith("w")) {
            multiplier = 7L * 24 * 60 * 60 * 1000;
            input = input.substring(0, input.length() - 1);
        } else {
            return -1;
        }

        try {
            long value = Long.parseLong(input);
            return System.currentTimeMillis() + (value * multiplier);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String formatExpiryTime(long expiryTime) {
        long remaining = expiryTime - System.currentTimeMillis();
        if (remaining <= 0) {
            return "已过期";
        }

        long days = remaining / (24L * 60 * 60 * 1000);
        remaining %= (24L * 60 * 60 * 1000);
        long hours = remaining / (60L * 60 * 1000);
        remaining %= (60L * 60 * 1000);
        long minutes = remaining / (60L * 1000);

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天");
        if (hours > 0) sb.append(hours).append("小时");
        if (minutes > 0) sb.append(minutes).append("分钟");
        
        return sb.toString();
    }

    private boolean deleteCDKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("win9xshopandpay.cdkey.delete")) {
            sender.sendMessage(getMessage("no-permission", sender));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(getMessage("error-usage-delete-cdkey", sender));
            return true;
        }

        String key = args[2].toUpperCase();
        
        if (!cdKeyManager.deleteCDKey(key)) {
            sender.sendMessage(getMessage("cdkey-not-found", sender));
            return true;
        }

        sender.sendMessage(getMessage("cdkey-delete-success", sender));
        return true;
    }

    private boolean listCDKeys(CommandSender sender) {
        if (!sender.hasPermission("win9xshopandpay.cdkey.list")) {
            sender.sendMessage(getMessage("no-permission", sender));
            return true;
        }

        sender.sendMessage(getMessage("cdkey-list-title", sender));
        
        for (CDKey cdKey : cdKeyManager.getAllCDKeys()) {
            String expiryInfo;
            if (cdKey.hasExpiryDate()) {
                expiryInfo = getMessage("cdkey-expiry-info", sender, formatExpiryTime(cdKey.getExpiryTime()));
            } else {
                expiryInfo = getMessage("cdkey-expiry-permanent", sender);
            }
            
            sender.sendMessage(getMessage("cdkey-list-entry", sender, 
                    cdKey.getKey(), cdKey.getItem().getType().name(), 
                    cdKey.getItem().getAmount(), cdKey.getUses(), cdKey.getMaxUses(), expiryInfo));
        }

        return true;
    }

    private boolean handleCurrencyCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendCurrencyHelp(sender);
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "balance":
                return checkBalance(sender, args);
            case "give":
                return giveCurrency(sender, args);
            case "set":
                return setCurrency(sender, args);
            case "list":
                return listCurrencies(sender);
            default:
                sendCurrencyHelp(sender);
                return true;
        }
    }

    private boolean checkBalance(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("only-player", sender));
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("win9xshopandpay.currency.balance")) {
            player.sendMessage(getMessage("no-permission", player));
            return true;
        }
        
        if (args.length >= 3) {
            String currencyId = args[2].toLowerCase();
            Currency currency = currencyManager.getCurrency(currencyId);
            if (currency == null) {
                sender.sendMessage(getMessage("currency-invalid", sender));
                return true;
            }
            double balance = currencyManager.getBalance(player, currencyId);
            sender.sendMessage(getMessage("currency-balance-entry", sender, 
                    currency.getSymbol(), currency.getName(), ChatColor.YELLOW, balance));
        } else {
            sender.sendMessage(getMessage("currency-balance-title", sender));
            for (Currency currency : currencyManager.getAllCurrencies().values()) {
                double balance = currencyManager.getBalance(player, currency.getId());
                sender.sendMessage(getMessage("currency-balance-entry", sender, 
                        currency.getSymbol(), currency.getName(), ChatColor.YELLOW, balance));
            }
        }
        return true;
    }

    private boolean giveCurrency(CommandSender sender, String[] args) {
        if (!sender.hasPermission("win9xshopandpay.currency.give")) {
            sender.sendMessage(getMessage("no-permission", sender));
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(getMessage("error-usage-give-currency", sender));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-online", sender));
            return true;
        }

        String currencyId = args[3].toLowerCase();
        if (!currencyManager.isValidCurrency(currencyId)) {
            sender.sendMessage(getMessage("currency-invalid", sender));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("error-invalid-number", sender));
            return true;
        }

        currencyManager.deposit(target, currencyId, amount);
        Currency currency = currencyManager.getCurrency(currencyId);
        sender.sendMessage(getMessage("currency-give-success", sender, 
                target.getName(), currency.getSymbol(), amount, currency.getName()));
        target.sendMessage(getMessage("currency-give-receive", target, 
                currency.getSymbol(), amount, currency.getName()));
        return true;
    }

    private boolean setCurrency(CommandSender sender, String[] args) {
        if (!sender.hasPermission("win9xshopandpay.currency.set")) {
            sender.sendMessage(getMessage("no-permission", sender));
            return true;
        }

        if (args.length < 4) {
            sender.sendMessage(getMessage("error-usage-set-currency", sender));
            return true;
        }

        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(getMessage("player-not-online", sender));
            return true;
        }

        String currencyId = args[3].toLowerCase();
        if (!currencyManager.isValidCurrency(currencyId)) {
            sender.sendMessage(getMessage("currency-invalid", sender));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("error-invalid-number", sender));
            return true;
        }

        String playerId = target.getUniqueId().toString();
        currencyManager.getPlayerBalances().computeIfAbsent(playerId, k -> new java.util.HashMap<>()).put(currencyId, amount);
        currencyManager.savePlayerBalances();
        
        Currency currency = currencyManager.getCurrency(currencyId);
        sender.sendMessage(getMessage("currency-set-success", sender, 
                target.getName(), currency.getSymbol(), currency.getName(), amount));
        target.sendMessage(getMessage("currency-set-receive", target, 
                currency.getSymbol(), currency.getName(), amount));
        return true;
    }

    private boolean listCurrencies(CommandSender sender) {
        if (!sender.hasPermission("win9xshopandpay.currency.list")) {
            sender.sendMessage(getMessage("no-permission", sender));
            return true;
        }
        
        sender.sendMessage(getMessage("currency-list-title", sender));
        for (Currency currency : currencyManager.getAllCurrencies().values()) {
            String defaultTag = currency.isDefault() ? getMessage("currency-default-tag", sender) : "";
            sender.sendMessage(getMessage("currency-list-entry", sender, 
                    currency.getSymbol(), ChatColor.YELLOW, currency.getId(), currency.getName()) + defaultTag);
        }
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("win9xshopandpay.reload")) {
            sender.sendMessage(getMessage("no-permission", sender));
            return true;
        }

        plugin.reloadConfig();
        plugin.reloadAIConfig();
        sender.sendMessage(getMessage("reload-success", sender));
        return true;
    }

    private boolean handleGiveShopCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("only-player", sender));
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("win9xshopandpay.give_shop")) {
            player.sendMessage(getMessage("no-permission", player));
            return true;
        }

        int cooldownSeconds = plugin.getConfig().getInt("commands.give_shop_cooldown", 0);
        if (cooldownSeconds > 0) {
            String playerId = player.getUniqueId().toString();
            long now = System.currentTimeMillis();
            Long lastUsed = giveShopCooldowns.get(playerId);
            
            if (lastUsed != null && now - lastUsed < cooldownSeconds * 1000L) {
                long remaining = (cooldownSeconds * 1000L - (now - lastUsed)) / 1000L;
                player.sendMessage(getMessage("give-shop-cooldown", player, remaining));
                return true;
            }
            
            giveShopCooldowns.put(playerId, now);
        }

        ItemStack compass = ShopItemListener.createShopCompass(plugin, player);
        player.getInventory().addItem(compass);
        player.sendMessage(getMessage("give-shop-success", player));
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(getMessage("help-title", sender));
        sender.sendMessage(getMessage("help-shop", sender));
        sender.sendMessage(getMessage("help-cdkey", sender));
        sender.sendMessage(getMessage("help-currency", sender));
        if (sender.hasPermission("win9xshopandpay.give_shop")) {
            sender.sendMessage(getMessage("help-give-shop", sender));
        }
        if (sender.hasPermission("win9xshopandpay.reload")) {
            sender.sendMessage(getMessage("help-reload", sender));
        }
    }

    private void sendCDKeyHelp(CommandSender sender) {
        sender.sendMessage(getMessage("cdkey-help-title", sender));
        sender.sendMessage(getMessage("cdkey-help-redeem", sender));
        if (sender.hasPermission("win9xshopandpay.cdkey.create")) {
            sender.sendMessage(getMessage("cdkey-help-create", sender));
        }
        if (sender.hasPermission("win9xshopandpay.cdkey.delete")) {
            sender.sendMessage(getMessage("cdkey-help-delete", sender));
        }
        if (sender.hasPermission("win9xshopandpay.cdkey.list")) {
            sender.sendMessage(getMessage("cdkey-help-list", sender));
        }
    }

    private void sendCurrencyHelp(CommandSender sender) {
        sender.sendMessage(getMessage("currency-help-title", sender));
        sender.sendMessage(getMessage("currency-help-balance", sender));
        sender.sendMessage(getMessage("currency-help-list", sender));
        if (sender.hasPermission("win9xshopandpay.currency.give")) {
            sender.sendMessage(getMessage("currency-help-give", sender));
        }
        if (sender.hasPermission("win9xshopandpay.currency.set")) {
            sender.sendMessage(getMessage("currency-help-set", sender));
        }
    }

    private String getMessage(String key, CommandSender sender) {
        if (sender instanceof Player) {
            return languageManager.getMessage(key, (Player) sender);
        }
        return languageManager.getMessage(key, "en-US");
    }

    private String getMessage(String key, CommandSender sender, Object... replacements) {
        if (sender instanceof Player) {
            return languageManager.getMessage(key, (Player) sender, replacements);
        }
        return languageManager.getMessage(key, "en-US", replacements);
    }
}