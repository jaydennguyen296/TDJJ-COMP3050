package comp3050.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Multi-user credential store (name -> Player). Loads players.txt at startup,
// hashing plaintext passwords with the spec's SHA-256("name;password") scheme,
// and seeds the APP_USER/APP_PASS environment user as a fallback so v2
// deployments keep working.
public class PlayerRegistry {
    private static final PlayerRegistry INSTANCE = new PlayerRegistry();
    private final Map<String, Player> players = new ConcurrentHashMap<>();

    private PlayerRegistry() {
        seedFromEnv();
        loadPlayersFile();
    }

    public static PlayerRegistry getInstance() {
        return INSTANCE;
    }

    public Player find(String name) {
        if (name == null) {
            return null;
        }
        return players.get(name);
    }

    // Constant-time compare of the stored hash with the client's encpswrd
    // (hex case-insensitive; the server never sees the plaintext).
    public boolean verify(Player player, String candidateEncrypted) {
        if (player == null || candidateEncrypted == null || player.getEncpswrd() == null) {
            return false;
        }
        return MessageDigest.isEqual(
            player.getEncpswrd().getBytes(StandardCharsets.UTF_8),
            candidateEncrypted.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    // The v2 single user (APP_USER/APP_PASS env vars, populated from .env when
    // run via Docker) keeps working as a fallback.
    private void seedFromEnv() {
        String name = System.getenv("APP_USER");
        String password = System.getenv("APP_PASS");
        if (name == null || name.isBlank() || password == null) {
            return;
        }
        players.put(name, new Player(name, sha256Hex(name + ";" + password), "0"));
    }

    // players.txt is a Maven resource (src/main/resources/players.txt), loaded
    // from the classpath like map.txt. Each line:
    //   name;encpswrd;avatar   (a 64-hex middle field is already encrypted)
    //   name;password;avatar   (anything else is hashed as "name;password" on load)
    private void loadPlayersFile() {
        try (InputStream in = PlayerRegistry.class.getResourceAsStream("/players.txt")) {
            if (in == null) {
                return; // optional file; the env seed remains
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String raw;
                while ((raw = reader.readLine()) != null) {
                    String line = raw.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.split(";", -1);
                    if (parts.length < 3) {
                        continue; // malformed line; skip
                    }
                    String name = parts[0].trim();
                    String secret = parts[1].trim();
                    String avatar = parts[2].trim();
                    if (name.isEmpty()) {
                        continue;
                    }
                    String enc = looksHashed(secret)
                        ? secret.toLowerCase()
                        : sha256Hex(name + ";" + secret);
                    players.put(name, new Player(name, enc, avatar));
                }
            }
        } catch (IOException ex) {
            // Leave whatever was seeded from the environment in place.
        }
    }

    private boolean looksHashed(String secret) {
        if (secret == null || secret.length() != 64) {
            return false;
        }
        for (int i = 0; i < secret.length(); i++) {
            char c = secret.charAt(i);
            boolean hex = (c >= '0' && c <= '9')
                || (c >= 'a' && c <= 'f')
                || (c >= 'A' && c <= 'F');
            if (!hex) {
                return false;
            }
        }
        return true;
    }

    private static String sha256Hex(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                .digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}