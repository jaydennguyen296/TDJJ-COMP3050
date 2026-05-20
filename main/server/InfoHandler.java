import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class InfoHandler implements HttpHandler {

    private final TileMap tileMap;
    private final GameState gameState;

    public InfoHandler(TileMap tileMap, GameState gameState) {
        this.tileMap = tileMap;
        this.gameState = gameState;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        exchange.getResponseHeaders().set("Connection", "close");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        Integer requestY = null;
        Integer requestX = null;

        String query = exchange.getRequestURI().getQuery();
        if (query != null) {
            System.out.println("Query: " + query); //Print
            String[] parts = query.split("&");
            for (String part : parts) {
                System.out.println("Part: " + part); //Print
                String[] kv = part.split("=");
                for (String s : kv) {
                    System.out.println(s); //Print
                }
                if ("y".equals(kv[0])) {
                    requestY = Integer.parseInt(kv[1]);
                } else if ("x".equals(kv[0])) {
                    requestX = Integer.parseInt(kv[1]);
                }
            }
        }

        if (requestY == null || requestX == null) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        int playerY = gameState.getPlayerY();
        int playerX = gameState.getPlayerX();

        int top = Math.max(0, playerY - 5);
        int left = tileMap.wrapX(playerX - 5);
        int bottom = Math.min(tileMap.getHeight() - 1, playerY + 5);
        int right = tileMap.wrapX(playerX + 5);

        StringBuilder json = new StringBuilder();
        json.append("{");
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
            for (int col = 0; col <= 10; col++) {
                if (col > 0) {
                    json.append(",");
                }
                int x = tileMap.wrapX(playerX - 5 + col);
                json.append("\"").append(tileMap.getTileOrBlank(y, x)).append("\"");
            }
            json.append("]");
        }

        json.append("]}");

        byte[] body = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private void setCorsHeaders(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", origin == null ? "*" : origin);
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().set("Vary", "Origin");

        String requestPrivateNetwork = exchange.getRequestHeaders().getFirst("Access-Control-Request-Private-Network");
        if ("true".equalsIgnoreCase(requestPrivateNetwork)) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Private-Network", "true");
        }
    }
}
