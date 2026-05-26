package client.entity;

import client.game.AssetLoader;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

public class Tank {

    public static final int WIDTH = 36;
    public static final int HEIGHT = 40;
    public static final double ROTATION_SPEED = 0.05;
    public static final double MOVE_SPEED = 2.5;
    public static final int MAX_HEALTH = 100;
    public static final long SHOOT_COOLDOWN_MS = 400;

    private double x, y; // center position in pixels
    private double angle; // radians, 0 = up
    private int health;
    private final Team team;
    private final String playerId;
    private long lastShotTime;
    private boolean alive = true;
    private int score;

    private double speedMultiplier = 1.0;
    private boolean immune = false;
    private long shootCooldownMs = SHOOT_COOLDOWN_MS;

    public Tank(String playerId, Team team, double x, double y) {
        this.playerId = playerId;
        this.team = team;
        this.x = x;
        this.y = y;
        this.angle = (team == Team.RED) ? Math.PI : 0;
        this.health = MAX_HEALTH;
    }

    public void moveForward() {
        x += Math.sin(angle) * MOVE_SPEED * speedMultiplier;
        y -= Math.cos(angle) * MOVE_SPEED * speedMultiplier;
    }

    public void moveBackward() {
        x -= Math.sin(angle) * MOVE_SPEED * 0.6 * speedMultiplier;
        y += Math.cos(angle) * MOVE_SPEED * 0.6 * speedMultiplier;
    }

    public void rotateLeft() { angle -= ROTATION_SPEED; }
    public void rotateRight() { angle += ROTATION_SPEED; }

    public Bullet shoot() {
        long now = System.currentTimeMillis();
        if (!alive || now - lastShotTime < shootCooldownMs) return null;
        lastShotTime = now;
        double tipX = x + Math.sin(angle) * (HEIGHT / 2.0 + 22);
        double tipY = y - Math.cos(angle) * (HEIGHT / 2.0 + 22);
        return new Bullet(tipX, tipY, angle, playerId, team);
    }

    public void takeDamage(int damage) {
        if (!alive || immune) return;
        health -= damage;
        if (health <= 0) { health = 0; alive = false; }
    }

    public void addHealth(int amount) {
        if (!alive) return;
        health = Math.min(MAX_HEALTH, health + amount);
    }

    public void respawn(double spawnX, double spawnY) {
        x = spawnX;
        y = spawnY;
        health = MAX_HEALTH;
        alive = true;
        angle = (team == Team.RED) ? Math.PI : 0;
    }

    public void draw(Graphics2D g2) {
        if (!alive) return;
        AssetLoader assets = AssetLoader.get();
        BufferedImage bodyImg = assets.getTankImage(team);
        BufferedImage barrelImg = assets.getBarrelImage();

        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angle);

        // Barrel (extends upward from center)
        g2.drawImage(barrelImg,
                -barrelImg.getWidth() / 2,
                -HEIGHT / 2 - barrelImg.getHeight(),
                barrelImg.getWidth(), barrelImg.getHeight(), null);

        // Tank body sprite, centered
        g2.drawImage(bodyImg,
                -bodyImg.getWidth() / 2,
                -bodyImg.getHeight() / 2,
                bodyImg.getWidth(), bodyImg.getHeight(), null);

        g2.setTransform(old);

        // Shield glow when immune
        if (immune) {
            long t = System.currentTimeMillis();
            int alpha = (int) (100 + 80 * Math.sin(t * 0.008));
            g2.setColor(new Color(100, 180, 255, alpha));
            int r = WIDTH / 2 + 8;
            g2.fillOval((int) (x - r), (int) (y - r), r * 2, r * 2);
        }

        // Health bar above tank
        int barW = 40, barH = 5;
        double barX = x - barW / 2.0;
        double barY = y - HEIGHT / 2.0 - 14;
        g2.setColor(Color.DARK_GRAY);
        g2.fillRect((int) barX, (int) barY, barW, barH);
        double ratio = (double) health / MAX_HEALTH;
        g2.setColor(ratio > 0.5 ? Color.GREEN : ratio > 0.25 ? Color.YELLOW : Color.RED);
        g2.fillRect((int) barX, (int) barY, (int) (barW * ratio), barH);
    }

    public void drawLocalMarker(Graphics2D g2) {
        if (!alive) return;
        int cx = (int) x;
        int tipY = (int) (y - HEIGHT / 2.0 - 22);
        int[] xs = { cx - 7, cx + 7, cx };
        int[] ys = { tipY - 10, tipY - 10, tipY };
        g2.setColor(Color.WHITE);
        g2.fillPolygon(xs, ys, 3);
    }

    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(x - WIDTH / 2.0, y - HEIGHT / 2.0, WIDTH, HEIGHT);
    }

    // Getters & setters
    public double getX() { return x; }
    public double getY() { return y; }
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public double getAngle() { return angle; }
    public void setAngle(double angle) { this.angle = angle; }
    public int getHealth() { return health; }
    public void setHealth(int health) { this.health = health; }
    public Team getTeam() { return team; }
    public String getPlayerId() { return playerId; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
    public int getScore() { return score; }
    public void addScore(int points) { this.score += points; }
    public void setSpeedMultiplier(double m) { this.speedMultiplier = m; }
    public void setImmune(boolean immune) { this.immune = immune; }
    public void setShootCooldownMs(long ms) { this.shootCooldownMs = ms; }
    public boolean isImmune() { return immune; }
}

