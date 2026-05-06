import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class MoveHandler implements HttpHandler {

    private final TileMap tileMap;
    private GameState gameState;

    public MoveHandler(TileMap tileMap, GameState gameState) {
        this.tileMap = tileMap;
        this.gameState = gameState;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        setCorsHeaders(exchange);
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        
        int dy = 0;
        int dx = 0;

        String query = exchange.getRequestURI().getQuery();

        if (query != null) {
            String[] parts = query.split("&");
            for (String part : parts) {
                String[] kv = part.split("=");
                if (kv[0].equals("dy")) {
                    dy = Integer.parseInt(kv[1]);
                } else if (kv[0].equals("dx")) {
                    dx = Integer.parseInt(kv[1]);
                }
            }
        }

        //No diagonal moves 
        if (dy != 0 && dx != 0){
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        //only 1 step at a time
        if (Math.abs(dy) > 1 || Math.abs(dx) > 1){
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        int newY = gameState.getPlayerY() + dy;
        int newX = tileMap.wrapX(gameState.getPlayerX() + dx);

        if (tileMap.isBlocking(newY, newX)) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        // update player position
        gameState.setPlayerPosition(newY, newX);

        // send back new position 
        Headers headers = exchange.getResponseHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Connection", "close");

        String json = "{\"y\":" + newY + ",\"x\":" + newX + "}";
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        OutputStream os = exchange.getResponseBody();
        os.write(body);
        os.close(); 
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