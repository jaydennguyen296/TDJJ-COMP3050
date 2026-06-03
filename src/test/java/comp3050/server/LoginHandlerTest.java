package comp3050.server;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;

import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;

class LoginHandlerTest {

    private static HttpServer server;
    private static String baseUrl;
    private static HttpClient client;

    @BeforeAll
    static void setup() throws Exception {
        // Starts a small test server only for /login
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/login", new LoginHandler());
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
    void loginUsingGetReturns405() throws Exception {
        // V2: /login must use POST, not GET
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/login"))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(405, response.statusCode());
    }

    @Test
    void loginMissingNameReturns400() throws Exception {
        // V2: missing name field should be bad request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/login"))
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"encpswrd\":\"abc123\"}"
                ))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    @Test
    void loginMissingPasswordReturns400() throws Exception {
        // V2: missing encpswrd field should be bad request
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/login"))
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"name\":\"JUnitUser\"}"
                ))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(400, response.statusCode());
    }

    @Test
    void loginInvalidNameFormatReturns401() throws Exception {
        // V2: names can only contain letters and hyphens
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/login"))
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"name\":\"bad user 123\",\"encpswrd\":\"abc123\"}"
                ))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
    }

    @Test
    void loginWrongPasswordReturns401() throws Exception {
        // V2: wrong credentials should not create a session
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/login"))
                .POST(HttpRequest.BodyPublishers.ofString(
                        "{\"name\":\"JUnitUser\",\"encpswrd\":\"wrongpassword\"}"
                ))
                .header("Content-Type", "application/json")
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
    }
}