package comp3050.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import comp3050.GameState;
import comp3050.TileMap;

public class MoveHandler implements HttpHandler {

    private final TileMap tileMap;
    private final GameState gameState;

    public MoveHandler(TileMap tileMap, GameState gameState) {
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
        if (SessionManager.getInstance().getUser(token) == null) {
            sendResponse(he, 401, "{\"error\":\"not authenticated\"}");
            return;
        }

        Integer dyParam = queryParam(he, "dy");
        Integer dxParam = queryParam(he, "dx");
        int dy = dyParam == null ? 0 : dyParam;
        int dx = dxParam == null ? 0 : dxParam;

        // One tile per move, no diagonals; illegal moves are rejected with 204
        if ((dy != 0 && dx != 0) || Math.abs(dy) > 1 || Math.abs(dx) > 1) {
            he.sendResponseHeaders(204, -1);
            he.close();
            return;
        }

        // Target computed from the server's own position, x wraps like the map
        int newY = gameState.getPlayerY() + dy;
        int newX = tileMap.wrapX(gameState.getPlayerX() + dx);

        // Server-side collision check: blocked or out-of-bounds tiles reject the move
        if (tileMap.isBlocking(newY, newX)) {
            he.sendResponseHeaders(204, -1);
            he.close();
            return;
        }

        gameState.setPlayerPosition(newY, newX);
        sendResponse(he, 200, "{\"y\":" + newY + ",\"x\":" + newX + "}");
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
                    return null; // non-numeric counts as missing -> treated as 0
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
