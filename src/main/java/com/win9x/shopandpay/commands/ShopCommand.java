package com.win9x.shopandpay.commands;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.gui.ShopGUI;
import com.win9x.shopandpay.manager.LanguageManager;
import com.win9x.shopandpay.util.ColorCodeConverter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

    private final Win9xShopAndPay plugin;
    private final LanguageManager languageManager;

    public ShopCommand(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("只有玩家可以使用此命令!");
            return true;
        }

        Player player = (Player) sender;
        
        if (!player.hasPermission("win9xshopandpay.shop")) {
            ColorCodeConverter.sendMessage(player, languageManager.getMessageComponent("no-permission", player));
            return true;
        }

        ShopGUI shopGUI = new ShopGUI(plugin);
        shopGUI.openShop(player);
        return true;
    }
}