package comp3050.server;

// Minimal per-player credential record. A later multiplayer stage keeps live
// state (position/inventory) in its own registry; this carries identity only.
public class Player {
    private final String name;
    private final String encpswrd; // lowercase SHA-256 hex of "name;password"
    private final String avatar;

    public Player(String name, String encpswrd, String avatar) {
        this.name = name;
        this.encpswrd = encpswrd;
        this.avatar = avatar;
    }

    public String getName() {
        return name;
    }

    public String getEncpswrd() {
        return encpswrd;
    }

    public String getAvatar() {
        return avatar;
    }
}