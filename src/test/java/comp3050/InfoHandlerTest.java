package comp3050;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class InfoHandlerTest {

    public static void main(String[] args) throws Exception {
        System.out.println("Running InfoHandlerTest tests...");
        System.out.println();

        // Test 1: Valid request returns 200 with JSON
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:8000/info?y=5&x=5"))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 && response.body().contains("\"y\":5")) {
            System.out.println("Test 1 PASS: valid request returns 200 with JSON");
        } else {
            System.out.println("Test 1 FAIL: got status " + response.statusCode() + ", body: " + response.body());
        }

        // Test 2: Wrong location returns 204
        HttpRequest request2 = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:8000/info?y=999&x=999"))
            .GET()
            .build();
        HttpResponse<String> response2 = client.send(request2, HttpResponse.BodyHandlers.ofString());

        if (response2.statusCode() == 204) {
            System.out.println("Test 2 PASS: wrong location returns 204");
        } else {
            System.out.println("Test 2 FAIL: got status " + response2.statusCode() + ", body: " + response2.body());
        }
        
        // Test 3: Missing parameters returns 204
        HttpRequest request3 = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:8000/info"))
            .GET()
            .build();
        HttpResponse<String> response3 = client.send(request3, HttpResponse.BodyHandlers.ofString());

        if (response3.statusCode() == 204) {
            System.out.println("Test 3 PASS: missing parameters returns 204");
        } else {
            System.out.println("Test 3 FAIL: got status " + response3.statusCode() + ", body: " + response3.body());
        }

        // Test 4: JSON response contains all required fields (y, x, top, left, bottom, right, info)
        HttpRequest request4 = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:8000/info?y=5&x=5"))
            .GET()
            .build();
        HttpResponse<String> response4 = client.send(request4, HttpResponse.BodyHandlers.ofString());
        String body4 = response4.body();

        boolean hasALLFields = body4.contains("\"y\":")
                            && body4.contains("\"x\":")
                            && body4.contains("\"top\":")
                            && body4.contains("\"left\":")
                            && body4.contains("\"bottom\":")
                            && body4.contains("\"right\":")
                            && body4.contains("\"info\":");    

        if (response4.statusCode() == 200 && hasALLFields) {
            System.out.println("Test 4 PASS: JSON response contains all required fields");
        } else {
            System.out.println("Test 4 FAIL: got status " + response4.statusCode() + ", body: " + body4);
        }

        // Test 5: JSON content correctness
        // The info field should represent an 11x11 visible area around the player.
        HttpRequest request5 = HttpRequest.newBuilder()
            .uri(URI.create("http://127.0.0.1:8000/info?y=5&x=5"))
            .GET()
            .build();

        HttpResponse<String> response5 = client.send(request5, HttpResponse.BodyHandlers.ofString());
        String body5 = response5.body();

        // Check expected window boundaries
        boolean correctBounds =
            body5.contains("\"top\":0")
            && body5.contains("\"bottom\":10")
            && body5.contains("\"left\":0")
            && body5.contains("\"right\":10");

        // Count rows in the info array
        // Assumes rows are represented like ["a","b","c"...]
        int rowCount = body5.split("\\],\\[").length;

        // Rough column count check using first row
        int firstRowStart = body5.indexOf("[[");
        int firstRowEnd = body5.indexOf("]", firstRowStart);

        boolean correctColumns = false;

        if (firstRowStart != -1 && firstRowEnd != -1) {
            String firstRow = body5.substring(firstRowStart + 2, firstRowEnd);

            // Count commas + 1 = number of entries
            int columnCount = firstRow.split(",").length;

            correctColumns = (columnCount == 11);
        }

        boolean correctSize = (rowCount == 11) && correctColumns;

        if (response5.statusCode() == 200 && correctBounds && correctSize) {
            System.out.println("Test 5 PASS: info array and bounds are correct");
        } else {
            System.out.println("Test 5 FAIL:");
            System.out.println("Status: " + response5.statusCode());
            System.out.println("Bounds correct: " + correctBounds);
            System.out.println("Array size correct: " + correctSize);
            System.out.println("Body: " + body5);
        }


    }
}