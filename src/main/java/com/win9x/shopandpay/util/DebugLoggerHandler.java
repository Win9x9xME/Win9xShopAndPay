package com.win9x.shopandpay.util;

import com.win9x.shopandpay.Win9xShopAndPay;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class DebugLoggerHandler extends Handler {

    private final Win9xShopAndPay plugin;
    private final String pluginLoggerName;

    public DebugLoggerHandler(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.pluginLoggerName = plugin.getName();
    }

    @Override
    public void publish(LogRecord record) {
        if (!isDebugEnabled()) {
            return;
        }

        String loggerName = record.getLoggerName();
        if (loggerName == null || !loggerName.equals(pluginLoggerName)) {
            return;
        }

        String message = format(record);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage("§7[DEBUG] §f" + message);
            }
        }
    }

    private boolean isDebugEnabled() {
        return plugin.getConfig().getBoolean("de-bug.enabled", false);
    }

    private String format(LogRecord record) {
        String message = record.getMessage();
        if (record.getParameters() != null && record.getParameters().length > 0) {
            try {
                message = String.format(message, record.getParameters());
            } catch (Exception e) {
            }
        }
        return message;
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}