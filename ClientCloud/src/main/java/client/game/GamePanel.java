package client.game;

import client.entity.Bullet;
import client.entity.Explosion;
import client.entity.PowerUp;
import client.entity.Tank;
import client.entity.Team;
import client.net.GameMessage;
import client.net.NetworkClient;

import javax.swing.*;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class GamePanel extends JPanel implements Runnable {

    public static final int VIEW_W = 1248;
    public static final int VIEW_H = 720;

    private static final int FPS           = 60;
    private static final int POWERUP_COUNT = 14;
    private static final int TOTAL_ROUNDS  = 3;

    private static final String[] ROUND_MAPS = {
        "/maps/bigBattleMap.txt",
        "/maps/mapaVolcanico.txt",
        "/maps/mapaHielo.txt"
    };
    private static final String[] ROUND_NAMES = { "Gran Batalla", "Volcánico", "Ártico" };

    private final KeyHandler keyHandler = new KeyHandler();
    private final GameMap    gameMap    = new GameMap();
    private final HUD        hud        = new HUD();

    private final Tank               localTank;
    private final Map<String, Tank>  remoteTanks = new ConcurrentHashMap<>();
    private final List<Bullet>       bullets     = new ArrayList<>();
    private final List<Explosion>    explosions  = new ArrayList<>();
    private final List<PowerUp>      powerUps    = new ArrayList<>();

    // Kill scores (current round)
    private int redScore, blueScore, greenScore, yellowScore;

    // Round wins (cumulative across rounds)
    private int redWins, blueWins, greenWins, yellowWins;

    private int currentRound  = 1;
    private boolean roundOver = false;
    private boolean gameOver  = false;
    private String  roundWinnerTeam = null;

    private final int  teamCount;
    private Thread     gameThread;
    private final NetworkClient net;
    private final boolean testMode;

    private long speedUntil    = 0;
    private long immunityUntil = 0;
    private long ammoUntil     = 0;

    // Used to fire N-key round advance only once per press
    private boolean lastNextRound = false;

    public GamePanel(String playerName, Team team, String mapResource,
                     int teamCount, long seed, NetworkClient net) {
        this.teamCount = teamCount;
        this.net       = net;
        this.testMode  = (net == null);

        AssetLoader.get().load();
        gameMap.load(mapResource);

        setPreferredSize(new Dimension(VIEW_W, VIEW_H));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        addKeyListener(keyHandler);
        setFocusable(true);

        double[] spawn = spawnPosition(team, teamCount);
        localTank = new Tank(playerName, team, spawn[0], spawn[1]);

        spawnPowerUps(seed);
    }

    // ---- Power-up spawning ----

    private void spawnPowerUps(long seed) {
        Random rng = new Random(seed);
        int ts     = GameMap.TILE_SIZE;
        int cols   = gameMap.getCols();
        int rows   = gameMap.getRows();

        List<int[]> candidates = new ArrayList<>();
        for (int r = 2; r < rows - 2; r++) {
            for (int c = 2; c < cols - 2; c++) {
                if (!gameMap.isSolid(c * ts + ts / 2.0, r * ts + ts / 2.0))
                    candidates.add(new int[]{c, r});
            }
        }
        Collections.shuffle(candidates, rng);

        Set<String>    usedKeys = new HashSet<>();
        PowerUp.Type[] types    = PowerUp.Type.values();
        int added = 0;
        for (int[] pos : candidates) {
            if (added >= POWERUP_COUNT) break;
            String key = pos[0] + "," + pos[1];
            if (usedKeys.contains(key)) continue;
            usedKeys.add(key);
            powerUps.add(new PowerUp(pos[0] * ts + ts / 2.0, pos[1] * ts + ts / 2.0,
                    types[rng.nextInt(types.length)]));
            added++;
        }
    }

    // ---- Game loop ----

    public void startGame() {
        gameThread = new Thread(this, "GameLoop");
        gameThread.setDaemon(true);
        gameThread.start();
    }

    @Override
    public void run() {
        double interval = 1_000_000_000.0 / FPS;
        double delta    = 0;
        long   last     = System.nanoTime();
        while (gameThread != null) {
            long now = System.nanoTime();
            delta += (now - last) / interval;
            last   = now;
            if (delta >= 1) { update(); repaint(); delta--; }
            try { Thread.sleep(1); } catch (InterruptedException ignored) {}
        }
    }

    // ---- Update ----

    private void update() {
        // In test mode: N key ends the current round (simulate) or advances to next
        boolean nPressed = keyHandler.nextRound && !lastNextRound;
        lastNextRound = keyHandler.nextRound;

        if (roundOver) {
            if (testMode && nPressed && !gameOver) {
                int next = currentRound + 1;
                if (next <= TOTAL_ROUNDS) {
                    // advance round with local player as winner
                    resetForRound(next, ROUND_MAPS[next - 1], System.currentTimeMillis());
                }
            }
            return;
        }

        // In test mode: N key triggers a round-end simulation
        if (testMode && nPressed) {
            simulateRoundEnd();
            return;
        }

        // Respawn
        if (!localTank.isAlive() && keyHandler.respawn) {
            double[] sp = spawnPosition(localTank.getTeam(), teamCount);
            localTank.respawn(sp[0], sp[1]);
            if (net != null) net.send(
                    GameMessage.move(localTank.getPlayerId(), localTank.getTeam().name(),
                            localTank.getX(), localTank.getY(), localTank.getAngle(),
                            localTank.getHealth(), true));
        }

        if (localTank.isAlive()) {
            double oldX = localTank.getX(), oldY = localTank.getY();

            if (keyHandler.up)    localTank.moveForward();
            if (keyHandler.down)  localTank.moveBackward();
            if (keyHandler.left)  localTank.rotateLeft();
            if (keyHandler.right) localTank.rotateRight();

            if (collidesWithWalls(localTank) || collidesWithTanks(localTank)) {
                localTank.setX(oldX); localTank.setY(oldY);
            }

            if (keyHandler.shoot) {
                Bullet b = localTank.shoot();
                if (b != null) {
                    synchronized (bullets) { bullets.add(b); }
                    if (net != null) net.send(GameMessage.shoot(
                            b.getOwnerId(), b.getOwnerTeam().name(),
                            b.getX(), b.getY(), b.getAngle()));
                }
            }

            if (net != null && (keyHandler.up || keyHandler.down
                    || keyHandler.left || keyHandler.right)) {
                net.send(GameMessage.move(localTank.getPlayerId(), localTank.getTeam().name(),
                        localTank.getX(), localTank.getY(), localTank.getAngle(),
                        localTank.getHealth(), localTank.isAlive()));
            }
        }

        // Power-up timers
        long now = System.currentTimeMillis();
        localTank.setSpeedMultiplier(now < speedUntil ? 2.0 : 1.0);
        localTank.setImmune(now < immunityUntil);
        localTank.setShootCooldownMs(now < ammoUntil ? 100 : Tank.SHOOT_COOLDOWN_MS);

        if (localTank.isAlive()) {
            for (int i = 0; i < powerUps.size(); i++) {
                PowerUp p = powerUps.get(i);
                if (!p.isCollected() && localTank.getBounds().intersects(p.getBounds())) {
                    applyPowerUp(p.getType());
                    p.collect();
                    if (net != null) net.send(GameMessage.powerUpCollected(i));
                }
            }
        }

        // Bullets
        synchronized (bullets) {
            Iterator<Bullet> it = bullets.iterator();
            while (it.hasNext()) {
                Bullet b = it.next();
                b.update();
                if (!b.isActive()) { it.remove(); continue; }

                if (gameMap.isSolid(b.getX(), b.getY())) {
                    explosions.add(new Explosion(b.getX(), b.getY()));
                    it.remove(); continue;
                }

                if (b.getOwnerTeam() != localTank.getTeam() && localTank.isAlive()
                        && localTank.getBounds().intersects(b.getBounds().getBounds2D())) {
                    localTank.takeDamage(Bullet.DAMAGE);
                    explosions.add(new Explosion(b.getX(), b.getY()));
                    if (!localTank.isAlive()) {
                        explosions.add(new Explosion(localTank.getX(), localTank.getY()));
                        if (net != null) net.send(GameMessage.death(localTank.getPlayerId()));
                    } else {
                        if (net != null) net.send(
                                GameMessage.move(localTank.getPlayerId(), localTank.getTeam().name(),
                                        localTank.getX(), localTank.getY(), localTank.getAngle(),
                                        localTank.getHealth(), true));
                    }
                    it.remove(); continue;
                }

                boolean hit = false;
                for (Tank remote : remoteTanks.values()) {
                    if (!remote.isAlive() || b.getOwnerTeam() == remote.getTeam()) continue;
                    if (remote.getBounds().intersects(b.getBounds().getBounds2D())) {
                        remote.takeDamage(Bullet.DAMAGE);
                        explosions.add(new Explosion(b.getX(), b.getY()));
                        if (!remote.isAlive()) {
                            explosions.add(new Explosion(remote.getX(), remote.getY()));
                            if (b.getOwnerId().equals(localTank.getPlayerId()))
                                localTank.addScore(1);
                        }
                        b.setActive(false); hit = true; break;
                    }
                }
                if (hit) it.remove();
            }
        }
        explosions.removeIf(e -> { e.update(); return e.isFinished(); });
    }

    // ---- Rendering ----

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        double mapW = gameMap.getWidthPixels(), mapH = gameMap.getHeightPixels();
        double camX = clamp(localTank.getX(), VIEW_W / 2.0, mapW - VIEW_W / 2.0);
        double camY = clamp(localTank.getY(), VIEW_H / 2.0, mapH - VIEW_H / 2.0);
        double offX = VIEW_W / 2.0 - camX;
        double offY = VIEW_H / 2.0 - camY;

        g2.translate(offX, offY);
        gameMap.draw(g2);
        for (PowerUp p : powerUps) p.draw(g2);
        for (Tank t : remoteTanks.values()) t.draw(g2);
        localTank.draw(g2);
        localTank.drawLocalMarker(g2);
        synchronized (bullets) { for (Bullet b : bullets) b.draw(g2); }
        for (Explosion e : explosions) e.draw(g2);
        g2.translate(-offX, -offY);

        // HUD (screen space)
        hud.draw(g2, localTank, remoteTanks,
                redScore, blueScore, greenScore, yellowScore,
                redWins, blueWins, greenWins, yellowWins,
                teamCount, currentRound, TOTAL_ROUNDS,
                VIEW_W, VIEW_H, speedUntil, immunityUntil, ammoUntil);

        if (!localTank.isAlive() && !roundOver) hud.drawDeathScreen(g2, VIEW_W, VIEW_H);

        if (roundOver) drawInterRoundOverlay(g2);

        g2.dispose();
    }

    // ---- Inter-round overlay ----

    private void drawInterRoundOverlay(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 185));
        g2.fillRect(0, 0, VIEW_W, VIEW_H);

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        int cx = VIEW_W / 2, cy = VIEW_H / 2;

        // Title: "FIN DE RONDA X/Y" or "FIN DE PARTIDA"
        Font bigFont = new Font("Monospaced", Font.BOLD, 50);
        g2.setFont(bigFont);
        FontMetrics fm = g2.getFontMetrics();
        String titleStr = gameOver
                ? "FIN DE PARTIDA"
                : "FIN DE RONDA " + currentRound + "/" + TOTAL_ROUNDS;
        int tx = cx - fm.stringWidth(titleStr) / 2;
        g2.setColor(new Color(80, 60, 0));
        g2.drawString(titleStr, tx + 3, cy - 130 + 3);
        g2.setColor(new Color(255, 215, 20));
        g2.drawString(titleStr, tx, cy - 130);

        // Winner
        if (roundWinnerTeam != null && !roundWinnerTeam.equals("DRAW")) {
            Team winner = null;
            try { winner = Team.valueOf(roundWinnerTeam); } catch (Exception ignored) {}
            Font winFont = new Font("Monospaced", Font.BOLD, 30);
            g2.setFont(winFont);
            fm = g2.getFontMetrics();
            String winStr = gameOver
                    ? "CAMPEÓN: " + roundWinnerTeam
                    : "GANADOR: " + roundWinnerTeam;
            int wx = cx - fm.stringWidth(winStr) / 2;
            g2.setColor(new Color(0, 0, 0, 160));
            g2.drawString(winStr, wx + 2, cy - 82 + 2);
            g2.setColor(winner != null ? winner.bodyColor : Color.WHITE);
            g2.drawString(winStr, wx, cy - 82);
        }

        // Round-win scoreboard
        drawRoundScoreboard(g2, cx, cy);

        // Hint
        Font hintFont = new Font("Monospaced", Font.PLAIN, 16);
        g2.setFont(hintFont);
        fm = g2.getFontMetrics();
        String hint;
        if (gameOver) {
            hint = "Partida terminada";
        } else if (testMode) {
            hint = "[ N ] = Continuar a la siguiente ronda";
        } else {
            hint = "Siguiente ronda en unos segundos...";
        }
        g2.setColor(new Color(160, 160, 160));
        g2.drawString(hint, cx - fm.stringWidth(hint) / 2, cy + 130);
    }

    private void drawRoundScoreboard(Graphics2D g2, int cx, int cy) {
        Team[]   teams = { Team.RED, Team.BLUE, Team.GREEN, Team.YELLOW };
        int[]    wins  = { redWins, blueWins, greenWins, yellowWins };
        int[]    kills = { redScore, blueScore, greenScore, yellowScore };

        int panelW = 360, panelH = 30 + teamCount * 34 + 14;
        int panelX = cx - panelW / 2, panelY = cy - 60;

        g2.setColor(new Color(0, 0, 0, 160));
        g2.fillRoundRect(panelX, panelY, panelW, panelH, 8, 8);
        g2.setColor(new Color(255, 255, 255, 25));
        g2.fillRoundRect(panelX, panelY, panelW, 2, 8, 8);

        g2.setFont(new Font("Monospaced", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(new Color(200, 200, 200));
        g2.drawString("CLASIFICACIÓN", cx - fm.stringWidth("CLASIFICACIÓN") / 2, panelY + 18);

        int dotS = 12, dotG = 5;
        int rowY = panelY + 34;
        for (int i = 0; i < teamCount; i++) {
            Color tc = teams[i].bodyColor;
            // Team name
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            fm = g2.getFontMetrics();
            g2.setColor(new Color(0, 0, 0, 130));
            g2.drawString(teams[i].name(), panelX + 16 + 1, rowY + 1);
            g2.setColor(tc);
            g2.drawString(teams[i].name(), panelX + 16, rowY);

            // Round-win dots
            int dotX = panelX + 100;
            for (int d = 0; d < TOTAL_ROUNDS; d++) {
                int dx = dotX + d * (dotS + dotG);
                int dy = rowY - dotS + 2;
                if (d < wins[i]) {
                    g2.setColor(tc);
                    g2.fillOval(dx, dy, dotS, dotS);
                } else {
                    g2.setColor(new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), 70));
                    g2.fillOval(dx, dy, dotS, dotS);
                    g2.setColor(new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), 140));
                    g2.drawOval(dx, dy, dotS - 1, dotS - 1);
                }
            }

            // Kills
            g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
            fm = g2.getFontMetrics();
            String killStr = kills[i] + " kills";
            g2.setColor(new Color(180, 180, 180));
            g2.drawString(killStr, panelX + panelW - fm.stringWidth(killStr) - 16, rowY);

            rowY += 34;
        }
    }

    // ---- Round reset (called on ROUND_START or test mode advance) ----

    private void resetForRound(int round, String mapResource, long seed) {
        currentRound = round;
        gameMap.load(mapResource);

        double[] sp = spawnPosition(localTank.getTeam(), teamCount);
        localTank.respawn(sp[0], sp[1]);
        speedUntil = immunityUntil = ammoUntil = 0;

        for (Tank t : remoteTanks.values()) {
            t.setAlive(true);
            t.setHealth(100);
        }

        synchronized (bullets) { bullets.clear(); }
        explosions.clear();
        powerUps.clear();
        spawnPowerUps(seed);

        redScore = blueScore = greenScore = yellowScore = 0;
        roundOver = false;
        roundWinnerTeam = null;
    }

    // ---- Test-mode round end simulation ----

    private void simulateRoundEnd() {
        // Award the round to the local player's team
        switch (localTank.getTeam()) {
            case RED    -> redWins++;
            case BLUE   -> blueWins++;
            case GREEN  -> greenWins++;
            case YELLOW -> yellowWins++;
        }
        roundWinnerTeam = localTank.getTeam().name();
        roundOver = true;
        if (currentRound >= TOTAL_ROUNDS) gameOver = true;
    }

    // ---- Spawn positions ----

    private double[] spawnPosition(Team team, int tc) {
        int ts   = GameMap.TILE_SIZE;
        int cols = gameMap.getCols();
        int rows = gameMap.getRows();
        return switch (team) {
            case RED    -> new double[]{ 3 * ts, 2 * ts };
            case BLUE   -> new double[]{ (cols - 4) * ts, 2 * ts };
            case GREEN  -> new double[]{ 3 * ts, (rows - 3) * ts };
            case YELLOW -> new double[]{ (cols - 4) * ts, (rows - 3) * ts };
        };
    }

    // ---- Power-up effects ----

    private void applyPowerUp(PowerUp.Type type) {
        long now = System.currentTimeMillis();
        switch (type) {
            case SPEED    -> speedUntil    = now + 5_000;
            case IMMUNITY -> immunityUntil = now + 4_000;
            case AMMO     -> ammoUntil     = now + 8_000;
            case HEALTH   -> localTank.addHealth(40);
        }
    }

    // ---- Collision helpers ----

    private boolean collidesWithWalls(Tank tank) {
        Rectangle2D bounds = tank.getBounds();
        for (Rectangle2D wall : gameMap.getWallsNear(tank.getX(), tank.getY(), GameMap.TILE_SIZE * 2)) {
            if (bounds.intersects(wall)) return true;
        }
        return false;
    }

    private boolean collidesWithTanks(Tank tank) {
        Rectangle2D bounds = tank.getBounds();
        for (Tank r : remoteTanks.values()) {
            if (r.isAlive() && bounds.intersects(r.getBounds())) return true;
        }
        return false;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    // ---- Network callbacks ----

    public void onRemoteTankUpdate(String id, Team team, double x, double y,
                                   double angle, int health, boolean alive) {
        Tank t = remoteTanks.computeIfAbsent(id, k -> new Tank(id, team, x, y));
        t.setX(x); t.setY(y); t.setAngle(angle);
        t.setHealth(health); t.setAlive(alive);
    }

    public void onRemoteBullet(double x, double y, double angle, String ownerId, Team team) {
        synchronized (bullets) { bullets.add(new Bullet(x, y, angle, ownerId, team)); }
    }

    public void onRemoteDeath(String id) {
        Tank t = remoteTanks.get(id);
        if (t != null) {
            t.setAlive(false); t.setHealth(0);
            explosions.add(new Explosion(t.getX(), t.getY()));
        }
    }

    public void onRemoteDisconnect(String id) { remoteTanks.remove(id); }

    public void onScoreUpdate(int r, int b, int g, int y) {
        redScore = r; blueScore = b; greenScore = g; yellowScore = y;
    }

    public void onRemotePowerUpCollected(int index) {
        if (index >= 0 && index < powerUps.size()) powerUps.get(index).collect();
    }

    public void onRoundEnd(int round, int total, String winner,
                           int rw, int bw, int gw, int yw) {
        currentRound    = round;
        roundWinnerTeam = winner;
        redWins   = rw; blueWins  = bw;
        greenWins = gw; yellowWins = yw;
        roundOver = true;
        if (round >= total) gameOver = true;
    }

    public void onRoundStart(int round, int total, String mapResource, long seed,
                             int rw, int bw, int gw, int yw) {
        redWins   = rw; blueWins  = bw;
        greenWins = gw; yellowWins = yw;
        SwingUtilities.invokeLater(() -> resetForRound(round, mapResource, seed));
    }

    public Tank getLocalTank() { return localTank; }
}
