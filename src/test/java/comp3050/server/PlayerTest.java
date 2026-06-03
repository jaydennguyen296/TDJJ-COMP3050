package comp3050.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void testPlayerStoresNamePasswordAndAvatar() {
        // V3: Player stores identity information loaded from players.txt
        Player player = new Player("Alice", "hashed-password", "3");

        assertEquals("Alice", player.getName());
        assertEquals("hashed-password", player.getEncpswrd());
        assertEquals("3", player.getAvatar());
    }
}