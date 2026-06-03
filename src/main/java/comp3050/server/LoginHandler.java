package comp3050.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LoginHandler implements HttpHandler {
    // Spec: names are ASCII letters and hyphens only, case-sensitive
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z-]+$");
    private static final Pattern JSON_FIELD_PATTERN =
        Pattern.compile("\"([^\"]+)\"\\s*:\\s*\"([^\"]*)\"");

    @Override
    public void handle(HttpExchange he) throws IOException {
        setCorsHeaders(he);
        if ("OPTIONS".equalsIgnoreCase(he.getRequestMethod())) {
            he.sendResponseHeaders(204, -1);
            he.close();
            return;
        }

        // Spec: LOGIN must be a POST message
        if (!"POST".equalsIgnoreCase(he.getRequestMethod())) {
            sendResponse(he, 405, "{\"error\":\"method not allowed\"}");
            return;
        }

        // Spec: the body is a textual JSON object with name and encpswrd
        String body = new String(he.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String name = extractJsonField(body, "name");
        String encpswrd = extractJsonField(body, "encpswrd");

        // Spec: if either or both fields are missing, the request is invalid -> 400
        if (name == null || encpswrd == null) {
            sendResponse(he, 400, "{\"error\":\"invalid request\"}");
            return;
        }

        // A present-but-malformed name (symbols, spaces, numbers) cannot match
        // any server record -> 401
        if (!NAME_PATTERN.matcher(name).matches()) {
            sendResponse(he, 401, "{\"error\":\"invalid credentials\"}");
            return;
        }

        // Spec: the server never decrypts; it compares the client's encpswrd
        // with the encrypted value held in the player registry
        Player player = PlayerRegistry.getInstance().find(name);
        if (player == null || !PlayerRegistry.getInstance().verify(player, encpswrd)) {
            sendResponse(he, 401, "{\"error\":\"invalid credentials\"}");
            return;
        }

        // Re-login replaces any existing session for this character (SessionManager);
        // the live world record is created on first login and restored on return
        String token = SessionManager.getInstance().createSession(name, player);
        WorldRegistry.getInstance().getOrCreate(name);
        sendResponse(he, 200, "{\"session\":\"" + token + "\"}");
    }

    private void sendResponse(HttpExchange he, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        he.getResponseHeaders().set("Content-Type", "application/json");
        he.sendResponseHeaders(status, bytes.length);
        OutputStream os = he.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private void setCorsHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin == null ? "*" : origin);
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        exchange.getResponseHeaders().set("Vary", "Origin");

        String requestPrivateNetwork = exchange.getRequestHeaders().getFirst("Access-Control-Request-Private-Network");
        if ("true".equalsIgnoreCase(requestPrivateNetwork)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Private-Network", "true");
        }
    }

    private String extractJsonField(String body, String key) {
        if (body == null || body.isBlank()) {
            return null;
        }
        Matcher matcher = JSON_FIELD_PATTERN.matcher(body);
        while (matcher.find()) {
            if (key.equals(matcher.group(1))) {
                return matcher.group(2);
            }
        }
        return null;
    }
}