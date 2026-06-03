package comp3050.server;

import comp3050.GameState;
import comp3050.TileMap;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class MoveHandlerTest {

    private static HttpServer server;
    private static String baseUrl;
    private static HttpClient client;
    private static String session;

    @BeforeAll
    static void setup() throws Exception {
        TileMap tileMap = new TileMap();
        GameState gameState = new GameState(5, 5);

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/move", new MoveHandler(tileMap, gameState));
        server.start();

        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        client = HttpClient.newHttpClient();

        session = SessionManager.getInstance().createSession("JUnitMoveUser");
    }

    @AfterAll
    static void tearDown() {
        server.stop(0);
    }

    @Test
    void moveWithoutSessionReturns401() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/move?dy=0&dx=1"))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
    }

    @Test
    void diagonalMoveWithSessionReturns204() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/move?dy=1&dx=1&session=" + session))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(204, response.statusCode());
    }

    @Test
    void moveMoreThanOneStepWithSessionReturns204() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/move?dy=2&dx=0&session=" + session))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(204, response.statusCode());
    }

    @Test
    void validNoMoveWithSessionReturns200() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/move?dy=0&dx=0&session=" + session))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        assertTrue(response.body().contains("\"y\":"));
        assertTrue(response.body().contains("\"x\":"));
    }
}