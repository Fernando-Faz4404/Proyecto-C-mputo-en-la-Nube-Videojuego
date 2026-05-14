package client.game;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized resource loader. Loads all sprites, tile images, and map files
 * from the classpath (src/main/resources/).
 *
 * <p>If a PNG is missing on disk, a colored placeholder is generated so the
 * game remains runnable without art assets.</p>
 */
public class AssetLoader {

    private final Map<Integer, BufferedImage> tileImages = new HashMap<>();

    private BufferedImage tankRed;
    private BufferedImage tankBlue;
    private BufferedImage barrel;
    private BufferedImage bulletRed;
    private BufferedImage bulletBlue;
    private BufferedImage[] explosionFrames;

    private static AssetLoader instance;

    private AssetLoader() {}

    public static AssetLoader get() {
        if (instance == null) {
            instance = new AssetLoader();
        }
        return instance;
    }

    /** Loads all assets. Call once at startup. */
    public void load() {
        // Tiles (0=grass, 1=wall, 2=water, 3=sand)
        tileImages.put(0, loadImage("/tiles/grass.png", 48, 48, new Color(76, 128, 56)));
        tileImages.put(1, loadImage("/tiles/wall.png", 48, 48, new Color(100, 85, 70)));
        tileImages.put(2, loadImage("/tiles/water.png", 48, 48, new Color(50, 100, 180)));
        tileImages.put(3, loadImage("/tiles/sand.png", 48, 48, new Color(194, 178, 128)));

        // Tank sprites
        tankRed  = loadImage("/sprites/tank_red.png",  36, 40, new Color(200, 50, 50));
        tankBlue = loadImage("/sprites/tank_blue.png", 36, 40, new Color(50, 50, 200));
        barrel   = loadImage("/sprites/barrel.png",     6, 22, new Color(80, 80, 80));

        // Bullet sprites
        bulletRed  = loadImage("/sprites/bullet_red.png",  8, 8, new Color(255, 180, 50));
        bulletBlue = loadImage("/sprites/bullet_blue.png", 8, 8, new Color(50, 220, 255));

        // Explosion animation frames
        explosionFrames = new BufferedImage[5];
        Color[] expColors = {
            new Color(255, 255, 200), new Color(255, 200, 50),
            new Color(255, 150, 0),   new Color(200, 80, 0),
            new Color(100, 50, 0)
        };
        for (int i = 0; i < 5; i++) {
            explosionFrames[i] = loadImage("/sprites/explosion/exp_" + i + ".png", 48, 48, expColors[i]);
        }

        System.out.println("[AssetLoader] All assets loaded.");
    }

    /**
     * Loads a map .txt file from the classpath. Each line has space-separated tile IDs.
     * @param resourcePath e.g. "/maps/map01.txt"
     * @return 2D int array [row][col], or null if not found
     */
    public int[][] loadMap(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("[AssetLoader] Map not found: " + resourcePath);
                return null;
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            List<int[]> rows = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] tokens = line.split("\\s+");
                int[] row = new int[tokens.length];
                for (int i = 0; i < tokens.length; i++) {
                    row[i] = Integer.parseInt(tokens[i]);
                }
                rows.add(row);
            }
            return rows.toArray(new int[0][]);
        } catch (IOException | NumberFormatException e) {
            System.err.println("[AssetLoader] Error loading map " + resourcePath + ": " + e.getMessage());
            return null;
        }
    }

    private BufferedImage loadImage(String resourcePath, int pw, int ph, Color placeholderColor) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                if (img != null) return img;
            }
        } catch (IOException ignored) {}

        System.out.println("[AssetLoader] Missing " + resourcePath + " — using placeholder");
        return createPlaceholder(pw, ph, placeholderColor);
    }

    private BufferedImage createPlaceholder(int w, int h, Color color) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(0, 0, 0, 60));
        g.drawRect(0, 0, w - 1, h - 1);
        g.dispose();
        return img;
    }

    // ---- Getters ----

    public BufferedImage getTileImage(int tileId) {
        return tileImages.getOrDefault(tileId, tileImages.get(0));
    }

    public BufferedImage getTankImage(client.entity.Team team) {
        return team == client.entity.Team.RED ? tankRed : tankBlue;
    }

    public BufferedImage getBarrelImage() { return barrel; }

    public BufferedImage getBulletImage(client.entity.Team team) {
        return team == client.entity.Team.RED ? bulletRed : bulletBlue;
    }

    public BufferedImage[] getExplosionFrames() { return explosionFrames; }
}

