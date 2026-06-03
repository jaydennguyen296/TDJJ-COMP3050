package comp3050.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerRegistryTest {

    @Test
    void testFindNullReturnsNull() {
        // V3: null username should not find a player
        assertNull(PlayerRegistry.getInstance().find(null));
    }

    @Test
    void testVerifyNullPlayerReturnsFalse() {
        // V3: cannot verify credentials without a player record
        assertFalse(PlayerRegistry.getInstance().verify(null, "password"));
    }

    @Test
    void testVerifyNullPasswordReturnsFalse() {
        // V3: cannot verify when password/hash is missing
        Player player = new Player("Alice", "hashed-password", "3");

        assertFalse(PlayerRegistry.getInstance().verify(player, null));
    }

    @Test
    void testVerifyWrongPasswordReturnsFalse() {
        // V3: wrong encrypted password should fail
        Player player = new Player("Alice", "hashed-password", "3");

        assertFalse(PlayerRegistry.getInstance().verify(player, "wrong-password"));
    }
}