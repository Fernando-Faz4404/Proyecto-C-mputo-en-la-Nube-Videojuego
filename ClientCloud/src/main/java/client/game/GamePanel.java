package client.game;

import client.entity.Bullet;
import client.entity.Explosion;
import client.entity.Tank;
import client.entity.Team;
import client.net.NetworkClient;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GamePanel extends JPanel implements Runnable {

    private static final int FPS = 60;

    private final KeyHandler keyHandler = new KeyHandler();
    private final GameMap gameMap = new GameMap();
    private final HUD hud = new HUD();

    private final Tank localTank;
    private final Map<String, Tank> remoteTanks = new ConcurrentHashMap<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Explosion> explosions = new ArrayList<>();

    private int redScore;
    private int blueScore;
    private Thread gameThread;
    private NetworkClient networkClient;

    public GamePanel(String playerName, Team team, String mapResource) {
        // Load all assets first, then load the map
        AssetLoader.get().load();
        gameMap.load(mapResource);

        setPreferredSize(new Dimension(gameMap.getWidthPixels(), gameMap.getHeightPixels()));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        addKeyListener(keyHandler);
        setFocusable(true);

        // Spawn positions: RED on left, BLUE on right
        double spawnX = (team == Team.RED)
                ? 3 * GameMap.TILE_SIZE
                : (gameMap.getCols() - 4) * GameMap.TILE_SIZE;
        double spawnY = gameMap.getHeightPixels() / 2.0;
        localTank = new Tank(playerName, team, spawnX, spawnY);
    }

    public void connectToServer(String ip, int port) {
        networkClient = new NetworkClient(ip, port, this);
        networkClient.connect();
    }

    public void startGame() {
        gameThread = new Thread(this, "GameLoop");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    @Override
    public void run() {
        double drawInterval = 1_000_000_000.0 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();

        while (gameThread != null) {
            long now = System.nanoTime();
            delta += (now - lastTime) / drawInterval;
            lastTime = now;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
            }
            try { Thread.sleep(1); } catch (InterruptedException ignored) {}
        }
    }

    private void update() {
        // Respawn check
        if (!localTank.isAlive() && keyHandler.respawn) {
            double spawnX = (localTank.getTeam() == Team.RED)
                    ? 3 * GameMap.TILE_SIZE
                    : (gameMap.getCols() - 4) * GameMap.TILE_SIZE;
            localTank.respawn(spawnX, gameMap.getHeightPixels() / 2.0);
            if (networkClient != null) networkClient.sendMove(localTank);
        }

        // Movement & shooting
        if (localTank.isAlive()) {
            double oldX = localTank.getX(), oldY = localTank.getY();

            if (keyHandler.up)    localTank.moveForward();
            if (keyHandler.down)  localTank.moveBackward();
            if (keyHandler.left)  localTank.rotateLeft();
            if (keyHandler.right) localTank.rotateRight();

            // Revert if hitting a wall
            if (collidesWithWalls(localTank)) {
                localTank.setX(oldX);
                localTank.setY(oldY);
            }

            // Shoot
            if (keyHandler.shoot) {
                Bullet b = localTank.shoot();
                if (b != null) {
                    synchronized (bullets) { bullets.add(b); }
                    if (networkClient != null) networkClient.sendShoot(b);
                }
            }

            // Send position to server
            if (networkClient != null
                    && (keyHandler.up || keyHandler.down || keyHandler.left || keyHandler.right)) {
                networkClient.sendMove(localTank);
            }
        }

        // Update bullets
        synchronized (bullets) {
            Iterator<Bullet> it = bullets.iterator();
            while (it.hasNext()) {
                Bullet b = it.next();
                b.update();
                if (!b.isActive()) { it.remove(); continue; }

                // Wall collision
                if (gameMap.isSolid(b.getX(), b.getY())) {
                    explosions.add(new Explosion(b.getX(), b.getY()));
                    it.remove();
                    continue;
                }

                // Hit local tank (enemy bullet only)
                if (b.getOwnerTeam() != localTank.getTeam() && localTank.isAlive()
                        && localTank.getBounds().intersects(b.getBounds().getBounds2D())) {
                    localTank.takeDamage(Bullet.DAMAGE);
                    explosions.add(new Explosion(b.getX(), b.getY()));
                    if (!localTank.isAlive()) {
                        explosions.add(new Explosion(localTank.getX(), localTank.getY()));
                        if (networkClient != null) networkClient.sendDeath(localTank);
                    }
                    it.remove();
                    continue;
                }

                // Hit remote tanks
                boolean hit = false;
                for (Tank remote : remoteTanks.values()) {
                    if (!remote.isAlive() || b.getOwnerTeam() == remote.getTeam()) continue;
                    if (remote.getBounds().intersects(b.getBounds().getBounds2D())) {
                        remote.takeDamage(Bullet.DAMAGE);
                        explosions.add(new Explosion(b.getX(), b.getY()));
                        if (!remote.isAlive()) {
                            explosions.add(new Explosion(remote.getX(), remote.getY()));
                            if (b.getOwnerId().equals(localTank.getPlayerId())) {
                                localTank.addScore(1);
                                if (localTank.getTeam() == Team.RED) redScore++;
                                else blueScore++;
                            }
                        }
                        b.setActive(false);
                        hit = true;
                        break;
                    }
                }
                if (hit) it.remove();
            }
        }

        // Update explosions
        explosions.removeIf(e -> { e.update(); return e.isFinished(); });
    }

    private boolean collidesWithWalls(Tank tank) {
        Rectangle2D bounds = tank.getBounds();
        for (Rectangle2D wall : gameMap.getWallsNear(tank.getX(), tank.getY(), GameMap.TILE_SIZE * 2)) {
            if (bounds.intersects(wall)) return true;
        }
        return false;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        gameMap.draw(g2);

        for (Tank remote : remoteTanks.values()) remote.draw(g2);
        localTank.draw(g2);

        synchronized (bullets) {
            for (Bullet b : bullets) b.draw(g2);
        }
        for (Explosion e : explosions) e.draw(g2);

        int mw = gameMap.getWidthPixels(), mh = gameMap.getHeightPixels();
        hud.draw(g2, localTank, remoteTanks, redScore, blueScore, mw, mh);
        if (!localTank.isAlive()) hud.drawDeathScreen(g2, mw, mh);

        g2.dispose();
    }

    // --- Called from network receive thread ---

    public void onRemoteTankUpdate(String id, Team team, double x, double y,
                                   double angle, int health, boolean alive) {
        Tank t = remoteTanks.computeIfAbsent(id, k -> new Tank(id, team, x, y));
        t.setX(x);
        t.setY(y);
        t.setAngle(angle);
        t.setHealth(health);
        t.setAlive(alive);
    }

    public void onRemoteBullet(double x, double y, double angle, String ownerId, Team team) {
        synchronized (bullets) { bullets.add(new Bullet(x, y, angle, ownerId, team)); }
    }

    public void onRemoteDisconnect(String id) { remoteTanks.remove(id); }

    public void onScoreUpdate(int red, int blue) { redScore = red; blueScore = blue; }

    public Tank getLocalTank() { return localTank; }
}

