package comp3050.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WorldRegistryTest {

    @Test
    void testGetOrCreateCreatesPlayerState() {
        // V3: WorldRegistry creates live state for each logged-in player
        PlayerState player = WorldRegistry.getInstance().getOrCreate("JUnitWorldUser");

        assertNotNull(player);
        assertEquals("JUnitWorldUser", player.getUsername());
        assertEquals(5, player.getY());
        assertEquals(5, player.getX());
    }

    @Test
    void testGetOrCreateReturnsSamePlayerState() {
        // V3: same username should return same saved state
        PlayerState first = WorldRegistry.getInstance().getOrCreate("JUnitSameUser");
        first.setPosition(6, 7);

        PlayerState second = WorldRegistry.getInstance().getOrCreate("JUnitSameUser");

        assertSame(first, second);
        assertEquals(6, second.getY());
        assertEquals(7, second.getX());
    }

    @Test
    void testOccupantAtFindsPlayer() {
        // V3: registry can find which player is standing on a tile
        PlayerState player = WorldRegistry.getInstance().getOrCreate("JUnitOccupantUser");
        player.setPosition(8, 8);

        assertEquals(player, WorldRegistry.getInstance().occupantAt(8, 8));
    }

    @Test
    void testTryMoveUpdatesPosition() {
        // V3: tryMove updates a player's position if tile is not occupied
        PlayerState player = WorldRegistry.getInstance().getOrCreate("JUnitMoveUser");

        boolean moved = WorldRegistry.getInstance().tryMove(player, 9, 9);

        assertTrue(moved);
        assertEquals(9, player.getY());
        assertEquals(9, player.getX());
    }

    @Test
    void testTryMoveBlockedByOtherPlayer() {
        // V3: two players should not occupy the same tile
        PlayerState first = WorldRegistry.getInstance().getOrCreate("JUnitBlockerOne");
        PlayerState second = WorldRegistry.getInstance().getOrCreate("JUnitBlockerTwo");

        first.setPosition(10, 10);

        boolean moved = WorldRegistry.getInstance().tryMove(second, 10, 10);

        assertFalse(moved);
        assertEquals(first, WorldRegistry.getInstance().occupantAt(10, 10));
    }
}