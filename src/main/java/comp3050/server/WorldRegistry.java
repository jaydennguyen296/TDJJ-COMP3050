package comp3050.server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Live per-player state registry (username -> PlayerState), distinct from the
// credential PlayerRegistry. Holds avatar-digit assignment and an atomic
// tryMove so only one avatar can hold a cell at a time.
public class WorldRegistry {
    private static final WorldRegistry INSTANCE = new WorldRegistry();
    private static final int SPAWN_Y = 5;
    private static final int SPAWN_X = 5;

    private final Map<String, PlayerState> players = new ConcurrentHashMap<>();
    private final Object moveLock = new Object();

    private WorldRegistry() {
    }

    public static WorldRegistry getInstance() {
        return INSTANCE;
    }

    // First login: spawn + assign avatar. Returning login: restore the existing
    // record — position and inventory survive logout by design (see LogoutHandler).
    public PlayerState getOrCreate(String username) {
        return players.computeIfAbsent(username,
            u -> new PlayerState(u, assignAvatar(u), SPAWN_Y, SPAWN_X));
    }

    public PlayerState get(String username) {
        if (username == null) {
            return null;
        }
        return players.get(username);
    }

    // The player currently standing at (y,x), or null. Used for mutual blocking
    // and for overlaying avatar digits onto INFO cells.
    public PlayerState occupantAt(int y, int x) {
        for (PlayerState p : players.values()) {
            if (p.getY() == y && p.getX() == x) {
                return p;
            }
        }
        return null;
    }

    // Atomically move 'player' to (newY,newX) only if no OTHER player occupies it.
    public boolean tryMove(PlayerState player, int newY, int newX) {
        synchronized (moveLock) {
            PlayerState occupant = occupantAt(newY, newX);
            if (occupant != null && occupant != player) {
                return false;
            }
            player.setPosition(newY, newX);
            return true;
        }
    }

    // The credential record's avatar digit (players.txt third field) wins;
    // otherwise hash the name into 0-9 and probe for an unused digit.
    private char assignAvatar(String username) {
        Player record = PlayerRegistry.getInstance().find(username);
        if (record != null && record.getAvatar() != null && !record.getAvatar().isEmpty()) {
            char first = record.getAvatar().charAt(0);
            if (first >= '0' && first <= '9') {
                return first;
            }
        }
        int start = Math.floorMod(username.hashCode(), 10);
        for (int i = 0; i < 10; i++) {
            char candidate = (char) ('0' + ((start + i) % 10));
            if (!digitInUse(candidate)) {
                return candidate;
            }
        }
        return (char) ('0' + start); // all 10 taken: allow reuse
    }

    private boolean digitInUse(char digit) {
        for (PlayerState p : players.values()) {
            if (p.getAvatar() == digit) {
                return true;
            }
        }
        return false;
    }
}