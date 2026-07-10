package com.win9x.shopandpay.util;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ColorCodeConverter {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static BukkitAudiences bukkitAudiences;

    public static void setBukkitAudiences(BukkitAudiences audiences) {
        bukkitAudiences = audiences;
    }

    public static Component convert(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }
        return LEGACY_SERIALIZER.deserialize(message);
    }

    public static void sendMessage(Player player, String message) {
        if (player != null && message != null) {
            sendMessage(player, convert(message));
        }
    }

    public static void sendMessage(Player player, Component component) {
        if (player != null && component != null) {
            if (bukkitAudiences != null) {
                bukkitAudiences.player(player).sendMessage(component);
            } else {
                player.sendMessage(LEGACY_SERIALIZER.serialize(component));
            }
        }
    }

    public static void sendMessage(CommandSender sender, Component component) {
        if (sender instanceof Player) {
            sendMessage((Player) sender, component);
        } else {
            sender.sendMessage(LEGACY_SERIALIZER.serialize(component));
        }
    }

    public static String toLegacy(Component component) {
        if (component == null) {
            return "";
        }
        return LEGACY_SERIALIZER.serialize(component);
    }

    public static Component createColoredText(String text, NamedTextColor color) {
        return Component.text(text, color);
    }

    public static Component createColoredText(String text, String hexColor) {
        return Component.text(text).color(net.kyori.adventure.text.format.TextColor.fromHexString(hexColor));
    }
}