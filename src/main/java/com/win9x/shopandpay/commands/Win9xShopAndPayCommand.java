package com.win9x.shopandpay.commands;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.CDKey;
import com.win9x.shopandpay.data.Currency;
import com.win9x.shopandpay.gui.LotteryGUI;
import com.win9x.shopandpay.gui.ShopGUI;
import com.win9x.shopandpay.gui.ShopItemListener;
import com.win9x.shopandpay.manager.CDKeyManager;
import com.win9x.shopandpay.manager.CurrencyManager;
import com.win9x.shopandpay.manager.LanguageManager;
import com.win9x.shopandpay.util.ColorCodeConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Win9xShopAndPayCommand implements CommandExecutor, TabCompleter {

    private final Win9xShopAndPay plugin;
    private final CurrencyManager currencyManager;
    private final CDKeyManager cdKeyManager;
    private final LanguageManager languageManager;
    private final Map<String, Long> giveShopCooldowns = new ConcurrentHashMap<>();

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
            case "lottery":
                return handleLotteryCommand(sender, args);
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
            sendMessage(sender, "only-player");
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("win9xshopandpay.shop")) {
            sendMessage(player, "no-permission");
            return true;
        }

        String currencyId = currencyManager.getDefaultCurrency().getId();
        if (args.length >= 2) {
            currencyId = args[1].toLowerCase();
        }

        Currency currency = currencyManager.getCurrency(currencyId);
        if (currency == null) {
            sendMessage(player, "currency-invalid");
            return true;
        }

        sendMessage(player, "text-shop-title");
        
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
            
            sendMessage(player, "text-shop-item", itemName, shopItem.getItem().getAmount(), 
                    currency.getName(), priceStr);
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
            sendMessage(sender, "only-player");
            return true;
        }

        if (args.length < 3) {
            sendMessage(sender, "error-usage-redeem-cdkey");
            return true;
        }

        Player player = (Player) sender;
        String key = args[2].toUpperCase();

        CDKeyManager.RedeemResult result = cdKeyManager.redeemCDKey(key, player);
        if (result == CDKeyManager.RedeemResult.NOT_FOUND) {
            sendMessage(player, "cdkey-not-found");
            return true;
        } else if (result == CDKeyManager.RedeemResult.EXPIRED) {
            sendMessage(player, "cdkey-invalid-or-expired");
            return true;
        } else if (result == CDKeyManager.RedeemResult.ALREADY_USED) {
            sendMessage(player, "cdkey-already-used");
            return true;
        }

        sendMessage(player, "cdkey-redeem-success");
        return true;
    }

    private boolean createCDKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("win9xshopandpay.cdkey.create")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length < 4) {
            sendMessage(sender, "error-usage-create-cdkey");
            sendMessage(sender, "error-material-example");
            sendMessage(sender, "error-expiry-format");
            return true;
        }

        Material material;
        try {
            material = Material.valueOf(args[2].toUpperCase());
        } catch (IllegalArgumentException e) {
            sendMessage(sender, "error-invalid-material");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "error-invalid-number");
            return true;
        }

        int maxUses = 1;
        if (args.length >= 5) {
            try {
                maxUses = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sendMessage(sender, "error-invalid-max-uses");
                return true;
            }
        }

        long expiryTime = 0;
        if (args.length >= 6) {
            expiryTime = parseExpiryTime(args[5]);
            if (expiryTime < 0) {
                sendMessage(sender, "error-invalid-expiry");
                sendMessage(sender, "error-expiry-format");
                return true;
            }
        }

        ItemStack item = new ItemStack(material, amount);
        String key = cdKeyManager.generateCDKey(item, maxUses, expiryTime);

        sendMessage(sender, "cdkey-create-success", key);
        sendMessage(sender, "cdkey-create-item", material.name(), amount);
        sendMessage(sender, "cdkey-create-max-uses", maxUses);
        if (expiryTime > 0) {
            sendMessage(sender, "cdkey-create-expiry", formatExpiryTime(expiryTime));
        } else {
            sendMessage(sender, "cdkey-create-permanent");
        }
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Component copyMessage = Component.text("[点击复制CDKey] ", NamedTextColor.GOLD)
                    .append(Component.text(key, NamedTextColor.GREEN))
                    .clickEvent(ClickEvent.copyToClipboard(key));
            plugin.getBukkitAudiences().player(player).sendMessage(copyMessage);
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
            sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length < 3) {
            sendMessage(sender, "error-usage-delete-cdkey");
            return true;
        }

        String key = args[2].toUpperCase();
        
        if (!cdKeyManager.deleteCDKey(key)) {
            sendMessage(sender, "cdkey-not-found");
            return true;
        }

        sendMessage(sender, "cdkey-delete-success");
        return true;
    }

    private boolean listCDKeys(CommandSender sender) {
        if (!sender.hasPermission("win9xshopandpay.cdkey.list")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        sendMessage(sender, "cdkey-list-title");
        
        for (CDKey cdKey : cdKeyManager.getAllCDKeys()) {
            String expiryInfo;
            if (cdKey.hasExpiryDate()) {
                expiryInfo = getMessage("cdkey-expiry-info", sender, formatExpiryTime(cdKey.getExpiryTime()));
            } else {
                expiryInfo = getMessage("cdkey-expiry-permanent", sender);
            }
            
            sendMessage(sender, "cdkey-list-entry", 
                    cdKey.getKey(), cdKey.getItem().getType().name(), 
                    cdKey.getItem().getAmount(), cdKey.getUses(), cdKey.getMaxUses(), expiryInfo);
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
            sendMessage(sender, "only-player");
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("win9xshopandpay.currency.balance")) {
            sendMessage(player, "no-permission");
            return true;
        }
        
        if (args.length >= 3) {
            String currencyId = args[2].toLowerCase();
            Currency currency = currencyManager.getCurrency(currencyId);
            if (currency == null) {
                sendMessage(player, "currency-invalid");
                return true;
            }
            double balance = currencyManager.getBalance(player, currencyId);
            Component balanceMsg = Component.text(currency.getSymbol() + currency.getName() + ": ", NamedTextColor.YELLOW)
                    .append(Component.text(String.valueOf(balance), NamedTextColor.WHITE));
            ColorCodeConverter.sendMessage(player, balanceMsg);
        } else {
            sendMessage(player, "currency-balance-title");
            for (Currency currency : currencyManager.getAllCurrencies().values()) {
                double balance = currencyManager.getBalance(player, currency.getId());
                Component balanceMsg = Component.text(currency.getSymbol() + currency.getName() + ": ", NamedTextColor.YELLOW)
                        .append(Component.text(String.valueOf(balance), NamedTextColor.WHITE));
                ColorCodeConverter.sendMessage(player, balanceMsg);
            }
        }
        return true;
    }

    private boolean giveCurrency(CommandSender sender, String[] args) {
        if (!sender.hasPermission("win9xshopandpay.currency.give")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length < 5) {
            sendMessage(sender, "error-usage-give-currency");
            return true;
        }

        OfflinePlayer offlinePlayer = getOfflinePlayerByName(args[2]);
        if (offlinePlayer == null) {
            sendMessage(sender, "player-not-found");
            return true;
        }

        String currencyId = args[3].toLowerCase();
        if (!currencyManager.isValidCurrency(currencyId)) {
            sendMessage(sender, "currency-invalid");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "error-invalid-number");
            return true;
        }

        currencyManager.deposit(offlinePlayer, currencyId, amount);
        Currency currency = currencyManager.getCurrency(currencyId);
        sendMessage(sender, "currency-give-success", 
                offlinePlayer.getName(), currency.getSymbol(), amount, currency.getName());
        
        if (offlinePlayer.isOnline()) {
            sendMessage(offlinePlayer.getPlayer(), "currency-give-receive", 
                    currency.getSymbol(), amount, currency.getName());
        }
        return true;
    }

    private boolean setCurrency(CommandSender sender, String[] args) {
        if (!sender.hasPermission("win9xshopandpay.currency.set")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length < 5) {
            sendMessage(sender, "error-usage-set-currency");
            return true;
        }

        OfflinePlayer offlinePlayer = getOfflinePlayerByName(args[2]);
        if (offlinePlayer == null) {
            sendMessage(sender, "player-not-found");
            return true;
        }

        String currencyId = args[3].toLowerCase();
        if (!currencyManager.isValidCurrency(currencyId)) {
            sendMessage(sender, "currency-invalid");
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[4]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "error-invalid-number");
            return true;
        }

        currencyManager.setBalance(offlinePlayer, currencyId, amount);
        
        Currency currency = currencyManager.getCurrency(currencyId);
        sendMessage(sender, "currency-set-success", 
                offlinePlayer.getName(), currency.getSymbol(), currency.getName(), amount);
        
        if (offlinePlayer.isOnline()) {
            sendMessage(offlinePlayer.getPlayer(), "currency-set-receive", 
                    currency.getSymbol(), currency.getName(), amount);
        }
        return true;
    }

    private boolean listCurrencies(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "only-player");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("win9xshopandpay.currency.list")) {
            sendMessage(player, "no-permission");
            return true;
        }
        
        sendMessage(player, "currency-list-title");
        for (Currency currency : currencyManager.getAllCurrencies().values()) {
            String defaultTag = currency.isDefault() ? getMessage("currency-default-tag", player) : "";
            Component entryMsg = Component.text(currency.getSymbol())
                    .append(Component.text(currency.getId(), NamedTextColor.YELLOW))
                    .append(Component.text(" - " + currency.getName() + defaultTag));
            ColorCodeConverter.sendMessage(player, entryMsg);
        }
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("win9xshopandpay.reload")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        plugin.reloadConfig();
        languageManager.reloadLanguages();
        currencyManager.reload();
        plugin.getShopManager().reload();
        cdKeyManager.reload();
        plugin.getLotteryManager().reload();
        plugin.getEasterEggManager().reload();
        sendMessage(sender, "reload-success");
        return true;
    }

    private boolean handleGiveShopCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "only-player");
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("win9xshopandpay.give_shop")) {
            sendMessage(player, "no-permission");
            return true;
        }

        int cooldownSeconds = plugin.getConfig().getInt("commands.give_shop_cooldown", 0);
        if (cooldownSeconds > 0) {
            String playerId = player.getUniqueId().toString();
            long now = System.currentTimeMillis();
            Long lastUsed = giveShopCooldowns.get(playerId);
            
            if (lastUsed != null && now - lastUsed < cooldownSeconds * 1000L) {
                long remaining = (cooldownSeconds * 1000L - (now - lastUsed)) / 1000L;
                sendMessage(player, "give-shop-cooldown", remaining);
                return true;
            }
            
            giveShopCooldowns.put(playerId, now);
        }

        ItemStack compass = ShopItemListener.createShopCompass(plugin, player);
        player.getInventory().addItem(compass);
        sendMessage(player, "give-shop-success");
        return true;
    }

    private boolean handleLotteryCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "only-player");
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("win9xshopandpay.lottery")) {
            sendMessage(player, "no-permission");
            return true;
        }

        LotteryGUI lotteryGUI = plugin.getLotteryGUI();
        lotteryGUI.openLottery(player);
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "help-title");
        sendMessage(sender, "help-shop");
        sendMessage(sender, "help-cdkey");
        sendMessage(sender, "help-currency");
        sendMessage(sender, "help-lottery");
        if (sender.hasPermission("win9xshopandpay.give_shop")) {
            sendMessage(sender, "help-give-shop");
        }
        if (sender.hasPermission("win9xshopandpay.reload")) {
            sendMessage(sender, "help-reload");
        }
    }

    private void sendCDKeyHelp(CommandSender sender) {
        sendMessage(sender, "cdkey-help-title");
        sendMessage(sender, "cdkey-help-redeem");
        if (sender.hasPermission("win9xshopandpay.cdkey.create")) {
            sendMessage(sender, "cdkey-help-create");
        }
        if (sender.hasPermission("win9xshopandpay.cdkey.delete")) {
            sendMessage(sender, "cdkey-help-delete");
        }
        if (sender.hasPermission("win9xshopandpay.cdkey.list")) {
            sendMessage(sender, "cdkey-help-list");
        }
    }

    private void sendCurrencyHelp(CommandSender sender) {
        sendMessage(sender, "currency-help-title");
        sendMessage(sender, "currency-help-balance");
        sendMessage(sender, "currency-help-list");
        if (sender.hasPermission("win9xshopandpay.currency.give")) {
            sendMessage(sender, "currency-help-give");
        }
        if (sender.hasPermission("win9xshopandpay.currency.set")) {
            sendMessage(sender, "currency-help-set");
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

    private void sendMessage(CommandSender sender, String key) {
        if (sender instanceof Player) {
            ColorCodeConverter.sendMessage((Player) sender, languageManager.getMessageComponent(key, (Player) sender));
        } else {
            sender.sendMessage(ColorCodeConverter.toLegacy(languageManager.getMessageComponent(key, "en-US")));
        }
    }

    private void sendMessage(CommandSender sender, String key, Object... replacements) {
        if (sender instanceof Player) {
            ColorCodeConverter.sendMessage((Player) sender, languageManager.getMessageComponent(key, (Player) sender, replacements));
        } else {
            sender.sendMessage(ColorCodeConverter.toLegacy(languageManager.getMessageComponent(key, "en-US", replacements)));
        }
    }

    private OfflinePlayer getOfflinePlayerByName(String name) {
        Player onlinePlayer = plugin.getServer().getPlayer(name);
        if (onlinePlayer != null) {
            return onlinePlayer;
        }
        for (OfflinePlayer offlinePlayer : plugin.getServer().getOfflinePlayers()) {
            if (name.equalsIgnoreCase(offlinePlayer.getName())) {
                return offlinePlayer;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> suggestions = new ArrayList<>();
        
        if (args.length == 1) {
            suggestions.add("shop");
            suggestions.add("cdkey");
            suggestions.add("currency");
            suggestions.add("lottery");
            if (sender.hasPermission("win9xshopandpay.give_shop")) {
                suggestions.add("give_shop");
            }
            if (sender.hasPermission("win9xshopandpay.reload")) {
                suggestions.add("reload");
            }
        } else if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "shop":
                    for (Currency currency : currencyManager.getAllCurrencies().values()) {
                        suggestions.add(currency.getId());
                    }
                    break;
                case "cdkey":
                    if (sender.hasPermission("win9xshopandpay.cdkey.redeem")) {
                        suggestions.add("redeem");
                    }
                    if (sender.hasPermission("win9xshopandpay.cdkey.create")) {
                        suggestions.add("create");
                    }
                    if (sender.hasPermission("win9xshopandpay.cdkey.delete")) {
                        suggestions.add("delete");
                    }
                    if (sender.hasPermission("win9xshopandpay.cdkey.list")) {
                        suggestions.add("list");
                    }
                    break;
                case "currency":
                    suggestions.add("balance");
                    suggestions.add("list");
                    if (sender.hasPermission("win9xshopandpay.currency.give")) {
                        suggestions.add("give");
                    }
                    if (sender.hasPermission("win9xshopandpay.currency.set")) {
                        suggestions.add("set");
                    }
                    break;
            }
        } else if (args.length == 3) {
            switch (args[0].toLowerCase()) {
                case "cdkey":
                    switch (args[1].toLowerCase()) {
                        case "create":
                            for (Material material : Material.values()) {
                                if (material.isItem()) {
                                    suggestions.add(material.name());
                                }
                            }
                            break;
                        case "redeem":
                        case "delete":
                            for (CDKey cdKey : cdKeyManager.getAllCDKeys()) {
                                suggestions.add(cdKey.getKey());
                            }
                            break;
                    }
                    break;
                case "currency":
                    switch (args[1].toLowerCase()) {
                        case "give":
                        case "set":
                            for (Player player : plugin.getServer().getOnlinePlayers()) {
                                suggestions.add(player.getName());
                            }
                            break;
                        case "balance":
                            for (Currency currency : currencyManager.getAllCurrencies().values()) {
                                suggestions.add(currency.getId());
                            }
                            break;
                    }
                    break;
            }
        } else if (args.length == 4) {
            switch (args[0].toLowerCase()) {
                case "currency":
                    switch (args[1].toLowerCase()) {
                        case "give":
                        case "set":
                            for (Currency currency : currencyManager.getAllCurrencies().values()) {
                                suggestions.add(currency.getId());
                            }
                            break;
                    }
                    break;
                case "cdkey":
                    switch (args[1].toLowerCase()) {
                        case "create":
                            suggestions.add("1");
                            suggestions.add("64");
                            break;
                    }
                    break;
            }
        } else if (args.length == 5) {
            switch (args[0].toLowerCase()) {
                case "cdkey":
                    switch (args[1].toLowerCase()) {
                        case "create":
                            suggestions.add("1");
                            suggestions.add("10");
                            suggestions.add("100");
                            break;
                    }
                    break;
            }
        } else if (args.length == 6) {
            switch (args[0].toLowerCase()) {
                case "cdkey":
                    switch (args[1].toLowerCase()) {
                        case "create":
                            suggestions.add("1d");
                            suggestions.add("1h");
                            suggestions.add("30m");
                            suggestions.add("1w");
                            break;
                    }
                    break;
            }
        }
        
        String lastArg = args[args.length - 1].toLowerCase();
        return suggestions.stream()
                .filter(s -> s.toLowerCase().startsWith(lastArg))
                .collect(Collectors.toList());
    }
}