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

public class UseHandlerTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        TileMap tileMap = new TileMap();

        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/use", new UseHandler(tileMap));
        server.start();

        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void testUseHandlerCanBeCreated() throws Exception {
        // checks UseHandler object can be created

        TileMap tileMap = new TileMap();
        UseHandler handler = new UseHandler(tileMap);

        assertNotNull(handler);
    }

    @Test
    void testUseWithoutTokenReturns401() throws IOException {
        // checks user cannot use objects without login token

        URL url = new URL("http://localhost:" + port + "/use");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        assertEquals(401, conn.getResponseCode());
    }

    @Test
    void testUseDiagonalReturns204() throws IOException {
        // checks diagonal use is not allowed

        String token = SessionManager.getInstance().createSession("admin");

        URL url = new URL("http://localhost:" + port + "/use?dy=1&dx=1");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);

        assertEquals(204, conn.getResponseCode());
    }

    @Test
    void testUseTooFarReturns204() throws IOException {
        // checks using more than one tile away is not allowed

        String token = SessionManager.getInstance().createSession("admin");

        URL url = new URL("http://localhost:" + port + "/use?dy=0&dx=2");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);

        assertEquals(204, conn.getResponseCode());
    }

    @Test
    void testUseCurrentCellReturns204or200() throws IOException {
        // checks use can target current cell
        // 200 if usable door exists
        // 204 if no door exists

        String token = SessionManager.getInstance().createSession("admin");

        URL url = new URL("http://localhost:" + port + "/use?dy=0&dx=0");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + token);

        int status = conn.getResponseCode();

        boolean validStatus = status == 200 || status == 204;
        assertEquals(true, validStatus);
    }
}