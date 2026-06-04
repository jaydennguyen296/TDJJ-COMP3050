package comp3050.server;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Counter;
import io.prometheus.client.Histogram;
import io.prometheus.client.exporter.common.TextFormat;

import comp3050.TileMap;

public class Server {

    // Prometheus metrics, defined ONCE here: a metric name may only be
    // registered once per JVM, so the fields live in the composition root
    // rather than being duplicated across handler classes (which would throw
    // "Collector already registered" at class-load time). Handlers record
    // into these with their own endpoint label.
    static final Counter GAME_REQUESTS = Counter.build()
            .name("game_requests_total").help("Total game requests")
            .labelNames("endpoint", "status").register();

    static final Histogram GAME_LATENCY = Histogram.build()
            .name("game_request_duration_seconds").help("Game request latency")
            .labelNames("endpoint").register();

    public static void main(String[] args) throws Exception {
        TileMap tileMap = new TileMap();
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/login", new LoginHandler(tileMap));
        server.createContext("/logout", new LogoutHandler(tileMap));
        server.createContext("/info", new InfoHandler(tileMap));
        server.createContext("/move", new MoveHandler(tileMap));
        server.createContext("/take", new TakeHandler(tileMap));
        server.createContext("/place", new PlaceHandler(tileMap));
        server.createContext("/use", new UseHandler(tileMap));
        // Prometheus scrape endpoint: serialises every registered metric in
        // the text exposition format (version 0.0.4).
        server.createContext("/metrics", exchange -> {
            StringWriter writer = new StringWriter();
            TextFormat.write004(writer,
                    CollectorRegistry.defaultRegistry.metricFamilySamples());
            byte[] response = writer.toString().getBytes();
            exchange.getResponseHeaders().set("Content-Type", TextFormat.CONTENT_TYPE_004);
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });
        server.setExecutor(null); // creates a default executor
        server.start();
    }

}
