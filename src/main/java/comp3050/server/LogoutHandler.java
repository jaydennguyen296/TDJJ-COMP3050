package comp3050.server;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LogoutHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange he) throws IOException {
        setCorsHeaders(he);
        if ("OPTIONS".equalsIgnoreCase(he.getRequestMethod())) {
            he.sendResponseHeaders(204, -1);
            he.close();
            return;
        }

        // POST per the spec; GET also accepted because the frontend logs out
        // via GET /logout?session=...
        String method = he.getRequestMethod();
        if (!"POST".equalsIgnoreCase(method) && !"GET".equalsIgnoreCase(method)) {
            sendResponse(he, 405, "{\"error\":\"method not allowed\"}");
            return;
        }

        String token = extractToken(he);

        if (SessionManager.getInstance().invalidate(token)) {
            // Design choice: RETAIN the player's PlayerState (position +
            // inventory + avatar) in WorldRegistry so a returning login
            // restores it. Only the session token is revoked here.
            // Alternative (NOT chosen): drop the player's items at their
            // (y,x) and forget the record; SessionManager.getUser(token)
            // before invalidating is the hook point for that policy.
            sendResponse(he, 200, "{\"message\":\"logged out\"}");
        } else {
            sendResponse(he, 401, "{\"error\":\"invalid token\"}");
        }
    }

    private String extractToken(HttpExchange he) {
        String auth = he.getRequestHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        String query = he.getRequestURI().getQuery();
        if (query == null) {
            return null;
        }
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && "session".equals(pair[0])) {
                return pair[1];
            }
        }
        return null;
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

    private void sendResponse(HttpExchange he, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        he.getResponseHeaders().set("Content-Type", "application/json");
        he.sendResponseHeaders(status, bytes.length);
        OutputStream os = he.getResponseBody();
        os.write(bytes);
        os.close();
    }
}