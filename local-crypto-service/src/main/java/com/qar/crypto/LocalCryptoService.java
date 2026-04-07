package com.qar.crypto;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class LocalCryptoService {

    private static final int DEFAULT_PORT = 18234;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    private HttpServer server;
    private final int port;
    private final AesCryptoService aesService;

    public LocalCryptoService() {
        this(DEFAULT_PORT);
    }

    public LocalCryptoService(int port) {
        this.port = port;
        this.aesService = new AesCryptoService();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        
        server.createContext("/encrypt", new EncryptHandler());
        server.createContext("/decrypt", new DecryptHandler());
        server.createContext("/health", new HealthHandler());
        server.createContext("/status", new StatusHandler());
        server.createContext("/keys", new KeysHandler());
        
        server.setExecutor(null);
        server.start();
        
        System.out.println("========================================");
        System.out.println("  QAR 本地加密服务已启动");
        System.out.println("  监听地址: http://127.0.0.1:" + port);
        System.out.println("========================================");
        System.out.println();
        System.out.println("可用接口:");
        System.out.println("  POST /encrypt  - 加密数据");
        System.out.println("  POST /decrypt  - 解密数据");
        System.out.println("  GET  /health   - 健康检查");
        System.out.println("  GET  /status   - 服务状态");
        System.out.println();
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("本地加密服务已停止");
        }
    }

    private class EncryptHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                sendCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!exchange.getRequestMethod().equals("POST")) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String body = readBody(exchange);
                Map<String, Object> request = GSON.fromJson(body, Map.class);
                
                String data = (String) request.get("data");
                String policy = (String) request.getOrDefault("policy", "role:user");
                
                if (data == null || data.isEmpty()) {
                    sendError(exchange, 400, "Missing 'data' field");
                    return;
                }

                Map<String, Object> result = aesService.encrypt(data, policy);
                sendJson(exchange, 200, result);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Encryption failed: " + e.getMessage());
            }
        }
    }

    private class DecryptHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equals("OPTIONS")) {
                sendCorsHeaders(exchange);
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if (!exchange.getRequestMethod().equals("POST")) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                String body = readBody(exchange);
                Map<String, Object> request = GSON.fromJson(body, Map.class);
                
                String encryptedData = (String) request.get("encryptedData");
                String wrappedKey = (String) request.get("wrappedKey");
                String policy = (String) request.getOrDefault("policy", "");
                
                if (encryptedData == null || encryptedData.isEmpty()) {
                    sendError(exchange, 400, "Missing 'encryptedData' field");
                    return;
                }

                Map<String, Object> result = aesService.decrypt(encryptedData, wrappedKey, policy);
                sendJson(exchange, 200, result);
                
            } catch (Exception e) {
                e.printStackTrace();
                sendError(exchange, 500, "Decryption failed: " + e.getMessage());
            }
        }
    }

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "ok");
            response.put("service", "qar-local-crypto");
            sendJson(exchange, 200, response);
        }
    }

    private class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> response = new HashMap<>();
            response.put("status", "running");
            response.put("service", "QAR Local Crypto Service");
            response.put("version", "1.0.0");
            response.put("port", port);
            response.put("crypto", "AES-256-GCM");
            response.put("labe", "pending");
            response.put("storedKeys", aesService.getKeyCount());
            sendJson(exchange, 200, response);
        }
    }

    private class KeysHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Object> response = new HashMap<>();
            response.put("keys", aesService.getStoredKeys());
            sendJson(exchange, 200, response);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private void sendCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendJson(HttpExchange exchange, int code, Map<String, Object> data) throws IOException {
        String json = GSON.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(code, bytes.length);
        
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private void sendError(HttpExchange exchange, int code, String message) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("error", message);
        sendJson(exchange, code, error);
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, using default: " + DEFAULT_PORT);
            }
        }
        
        LocalCryptoService service = new LocalCryptoService(port);
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n正在关闭服务...");
            service.stop();
        }));
        
        try {
            service.start();
            Thread.currentThread().join();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
