package com.win9x.shopandpay.server;

import com.win9x.shopandpay.Win9xShopAndPay;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.HashSet;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

public class SimpleHttpServer {
    
    private HttpServer httpServer;
    private final Win9xShopAndPay plugin;
    private int port;
    private volatile boolean running;
    private Timer timeoutTimer;
    
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".html", ".htm", ".css", ".js", ".json", ".png", ".jpg", ".jpeg", 
            ".gif", ".svg", ".ico", ".txt", ".xml"
    );
    
    public SimpleHttpServer(Win9xShopAndPay plugin) {
        this.plugin = plugin;
        this.port = plugin.getConfig().getInt("gui.buy-currency-server-port", 8080);
    }
    
    public void start() {
        if (running) {
            cancelTimeout();
            scheduleStop();
            return;
        }
        
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/", new RootHandler());
            httpServer.createContext("/buy-currency", new BuyCurrencyHandler());
            httpServer.setExecutor(null);
            httpServer.start();
            
            running = true;
            scheduleStop();
            plugin.getLogger().info("内置HTTP服务器已启动，端口: " + port);
            plugin.getLogger().info("购买币种页面地址: http://localhost:" + port + "/buy-currency");
        } catch (IOException e) {
            plugin.getLogger().severe("启动内置HTTP服务器失败: " + e.getMessage());
        }
    }
    
    private void scheduleStop() {
        cancelTimeout();
        
        int timeoutSeconds = plugin.getConfig().getInt("gui.buy-currency-server-timeout", 180);
        if (timeoutSeconds <= 0) {
            return;
        }
        
        timeoutTimer = new Timer();
        timeoutTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                stop();
                plugin.getLogger().info("内置HTTP服务器已超时自动关闭");
            }
        }, timeoutSeconds * 1000L);
        
        plugin.getLogger().info("内置HTTP服务器将在 " + timeoutSeconds + " 秒后自动关闭");
    }
    
    private void cancelTimeout() {
        if (timeoutTimer != null) {
            timeoutTimer.cancel();
            timeoutTimer = null;
        }
    }
    
    public void stop() {
        cancelTimeout();
        
        if (!running || httpServer == null) {
            return;
        }
        
        httpServer.stop(0);
        running = false;
        plugin.getLogger().info("内置HTTP服务器已停止");
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public int getPort() {
        return port;
    }
    
    public String getBaseUrl() {
        if (!running) {
            return "";
        }
        return "http://localhost:" + port;
    }
    
    private class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "<html><body><h1>Win9xShopAndPay HTTP Server</h1></body></html>";
            sendResponse(exchange, response, "text/html");
        }
    }
    
    private class BuyCurrencyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String fileName = plugin.getConfig().getString("gui.buy-currency-file", "buy-currency.html");
            
            if (!isValidFileName(fileName)) {
                String response = "<html><body><h1>访问被拒绝</h1><p>无效的文件名</p></body></html>";
                exchange.sendResponseHeaders(403, response.getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
                exchange.close();
                return;
            }
            
            File htmlFile = new File(plugin.getDataFolder(), fileName);
            
            try {
                if (!htmlFile.getCanonicalPath().startsWith(plugin.getDataFolder().getCanonicalPath())) {
                    String response = "<html><body><h1>访问被拒绝</h1><p>非法路径访问</p></body></html>";
                    exchange.sendResponseHeaders(403, response.getBytes(StandardCharsets.UTF_8).length);
                    exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
                    exchange.close();
                    return;
                }
            } catch (IOException e) {
                String response = "<html><body><h1>服务器错误</h1></body></html>";
                exchange.sendResponseHeaders(500, response.getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
                exchange.close();
                return;
            }
            
            if (!htmlFile.exists()) {
                String response = "<html><body><h1>文件未找到</h1><p>" + fileName + " 不存在于插件配置文件夹中</p></body></html>";
                exchange.sendResponseHeaders(404, response.getBytes(StandardCharsets.UTF_8).length);
                exchange.getResponseBody().write(response.getBytes(StandardCharsets.UTF_8));
                exchange.close();
                return;
            }
            
            byte[] content = Files.readAllBytes(htmlFile.toPath());
            String contentType = getContentType(fileName);
            
            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
            exchange.sendResponseHeaders(200, content.length);
            exchange.getResponseBody().write(content);
            exchange.close();
        }
    }
    
    private boolean isValidFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return false;
        }
        
        String lowerFileName = fileName.toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lowerFileName.endsWith(ext)) {
                return true;
            }
        }
        
        return false;
    }
    
    private void sendResponse(HttpExchange exchange, String response, String contentType) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
    
    private String getContentType(String fileName) {
        if (fileName.endsWith(".html")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".gif")) return "image/gif";
        if (fileName.endsWith(".svg")) return "image/svg+xml";
        if (fileName.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}