package comp3050.server;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import comp3050.TileMap;

public class PlaceHandlerTest {
    // testPlaceHandlerCanBeCreated
    // checks that the PlaceHandler constructor works correctly
    // verifies the object can be created without crashing

    @Test
    void testPlaceHandlerCanBeCreated() throws Exception {
        TileMap map = new TileMap();
        PlaceHandler handler = new PlaceHandler(map);

        assertNotNull(handler);
    }
}