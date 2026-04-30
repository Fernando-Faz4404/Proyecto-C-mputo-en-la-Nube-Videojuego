package client.entity;

import client.game.AssetLoader;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

public class Bullet {

    public static final double SPEED = 6.0;
    public static final int RADIUS = 4;
    public static final int DAMAGE = 20;
    public static final int MAX_LIFETIME_FRAMES = 180;

    private double x, y;
    private final double angle;
    private final String ownerId;
    private final Team ownerTeam;
    private int lifetime;
    private boolean active = true;

    public Bullet(double x, double y, double angle, String ownerId, Team ownerTeam) {
        this.x = x;
        this.y = y;
        this.angle = angle;
        this.ownerId = ownerId;
        this.ownerTeam = ownerTeam;
    }

    public void update() {
        x += Math.sin(angle) * SPEED;
        y -= Math.cos(angle) * SPEED;
        lifetime++;
        if (lifetime > MAX_LIFETIME_FRAMES) active = false;
    }

    public void draw(Graphics2D g2) {
        if (!active) return;
        BufferedImage img = AssetLoader.get().getBulletImage(ownerTeam);
        AffineTransform old = g2.getTransform();
        g2.translate(x, y);
        g2.rotate(angle);
        g2.drawImage(img, -img.getWidth() / 2, -img.getHeight() / 2, null);
        g2.setTransform(old);
    }

    public Ellipse2D getBounds() {
        return new Ellipse2D.Double(x - RADIUS, y - RADIUS, RADIUS * 2, RADIUS * 2);
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getAngle() { return angle; }
    public String getOwnerId() { return ownerId; }
    public Team getOwnerTeam() { return ownerTeam; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

