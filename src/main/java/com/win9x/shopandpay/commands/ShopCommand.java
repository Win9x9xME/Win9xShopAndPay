package com.win9x.shopandpay.commands;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.gui.ShopGUI;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final Win9xShopAndPay plugin;

    public ShopCommand(Win9xShopAndPay plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("win9xshopandpay.shop")) {
            player.sendMessage(ChatColor.RED + "没有权限使用此命令!");
            return true;
        }

        ShopGUI shopGUI = new ShopGUI(plugin);
        shopGUI.openShop(player);
        return true;
    }
}