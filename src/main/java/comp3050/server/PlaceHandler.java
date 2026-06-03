package comp3050.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import comp3050.TileMap;

public class PlaceHandler implements HttpHandler {

    private final TileMap tileMap;

    public PlaceHandler(TileMap tileMap) {
        this.tileMap = tileMap;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        setCorsHeaders(he);
        he.getResponseHeaders().set("Connection", "close");

        if ("OPTIONS".equalsIgnoreCase(he.getRequestMethod())) {
            he.sendResponseHeaders(204, -1);
            he.close();
            return;
        }

        String token = extractToken(he);
        String username = SessionManager.getInstance().getUser(token);
        if (username == null) {
            sendResponse(he, 401, "{\"error\":\"not authenticated\"}");
            return;
        }
        PlayerState player = WorldRegistry.getInstance().getOrCreate(username);

        // PLACE acts on THIS player's current cell.
        int y = player.getY();
        int x = player.getX();
        List<Character> inv = player.getInventory();

        // 204 if the inventory is empty (nothing to place).
        if (inv.isEmpty()) {
            he.sendResponseHeaders(204, -1);
            he.close();
            return;
        }

        // 204 if the cell already holds an item (one item per square).
        if (itemClass(tileMap.getItem(y, x)) != 0) {
            he.sendResponseHeaders(204, -1);
            he.close();
            return;
        }

        // Drop the first held item onto the ground.
        char kind = inv.remove(0);
        tileMap.setItem(y, x, kind);

        sendResponse(he, 200, "{\"placed\":\"" + kind + "\"}");
    }

    // Item-kind -> item-class marker. 'T'=tool, 'D'=drink, 'A'=artifact.
    // Returns 0 if the char is not a takeable item.
    private static char itemClass(char kind) {
        switch (kind) {
            case 'a': return 'T'; // axe          -> tool
            case 'c': return 'D'; // cyan potion  -> drink
            case 'h': return 'D'; // heart potion -> drink
            case 'k': return 'A'; // key          -> artifact
            default:  return 0;
        }
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

    // Get Authorization token: Bearer header first, ?session= fallback
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