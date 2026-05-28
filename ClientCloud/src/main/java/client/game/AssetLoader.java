package client.game;

import client.entity.Team;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssetLoader {

    private final Map<Integer, BufferedImage> tileImages = new HashMap<>();
    private final Map<String, BufferedImage> powerUpImages = new HashMap<>();

    private BufferedImage tankRed, tankBlue, tankGreen, tankYellow;
    private BufferedImage barrel;
    private BufferedImage bulletRed, bulletBlue, bulletGreen, bulletYellow;
    private BufferedImage[] explosionFrames;

    private static AssetLoader instance;
    private AssetLoader() {}

    public static AssetLoader get() {
        if (instance == null) instance = new AssetLoader();
        return instance;
    }

    public void load() {
        // ---- Tiles ----
        tileImages.put(0, loadTile("/tiles/grass.png", new Color(76, 128, 56)));
        tileImages.put(1, loadTile("/tiles/wall.png", new Color(100, 85, 70)));
        tileImages.put(2, loadTile("/tiles/water.png", new Color(50, 100, 180)));
        tileImages.put(3, loadTile("/tiles/sand.png", new Color(194, 178, 128)));
        tileImages.put(4, loadTile("/tiles/lava.png", new Color(255, 80, 0)));
        tileImages.put(5, loadTile("/tiles/rocaVolcanica.png",new Color(60, 60, 60)));
        tileImages.put(6, loadTile("/tiles/aguaHielo.png", new Color(180, 240, 255)));
        tileImages.put(7, loadTile("/tiles/muroHielo.png", new Color(180, 220, 255)));
        tileImages.put(8, loadTile("/tiles/sueloHielo.png", new Color(220, 240, 255)));

        // ---- Tanks: load sprite, rotate -90° (facing right → facing up), scale to 48×48 ----
        BufferedImage raw1 = loadRaw("/Sprites/tanque1Derecha.png");
        BufferedImage raw2 = loadRaw("/Sprites/tanque2Derecha.png");

        if (raw1 == null) raw1 = makeSolidPlaceholder(16, 16, new Color(200, 50, 50));
        if (raw2 == null) raw2 = makeSolidPlaceholder(16, 16, new Color(50, 50, 200));

        BufferedImage tank1Up = rotateImage(raw1, -Math.PI / 2);
        BufferedImage tank2Up = rotateImage(raw2, -Math.PI / 2);

        tankRed = scaleNN(tintImage(tank1Up, new Color(220, 55, 55)), 48, 48);
        tankBlue = scaleNN(tintImage(tank2Up, new Color(55, 90, 220)), 48, 48);
        tankGreen = scaleNN(tintImage(tank1Up, new Color(50, 200, 50)), 48, 48);
        tankYellow = scaleNN(tintImage(tank2Up, new Color(230, 200, 0)), 48, 48);

        // ---- Barrel (programmatic — thin rectangle) ----
        barrel = makeSolidPlaceholder(6, 22, new Color(80, 80, 80));

        // ---- Bullets ----
        BufferedImage rawBullet = loadRaw("/Sprites/bala.png");
        if (rawBullet == null) rawBullet = makeSolidPlaceholder(8, 8, new Color(255, 230, 50));
        bulletRed = scaleNN(rawBullet, 12, 12);
        bulletBlue = scaleNN(tintImage(rawBullet, new Color(100, 180, 255)), 12, 12);
        bulletGreen = scaleNN(tintImage(rawBullet, new Color(100, 255, 100)), 12, 12);
        bulletYellow = scaleNN(tintImage(rawBullet, new Color(255, 230, 80)), 12, 12);

        // ---- Power-up sprites ----
        loadPowerUpSprite("HEALTH", "/Sprites/cura.png", new Color(50, 210, 80));
        loadPowerUpSprite("IMMUNITY", "/Sprites/escudo.png", new Color(100, 180, 255));
        loadPowerUpSprite("SPEED", "/Sprites/velocidad.png", new Color(255, 215, 0));
        loadPowerUpSprite("AMMO", "/Sprites/balaEspecial.png", new Color(255, 100, 50));

        // ---- Explosions (programmatic) ----
        Color[] expColors = {
            new Color(255, 255, 200), new Color(255, 200, 50),
            new Color(255, 150, 0), new Color(200, 80, 0), new Color(100, 50, 0)
        };
        explosionFrames = new BufferedImage[5];
        for (int i = 0; i < 5; i++) {
            explosionFrames[i] = makeCirclePlaceholder(48, 48, expColors[i]);
        }

        System.out.println("[AssetLoader] Assets loaded.");
    }

    // ---- Tile helper ----

    private BufferedImage loadTile(String path, Color fallback) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                if (img != null) return scaleNN(img, 48, 48);
            }
        } catch (IOException ignored) {}
        return makeSolidPlaceholder(48, 48, fallback);
    }

    // ---- Map loader ----

    public int[][] loadMap(String resourcePath) {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) { System.err.println("[AssetLoader] Map missing: " + resourcePath); return null; }
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is));
            List<int[]> rows = new ArrayList<>();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] tokens = line.split("\\s+");
                int[] row = new int[tokens.length];
                for (int i = 0; i < tokens.length; i++) row[i] = Integer.parseInt(tokens[i]);
                rows.add(row);
            }
            return rows.toArray(new int[0][]);
        } catch (IOException | NumberFormatException e) {
            System.err.println("[AssetLoader] Map error: " + e.getMessage());
            return null;
        }
    }

    // ---- Image transform helpers ----

    /** Rotate image by given radians around its center. */
    private BufferedImage rotateImage(BufferedImage src, double radians) {
        int w = src.getWidth(), h = src.getHeight();
        double cos = Math.abs(Math.cos(radians)), sin = Math.abs(Math.sin(radians));
        int nw = (int) Math.ceil(cos * w + sin * h);
        int nh = (int) Math.ceil(sin * w + cos * h);
        BufferedImage result = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.translate(nw / 2.0, nh / 2.0);
        g.rotate(radians);
        g.drawImage(src, -w / 2, -h / 2, null);
        g.dispose();
        return result;
    }

    /** Scale using nearest-neighbor (preserves pixel-art look). */
    private BufferedImage scaleNN(BufferedImage src, int tw, int th) {
        BufferedImage result = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(src, 0, 0, tw, th, null);
        g.dispose();
        return result;
    }

    /** Tint by converting to grayscale then applying color. */
    private BufferedImage tintImage(BufferedImage src, Color tint) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int tr = tint.getRed(), tg = tint.getGreen(), tb = tint.getBlue();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                if (a == 0) { result.setRGB(x, y, 0); continue; }
                int r = (argb >> 16) & 0xFF;
                int gv = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                int lum = (int)(0.299 * r + 0.587 * gv + 0.114 * b);
                int nr = clamp(lum * tr / 140);
                int ng = clamp(lum * tg / 140);
                int nb = clamp(lum * tb / 140);
                result.setRGB(x, y, (a << 24) | (nr << 16) | (ng << 8) | nb);
            }
        }
        return result;
    }

    private int clamp(int v) { return Math.min(255, Math.max(0, v)); }

    private void loadPowerUpSprite(String key, String path, Color fallback) {
        BufferedImage raw = loadRaw(path);
        if (raw == null) raw = makeSolidPlaceholder(16, 16, fallback);
        powerUpImages.put(key, scaleNN(raw, 22, 22));
    }

    private BufferedImage loadRaw(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is != null) return ImageIO.read(is);
        } catch (IOException ignored) {}
        return null;
    }

    // ---- Placeholder generators ----

    private BufferedImage makeSolidPlaceholder(int w, int h, Color c) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(0, 0, 0, 60));
        g.drawRect(0, 0, w - 1, h - 1);
        g.dispose();
        return img;
    }

    private BufferedImage makeCirclePlaceholder(int w, int h, Color c) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(c);
        g.fillOval(4, 4, w - 8, h - 8);
        g.dispose();
        return img;
    }

    // ---- Getters ----

    public BufferedImage getTileImage(int id) {
        return tileImages.getOrDefault(id, tileImages.get(0));
    }

    public BufferedImage getTankImage(Team team) {
        return switch (team) {
            case RED -> tankRed;
            case BLUE -> tankBlue;
            case GREEN -> tankGreen;
            case YELLOW -> tankYellow;
        };
    }

    public BufferedImage getBarrelImage() { return barrel; }

    public BufferedImage getBulletImage(Team team) {
        return switch (team) {
            case RED -> bulletRed;
            case BLUE -> bulletBlue;
            case GREEN -> bulletGreen;
            case YELLOW -> bulletYellow;
        };
    }

    public BufferedImage getPowerUpImage(String typeKey) {
        return powerUpImages.get(typeKey);
    }

    public BufferedImage[] getExplosionFrames() { return explosionFrames; }
}
