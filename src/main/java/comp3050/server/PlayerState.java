package comp3050.server;

import java.util.ArrayList;
import java.util.List;

// Live per-player game state (position, inventory, avatar digit). Created on
// first login and retained across logout so a returning player resumes where
// they left off. Credentials live separately in Player/PlayerRegistry.
public class PlayerState {
    private final String username;
    private final char avatar;          // one of '0'..'9'
    private int y;
    private int x;

    // Held item-kind chars (e.g. 'a','c','h','k'), at most MAX_INVENTORY of them.
    private final List<Character> inventory = new ArrayList<>();

    public PlayerState(String username, char avatar, int startingY, int startingX) {
        this.username = username;
        this.avatar = avatar;
        this.y = startingY;
        this.x = startingX;
    }

    public String getUsername() {
        return username;
    }

    public char getAvatar() {
        return avatar;
    }

    public int getY() {
        return y;
    }

    public int getX() {
        return x;
    }

    public void setPosition(int y, int x) {
        this.y = y;
        this.x = x;
    }

    public List<Character> getInventory() {
        return inventory;
    }
}