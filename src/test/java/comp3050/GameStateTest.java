package comp3050;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {

    @Test
    void testInitialPlayerPosition() {
        // Checks that the player starts at the position we give to GameState
        GameState state = new GameState(5, 5);

        assertEquals(5, state.getPlayerY());
        assertEquals(5, state.getPlayerX());
    }

    @Test
    void testSetPlayerPosition() {
        // Checks that GameState updates the player location correctly
        GameState state = new GameState(5, 5);

        state.setPlayerPosition(6, 7);

        assertEquals(6, state.getPlayerY());
        assertEquals(7, state.getPlayerX());
    }
}