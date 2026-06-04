package comp3050;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TileMapTest {

    // Checks map loads successfully from map.txt
    @Test
    void testMapLoadsCorrectly() throws Exception {
        TileMap map = new TileMap();

        assertNotNull(map);
        assertTrue(map.getWidth() > 0);
        assertTrue(map.getHeight() > 0);
    }

    // Checks valid and invalid map coordinates
    @Test
    void testInBounds() throws Exception {
        TileMap map = new TileMap();

        assertTrue(map.isInBounds(0, 0));
        assertFalse(map.isInBounds(-1, 0));
        assertFalse(map.isInBounds(0, -1));
        assertFalse(map.isInBounds(map.getHeight(), 0));
    }

    // Checks out-of-bounds locations block movement
    @Test
    void testOutOfBoundsBlocksMovement() throws Exception {
        TileMap map = new TileMap();

        assertTrue(map.isBlocking(-1, 0));
    }

    // Checks horizontal map wrapping works correctly
    @Test
    void testWrapX() throws Exception {
        TileMap map = new TileMap();

        assertEquals(0, map.wrapX(0));
        assertEquals(map.getWidth() - 1, map.wrapX(-1));
        assertEquals(0, map.wrapX(map.getWidth()));
    }

    // Checks item can be added to a map tile
    @Test
    void testSetItemAddsItem() throws Exception {
        TileMap map = new TileMap();

        map.setItem(0, 0, 'k');

        assertEquals('k', map.getItem(0, 0));
    }

    // Checks item is removed from tile correctly
    @Test
    void testClearItemRemovesItem() throws Exception {
        TileMap map = new TileMap();

        map.setItem(0, 0, 'a');
        map.clearItem(0, 0);

        assertEquals(0, map.getItem(0, 0));
    }

    // Checks removing a missing item returns false
    @Test
    void testRemoveItemReturnsFalseWhenMissing() throws Exception {
        TileMap map = new TileMap();

        boolean removed = map.removeItem(0, 0, 'k');

        assertFalse(removed);
    }

    // Checks player avatar digit stays at the end of cell string
    @Test
    void testPlayerDigitStaysLastAfterSetItem() throws Exception {
        TileMap map = new TileMap();

        map.setPlayer(0, 0, '1');
        map.setItem(0, 0, 'k');

        assertEquals("gk1", map.getLocationString(0, 0));
    }

    // Checks player avatar can be removed from map
    @Test
    void testClearPlayerRemovesPlayerDigit() throws Exception {
        TileMap map = new TileMap();

        map.setPlayer(0, 0, '1');
        map.clearPlayer(0, 0);

        assertEquals(0, map.occupantDigit(0, 0));
    }

    // Checks door state changes from closed to open
    // Checks door state changes from closed to open, then open to closed
    @Test
    void testToggleDoorChangesDoorState() throws Exception {
        TileMap map = new TileMap();

        int doorY = -1;
        int doorX = -1;

        for (int y = 0; y < map.getHeight(); y++) {
            for (int x = 0; x < map.getWidth(); x++) {
                if (map.isUsableDoor(y, x)) {
                    doorY = y;
                    doorX = x;
                    break;
                }
            }
            if (doorY != -1) {
                break;
            }
        }

        assertTrue(doorY != -1, "Map should contain at least one usable door");

        char firstState = map.toggleDoor(doorY, doorX);
        assertTrue(firstState == 'd' || firstState == 'D');

        char secondState = map.toggleDoor(doorY, doorX);
        assertTrue(secondState == 'd' || secondState == 'D');
        assertNotEquals(firstState, secondState);
    }
}