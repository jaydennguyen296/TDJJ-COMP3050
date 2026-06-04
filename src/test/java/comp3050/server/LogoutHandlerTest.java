package comp3050.server;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

import comp3050.TileMap;

class LogoutHandlerTest {

    private static HttpServer server;
    private static String baseUrl;
    private static HttpClient client;
    private static TileMap tileMap;

    @BeforeAll
    static void setup() throws Exception {
        // Starts a small test server only for /logout
        tileMap = new TileMap();
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/logout", new LogoutHandler(tileMap));
        server.start();

        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        client = HttpClient.newHttpClient();
    }

    @AfterAll
    static void tearDown() {
        // Stops the test server after all tests finish
        server.stop(0);
    }

    @Test
    void logoutInvalidTokenReturns401() throws Exception {
        // V2: an invalid session token cannot log out
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/logout?session=notARealToken"))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
    }

    @Test
    void logoutDropsItemsAndForgetsCharacter() throws Exception {
        // v3 design choice (spec p.13): on logout, all held items are placed
        // on the ground at the character's cell and the character record is
        // forgotten entirely — the next login starts fresh at spawn
        String token = SessionManager.getInstance().createSession("JUnitLogoutUser");
        PlayerState player = WorldRegistry.getInstance().getOrCreate("JUnitLogoutUser");
        player.setPosition(19, 1);
        player.getInventory().add('k');

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/logout?session=" + token))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, response.statusCode());
        // Record forgotten...
        assertNull(WorldRegistry.getInstance().get("JUnitLogoutUser"));
        // ...and the held key now lies on the ground where they stood.
        assertEquals('k', tileMap.getItem(19, 1));
    }

    @Test
    void logoutTokenCannotBeReused() throws Exception {
        // V2: a token is revoked by logout; a second logout with it is 401
        String token = SessionManager.getInstance().createSession("JUnitReuseUser");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/logout?session=" + token))
                .GET()
                .build();

        HttpResponse<String> first =
                client.send(request, HttpResponse.BodyHandlers.ofString());
        HttpResponse<String> second =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(200, first.statusCode());
        assertEquals(401, second.statusCode());
    }
}
