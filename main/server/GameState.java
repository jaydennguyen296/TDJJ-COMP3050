package comp3050;

public class GameState {

    private int playerY;
    private int playerX;

    public GameState(int playerY, int playerX) {
        this.playerY = playerY;
        this.playerX = playerX;
    }

    public int getPlayerY() {
        return playerY;
    }

    public int getPlayerX() {
        return playerX;
    }

    public void setPlayerPosition(int playerY, int playerX) {
        this.playerY = playerY;
        this.playerX = playerX;
    }
}