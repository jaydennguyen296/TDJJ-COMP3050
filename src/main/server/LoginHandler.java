import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LoginHandler implements HttpHandler {
    private static final String VALID_NAME = System.getenv("APP_USER");
    private static final String VALID_PASSWORD = System.getenv("APP_PASS");
    private static final String VALID_ENCRYPTED = resolveEncryptedPassword();
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

        if (!"POST".equalsIgnoreCase(he.getRequestMethod())) {
            sendResponse(he, 405, "{\"error\":\"method not allowed\"}");
            return;
        }

        String body = new String(he.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String name = firstNonNull(
            extractJsonField(body, "name"),
            extractFormField(body, "name"),
            extractFormField(body, "user")
        );
        String encpswrd = firstNonNull(
            extractJsonField(body, "encpswrd"),
            extractFormField(body, "encpswrd")
        );
        String plainPassword = firstNonNull(
            extractJsonField(body, "password"),
            extractFormField(body, "password"),
            extractFormField(body, "pass")
        );

        if (name == null || !NAME_PATTERN.matcher(name).matches()
            || (encpswrd == null && plainPassword == null)) {
            sendResponse(he, 400, "{\"error\":\"invalid request\"}");
            return;
        }

        if (VALID_NAME == null || (VALID_ENCRYPTED == null && VALID_PASSWORD == null)) {
            sendResponse(he, 500, "{\"error\":\"server credentials not configured\"}");
            return;
        }

        String candidateEncrypted = encpswrd;
        if (candidateEncrypted == null && plainPassword != null) {
            candidateEncrypted = sha256Hex(name + ";" + plainPassword);
        }

        boolean passwordMatches = false;
        if (candidateEncrypted != null && VALID_ENCRYPTED != null
            && VALID_ENCRYPTED.equals(candidateEncrypted.toLowerCase())) {
            passwordMatches = true;
        }
        if (!passwordMatches && plainPassword != null && VALID_PASSWORD != null
            && VALID_PASSWORD.equals(plainPassword)) {
            passwordMatches = true;
        }

        if (!VALID_NAME.equals(name) || !passwordMatches) {
            sendResponse(he, 401, "{\"error\":\"invalid credentials\"}");
            return;
        }

        String token = SessionManager.getInstance().createSession(name);
        sendResponse(he, 200, "{\"session\":\"" + token + "\"}");
    }

    private void sendResponse(HttpExchange he, int status, String body) throws IOException {
        he.getResponseHeaders().set("Content-Type", "application/json");
        he.sendResponseHeaders(status, body.getBytes().length);
        OutputStream os = he.getResponseBody();
        os.write(body.getBytes());
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

    private static String resolveEncryptedPassword() {
        String precomputed = System.getenv("APP_ENCPSWRD");
        if (precomputed != null && !precomputed.isBlank()) {
            return precomputed.toLowerCase();
        }
        String name = System.getenv("APP_USER");
        String password = System.getenv("APP_PASS");
        if (name == null || password == null) {
            return null;
        }
        return sha256Hex(name + ";" + password);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
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

    private String extractFormField(String body, String key) {
        if (body == null || body.isBlank()) {
            return null;
        }
        for (String param : body.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && key.equals(pair[0])) {
                return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private String firstNonNull(String... values) {
        for (String value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
