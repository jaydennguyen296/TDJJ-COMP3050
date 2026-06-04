package comp3050.server;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import comp3050.TileMap;

public class LogoutHandler implements HttpHandler {

    // Needed to drop the character's items on the ground at logout (retire).
    private final TileMap tileMap;

    public LogoutHandler(TileMap tileMap) {
        this.tileMap = tileMap;
    }

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
        // Resolve the character BEFORE invalidating revokes the token.
        String user = SessionManager.getInstance().getUser(token);

        if (SessionManager.getInstance().invalidate(token)) {
            // Design choice (spec p.13): "place all items held by a character
            // onto the ground and then forget the user information entirely."
            // The next login recreates the character at the spawn point —
            // which is where the supplied client boots its own position
            // belief, so a fresh login can never start desynced from the
            // server (the cause of the blank-avatar / endless-204 bug).
            WorldRegistry.getInstance().retire(user, tileMap);
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