package com.win9x.shopandpay.manager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.util.ColorCodeConverter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI chat backed by the sizhi.com bot API (ported from internalAI02.py).
 * Players toggle AI mode with /wsap ai and then chat freely; their messages
 * are sent to the API and the reply is shown back. All HTTP work runs off the
 * main thread.
 */
public class AiManager {

    private final Win9xShopAndPay plugin;
    private final Set<UUID> aiMode = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Long> lastAskTime = new ConcurrentHashMap<>();

    public AiManager(Win9xShopAndPay plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("ai.enabled", true);
    }

    public long getCooldownMillis() {
        int seconds = Math.max(0, plugin.getConfig().getInt("ai.cooldown-seconds", 3));
        return seconds * 1000L;
    }

    public String getAppId() {
        return plugin.getConfig().getString("ai.appid", "9ffcb5785ad9617bf4e64178ac64f7b1");
    }

    public String getApiUrl() {
        return plugin.getConfig().getString("ai.url", "https://api.sizhi.com/bot");
    }

    public String getPrefix() {
        return plugin.getConfig().getString("ai.prefix", "&b[AI]&r ");
    }

    public String getSuffix() {
        return plugin.getConfig().getString("ai.suffix", "");
    }

    public String getSystemPrompt() {
        return plugin.getConfig().getString("ai.system-prompt",
                "你是一个友好的Minecraft服务器助手，名叫Win9x助手。请用简洁的中文回答玩家的问题，"
                        + "不要编造规则，遇到不确定的内容请如实说明。");
    }

    public void setAiMode(UUID id, boolean on) {
        if (on) {
            aiMode.add(id);
        } else {
            aiMode.remove(id);
        }
    }

    public boolean isInAiMode(UUID id) {
        return aiMode.contains(id);
    }

    public void toggleAiMode(Player player) {
        UUID id = player.getUniqueId();
        if (aiMode.contains(id)) {
            aiMode.remove(id);
            msg(player, "ai-mode-off");
        } else {
            aiMode.add(id);
            msg(player, "ai-mode-on");
        }
    }

    public void askAsync(Player player, String spoken) {
        if (!isEnabled()) {
            msg(player, "ai-disabled");
            return;
        }
        long cooldown = getCooldownMillis();
        long now = System.currentTimeMillis();
        Long last = lastAskTime.get(player.getUniqueId());
        if (last != null && now - last < cooldown) {
            msg(player, "ai-cooldown", (cooldown - (now - last)) / 1000.0);
            return;
        }
        if (spoken != null && spoken.length() > 256) {
            spoken = spoken.substring(0, 256);
        }
        lastAskTime.put(player.getUniqueId(), now);
        msg(player, "ai-loading");
        String prompt = getSystemPrompt();
        String fullSpoken = (prompt == null || prompt.isEmpty()) ? spoken : prompt + "\n玩家说:" + spoken;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String answer = request(fullSpoken);
                final String response = answer != null ? answer : "（AI 未返回内容）";
                Bukkit.getScheduler().runTask(plugin, () -> sendReply(player, response));
            } catch (Exception e) {
                String err = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                Bukkit.getScheduler().runTask(plugin, () -> msg(player, "ai-error", err));
            }
        });
    }

    private void sendReply(Player player, String reply) {
        net.kyori.adventure.text.Component component = ColorCodeConverter.convert(getPrefix())
                .append(net.kyori.adventure.text.Component.text(reply))
                .append(ColorCodeConverter.convert(getSuffix()));
        ColorCodeConverter.sendMessage(player, component);
    }

    private String request(String spoken) throws IOException {
        String urlStr = getApiUrl()
                + "?appid=" + URLEncoder.encode(getAppId(), StandardCharsets.UTF_8)
                + "&spoken=" + URLEncoder.encode(spoken, StandardCharsets.UTF_8);
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        try {
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("Accept", "application/json");
            int code = conn.getResponseCode();
            BufferedReader reader;
            if (code >= 200 && code < 300) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            } else {
                reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            }
            StringBuilder sb = new StringBuilder();
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } finally {
                reader.close();
            }
            JsonObject obj = JsonParser.parseString(sb.toString()).getAsJsonObject();
            String status = obj.has("status") ? obj.get("status").getAsString() : "";
            if (!"0".equals(status)) {
                String message = obj.has("message") ? obj.get("message").getAsString() : "unknown";
                return "出错啦！错误码: " + status + " 原因: " + message;
            }
            return obj.getAsJsonObject("data").getAsJsonObject("info").get("text").getAsString();
        } finally {
            conn.disconnect();
        }
    }

    private void msg(Player player, String key, Object... args) {
        ColorCodeConverter.sendMessage(player, plugin.getLanguageManager().getMessageComponent(key, player, args));
    }
}
