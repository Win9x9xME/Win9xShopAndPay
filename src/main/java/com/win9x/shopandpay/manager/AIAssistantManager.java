package com.win9x.shopandpay.manager;

import com.win9x.shopandpay.Win9xShopAndPay;
import com.win9x.shopandpay.util.ColorCodeConverter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.configuration.ConfigurationSection;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AIAssistantManager {

    private final Win9xShopAndPay plugin;
    private final ExecutorService executorService;
    private volatile boolean enabled;
    private volatile String apiEndpoint;
    private volatile String apiKey;
    private volatile String model;
    private volatile String name;
    private volatile boolean enableContext;
    private volatile int contextLength;
    private volatile String systemPrompt;
    private volatile String apiFormat;
    private volatile Map<String, String> customHeaders;
    private volatile String requestTemplate;
    private volatile String responsePath;
    private final Map<String, List<Message>> playerContexts = new ConcurrentHashMap<>();

    public enum ApiFormat {
        OPENAI, ANTHROPIC, SPARK, MINIMAX, CUSTOM
    }

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
        this.apiFormat = plugin.getConfig().getString("ai-assistant.api-format", "openai");
        
        this.customHeaders = new HashMap<>();
        ConfigurationSection headersSection = plugin.getConfig().getConfigurationSection("ai-assistant.custom-headers");
        if (headersSection != null) {
            for (String key : headersSection.getKeys(false)) {
                if (isValidHeaderName(key)) {
                    customHeaders.put(key, headersSection.getString(key));
                } else {
                    plugin.getLogger().warning("Invalid header name: " + key + ", skipped");
                }
            }
        }
        
        this.requestTemplate = plugin.getConfig().getString("ai-assistant.request-template", 
                "{\"model\":\"{model}\",\"messages\":{messages}}");
        this.responsePath = plugin.getConfig().getString("ai-assistant.response-path", 
                "choices.0.message.content");
    }

    public boolean isEnabled() {
        return enabled && isValidApiEndpoint(apiEndpoint) && !apiKey.isEmpty();
    }

    private boolean isValidApiEndpoint(String endpoint) {
        if (endpoint == null || endpoint.isEmpty()) {
            return false;
        }
        try {
            URL url = URI.create(endpoint).toURL();
            String protocol = url.getProtocol();
            return "http".equals(protocol) || "https".equals(protocol);
        } catch (Exception e) {
            return false;
        }
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
                        Component prefix = Component.text("[" + name + "] ", NamedTextColor.AQUA);
                        Component content = Component.text(response, NamedTextColor.WHITE);
                        ColorCodeConverter.sendMessage(player, prefix.append(content));
                    });
                }
            } catch (Exception e) {
                plugin.getLogger().severe("AI API call failed: " + e.getMessage());
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    Component prefix = Component.text("[" + name + "] ", NamedTextColor.RED);
                    Component content = Component.text("AI助手暂时不可用，请稍后再试。");
                    ColorCodeConverter.sendMessage(player, prefix.append(content));
                });
            }
        });
    }

    private String callAI(String playerId, String prompt) throws Exception {
        URL url = URI.create(apiEndpoint).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        
        if (!customHeaders.isEmpty()) {
            for (Map.Entry<String, String> header : customHeaders.entrySet()) {
                conn.setRequestProperty(header.getKey(), header.getValue());
            }
        } else {
            ApiFormat format;
            try {
                format = ApiFormat.valueOf(apiFormat.toUpperCase());
            } catch (IllegalArgumentException e) {
                format = ApiFormat.OPENAI;
            }
            
            if (format == ApiFormat.ANTHROPIC) {
                conn.setRequestProperty("x-api-key", apiKey);
                conn.setRequestProperty("anthropic-version", "2023-06-01");
            } else {
                conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            }
        }
        
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(30000);

        String jsonPayload = buildJsonPayload(playerId, prompt);

        try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
            wr.writeBytes(jsonPayload);
            wr.flush();
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200 && responseCode != 201) {
            StringBuilder errorResponse = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = in.readLine()) != null) {
                    errorResponse.append(line);
                }
            }
            throw new Exception("API returned status code: " + responseCode + ", error: " + errorResponse.toString());
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
        ApiFormat format;
        try {
            format = ApiFormat.valueOf(apiFormat.toUpperCase());
        } catch (IllegalArgumentException e) {
            format = ApiFormat.OPENAI;
        }

        List<Message> allMessages = new ArrayList<>();
        
        if (!systemPrompt.isEmpty()) {
            allMessages.add(new Message("system", systemPrompt));
        }

        if (enableContext) {
            List<Message> context = playerContexts.get(playerId);
            if (context != null && !context.isEmpty()) {
                allMessages.addAll(context);
            }
        }
        
        allMessages.add(new Message("user", prompt));

        switch (format) {
            case ANTHROPIC:
                return buildAnthropicPayload(allMessages);
            case SPARK:
                return buildSparkPayload(allMessages);
            case MINIMAX:
                return buildMinimaxPayload(allMessages);
            case CUSTOM:
                return buildCustomPayload(allMessages);
            case OPENAI:
            default:
                return buildOpenAIPayload(allMessages);
        }
    }

    private String buildOpenAIPayload(List<Message> messages) {
        StringBuilder messagesBuilder = new StringBuilder("[");
        
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (i > 0) {
                messagesBuilder.append(",");
            }
            messagesBuilder.append(String.format("{\"role\":\"%s\",\"content\":\"%s\"}",
                    msg.getRole(), escapeJson(msg.getContent())));
        }
        
        messagesBuilder.append("]");
        
        return String.format("{\"model\":\"%s\",\"messages\":%s,\"temperature\":0.7}", 
                model, messagesBuilder.toString());
    }

    private String buildAnthropicPayload(List<Message> messages) {
        StringBuilder messagesBuilder = new StringBuilder("[");
        String systemContent = "";
        boolean firstMessage = true;
        
        for (Message msg : messages) {
            if ("system".equals(msg.getRole())) {
                systemContent = msg.getContent();
            } else {
                if (!firstMessage) {
                    messagesBuilder.append(",");
                }
                firstMessage = false;
                messagesBuilder.append(String.format("{\"role\":\"%s\",\"content\":[{\"type\":\"text\",\"text\":\"%s\"}]}",
                        msg.getRole(), escapeJson(msg.getContent())));
            }
        }
        
        messagesBuilder.append("]");
        
        if (!systemContent.isEmpty()) {
            return String.format("{\"model\":\"%s\",\"system\":\"%s\",\"messages\":%s,\"max_tokens\":4096,\"temperature\":0.7}",
                    model, escapeJson(systemContent), messagesBuilder.toString());
        }
        
        return String.format("{\"model\":\"%s\",\"messages\":%s,\"max_tokens\":4096,\"temperature\":0.7}",
                model, messagesBuilder.toString());
    }

    private String buildSparkPayload(List<Message> messages) {
        StringBuilder textBuilder = new StringBuilder("[");
        
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (i > 0) {
                textBuilder.append(",");
            }
            String role = "system".equals(msg.getRole()) ? "user" : msg.getRole();
            textBuilder.append(String.format("{\"role\":\"%s\",\"content\":\"%s\"}",
                    role, escapeJson(msg.getContent())));
        }
        
        textBuilder.append("]");
        
        return String.format("{\"header\":{\"app_id\":\"%s\"},\"parameter\":{\"chat\":{\"domain\":\"general\",\"temperature\":0.5,\"max_tokens\":4096}},\"payload\":{\"message\":{\"text\":%s}}}",
                apiKey, textBuilder.toString());
    }

    private String buildMinimaxPayload(List<Message> messages) {
        StringBuilder promptBuilder = new StringBuilder();
        String systemContent = "";
        
        for (Message msg : messages) {
            if ("system".equals(msg.getRole())) {
                systemContent = msg.getContent();
            } else {
                if (promptBuilder.length() > 0) {
                    promptBuilder.append("\\n");
                }
                promptBuilder.append(String.format("%s: %s", 
                        "user".equals(msg.getRole()) ? "User" : "Assistant", 
                        msg.getContent()));
            }
        }
        
        if (promptBuilder.length() > 0) {
            promptBuilder.append("\\nAssistant:");
        }
        
        if (!systemContent.isEmpty()) {
            return String.format("{\"model\":\"%s\",\"prompt\":\"%s\",\"system_prompt\":\"%s\",\"temperature\":0.7,\"max_tokens\":4096}",
                    model, escapeJson(promptBuilder.toString()), escapeJson(systemContent));
        }
        
        return String.format("{\"model\":\"%s\",\"prompt\":\"%s\",\"temperature\":0.7,\"max_tokens\":4096}",
                model, escapeJson(promptBuilder.toString()));
    }

    private String buildCustomPayload(List<Message> messages) {
        StringBuilder messagesBuilder = new StringBuilder("[");
        
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (i > 0) {
                messagesBuilder.append(",");
            }
            messagesBuilder.append(String.format("{\"role\":\"%s\",\"content\":\"%s\"}",
                    msg.getRole(), escapeJson(msg.getContent())));
        }
        
        messagesBuilder.append("]");
        
        String userPrompt = "";
        if (!messages.isEmpty()) {
            userPrompt = messages.get(messages.size() - 1).getContent();
        }
        
        String systemPromptValue = systemPrompt;
        if (messages.size() >= 2 && "system".equals(messages.get(0).getRole())) {
            systemPromptValue = messages.get(0).getContent();
        }
        
        return requestTemplate
                .replace("{model}", escapeJson(model))
                .replace("{messages}", messagesBuilder.toString())
                .replace("{system_prompt}", escapeJson(systemPromptValue))
                .replace("{user_prompt}", escapeJson(userPrompt));
    }

    private void addMessageToContext(String playerId, String role, String content) {
        List<Message> context = playerContexts.computeIfAbsent(playerId, k -> new ArrayList<>());
        context.add(new Message(role, content));

        while (context.size() > contextLength) {
            context.remove(0);
        }
    }

    private String parseResponse(String json) {
        ApiFormat format;
        try {
            format = ApiFormat.valueOf(apiFormat.toUpperCase());
        } catch (IllegalArgumentException e) {
            format = ApiFormat.OPENAI;
        }

        String path;
        switch (format) {
            case ANTHROPIC:
                path = "content.0.text";
                break;
            case SPARK:
                path = "payload.choices.0.message.content";
                break;
            case MINIMAX:
                path = "reply";
                break;
            case CUSTOM:
                path = responsePath;
                break;
            case OPENAI:
            default:
                path = "choices.0.message.content";
                break;
        }

        return extractValueByPath(json, path);
    }

    private String extractValueByPath(String json, String path) {
        if (path == null || path.isEmpty()) {
            return extractJSONValue(json, "\"content\"");
        }

        String[] parts = path.split("\\.");
        String current = json;

        for (String part : parts) {
            if (part.matches("\\d+")) {
                int index = Integer.parseInt(part);
                current = extractArrayElement(current, index);
            } else {
                current = extractObjectField(current, part);
            }

            if (current == null) {
                return null;
            }
        }

        if (current != null && current.startsWith("\"")) {
            current = current.substring(1, current.length() - 1);
            current = current.replace("\\n", "\n").replace("\\\"", "\"").replace("\\'", "'");
        }

        return current;
    }

    private String extractObjectField(String json, String fieldName) {
        String key = "\"" + fieldName + "\"";
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

        char firstChar = json.charAt(valueStart);
        if (firstChar == '"') {
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
                    return "\"" + sb.toString() + "\"";
                } else {
                    sb.append(c);
                }
            }

            return "\"" + sb.toString() + "\"";
        } else if (firstChar == '{') {
            int depth = 1;
            for (int i = valueStart + 1; i < json.length(); i++) {
                if (json.charAt(i) == '{' && json.charAt(i - 1) != '\\') depth++;
                if (json.charAt(i) == '}' && json.charAt(i - 1) != '\\') depth--;
                if (depth == 0) {
                    return json.substring(valueStart, i + 1);
                }
            }
        } else if (firstChar == '[') {
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

    private String extractArrayElement(String json, int index) {
        int bracketIndex = json.indexOf("[");
        if (bracketIndex == -1) {
            return null;
        }

        int startIndex = bracketIndex + 1;
        int currentIndex = 0;
        int depth = 0;

        StringBuilder element = new StringBuilder();

        for (int i = startIndex; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '{' || c == '[') {
                depth++;
                element.append(c);
            } else if (c == '}' || c == ']') {
                depth--;
                element.append(c);
                if (depth == 0 && currentIndex == index) {
                    return element.toString();
                }
            } else if (c == ',' && depth == 0) {
                if (currentIndex == index) {
                    return element.toString().trim();
                }
                currentIndex++;
                element = new StringBuilder();
            } else {
                element.append(c);
            }
        }

        if (currentIndex == index) {
            return element.toString().trim();
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
            for (int j = valueStart + 1; j < json.length(); j++) {
                if (json.charAt(j) == '[' && json.charAt(j - 1) != '\\') depth++;
                if (json.charAt(j) == ']' && json.charAt(j - 1) != '\\') depth--;
                if (depth == 0) {
                    return json.substring(valueStart, j + 1);
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

    private boolean isValidHeaderName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        
        return name.matches("^[a-zA-Z0-9!#$%&'*+-.^_`|~]+$");
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