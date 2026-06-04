package comp3050.server;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

import comp3050.TileMap;

public class Server {

    public static void main(String[] args) throws Exception {
        TileMap tileMap = new TileMap();
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/login", new LoginHandler());
        server.createContext("/logout", new LogoutHandler());
        server.createContext("/info", new InfoHandler(tileMap));
        server.createContext("/move", new MoveHandler(tileMap));
        server.createContext("/take", new TakeHandler(tileMap));
        server.createContext("/place", new PlaceHandler(tileMap));
        server.createContext("/use", new UseHandler(tileMap));
        // Concurrency: the default executor can dispatch exchanges concurrently.
        // SessionManager and WorldRegistry are ConcurrentHashMap-backed, and
        // WorldRegistry.tryMove synchronises cell claims so two players cannot
        // claim the same cell. The TileMap overlay HashMap is mutated from
        // handler threads; acceptable here, a candidate for hardening later.
        server.setExecutor(null); // creates a default executor
        server.start();
    }

}