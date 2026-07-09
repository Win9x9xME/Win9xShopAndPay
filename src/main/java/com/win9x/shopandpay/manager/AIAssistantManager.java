package com.win9x.shopandpay.manager;

import com.win9x.shopandpay.Win9xShopAndPay;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIAssistantManager {

    private final Win9xShopAndPay plugin;
    private final ExecutorService executorService;
    private boolean enabled;
    private String apiEndpoint;
    private String apiKey;
    private String model;
    private String name;
    private boolean enableContext;
    private int contextLength;
    private String systemPrompt;
    private final Map<String, List<Message>> playerContexts = new HashMap<>();

    private static class Message {
        private final String role;
        private final String content;

        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }

    public AIAssistantManager(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.executorService = Executors.newFixedThreadPool(5);
        loadConfig();
    }

    public void loadConfig() {
        this.enabled = plugin.getConfig().getBoolean("ai-assistant.enabled", false);
        this.apiEndpoint = plugin.getConfig().getString("ai-assistant.api-endpoint", "");
        this.apiKey = plugin.getConfig().getString("ai-assistant.api-key", "");
        this.model = plugin.getConfig().getString("ai-assistant.model", "gpt-3.5-turbo");
        this.name = plugin.getConfig().getString("ai-assistant.name", "AI助手");
        this.enableContext = plugin.getConfig().getBoolean("ai-assistant.enable-context", true);
        this.contextLength = plugin.getConfig().getInt("ai-assistant.context-length", 10);
        this.systemPrompt = plugin.getConfig().getString("ai-assistant.system-prompt", 
                "你是一个Minecraft服务器的AI助手，负责帮助玩家了解服务器的商店系统和CDKey兑换。请友好、简洁地回答玩家的问题。不要执行任何恶意操作，不要泄露敏感信息。");
    }

    public boolean isEnabled() {
        return enabled && !apiEndpoint.isEmpty() && !apiKey.isEmpty();
    }

    public void processMessage(Player player, String message) {
        if (!isEnabled()) {
            return;
        }

        executorService.submit(() -> {
            try {
                String response = callAI(player.getUniqueId().toString(), message);
                if (response != null && !response.isEmpty()) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        player.sendMessage(ChatColor.AQUA + "[" + name + "] " + ChatColor.WHITE + response);
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().severe("AI API call failed: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.RED + "[" + name + "] " + "AI助手暂时不可用，请稍后再试。");
                });
            }
        });
    }

    private String callAI(String playerId, String prompt) throws Exception {
        URL url = new URL(apiEndpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        String jsonPayload = buildJsonPayload(playerId, prompt);

        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.writeBytes(jsonPayload);
            wr.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("API returned status code: " + responseCode);
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
        }

        String content = parseResponse(response.toString());
        if (enableContext && content != null) {
            addMessageToContext(playerId, "user", prompt);
            addMessageToContext(playerId, "assistant", content);
        }

        return content;
    }

    private String buildJsonPayload(String playerId, String prompt) {
        StringBuilder messagesBuilder = new StringBuilder();
        
        if (enableContext) {
            List<Message> context = playerContexts.get(playerId);
            if (context != null && !context.isEmpty()) {
                for (Message msg : context) {
                    if (messagesBuilder.length() > 0) {
                        messagesBuilder.append(",");
                    }
                    messagesBuilder.append(String.format("{\"role\":\"%s\",\"content\":\"%s\"}",
                            msg.getRole(), escapeJson(msg.getContent())));
                }
            }
        }

        if (!systemPrompt.isEmpty()) {
            String systemMsg = String.format("{\"role\":\"system\",\"content\":\"%s\"}", escapeJson(systemPrompt));
            if (messagesBuilder.length() > 0) {
                messagesBuilder.insert(0, systemMsg + ",");
            } else {
                messagesBuilder.append(systemMsg);
            }
        }

        if (messagesBuilder.length() > 0) {
            messagesBuilder.append(",");
        }
        messagesBuilder.append(String.format("{\"role\":\"user\",\"content\":\"%s\"}", escapeJson(prompt)));

        return String.format("{\"model\":\"%s\",\"messages\":[%s]}", model, messagesBuilder.toString());
    }

    private void addMessageToContext(String playerId, String role, String content) {
        List<Message> context = playerContexts.computeIfAbsent(playerId, k -> new ArrayList<>());
        context.add(new Message(role, content));
        
        while (context.size() > contextLength) {
            context.remove(0);
        }
    }

    private String parseResponse(String json) {
        String content = extractJSONValue(json, "\"content\"");
        if (content != null) {
            return content.replace("\\n", "\n").replace("\\\"", "\"").replace("\\'", "'");
        }
        return null;
    }

    private String extractJSONValue(String json, String key) {
        int keyIndex = json.indexOf(key);
        if (keyIndex == -1) {
            return null;
        }
        
        int colonIndex = json.indexOf(":", keyIndex + key.length());
        if (colonIndex == -1) {
            return null;
        }
        
        int valueStart = colonIndex + 1;
        while (valueStart < json.length() && (json.charAt(valueStart) == ' ' || json.charAt(valueStart) == '\t')) {
            valueStart++;
        }
        
        if (valueStart >= json.length()) {
            return null;
        }
        
        if (json.charAt(valueStart) == '"') {
            valueStart++;
            StringBuilder sb = new StringBuilder();
            boolean escaped = false;
            
            for (int i = valueStart; i < json.length(); i++) {
                char c = json.charAt(i);
                
                if (escaped) {
                    sb.append(c);
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    return sb.toString();
                } else {
                    sb.append(c);
                }
            }
            
            return sb.toString();
        } else if (json.charAt(valueStart) == '{') {
            int depth = 1;
            for (int i = valueStart + 1; i < json.length(); i++) {
                if (json.charAt(i) == '{' && json.charAt(i - 1) != '\\') depth++;
                if (json.charAt(i) == '}' && json.charAt(i - 1) != '\\') depth--;
                if (depth == 0) {
                    return json.substring(valueStart, i + 1);
                }
            }
        } else if (json.charAt(valueStart) == '[') {
            int depth = 1;
            for (int i = valueStart + 1; i < json.length(); i++) {
                if (json.charAt(i) == '[' && json.charAt(i - 1) != '\\') depth++;
                if (json.charAt(i) == ']' && json.charAt(i - 1) != '\\') depth--;
                if (depth == 0) {
                    return json.substring(valueStart, i + 1);
                }
            }
        } else {
            int endIndex = valueStart;
            while (endIndex < json.length() && 
                   json.charAt(endIndex) != ',' && 
                   json.charAt(endIndex) != '}' && 
                   json.charAt(endIndex) != ']') {
                endIndex++;
            }
            return json.substring(valueStart, endIndex).trim();
        }
        
        return null;
    }

    private String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");
    }

    public String getName() {
        return name;
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public void clearContext(String playerId) {
        playerContexts.remove(playerId);
    }
}