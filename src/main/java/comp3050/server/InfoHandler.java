package comp3050.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import comp3050.GameState;
import comp3050.TileMap;

public class InfoHandler implements HttpHandler {

    // Player sees an 11x11 window: VIEW_RADIUS tiles in each direction
    private static final int VIEW_RADIUS = 5;

    private final TileMap tileMap;
    private final GameState gameState;

    public InfoHandler(TileMap tileMap, GameState gameState) {
        this.tileMap = tileMap;
        this.gameState = gameState;
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
        String user = SessionManager.getInstance().getUser(token);

        if (user == null) {
            sendResponse(he, 401, "{\"error\":\"not authenticated\"}");
            return;
        }

        int playerY = gameState.getPlayerY();
        int playerX = gameState.getPlayerX();

        // The client reports where it believes the player is; if that is
        // missing or stale, reply 204 (no content) so it knows to resync.
        Integer requestY = queryParam(he, "y");
        Integer requestX = queryParam(he, "x");
        if (requestY == null || requestX == null
                || requestY.intValue() != playerY
                || requestX.intValue() != playerX) {
            he.sendResponseHeaders(204, -1);
            he.close();
            return;
        }

        int top = Math.max(0, playerY - VIEW_RADIUS);
        int left = tileMap.wrapX(playerX - VIEW_RADIUS);
        int bottom = Math.min(tileMap.getHeight() - 1, playerY + VIEW_RADIUS);
        int right = tileMap.wrapX(playerX + VIEW_RADIUS);

        StringBuilder json = new StringBuilder();
        json.append("{");
        // Names are ASCII letters and hyphens only (spec), so no JSON escaping needed
        json.append("\"user\":\"").append(user).append("\",");
        json.append("\"y\":").append(playerY).append(",");
        json.append("\"x\":").append(playerX).append(",");
        json.append("\"top\":").append(top).append(",");
        json.append("\"left\":").append(left).append(",");
        json.append("\"bottom\":").append(bottom).append(",");
        json.append("\"right\":").append(right).append(",");
        json.append("\"info\":[");

        for (int y = top; y <= bottom; y++) {
            if (y > top) {
                json.append(",");
            }
            json.append("[");
            for (int col = 0; col <= 2 * VIEW_RADIUS; col++) {
                if (col > 0) {
                    json.append(",");
                }
                int x = tileMap.wrapX(playerX - VIEW_RADIUS + col);
                json.append("\"").append(tileMap.getTileOrBlank(y, x)).append("\"");
            }
            json.append("]");
        }

        json.append("]}");
        sendResponse(he, 200, json.toString());
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

    private Integer queryParam(HttpExchange he, String name) {
        String query = he.getRequestURI().getQuery();
        if (query == null) {
            return null;
        }
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (pair.length == 2 && name.equals(pair[0])) {
                try {
                    return Integer.valueOf(pair[1]);
                } catch (NumberFormatException e) {
                    return null; // non-numeric counts as missing -> 204
                }
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
