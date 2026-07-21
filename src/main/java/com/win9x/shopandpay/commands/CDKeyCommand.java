package com.win9x.shopandpay.commands;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.CDKey;
import com.win9x.shopandpay.manager.CDKeyManager;
import com.win9x.shopandpay.manager.LanguageManager;
import com.win9x.shopandpay.util.ColorCodeConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CDKeyCommand implements CommandExecutor {

    private final Win9xShopAndPay plugin;
    private final CDKeyManager cdKeyManager;
    private final LanguageManager languageManager;

    public CDKeyCommand(Win9xShopAndPay plugin) {
        this.plugin = plugin;
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
            case "redeem":
                return redeemCDKey(sender, args);
            case "create":
                return createCDKey(sender, args);
            case "delete":
                return deleteCDKey(sender, args);
            case "list":
                return listCDKeys(sender);
            default:
                sendHelp(sender);
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

        if (args.length < 2) {
            sendMessage(sender, "error-usage-redeem-cdkey");
            return true;
        }

        String key = args[1].toUpperCase();

        CDKeyManager.RedeemResult result = cdKeyManager.redeemCDKey(key, player);
        if (result == CDKeyManager.RedeemResult.NOT_FOUND) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("cdkey-not-found", player));
            return true;
        } else if (result == CDKeyManager.RedeemResult.EXPIRED) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("cdkey-invalid-or-expired", player));
            return true;
        } else if (result == CDKeyManager.RedeemResult.ALREADY_USED) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("cdkey-already-used", player));
            return true;
        } else if (result == CDKeyManager.RedeemResult.INVENTORY_FULL) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("cdkey-inventory-full", player));
            return true;
        }

        ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("cdkey-redeem-success", player));
        return true;
    }

    private boolean createCDKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("win9xshopandpay.cdkey.create")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length < 3) {
            sendMessage(sender, "error-usage-create-cdkey");
            sendMessage(sender, "error-material-example");
            return true;
        }

        String itemType = args[1];
        Material material = Material.matchMaterial(itemType);
        
        if (material == null || material == Material.AIR) {
            if (itemType.contains(":")) {
                String[] parts = itemType.split(":", 2);
                String namespace = parts[0].toLowerCase();
                String key = parts[1].toLowerCase();
                material = Material.matchMaterial(namespace + ":" + key);
            }
        }
        
        if (material == null || material == Material.AIR) {
            sendMessage(sender, "error-invalid-material");
            return true;
        }

        if (!material.isItem()) {
            sendMessage(sender, "error-material-not-item");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "error-invalid-number");
            return true;
        }

        if (amount < 1 || amount > material.getMaxStackSize()) {
            sendMessage(sender, "error-amount-range", material.getMaxStackSize());
            return true;
        }

        int maxUses = 1;
        if (args.length >= 4) {
            try {
                maxUses = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sendMessage(sender, "error-invalid-max-uses");
                return true;
            }
        }

        if (maxUses < 1 || maxUses > 100000) {
            sendMessage(sender, "error-max-uses-range");
            return true;
        }

        ItemStack item = new ItemStack(material, amount);
        String key = cdKeyManager.generateCDKey(item, maxUses, 0);

        sendMessage(sender, "cdkey-create-success", key);
        sendMessage(sender, "cdkey-create-item", material.name(), amount);
        sendMessage(sender, "cdkey-create-max-uses", maxUses);
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Component copyMessage = Component.text("[点击复制CDKey] ", NamedTextColor.GOLD)
                    .append(Component.text(key, NamedTextColor.GREEN))
                    .clickEvent(ClickEvent.copyToClipboard(key));
            plugin.getBukkitAudiences().player(player).sendMessage(copyMessage);
        }
        
        return true;
    }

    private boolean deleteCDKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("win9xshopandpay.cdkey.delete")) {
            sendMessage(sender, "no-permission");
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, "error-usage-delete-cdkey");
            return true;
        }

        String key = args[1].toUpperCase();
        
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
            String expiryInfo = "";
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

    private void sendHelp(CommandSender sender) {
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
}