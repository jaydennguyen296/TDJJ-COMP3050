import java.io.IOException;
import java.io.OutputStream;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class MoveHandler implements HttpHandler {

    private GameState gameState;

    public MoveHandler(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        
        // handle CORS like MyHandler
        String origin = he.getRequestHeaders().getFirst("Origin");
        he.getResponseHeaders().add("Access-Control-Allow-Origin", origin);
        he.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, OPTIONS");
        he.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        int dy = 0;
        int dx = 0;

        //get query from url
        String query = he.getRequestURI().getQuery();

        if (query != null){
            String[] parts = query.split("&");
            for (String part : parts){
                String[] kv = part.split("=");
                if (kv[0].equals("dy")){
                    dy = Integer.parseInt(kv[1]);
                } else if (kv[0].equals("dx")){
                    dx = Integer.parseInt(kv[1]);
                }
            }
        }

        //No diagonal moves 
        if (dy != 0 && dx != 0){
            he.sendResponseHeaders(204, -1);
            he.close();
            return;
        }

        //only 1 step at a time
        if (Math.abs(dy) > 1 || Math.abs(dx > 1)){
            he.sendResponseHeaders(204, -1);
            he.close();
            return;
        }

        //work out new position 
        int newY = gameState.getPlayerY() + dy;
        int newX = gameState.getPlayerX() + dx;

        //check if blocked or out of bounds
        if (gameState.isValidPosition(newY, newX)){
            he.sendResponseHeaders(204, -1);
            he.close();
            return;
        }

        // update player position
        gameState.setPlayerPosition(newY, newX);

        // send back new position 
        Headers headers = he.getResponseHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("Connection", "close");

        String json = "{\"y\":" + newY + ",\"x\":" + newX + "}";
        he.sendResponseHeaders(200, json.length());
        OutputStream os = he.getResponseBody();
        os.write(json.getBytes());
        os.close(); 
    }
}
