package comp3050.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerStateTest {

    @Test
    void testPlayerStateInitialValues() {
        // V3: PlayerState stores live player position, avatar, and inventory
        PlayerState state = new PlayerState("Alice", '3', 5, 5);

        assertEquals("Alice", state.getUsername());
        assertEquals('3', state.getAvatar());
        assertEquals(5, state.getY());
        assertEquals(5, state.getX());
        assertNotNull(state.getInventory());
        assertTrue(state.getInventory().isEmpty());
    }

    @Test
    void testSetPlayerPosition() {
        // V3: Player position can update when the player moves
        PlayerState state = new PlayerState("Alice", '3', 5, 5);

        state.setPosition(6, 7);

        assertEquals(6, state.getY());
        assertEquals(7, state.getX());
    }

    @Test
    void testInventoryCanStoreItem() {
        // V3: inventory can hold item characters such as key/item symbols
        PlayerState state = new PlayerState("Alice", '3', 5, 5);

        state.getInventory().add('k');

        assertEquals(1, state.getInventory().size());
        assertTrue(state.getInventory().contains('k'));
    }
}