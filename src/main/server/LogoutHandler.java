import java.io.IOException;
import java.io.OutputStream;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LogoutHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange he) throws IOException {
        if (!"POST".equalsIgnoreCase(he.getRequestMethod())) {
            sendResponse(he, 405, "{\"error\":\"method not allowed\"}");
            return;
        }

        String token = extractToken(he);

        if (SessionManager.getInstance().invalidate(token)) {
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
        return null;
    }

    private void sendResponse(HttpExchange he, int status, String body)
            throws IOException {
        he.getResponseHeaders().set("Content-Type", "application/json");
        he.sendResponseHeaders(status, body.getBytes().length);
        OutputStream os = he.getResponseBody();
        os.write(body.getBytes());
        os.close();
    }
}
