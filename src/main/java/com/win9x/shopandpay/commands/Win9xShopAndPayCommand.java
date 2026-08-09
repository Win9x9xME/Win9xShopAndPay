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
            case "market":
                return handleMarketCommand(sender, args);
            case "gui":
                return handleGuiCommand(sender, args);
            case "buyback":
                return handleBuybackCommand(sender);
            case "ai":
                return handleAiCommand(sender);
            case "loan":
                return handleLoanCommand(sender, args);
            case "ticket":
                return handleTicketCommand(sender, args);
            case "bank":
                return handleBankCommand(sender, args);
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

        Player player = (Player) sender;

        if (!player.hasPermission("win9xshopandpay.cdkey.redeem")) {
            sendMessage(player, "no-permission");
            return true;
        }

        if (args.length < 3) {
            sendMessage(sender, "error-usage-redeem-cdkey");
            return true;
        }

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
        } else if (result == CDKeyManager.RedeemResult.INVENTORY_FULL) {
            sendMessage(player, "cdkey-inventory-full");
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

        if (!material.isItem()) {
            sendMessage(sender, "error-material-not-item");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "error-invalid-number");
            return true;
        }

        if (amount < 1 || amount > material.getMaxStackSize()) {
            sendMessage(sender, "error-amount-range", material.getMaxStackSize());
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

        if (maxUses < 1 || maxUses > 100000) {
            sendMessage(sender, "error-max-uses-range");
            return true;
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
        plugin.getMarketManager().reload();
        plugin.getBuybackManager().reload();
        plugin.getGuiShopManager().reload();
        plugin.getLoanManager().reload();
        plugin.getLotteryTicketManager().reload();
        plugin.getBankManager().reload();
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

    private boolean handleMarketCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "only-player");
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("win9xshopandpay.market.use")) {
            sendMessage(player, "no-permission");
            return true;
        }

        if (args.length >= 2 && "sell".equalsIgnoreCase(args[1])) {
            if (!player.hasPermission("win9xshopandpay.market.sell")) {
                sendMessage(player, "no-permission");
                return true;
            }
            if (args.length < 3) {
                sendMessage(player, "error-usage-market-sell");
                return true;
            }
            double price;
            try {
                price = Double.parseDouble(args[2]);
            } catch (NumberFormatException e) {
                sendMessage(player, "error-invalid-number");
                return true;
            }
            com.win9x.shopandpay.manager.MarketManager.ListResult result =
                    plugin.getMarketManager().listItem(player, price);
            switch (result) {
                case SUCCESS:
                    sendMessage(player, "market-list-success", price);
                    break;
                case NO_ITEM:
                    sendMessage(player, "market-no-item");
                    break;
                case INVALID_PRICE:
                    sendMessage(player, "market-invalid-price");
                    break;
                case AMOUNT_TOO_LARGE:
                    sendMessage(player, "market-amount-too-large", plugin.getMarketManager().getMaxListAmount());
                    break;
                case TOO_MANY_LISTINGS:
                    sendMessage(player, "market-too-many-listings", plugin.getMarketManager().getMaxListingsPerPlayer());
                    break;
                case FEE_FAILED:
                    sendMessage(player, "market-fee-failed", plugin.getMarketManager().getListFee());
                    break;
            }
            return true;
        }

        if (args.length >= 2 && "my".equalsIgnoreCase(args[1])) {
            plugin.getMarketGUI().openMyListings(player, 0);
        } else {
            plugin.getMarketGUI().openBrowse(player, 0);
        }
        return true;
    }

    private boolean handleGuiCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sendGuiHelp(sender);
            return true;
        }

        switch (args[1].toLowerCase()) {
            case "shop":
                return handleGuiShop(sender);
            default:
                sendGuiHelp(sender);
                return true;
        }
    }

    private boolean handleGuiShop(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "only-player");
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("win9xshopandpay.gui.shop")) {
            sendMessage(player, "no-permission");
            return true;
        }

        plugin.getGuiShopLauncherGUI().open(player);
        return true;
    }

    private void sendGuiHelp(CommandSender sender) {
        sendMessage(sender, "gui-help-title");
        sendMessage(sender, "gui-help-shop");
    }

    private boolean handleBuybackCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "only-player");
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("win9xshopandpay.buyback")) {
            sendMessage(player, "no-permission");
            return true;
        }

        if (!plugin.getBuybackManager().isEnabled()) {
            sendMessage(player, "buyback-disabled");
            return true;
        }

        plugin.getBuybackGUI().openBuyback(player);
        return true;
    }

    private boolean handleAiCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "only-player");
            return true;
        }
        Player player = (Player) sender;

        if (!player.hasPermission("win9xshopandpay.ai")) {
            sendMessage(player, "no-permission");
            return true;
        }

        if (!plugin.getAiManager().isEnabled()) {
            sendMessage(player, "ai-disabled");
            return true;
        }

        plugin.getAiManager().toggleAiMode(player);
        return true;
    }

    private boolean handleLoanCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "only-player");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("win9xshopandpay.loan")) {
            sendMessage(player, "no-permission");
            return true;
        }
        if (args.length < 2) {
            sendLoanHelp(sender);
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "borrow":
                return loanBorrow(player, args);
            case "repay":
                return loanRepay(player, args);
            case "info":
                return loanInfo(player);
            default:
                sendLoanHelp(sender);
                return true;
        }
    }

    private boolean loanBorrow(Player player, String[] args) {
        if (args.length < 3) {
            sendMessage(player, "error-usage-loan-borrow");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sendMessage(player, "error-invalid-number");
            return true;
        }
        com.win9x.shopandpay.manager.LoanManager.BorrowResult r =
                plugin.getLoanManager().borrow(player, amount);
        switch (r) {
            case SUCCESS:
            case DISABLED:
                sendMessage(player, "loan-borrow-" + r.name().toLowerCase());
                break;
            case INVALID_AMOUNT:
                sendMessage(player, "loan-borrow-invalid-amount");
                break;
            case TOO_LARGE:
                sendMessage(player, "loan-borrow-too-large", plugin.getLoanManager().getMaxAmount());
                break;
            case TOO_MANY_LOANS:
                sendMessage(player, "loan-borrow-too-many", plugin.getLoanManager().getMaxActiveLoans());
                break;
        }
        return true;
    }

    private boolean loanRepay(Player player, String[] args) {
        if (args.length < 3) {
            sendMessage(player, "error-usage-loan-repay");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sendMessage(player, "error-invalid-number");
            return true;
        }
        com.win9x.shopandpay.manager.LoanManager.RepayResult r =
                plugin.getLoanManager().repay(player, amount);
        switch (r) {
            case FULLY_PAID:
                sendMessage(player, "loan-repay-fully-paid");
                break;
            case SUCCESS:
                sendMessage(player, "loan-repay-success");
                break;
            case NO_ACTIVE_LOAN:
                sendMessage(player, "loan-no-active");
                break;
            case INVALID_AMOUNT:
                sendMessage(player, "loan-borrow-invalid-amount");
                break;
            case INSUFFICIENT_FUNDS:
                sendMessage(player, "loan-repay-insufficient");
                break;
        }
        return true;
    }

    private boolean loanInfo(Player player) {
        java.util.List<com.win9x.shopandpay.data.Loan> active =
                plugin.getLoanManager().getActiveLoans(player.getUniqueId());
        if (active.isEmpty()) {
            sendMessage(player, "loan-no-active");
            return true;
        }
        sendMessage(player, "loan-info-title");
        for (com.win9x.shopandpay.data.Loan loan : active) {
            long remainingMs = loan.getDueTime() - System.currentTimeMillis();
            long days = remainingMs / (24L * 60 * 60 * 1000);
            sendMessage(player, "loan-info-entry", loan.getId(), loan.getPrincipal(),
                    loan.getTotalOwed(), loan.getRemaining(), Math.max(0, days));
        }
        return true;
    }

    private void sendLoanHelp(CommandSender sender) {
        sendMessage(sender, "loan-help-title");
        sendMessage(sender, "loan-help-borrow");
        sendMessage(sender, "loan-help-repay");
        sendMessage(sender, "loan-help-info");
    }

    private boolean handleTicketCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "only-player");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("win9xshopandpay.ticket")) {
            sendMessage(player, "no-permission");
            return true;
        }
        if (args.length < 2) {
            sendTicketHelp(sender);
            return true;
        }
        switch (args[1].toLowerCase()) {
            case "buy":
                return ticketBuy(player, args);
            case "info":
                return ticketInfo(player);
            case "history":
                return ticketHistory(player);
            default:
                sendTicketHelp(sender);
                return true;
        }
    }

    private boolean ticketBuy(Player player, String[] args) {
        if (args.length < 3) {
            sendMessage(player, "error-usage-ticket-buy");
            return true;
        }
        double amount;
        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sendMessage(player, "error-invalid-number");
            return true;
        }
        com.win9x.shopandpay.manager.LotteryTicketManager.BuyResult r =
                plugin.getLotteryTicketManager().buy(player, amount);
        switch (r) {
            case SUCCESS:
                sendMessage(player, "ticket-buy-success", amount);
                break;
            case DISABLED:
                sendMessage(player, "ticket-disabled");
                break;
            case INVALID_AMOUNT:
                sendMessage(player, "ticket-buy-invalid");
                break;
            case BELOW_MIN:
                sendMessage(player, "ticket-buy-below-min", plugin.getLotteryTicketManager().getMinAmount());
                break;
            case NOT_OPEN:
                sendMessage(player, "ticket-not-open");
                break;
        }
        return true;
    }

    private boolean ticketInfo(Player player) {
        com.win9x.shopandpay.data.LotteryTicketRound round = plugin.getLotteryTicketManager().getCurrentRound();
        if (round == null) {
            sendMessage(player, "ticket-disabled");
            return true;
        }
        long remainingMs = Math.max(0, round.getCloseTime() - System.currentTimeMillis());
        long hours = remainingMs / (60L * 60 * 1000);
        sendMessage(player, "ticket-info", round.getRoundNumber(), round.getTotalPool(), hours);
        return true;
    }

    private boolean ticketHistory(Player player) {
        java.util.List<com.win9x.shopandpay.data.LotteryTicketRound> hist =
                plugin.getLotteryTicketManager().getHistory();
        if (hist.isEmpty()) {
            sendMessage(player, "ticket-history-empty");
            return true;
        }
        sendMessage(player, "ticket-history-title");
        int shown = 0;
        for (com.win9x.shopandpay.data.LotteryTicketRound round : hist) {
            if (shown++ >= 5) {
                break;
            }
            if (round.getWinnerUUID() == null) {
                sendMessage(player, "ticket-history-entry-none", round.getRoundNumber(), round.getTotalPool());
            } else {
                sendMessage(player, "ticket-history-entry", round.getRoundNumber(),
                        round.getWinnerName(), round.getPrize());
            }
        }
        return true;
    }

    private void sendTicketHelp(CommandSender sender) {
        sendMessage(sender, "ticket-help-title");
        sendMessage(sender, "ticket-help-buy");
        sendMessage(sender, "ticket-help-info");
        sendMessage(sender, "ticket-help-history");
    }

    private boolean handleBankCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, "only-player");
            return true;
        }
        Player player = (Player) sender;
        if (!player.hasPermission("win9xshopandpay.bank")) {
            sendMessage(player, "no-permission");
            return true;
        }
        if (args.length < 2) {
            return bankInfo(player);
        }
        switch (args[1].toLowerCase()) {
            case "info":
                return bankInfo(player);
            case "deposit":
                return bankDeposit(player, args);
            case "withdraw":
                return bankWithdraw(player, args);
            case "claim":
                return bankClaim(player, args);
            case "early":
                return bankEarly(player, args);
            default:
                sendBankHelp(sender);
                return true;
        }
    }

    private boolean bankInfo(Player player) {
        double demand = plugin.getBankManager().getDemandBalance(player.getUniqueId());
        sendMessage(player, "bank-info-demand", demand);
        java.util.List<com.win9x.shopandpay.data.FixedDeposit> deposits =
                plugin.getBankManager().getDepositsBy(player.getUniqueId());
        if (!deposits.isEmpty()) {
            sendMessage(player, "bank-info-fixed-title");
            for (com.win9x.shopandpay.data.FixedDeposit d : deposits) {
                long remainingMs = d.getMatureTime() - System.currentTimeMillis();
                long days = remainingMs / (24L * 60 * 60 * 1000);
                String statusKey = d.getStatus() == com.win9x.shopandpay.data.FixedDeposit.Status.MATURED
                        ? "bank-fixed-matured" : "bank-fixed-active";
                sendMessage(player, "bank-info-fixed-entry", d.getId(), d.getPrincipal(),
                        d.getTermDays(), d.getRate(), Math.max(0, days),
                        getMessage(statusKey, player));
            }
        }
        return true;
    }

    private boolean bankDeposit(Player player, String[] args) {
        if (args.length < 4) {
            sendMessage(player, "error-usage-bank-deposit");
            return true;
        }
        String type = args[2].toLowerCase();
        double amount;
        try {
            amount = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            sendMessage(player, "error-invalid-number");
            return true;
        }
        if ("demand".equals(type)) {
            com.win9x.shopandpay.manager.BankManager.DemandResult r =
                    plugin.getBankManager().depositDemand(player, amount);
            switch (r) {
                case SUCCESS:
                    sendMessage(player, "bank-deposit-demand-success", amount);
                    break;
                case DISABLED:
                    sendMessage(player, "bank-disabled");
                    break;
                case INVALID_AMOUNT:
                    sendMessage(player, "bank-invalid-amount");
                    break;
                case INSUFFICIENT_FUNDS:
                    sendMessage(player, "bank-insufficient");
                    break;
            }
            return true;
        }
        if ("fixed".equals(type)) {
            if (args.length < 5) {
                sendMessage(player, "error-usage-bank-deposit-fixed");
                return true;
            }
            int termDays;
            try {
                termDays = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                sendMessage(player, "error-invalid-number");
                return true;
            }
            com.win9x.shopandpay.manager.BankManager.FixedResult r =
                    plugin.getBankManager().depositFixed(player, termDays, amount);
            switch (r) {
                case SUCCESS:
                    sendMessage(player, "bank-deposit-fixed-success", amount, termDays);
                    break;
                case DISABLED:
                    sendMessage(player, "bank-disabled");
                    break;
                case INVALID_AMOUNT:
                    sendMessage(player, "bank-invalid-amount");
                    break;
                case INSUFFICIENT_FUNDS:
                    sendMessage(player, "bank-insufficient");
                    break;
                case INVALID_TERM:
                    StringBuilder sb = new StringBuilder();
                    plugin.getBankManager().getFixedTerms().keySet().forEach(sb::append);
                    String terms = sb.toString().replaceAll("(\\d+)", "$1天 ").trim();
                    sendMessage(player, "bank-invalid-term", terms);
                    break;
            }
            return true;
        }
        sendMessage(player, "error-usage-bank-deposit");
        return true;
    }

    private boolean bankWithdraw(Player player, String[] args) {
        if (args.length < 3) {
            sendMessage(player, "error-usage-bank-withdraw");
            return true;
        }
        double amount = 0;
        if (args.length >= 4) {
            try {
                amount = Double.parseDouble(args[3]);
            } catch (NumberFormatException e) {
                sendMessage(player, "error-invalid-number");
                return true;
            }
        }
        com.win9x.shopandpay.manager.BankManager.DemandResult r =
                plugin.getBankManager().withdrawDemand(player, amount);
        switch (r) {
            case SUCCESS:
                sendMessage(player, "bank-withdraw-success", amount <= 0 ? "全部" : String.valueOf(amount));
                break;
            case DISABLED:
                sendMessage(player, "bank-disabled");
                break;
            case INVALID_AMOUNT:
                sendMessage(player, "bank-invalid-amount");
                break;
            case INSUFFICIENT_FUNDS:
                sendMessage(player, "bank-insufficient");
                break;
        }
        return true;
    }

    private boolean bankClaim(Player player, String[] args) {
        if (args.length < 3) {
            sendMessage(player, "error-usage-bank-claim");
            return true;
        }
        com.win9x.shopandpay.manager.BankManager.ClaimResult r =
                plugin.getBankManager().claim(player, args[2]);
        switch (r) {
            case SUCCESS:
                sendMessage(player, "bank-claim-success", args[2]);
                break;
            case NOT_FOUND:
                sendMessage(player, "bank-not-found");
                break;
            case NOT_MATURED:
                sendMessage(player, "bank-not-matured");
                break;
            case NOT_OWNER:
                sendMessage(player, "bank-not-owner");
                break;
            case ALREADY_CLAIMED:
                sendMessage(player, "bank-already-claimed");
                break;
        }
        return true;
    }

    private boolean bankEarly(Player player, String[] args) {
        if (args.length < 3) {
            sendMessage(player, "error-usage-bank-early");
            return true;
        }
        com.win9x.shopandpay.manager.BankManager.EarlyResult r =
                plugin.getBankManager().earlyWithdraw(player, args[2]);
        switch (r) {
            case SUCCESS:
                sendMessage(player, "bank-early-success", args[2]);
                break;
            case NOT_FOUND:
                sendMessage(player, "bank-not-found");
                break;
            case NOT_OWNER:
                sendMessage(player, "bank-not-owner");
                break;
            case ALREADY_CLAIMED:
                sendMessage(player, "bank-already-claimed");
                break;
        }
        return true;
    }

    private void sendBankHelp(CommandSender sender) {
        sendMessage(sender, "bank-help-title");
        sendMessage(sender, "bank-help-info");
        sendMessage(sender, "bank-help-deposit");
        sendMessage(sender, "bank-help-withdraw");
        sendMessage(sender, "bank-help-claim");
        sendMessage(sender, "bank-help-early");
    }

    private void sendHelp(CommandSender sender) {
        sendMessage(sender, "help-title");
        sendMessage(sender, "help-shop");
        sendMessage(sender, "help-cdkey");
        sendMessage(sender, "help-currency");
        sendMessage(sender, "help-lottery");
        if (sender.hasPermission("win9xshopandpay.market.use")) {
            sendMessage(sender, "help-market");
        }
        if (sender.hasPermission("win9xshopandpay.gui.shop")) {
            sendMessage(sender, "help-gui-shop");
        }
        if (sender.hasPermission("win9xshopandpay.buyback")) {
            sendMessage(sender, "help-buyback");
        }
        if (sender.hasPermission("win9xshopandpay.ai")) {
            sendMessage(sender, "help-ai");
        }
        if (sender.hasPermission("win9xshopandpay.loan")) {
            sendMessage(sender, "help-loan");
        }
        if (sender.hasPermission("win9xshopandpay.ticket")) {
            sendMessage(sender, "help-ticket");
        }
        if (sender.hasPermission("win9xshopandpay.bank")) {
            sendMessage(sender, "help-bank");
        }
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
            if (sender.hasPermission("win9xshopandpay.market.use")) {
                suggestions.add("market");
            }
            if (sender.hasPermission("win9xshopandpay.gui.shop")) {
                suggestions.add("gui");
            }
            if (sender.hasPermission("win9xshopandpay.buyback")) {
                suggestions.add("buyback");
            }
            if (sender.hasPermission("win9xshopandpay.ai")) {
                suggestions.add("ai");
            }
            if (sender.hasPermission("win9xshopandpay.loan")) {
                suggestions.add("loan");
            }
            if (sender.hasPermission("win9xshopandpay.ticket")) {
                suggestions.add("ticket");
            }
            if (sender.hasPermission("win9xshopandpay.bank")) {
                suggestions.add("bank");
            }
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
                case "market":
                    suggestions.add("my");
                    if (sender.hasPermission("win9xshopandpay.market.sell")) {
                        suggestions.add("sell");
                    }
                    break;
                case "gui":
                    if (sender.hasPermission("win9xshopandpay.gui.shop")) {
                        suggestions.add("shop");
                    }
                    break;
                case "loan":
                    suggestions.add("borrow");
                    suggestions.add("repay");
                    suggestions.add("info");
                    break;
                case "ticket":
                    suggestions.add("buy");
                    suggestions.add("info");
                    suggestions.add("history");
                    break;
                case "bank":
                    suggestions.add("info");
                    suggestions.add("deposit");
                    suggestions.add("withdraw");
                    suggestions.add("claim");
                    suggestions.add("early");
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
                            break;
                        case "delete":
                            if (sender.hasPermission("win9xshopandpay.cdkey.delete")) {
                                for (CDKey cdKey : cdKeyManager.getAllCDKeys()) {
                                    suggestions.add(cdKey.getKey());
                                }
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
                case "bank":
                    switch (args[1].toLowerCase()) {
                        case "deposit":
                            suggestions.add("demand");
                            suggestions.add("fixed");
                            break;
                        case "withdraw":
                            suggestions.add("demand");
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
                case "bank":
                    if ("deposit".equalsIgnoreCase(args[1]) && "fixed".equalsIgnoreCase(args[2])) {
                        for (Integer term : plugin.getBankManager().getFixedTerms().keySet()) {
                            suggestions.add(String.valueOf(term));
                        }
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