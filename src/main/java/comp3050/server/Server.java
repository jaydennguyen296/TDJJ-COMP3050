package comp3050.server;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpServer;

import comp3050.GameState;
import comp3050.TileMap;

public class Server {

    public static void main(String[] args) throws Exception {
        TileMap tileMap = new TileMap();
        GameState gameState = new GameState(5, 5);
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        server.createContext("/login", new LoginHandler());
        server.createContext("/logout", new LogoutHandler());
        server.createContext("/info", new InfoHandler(tileMap, gameState));
        server.createContext("/move", new MoveHandler(tileMap, gameState)); //check if the variables are correct
        server.setExecutor(null); // creates a default executor
        server.start();
    }

}