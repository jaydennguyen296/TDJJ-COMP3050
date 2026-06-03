package comp3050.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> userToToken = new ConcurrentHashMap<>();

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public String createSession(String username) {
        String existingToken = userToToken.get(username);
        if (existingToken != null) {
            sessions.remove(existingToken);
        }

        // Hyphens removed so tokens stay alphanumeric-only per the spec
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, username);
        userToToken.put(username, token);
        return token;
    }

    public String getUser(String token) {
        if (token == null) return null;
        return sessions.get(token);
    }

    public boolean invalidate(String token) {
        if (token == null) return false;
        String user = sessions.remove(token);
        if (user == null) {
            return false;
        }
        userToToken.remove(user, token);
        return true;
    }
}
