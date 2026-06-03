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

class InfoHandlerTest {

    private static HttpServer server;
    private static String baseUrl;
    private static HttpClient client;
    private static String session;

    @BeforeAll
    static void setup() throws Exception {

        // Create test map and game state
        TileMap tileMap = new TileMap();
        GameState gameState = new GameState(5, 5);

        // Start temporary test server
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/info", new InfoHandler(tileMap, gameState));
        server.start();

        int port = server.getAddress().getPort();
        baseUrl = "http://127.0.0.1:" + port;

        client = HttpClient.newHttpClient();

        // Create valid V2 session token
        session = SessionManager.getInstance().createSession("JUnitInfoUser");
    }

    @AfterAll
    static void tearDown() {
        // Stop test server after all tests finish
        server.stop(0);
    }

    @Test
    void infoWithoutSessionReturns401() throws Exception {

        // V2 requires authentication
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/info?y=5&x=5"))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
    }

    @Test
    void infoWrongLocationReturns204() throws Exception {

        // Client position does not match server position
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/info?y=999&x=999&session=" + session))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(204, response.statusCode());
    }

    @Test
    void infoCorrectLocationReturns200() throws Exception {

        // Correct player position should return map information
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/info?y=5&x=5&session=" + session))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());

        // Check important JSON fields exist
        assertTrue(response.body().contains("\"user\""));
        assertTrue(response.body().contains("\"y\""));
        assertTrue(response.body().contains("\"x\""));
        assertTrue(response.body().contains("\"top\""));
        assertTrue(response.body().contains("\"left\""));
        assertTrue(response.body().contains("\"bottom\""));
        assertTrue(response.body().contains("\"right\""));
        assertTrue(response.body().contains("\"info\""));
    }
}