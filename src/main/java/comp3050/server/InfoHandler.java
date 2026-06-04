package comp3050.server;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import io.prometheus.client.Histogram;

import comp3050.TileMap;

public class InfoHandler implements HttpHandler {

    // Player sees an 11x11 window: VIEW_RADIUS tiles in each direction
    private static final int VIEW_RADIUS = 5;

    private final TileMap tileMap;

    // Last 200 body sent per SESSION TOKEN. If a repeat INFO request would
    // produce an identical body (nothing changed in their window), reply 204
    // instead of re-sending the same JSON -- spec-sanctioned bandwidth
    // optimisation ("the last identical request from the same client").
    // Keyed by token, NOT username: a fresh login (new token) is a new client
    // with no map data yet, so it must always receive its first 200 body --
    // keying by username starved second browser sessions into blank maps.
    private final ConcurrentHashMap<String, String> lastBody = new ConcurrentHashMap<>();

    public InfoHandler(TileMap tileMap) {
        this.tileMap = tileMap;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        // Counted in the finally so every request (incl. the frequent 204
        // nothing-changed replies) is observed along with its response
        // status; an exception before a response is sent counts as a 500.
        Histogram.Timer timer = Server.GAME_LATENCY.labels("/info").startTimer();
        try {
            doHandle(he);
        } finally {
            timer.observeDuration();
            int code = he.getResponseCode(); // -1 if no response was sent
            Server.GAME_REQUESTS.labels("/info",
                    code == -1 ? "500" : String.valueOf(code)).inc();
        }
    }

    private void doHandle(HttpExchange he) throws IOException {
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

        // Centre the window on THIS player's live position.
        PlayerState self = WorldRegistry.getInstance().getOrCreate(user);
        int playerY = self.getY();
        int playerX = self.getX();

        // The client reports where it believes the player is. A missing or
        // malformed y/x is an invalid request -> 204 (spec). But a PRESENT,
        // mismatching y/x means the client has desynced from the server (a
        // lost MOVE response, blocking by another player, or a re-login it
        // didn't initiate). Replying 204 there is unrecoverable: the client
        // never receives fresh map data, so its avatar vanishes and it loops
        // on 204s forever. Instead always answer with the AUTHORITATIVE
        // window, centred on the server's position and carrying the real
        // y/x, so a stale client redraws itself back into sync.
        Integer requestY = queryParam(he, "y");
        Integer requestX = queryParam(he, "x");
        if (requestY == null || requestX == null) {
            he.sendResponseHeaders(204, -1);
            he.close();
            return;
        }
        boolean clientStale = requestY.intValue() != playerY
                || requestX.intValue() != playerX;

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
                String cell = tileMap.getLocationString(y, x);
                // The client draws players from the map data, so any cell with
                // a player on it carries their avatar digit (0-9), drawn LAST.
                // Live digits stay in WorldRegistry, not the TileMap overlay;
                // they are appended at serialise time only.
                PlayerState occupant = WorldRegistry.getInstance().occupantAt(y, x);
                if (occupant != null) {
                    cell = cell + occupant.getAvatar();
                }
                json.append("\"").append(cell).append("\"");
            }
            json.append("]");
        }

        json.append("]}");
        String body = json.toString();
        // Identical to what this session last received -> nothing changed, 204.
        // Any world change (own move, other players, take/place/use) alters
        // the body, so the next request naturally returns 200 again.
        // EXCEPT when the client's reported y/x is stale: its picture of the
        // world is wrong regardless of whether ours changed, so it must get
        // the full body to resync (its screen has lost the avatar).
        if (!clientStale && body.equals(lastBody.get(token))) {
            he.sendResponseHeaders(204, -1);
            he.close();
            return;
        }
        lastBody.put(token, body);
        sendResponse(he, 200, body);
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