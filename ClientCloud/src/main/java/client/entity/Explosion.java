package client.entity;

import client.game.AssetLoader;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public class Explosion {

    private final double x, y;
    private int frame;
    private static final int FRAMES_PER_SPRITE = 6;

    public Explosion(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void update() { frame++; }

    public boolean isFinished() {
        return frame >= AssetLoader.get().getExplosionFrames().length * FRAMES_PER_SPRITE;
    }

    public void draw(Graphics2D g2) {
        BufferedImage[] frames = AssetLoader.get().getExplosionFrames();
        int idx = frame / FRAMES_PER_SPRITE;
        if (idx >= frames.length) return;
        BufferedImage img = frames[idx];
        g2.drawImage(img, (int) (x - img.getWidth() / 2.0), (int) (y - img.getHeight() / 2.0), null);
    }
}

