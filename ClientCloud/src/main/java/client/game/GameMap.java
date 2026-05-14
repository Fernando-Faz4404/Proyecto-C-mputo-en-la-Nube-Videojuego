package client.game;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Tile-based game map loaded from a .txt resource file via {@link AssetLoader}.
 * Tile IDs: 0=grass, 1=wall, 2=water, 3=sand.
 * Walls (1) and water (2) are solid.
 */
public class GameMap {

    public static final int TILE_SIZE = 48;

    private int[][] tiles;
    private int cols;
    private int rows;

    private static final int[][] DEFAULT_MAP = {
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
    };

    /** Loads map from resource path. Falls back to default if missing. */
    public void load(String mapResource) {
        int[][] loaded = AssetLoader.get().loadMap(mapResource);
        if (loaded != null && loaded.length > 0) {
            tiles = loaded;
        } else {
            tiles = DEFAULT_MAP;
        }
        rows = tiles.length;
        cols = tiles[0].length;
    }

    public void draw(Graphics2D g2) {
        AssetLoader assets = AssetLoader.get();
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                BufferedImage img = assets.getTileImage(tiles[row][col]);
                g2.drawImage(img, col * TILE_SIZE, row * TILE_SIZE, TILE_SIZE, TILE_SIZE, null);
            }
        }
    }

    public boolean isSolid(double px, double py) {
        int col = (int) (px / TILE_SIZE);
        int row = (int) (py / TILE_SIZE);
        if (col < 0 || col >= cols || row < 0 || row >= rows) return true;
        int tile = tiles[row][col];
        return tile == 1 || tile == 2;
    }

    public List<Rectangle2D> getWallsNear(double cx, double cy, double range) {
        List<Rectangle2D> walls = new ArrayList<>();
        int minCol = Math.max(0, (int) ((cx - range) / TILE_SIZE));
        int maxCol = Math.min(cols - 1, (int) ((cx + range) / TILE_SIZE));
        int minRow = Math.max(0, (int) ((cy - range) / TILE_SIZE));
        int maxRow = Math.min(rows - 1, (int) ((cy + range) / TILE_SIZE));
        for (int r = minRow; r <= maxRow; r++) {
            for (int c = minCol; c <= maxCol; c++) {
                if (tiles[r][c] == 1 || tiles[r][c] == 2) {
                    walls.add(new Rectangle2D.Double(c * TILE_SIZE, r * TILE_SIZE, TILE_SIZE, TILE_SIZE));
                }
            }
        }
        return walls;
    }

    public int getWidthPixels()  { return cols * TILE_SIZE; }
    public int getHeightPixels() { return rows * TILE_SIZE; }
    public int getCols() { return cols; }
    public int getRows() { return rows; }
}

