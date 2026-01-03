package com.teraapi.stream;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

/**
 * Lightweight HTTP Server for Stream Processing Service
 * Uses Java Standard Library (com.sun.net.httpserver) for minimal dependencies
 */
public class StreamProcessingService {

    private static final int PORT = 8080;
    private static final String JWT_SECRET = getRequiredEnvVar("JWT_SECRET");
    private static final String IDENTITY_INTROSPECTION_URL = System.getenv("IDENTITY_INTROSPECTION_URL");
    
    private static String getRequiredEnvVar(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                "CRITICAL: Environment variable '" + name + "' is required but not set. " +
                "Please configure all required secrets before starting the application."
            );
        }
        return value;
    }

    private HttpServer server;
    private StreamRequestHandler requestHandler;

    public static void main(String[] args) throws IOException {
        StreamProcessingService service = new StreamProcessingService();
        service.start();
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(PORT), 0);
        requestHandler = new StreamRequestHandler(JWT_SECRET, IDENTITY_INTROSPECTION_URL);

        // Register HTTP handlers
        server.createContext("/", this::handleRequest);
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));

        server.start();
        System.out.println("StreamProcessingService started on port " + PORT);
        System.out.println("Ready to process authenticated requests");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("StreamProcessingService stopped");
        }
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String authHeader = exchange.getRequestHeaders().getFirst("Authorization");

            System.out.println("[" + System.currentTimeMillis() + "] " + method + " " + path);

            requestHandler.handleRequest(method, path, authHeader, exchange.getRequestBody(), exchange.getResponseBody());

        } catch (Exception e) {
            System.err.println("Error handling request: " + e.getMessage());
            e.printStackTrace();
            try {
                exchange.sendResponseHeaders(500, -1);
            } catch (IOException ignored) {}
        } finally {
            exchange.close();
        }
    }
}
