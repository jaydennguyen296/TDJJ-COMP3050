import java.util.Map;
import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final SessionManager INSTANCE = new SessionManager();
    private static final String TOKEN_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final int TOKEN_LENGTH = 32;
    private final SecureRandom random = new SecureRandom();
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

        String token = generateToken();
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

    private String generateToken() {
        String token;
        do {
            StringBuilder sb = new StringBuilder(TOKEN_LENGTH);
            for (int i = 0; i < TOKEN_LENGTH; i++) {
                int idx = random.nextInt(TOKEN_CHARS.length());
                sb.append(TOKEN_CHARS.charAt(idx));
            }
            token = sb.toString();
        } while (sessions.containsKey(token));
        return token;
    }
}