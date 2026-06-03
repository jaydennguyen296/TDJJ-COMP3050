package comp3050.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userToToken = new ConcurrentHashMap<>();
    // Enriched session: token -> full player record, so handlers can find player state.
    private final Map<String, Player> tokenToPlayer = new ConcurrentHashMap<>();

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public String createSession(String username) {
        return createSession(username, null);
    }

    public String createSession(String username, Player player) {
        String existingToken = userToToken.get(username);
        if (existingToken != null) {
            sessions.remove(existingToken);
            tokenToPlayer.remove(existingToken);
        }

        // Hyphens removed so tokens stay alphanumeric-only per the spec
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, username);
        userToToken.put(username, token);
        if (player != null) {
            tokenToPlayer.put(token, player);
        }
        return token;
    }

    public String getUser(String token) {
        if (token == null) return null;
        return sessions.get(token);
    }

    // Look up the full player record for a token (null if token invalid or no record).
    public Player getPlayer(String token) {
        if (token == null) return null;
        return tokenToPlayer.get(token);
    }

    public boolean invalidate(String token) {
        if (token == null) return false;
        String user = sessions.remove(token);
        if (user == null) {
            return false;
        }
        userToToken.remove(user, token);
        tokenToPlayer.remove(token);
        return true;
    }
}