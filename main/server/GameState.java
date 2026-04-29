package comp3050;

public class GameState {

    private int playerY;
    private int playerX;

    public GameState(int startingY, int startingX) {
        this.playerY = startingY;
        this.playerX = startingX;
    }

    public int getPlayerY() {
        return playerY;
    }

    public int getPlayerX() {
        return playerX;
    }

    public void setPlayerPosition(int y, int x) {
        this.playerY = y;
        this.playerX = x;
    }
}