package comp3050.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import comp3050.TileMap;

public class InfoHandlerTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        TileMap tileMap = new TileMap();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/info", new InfoHandler(tileMap));
        server.start();

        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void testInfoHandlerCanBeCreated() throws Exception {
        // checks InfoHandler object can be created

        TileMap tileMap = new TileMap();
        InfoHandler handler = new InfoHandler(tileMap);

        assertNotNull(handler);
    }

    @Test
    void testInfoWithoutTokenReturns401() throws IOException {
        // checks user cannot access info without login token

        URL url = new URL("http://localhost:" + port + "/info?y=0&x=0");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        assertEquals(401, conn.getResponseCode());
    }

    @Test
    void testInfoMissingPositionReturns204() throws IOException {
        // checks info returns 204 if y and x are missing

        String token = SessionManager.getInstance().createSession("admin");

        URL url = new URL("http://localhost:" + port + "/info");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);

        assertEquals(204, conn.getResponseCode());
    }

    @Test
    void testInfoWrongPositionReturns204() throws IOException {
        // checks info returns 204 if player position is wrong

        String token = SessionManager.getInstance().createSession("admin");

        URL url = new URL("http://localhost:" + port + "/info?y=99&x=99");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);

        assertEquals(204, conn.getResponseCode());
    }

    @Test
    void testInfoCorrectPositionReturns200() throws IOException {
        // checks info works when y and x match the player's real position

        String token = SessionManager.getInstance().createSession("admin");

        PlayerState player = WorldRegistry.getInstance().getOrCreate("admin");
        int y = player.getY();
        int x = player.getX();

        URL url = new URL("http://localhost:" + port + "/info?y=" + y + "&x=" + x);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);

        assertEquals(200, conn.getResponseCode());
    }
}