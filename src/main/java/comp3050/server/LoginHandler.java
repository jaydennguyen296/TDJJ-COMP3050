package comp3050.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LoginHandler implements HttpHandler {
    private static final String VALID_NAME = System.getenv("APP_USER");
    private static final String VALID_ENCRYPTED = resolveEncryptedPassword();

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

        // Spec: if either or both fields are missing, the request is invalid;
        // a name with symbols, spaces, or numbers is likewise invalid
        if (name == null || encpswrd == null || !NAME_PATTERN.matcher(name).matches()) {
            sendResponse(he, 400, "{\"error\":\"invalid request\"}");
            return;
        }

        if (VALID_NAME == null || VALID_ENCRYPTED == null) {
            sendResponse(he, 500, "{\"error\":\"server credentials not configured\"}");
            return;
        }

        // Spec: the server never decrypts; it compares the client's encpswrd
        // with the encrypted value held on the server
        if (!VALID_NAME.equals(name) || !constantTimeEquals(VALID_ENCRYPTED, encpswrd.toLowerCase())) {
            sendResponse(he, 401, "{\"error\":\"invalid credentials\"}");
            return;
        }

        // Re-login replaces any existing session for this character (SessionManager)
        String token = SessionManager.getInstance().createSession(name);
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

    // Derived once at startup from APP_USER/APP_PASS, per the spec's
    // SHA-256("name;password") scheme. The plaintext is never compared.
    private static String resolveEncryptedPassword() {
        String name = System.getenv("APP_USER");
        String password = System.getenv("APP_PASS");
        if (name == null || password == null) {
            return null;
        }
        return sha256Hex(name + ";" + password);
    }

    // Spec example: sha256Hex("Baelin;Nice day for fishing.")
    //   = "841eb70b9dd5019642751955afbb960a0f741129877db34ceb420a8fb4a9d1dd"
    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
            a.getBytes(StandardCharsets.UTF_8),
            b.getBytes(StandardCharsets.UTF_8));
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
