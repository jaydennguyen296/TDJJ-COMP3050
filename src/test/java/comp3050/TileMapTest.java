package comp3050;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TileMapTest {

    @Test
    void testMapLoadsCorrectly() throws Exception {
        TileMap map = new TileMap();

        assertNotNull(map);
        assertTrue(map.getWidth() > 0);
        assertTrue(map.getHeight() > 0);
    }

    @Test
    void testInBounds() throws Exception {
        TileMap map = new TileMap();

        assertTrue(map.isInBounds(0, 0));
        assertFalse(map.isInBounds(-1, 0));
        assertFalse(map.isInBounds(0, -1));
        assertFalse(map.isInBounds(map.getHeight(), 0));
    }

    @Test
    void testBlockingTiles() throws Exception {
        TileMap map = new TileMap();

        assertTrue(map.isBlocking(-1, 0)); // out of bounds blocks movement
        assertFalse(map.isBlocking(5, 5)); // player start should be walkable
    }

    @Test
    void testWrapX() throws Exception {
        TileMap map = new TileMap();

        assertEquals(0, map.wrapX(0));
        assertEquals(map.getWidth() - 1, map.wrapX(-1));
        assertEquals(0, map.wrapX(map.getWidth()));
    }
}