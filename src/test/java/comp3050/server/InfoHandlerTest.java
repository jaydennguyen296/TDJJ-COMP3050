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
    void testInfoStalePositionReturnsAuthoritative200() throws IOException {
        // A client whose y/x belief has desynced from the server must get the
        // authoritative window back (with the real y/x) so it can recover --
        // a 204 here would leave it stuck with no avatar on screen.

        String token = SessionManager.getInstance().createSession("admin");
        PlayerState player = WorldRegistry.getInstance().getOrCreate("admin");

        URL url = new URL("http://localhost:" + port + "/info?y=99&x=99");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);

        assertEquals(200, conn.getResponseCode());
        String body = new String(conn.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        // Body carries the server's real position, not the stale request's
        org.junit.jupiter.api.Assertions.assertTrue(
            body.contains("\"y\":" + player.getY() + ",\"x\":" + player.getX()));
    }

    @Test
    void testInfoStaleClientBypassesUnchangedBody204() throws IOException {
        // After a correct 200, an identical world normally dedups to 204 --
        // but a STALE client must still receive the full body to resync.

        String token = SessionManager.getInstance().createSession("admin");
        PlayerState player = WorldRegistry.getInstance().getOrCreate("admin");
        int y = player.getY();
        int x = player.getX();

        URL first = new URL("http://localhost:" + port + "/info?y=" + y + "&x=" + x);
        HttpURLConnection firstConn = (HttpURLConnection) first.openConnection();
        firstConn.setRequestProperty("Authorization", "Bearer " + token);
        assertEquals(200, firstConn.getResponseCode());

        // Same world, stale belief: must be 200, not the dedup 204
        URL stale = new URL("http://localhost:" + port + "/info?y=99&x=99");
        HttpURLConnection staleConn = (HttpURLConnection) stale.openConnection();
        staleConn.setRequestProperty("Authorization", "Bearer " + token);
        assertEquals(200, staleConn.getResponseCode());
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