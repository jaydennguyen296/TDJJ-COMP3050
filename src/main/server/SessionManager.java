import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public static SessionManager getInstance() {
        return INSTANCE;
    }

    public String createSession(String username) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, username);
        return token;
    }

    public String getUser(String token) {
        if (token == null) return null;
        return sessions.get(token);
    }

    public boolean invalidate(String token) {
        if (token == null) return false;
        return sessions.remove(token) != null;
    }
}