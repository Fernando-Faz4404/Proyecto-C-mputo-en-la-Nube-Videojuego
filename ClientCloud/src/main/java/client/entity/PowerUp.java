package client.entity;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

public class PowerUp {

    public enum Type {
        SPEED, HEALTH, IMMUNITY, AMMO;

        public Color color() {
            return switch (this) {
                case SPEED -> new Color(255, 215, 0);
                case HEALTH -> new Color(50, 210, 80);
                case IMMUNITY -> new Color(100, 180, 255);
                case AMMO -> new Color(255, 100, 50);
            };
        }

        public String symbol() {
            return switch (this) {
                case SPEED -> "S";
                case HEALTH -> "+";
                case IMMUNITY -> "*";
                case AMMO -> "A";
            };
        }

        public String label() {
            return switch (this) {
                case SPEED -> "SPEED";
                case HEALTH -> "HEALTH";
                case IMMUNITY -> "SHIELD";
                case AMMO -> "RAPID";
            };
        }
    }

    private static final int SIZE = 22;
    private static final long RESPAWN_DELAY_MS = 15_000;
    private static final Font SYMBOL_FONT = new Font("Monospaced", Font.BOLD, 13);

    private final double x, y;
    private final Type type;
    private boolean collected = false;
    private long collectedAt = 0;

    public PowerUp(double x, double y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    public void update() {
        // Single-use: no respawn
    }

    public void draw(Graphics2D g2) {
        int ix = (int) (x - SIZE / 2.0);
        int iy = (int) (y - SIZE / 2.0);
        Color c = type.color();

        if (collected) {
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 30));
            g2.fillRoundRect(ix - 2, iy - 2, SIZE + 4, SIZE + 4, 6, 6);
            return;
        }

        // Pulsing glow ring
        long t = System.currentTimeMillis();
        int alpha = (int) (70 + 55 * Math.sin(t * 0.005));
        g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
        g2.fillOval(ix - 6, iy - 6, SIZE + 12, SIZE + 12);

        // Sprite icon if available, otherwise colored box fallback
        java.awt.image.BufferedImage icon =
                client.game.AssetLoader.get().getPowerUpImage(type.name());
        if (icon != null) {
            g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g2.drawImage(icon, ix, iy, SIZE, SIZE, null);
        } else {
            g2.setColor(c);
            g2.fillRoundRect(ix, iy, SIZE, SIZE, 6, 6);
            g2.setColor(Color.WHITE);
            g2.setFont(SYMBOL_FONT);
            int sw = g2.getFontMetrics().stringWidth(type.symbol());
            g2.drawString(type.symbol(), (int) (x - sw / 2.0), (int) (y + 5));
        }
    }

    public void collect() {
        collected = true;
        collectedAt = System.currentTimeMillis();
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x - SIZE / 2.0, y - SIZE / 2.0, SIZE, SIZE);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public Type getType() { return type; }
    public boolean isCollected() { return collected; }
}
