package com.win9x.shopandpay.commands;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.data.CDKey;
import com.win9x.shopandpay.manager.CDKeyManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CDKeyCommand implements CommandExecutor {

    private final Win9xShopAndPay plugin;
    private final CDKeyManager cdKeyManager;

    public CDKeyCommand(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.cdKeyManager = plugin.getCDKeyManager();
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
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /cdkey redeem <key>");
            return true;
        }

        Player player = (Player) sender;
        String key = args[1].toUpperCase();

        CDKeyManager.RedeemResult result = cdKeyManager.redeemCDKey(key, player);
        if (result == CDKeyManager.RedeemResult.NOT_FOUND) {
            player.sendMessage(ChatColor.RED + "CDKey不存在!");
            return true;
        } else if (result == CDKeyManager.RedeemResult.EXPIRED) {
            player.sendMessage(ChatColor.RED + "CDKey已过期!");
            return true;
        } else if (result == CDKeyManager.RedeemResult.ALREADY_USED) {
            player.sendMessage(ChatColor.RED + "你已经使用过这个CDKey了!");
            return true;
        }

        player.sendMessage(ChatColor.GREEN + "兑换成功!");
        return true;
    }

    private boolean createCDKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("win9xshopandpay.cdkey.create")) {
            sender.sendMessage(ChatColor.RED + "没有权限使用此命令!");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "用法: /cdkey create <物品类型> <数量> [最大使用次数]");
            sender.sendMessage(ChatColor.GRAY + "物品类型示例: DIAMOND, EMERALD, IRON_INGOT");
            return true;
        }

        Material material;
        try {
            material = Material.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "无效的物品类型!");
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "数量必须是数字!");
            return true;
        }

        int maxUses = 1;
        if (args.length >= 4) {
            try {
                maxUses = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatColor.RED + "最大使用次数必须是数字!");
                return true;
            }
        }

        ItemStack item = new ItemStack(material, amount);
        String key = cdKeyManager.generateCDKey(item, maxUses, 0);

        sender.sendMessage(ChatColor.GREEN + "CDKey 创建成功: " + ChatColor.YELLOW + key);
        sender.sendMessage(ChatColor.GRAY + "物品: " + material.name() + " x" + amount);
        sender.sendMessage(ChatColor.GRAY + "最大使用次数: " + maxUses);
        return true;
    }

    private boolean deleteCDKey(CommandSender sender, String[] args) {
        if (!sender.hasPermission("win9xshopandpay.cdkey.delete")) {
            sender.sendMessage(ChatColor.RED + "没有权限使用此命令!");
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /cdkey delete <key>");
            return true;
        }

        String key = args[1].toUpperCase();
        
        if (!cdKeyManager.deleteCDKey(key)) {
            sender.sendMessage(ChatColor.RED + "CDKey不存在!");
            return true;
        }

        sender.sendMessage(ChatColor.GREEN + "CDKey 删除成功!");
        return true;
    }

    private boolean listCDKeys(CommandSender sender) {
        if (!sender.hasPermission("win9xshopandpay.cdkey.list")) {
            sender.sendMessage(ChatColor.RED + "没有权限使用此命令!");
            return true;
        }

        sender.sendMessage(ChatColor.GOLD + "--- CDKey 列表 ---");
        
        for (CDKey cdKey : cdKeyManager.getAllCDKeys()) {
            sender.sendMessage(ChatColor.YELLOW + cdKey.getKey() + 
                    ChatColor.GRAY + " | " + cdKey.getItem().getType().name() + 
                    " x" + cdKey.getItem().getAmount() + 
                    ChatColor.GRAY + " | 使用次数: " + cdKey.getUses() + "/" + cdKey.getMaxUses());
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "--- Win9xShopAndPay CDKey 命令 ---");
        sender.sendMessage(ChatColor.GRAY + "/cdkey redeem <key> - 兑换CDKey");
        if (sender.hasPermission("win9xshopandpay.cdkey.create")) {
            sender.sendMessage(ChatColor.GRAY + "/cdkey create <物品类型> <数量> [最大使用次数] - 创建CDKey");
        }
        if (sender.hasPermission("win9xshopandpay.cdkey.delete")) {
            sender.sendMessage(ChatColor.GRAY + "/cdkey delete <key> - 删除CDKey");
        }
        if (sender.hasPermission("win9xshopandpay.cdkey.list")) {
            sender.sendMessage(ChatColor.GRAY + "/cdkey list - 列出所有CDKey");
        }
    }
}