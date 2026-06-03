package comp3050;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class MoveHandlerTest {

    public static void main(String[] args) throws Exception {
        System.out.println("Running MoveHandlerTest tests...");
        System.out.println();

        HttpClient client = HttpClient.newHttpClient();

        // Test 1: Valid east move returns 200 with new position
        HttpRequest request1 = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:8000/move?dy=0&dx=1"))
            .GET()
            .build();
        HttpResponse<String> response1 = client.send(request1, HttpResponse.BodyHandlers.ofString());

        if (response1.statusCode() == 200 && response1.body().contains("\"x\":")) {
            System.out.println("Test 1 PASS: valid east move returns 200 with new position");
        } else {
            System.out.println("Test 1 FAIL: got status " + response1.statusCode() + ", body: " + response1.body());
        }

        // Test 2: Diagonal move returns 204 (not allowed by spec)
        HttpRequest request2 = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:8000/move?dy=1&dx=1"))
            .GET()
            .build();
        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());

        if (response2.statusCode() == 204) {
            System.out.println("Test 2 PASS: diagonal move returns 204");
        } else {
            System.out.println("Test 2 FAIL: got status " + response2.statusCode() + ", body: " + response2.body());
        }

        // Test 3: Move more than 1 step returns 204 (not allowed by spec)
        HttpRequest request3 = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:8000/move?dy=2&dx=0"))
            .GET()
            .build();
        HttpResponse<String> response3 = client.send(request3, HttpResponse.BodyHandlers.ofString());

        if (response3.statusCode() == 204) {
            System.out.println("Test 3 PASS: move more than 1 step returns 204");
        } else {
            System.out.println("Test 3 FAIL: got status " + response3.statusCode() + ", body: " + response3.body());
        }
    }
}