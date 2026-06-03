package comp3050.server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    @Test
    void testCreateSessionReturnsToken() {
        String token = SessionManager.getInstance().createSession("JUnitUser");

        assertNotNull(token);
        assertFalse(token.isBlank());
    }

    @Test
    void testValidSessionReturnsUser() {
        String token = SessionManager.getInstance().createSession("JUnitUser2");

        assertEquals("JUnitUser2", SessionManager.getInstance().getUser(token));
    }

    @Test
    void testInvalidSessionReturnsNull() {
        assertNull(SessionManager.getInstance().getUser("fake-token"));
    }

    @Test
    void testLogoutInvalidatesSession() {
        String token = SessionManager.getInstance().createSession("JUnitUser3");

        assertTrue(SessionManager.getInstance().invalidate(token));
        assertNull(SessionManager.getInstance().getUser(token));
    }

    @Test
    void testInvalidatingBadTokenReturnsFalse() {
        assertFalse(SessionManager.getInstance().invalidate("bad-token"));
    }
}