package comp3050.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import comp3050.TileMap;

public class TakeHandlerTest {
    // testTakeHandlerCanBeCreated
    // checks that the TakeHandler constructor works correctly
    // verifies the object can be created without crashing

    @Test
    void testTakeHandlerCanBeCreated() throws Exception {
        TileMap map = new TileMap();
        TakeHandler handler = new TakeHandler(map);

        assertNotNull(handler);
    }
}