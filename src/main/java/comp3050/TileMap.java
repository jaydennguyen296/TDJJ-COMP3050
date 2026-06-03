package comp3050;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TileMap {
    private final char[][] tiles;
    private final int height;
    private final int width;

    public TileMap() throws IOException {
        List<String> rows = readMapLines()
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

    //Same horizontal wrap as the client 
    public int wrapX(int x) {
        int w = width;
        if (w <= 0) {
            return x;
        }
        int m = x % w;
        return m < 0 ? m + w : m;
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

    // map.txt is a Maven resource (src/main/resources/map.txt), loaded from the classpath
    private static List<String> readMapLines() throws IOException {
        try (InputStream in = TileMap.class.getResourceAsStream("/map.txt")) {
            if (in == null) {
                throw new IOException("Could not find map.txt on the classpath (expected src/main/resources/map.txt).");
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                List<String> lines = new ArrayList<>();
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                }
                return lines;
            }
        }
    }
}