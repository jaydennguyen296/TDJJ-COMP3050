package comp3050;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TileMap {
    private final char[][] tiles;
    private final int height;
    private final int width;

    // Dynamic overlay layer, sparse: key = y*width + x, value = the non-ground
    // kinds stacked on that cell in draw order (door state, then items, then a
    // single trailing player avatar digit). Ground stays in `tiles` and is always
    // emitted first; this map is empty for the vast majority of cells.
    private final Map<Long, String> overlays = new HashMap<>();

    public TileMap() throws IOException {
        List<String> rows = readMapLines()
            .stream()
            .filter(line -> !line.isBlank())
            .toList();

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Map cannot be empty.");
        }

        // Each map.txt row is a list of whitespace-separated cells, matching the
        // client's layered map strings: the cell's first char is the ground kind,
        // any further chars (door state, items, bridge) start that cell's overlay.
        // e.g. "wd" = open door on woodenboards, "Wb" = bridge over water.
        List<String[]> grid = rows.stream()
            .map(r -> r.trim().split("\\s+"))
            .toList();

        this.height = grid.size();
        this.width = grid.get(0).length;

        if (width == 0) {
            throw new IllegalArgumentException("Map rows cannot be empty.");
        }

        this.tiles = new char[height][width];

        for (int y = 0; y < height; y++) {
            String[] row = grid.get(y);

            if (row.length != width) {
                throw new IllegalArgumentException(
                    "Map must be rectangular. Row " + y +
                    " has " + row.length + " cells but expected " + width + "."
                );
            }

            for (int x = 0; x < width; x++) {
                String cell = row[x];
                tiles[y][x] = cell.charAt(0);
                if (cell.length() > 1) {
                    overlays.put(key(y, x), cell.substring(1));
                }
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

    // Blocking follows the client's rule exactly (script.js last_blocking): the
    // LAST kind in the cell's layered string decides. So a bridge over water
    // ("Wb") and an open door ("wd") are walkable, while walls/water/closed
    // doors ("fD") and player avatar digits block.
    public boolean isBlocking(int y, int x) {
        if (!isInBounds(y, x)) {
            return true;
        }
        String location = getLocationString(y, x);
        char last = location.charAt(location.length() - 1);
        return last == 'B' || last == 'D' || last == 'S' || last == 'W'
            || (last >= '0' && last <= '9');
    }

    // Sparse overlay key. Long avoids overflow on large maps.
    private long key(int y, int x) {
        return (long) y * width + x;
    }

    // The full layered location string: ground char FIRST, then any overlay
    // (door state, items) in draw order, with the player avatar digit (if any)
    // LAST. Out of bounds yields a single blank, matching getTileOrBlank.
    public String getLocationString(int y, int x) {
        if (!isInBounds(y, x)) {
            return " ";
        }
        String overlay = overlays.get(key(y, x));
        if (overlay == null) {
            return String.valueOf(tiles[y][x]);
        }
        return tiles[y][x] + overlay;
    }

    // ----- Door support (USE). -----

    // A usable door is either closed 'D' or open 'd' in the overlay.
    public boolean isUsableDoor(int y, int x) {
        if (!isInBounds(y, x)) {
            return false;
        }
        String overlay = overlays.get(key(y, x));
        if (overlay == null) {
            return false;
        }
        return overlay.indexOf('D') >= 0 || overlay.indexOf('d') >= 0;
    }

    // Toggle a door at (y,x): closed 'D' <-> open 'd'. Returns the new door state
    // char, or 0 if there was no door overlay here.
    public char toggleDoor(int y, int x) {
        if (!isInBounds(y, x)) {
            return 0;
        }
        long k = key(y, x);
        String overlay = overlays.get(k);
        if (overlay == null) {
            return 0;
        }
        int di = overlay.indexOf('D');
        if (di >= 0) {
            setOverlayString(k, overlay.substring(0, di) + 'd' + overlay.substring(di + 1));
            return 'd';
        }
        int oi = overlay.indexOf('d');
        if (oi >= 0) {
            setOverlayString(k, overlay.substring(0, oi) + 'D' + overlay.substring(oi + 1));
            return 'D';
        }
        return 0;
    }

    // ----- Item support (TAKE/PLACE). 0 ('\0') means 'no item'. -----

    // The item kind currently lying on the ground at (y,x), or 0 if none.
    // Items are the letters a,c,h,k per the spec item table.
    public char getItem(int y, int x) {
        if (!isInBounds(y, x)) {
            return 0;
        }
        String overlay = overlays.get(key(y, x));
        if (overlay == null) {
            return 0;
        }
        for (int i = 0; i < overlay.length(); i++) {
            char c = overlay.charAt(i);
            if (c == 'a' || c == 'c' || c == 'h' || c == 'k') {
                return c;
            }
        }
        return 0;
    }

    // Remove any item kind from (y,x).
    public void clearItem(int y, int x) {
        char item = getItem(y, x);
        if (item != 0) {
            removeItem(y, x, item);
        }
    }

    // Remove a single item kind from (y,x). Returns true if it was present.
    public boolean removeItem(int y, int x, char item) {
        if (!isInBounds(y, x)) {
            return false;
        }
        long k = key(y, x);
        String overlay = overlays.get(k);
        if (overlay == null) {
            return false;
        }
        int idx = overlay.indexOf(item);
        if (idx < 0) {
            return false;
        }
        setOverlayString(k, overlay.substring(0, idx) + overlay.substring(idx + 1));
        return true;
    }

    // Place an item kind on (y,x), inserted before any trailing player digit so
    // the avatar stays LAST.
    public void setItem(int y, int x, char item) {
        if (!isInBounds(y, x)) {
            return;
        }
        long k = key(y, x);
        String overlay = overlays.get(k);
        if (overlay == null || overlay.isEmpty()) {
            setOverlayString(k, String.valueOf(item));
            return;
        }
        char last = overlay.charAt(overlay.length() - 1);
        if (last >= '0' && last <= '9') {
            setOverlayString(k, overlay.substring(0, overlay.length() - 1) + item + last);
        } else {
            setOverlayString(k, overlay + item);
        }
    }

    // ----- Player avatar support (multiplayer stage). -----

    // The avatar digit currently at (y,x), or 0 if none.
    public char occupantDigit(int y, int x) {
        if (!isInBounds(y, x)) {
            return 0;
        }
        String overlay = overlays.get(key(y, x));
        if (overlay == null || overlay.isEmpty()) {
            return 0;
        }
        char last = overlay.charAt(overlay.length() - 1);
        return (last >= '0' && last <= '9') ? last : 0;
    }

    // Place the player's avatar digit (0-9) at (y,x), always LAST in the string.
    public void setPlayer(int y, int x, char digit) {
        if (!isInBounds(y, x)) {
            return;
        }
        long k = key(y, x);
        String overlay = overlays.get(k);
        if (overlay == null) {
            setOverlayString(k, String.valueOf(digit));
            return;
        }
        if (!overlay.isEmpty()) {
            char last = overlay.charAt(overlay.length() - 1);
            if (last >= '0' && last <= '9') {
                overlay = overlay.substring(0, overlay.length() - 1);
            }
        }
        setOverlayString(k, overlay + digit);
    }

    // Remove any trailing player avatar digit from (y,x).
    public void clearPlayer(int y, int x) {
        if (!isInBounds(y, x)) {
            return;
        }
        long k = key(y, x);
        String overlay = overlays.get(k);
        if (overlay == null || overlay.isEmpty()) {
            return;
        }
        char last = overlay.charAt(overlay.length() - 1);
        if (last >= '0' && last <= '9') {
            setOverlayString(k, overlay.substring(0, overlay.length() - 1));
        }
    }

    // Store an overlay string, dropping the key entirely when it becomes empty
    // so the map stays sparse and getLocationString stays a single char.
    private void setOverlayString(long k, String value) {
        if (value == null || value.isEmpty()) {
            overlays.remove(k);
        } else {
            overlays.put(k, value);
        }
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