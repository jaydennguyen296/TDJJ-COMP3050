package comp3050.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

public class LogoutHandlerTest {

    private HttpServer server;
    private int port;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/logout", new LogoutHandler());
        server.start();

        port = server.getAddress().getPort();
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    // Checks valid Bearer token can logout successfully
    @Test
    void testLogoutWithBearerTokenReturns200() throws IOException {
        String token = SessionManager.getInstance().createSession("admin");

        URL url = new URL("http://localhost:" + port + "/logout");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + token);

        assertEquals(200, conn.getResponseCode());
    }

    // Checks invalid token cannot logout
    @Test
    void testLogoutWithInvalidTokenReturns401() throws IOException {
        URL url = new URL("http://localhost:" + port + "/logout?session=fake-token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");

        assertEquals(401, conn.getResponseCode());
    }

    // Checks GET logout works because frontend uses GET /logout?session=...
    @Test
    void testLogoutWithSessionQueryReturns200() throws IOException {
        String token = SessionManager.getInstance().createSession("admin");

        URL url = new URL("http://localhost:" + port + "/logout?session=" + token);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        assertEquals(200, conn.getResponseCode());
    }

    // Checks unsupported method returns 405
    @Test
    void testLogoutWithUnsupportedMethodReturns405() throws IOException {
        URL url = new URL("http://localhost:" + port + "/logout");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");

        assertEquals(405, conn.getResponseCode());
    }

    // Checks OPTIONS preflight request works
    @Test
    void testLogoutOptionsReturns204() throws IOException {
        URL url = new URL("http://localhost:" + port + "/logout");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("OPTIONS");

        assertEquals(204, conn.getResponseCode());
    }
}