package comp3050;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TileMap {

    private static final String MAP_FILE = "src/main/resources/map.txt";

    private final char[][] tiles;
    private final int height;
    private final int width;

    public TileMap() throws IOException {
        List<String> rows = Files.readAllLines(Path.of(MAP_FILE))
            .stream()
            .filter(line -> !line.isBlank())
            .toList();

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Map cannot be empty.");
        }

        this.height = rows.size();
        this.width = rows.get(0).length();

        if (width == 0) {
            throw new IllegalArgumentException("Map rows cannot be empty.");
        }

        this.tiles = new char[height][width];

        for (int y = 0; y < height; y++) {
            String row = rows.get(y);

            if (row.length() != width) {
                throw new IllegalArgumentException(
                    "Map must be rectangular. Row " + y +
                    " has length " + row.length() +
                    " but expected " + width + "."
                );
            }

            for (int x = 0; x < width; x++) {
                tiles[y][x] = row.charAt(x);
            }
        }
    }

    public boolean isInBounds(int y, int x) {
        return y >= 0 && y < height && x >= 0 && x < width;
    }

    public char getTileOrBlank(int y, int x) {
        if (!isInBounds(y, x)) {
            return ' ';
        }

        return tiles[y][x];
    }

    public boolean isBlocking(int y, int x) {
        if (!isInBounds(y, x)) {
            return true;
        }

        char tile = tiles[y][x];

        return tile == 'B'
            || tile == 'D'
            || tile == 'S'
            || tile == 'W';
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }
}