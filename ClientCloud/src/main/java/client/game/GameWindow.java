package client.game;

import client.entity.Team;
import client.net.GameMessage;
import client.net.NetworkClient;

import javax.swing.*;
import java.awt.*;

/**
 * Root JFrame. Hosts MainMenuPanel → LobbyPanel → GamePanel in sequence.
 * Implements NetworkClient.EventListener to route messages to the active panel.
 */
public class GameWindow extends JFrame implements NetworkClient.EventListener {

    private static final int VIEWPORT_W = 1248;
    private static final int VIEWPORT_H = 720;

    private NetworkClient net;
    private LobbyPanel lobbyPanel;
    private GamePanel gamePanel;

    private String playerName;
    private String assignedTeam;
    private int teamCount;

    public GameWindow() {
        setTitle("Tank Wars");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        showMainMenu();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ---- Panel switching ----

    private void showMainMenu() {
        SoundManager.get().loadAll();
        SoundManager.get().setEngine(false);
        SoundManager.get().stopBgm();
        SoundManager.get().playMenuMusic();
        setContentPane(new MainMenuPanel(this::onJoinClicked, this::onTestMode));
        pack();
        setMinimumSize(new Dimension(900, 640));
        setLocationRelativeTo(null);
    }

    private void onTestMode() {
        playerName = "TestPlayer";
        assignedTeam = "RED";
        teamCount = 2;
        net = null; // no server

        Team team = Team.RED;
        gamePanel = new GamePanel(playerName, team, "/maps/bigBattleMap.txt",
                teamCount, System.currentTimeMillis(), null);

        setContentPane(gamePanel);
        setPreferredSize(new Dimension(VIEWPORT_W, VIEWPORT_H));
        pack();
        setSize(VIEWPORT_W, VIEWPORT_H);
        setLocationRelativeTo(null);

        gamePanel.setOnReturnToMenu(this::showMainMenu);
        gamePanel.startGame();
        gamePanel.requestFocusInWindow();
    }

    private void onJoinClicked(String name, int tc) {
        this.playerName = name;
        this.teamCount = tc;
        this.assignedTeam = "RED"; // will be overridden by server

        // Create network client (WebSocket URL)
        String serverUrl = loadEnvOrDefault("SERVER_URL", "wss://TU_DOMINIO_AQUI");

        net = new NetworkClient(serverUrl);
        net.setListener(this);

        // Show lobby immediately
        lobbyPanel = new LobbyPanel(name, tc);
        setContentPane(lobbyPanel);
        pack();
        setPreferredSize(new Dimension(860, 560));
        setSize(860, 560);
        setLocationRelativeTo(null);

        net.connect(name, "RED", tc); // team reassigned by server
    }

    private void showGame(GameMessage startMsg) {
        int tc = startMsg.teamCount != null ? startMsg.teamCount : 2;
        String map = startMsg.mapResource != null ? startMsg.mapResource : "/maps/bigBattleMap.txt";
        long seed = startMsg.seed != null ? startMsg.seed : System.currentTimeMillis();

        // Find our team from the player list
        if (startMsg.players != null) {
            for (GameMessage.LobbyPlayer lp : startMsg.players) {
                if (playerName.equals(lp.playerId) && lp.team != null) {
                    assignedTeam = lp.team;
                    break;
                }
            }
        }

        Team team = Team.valueOf(assignedTeam);
        gamePanel = new GamePanel(playerName, team, map, tc, seed, net);
        net.setListener(this);

        setContentPane(gamePanel);
        setPreferredSize(new Dimension(VIEWPORT_W, VIEWPORT_H));
        pack();
        setSize(VIEWPORT_W, VIEWPORT_H);
        setLocationRelativeTo(null);

        gamePanel.setOnReturnToMenu(() -> {
            net.disconnect();
            net = null;
            showMainMenu();
        });
        gamePanel.startGame();
        gamePanel.requestFocusInWindow();
    }

    // ---- NetworkClient.EventListener ----

    @Override
    public void onLobbyState(GameMessage msg) {
        if (lobbyPanel != null) lobbyPanel.updateFromMessage(msg);
    }

    @Override
    public void onGameStart(GameMessage msg) {
        SwingUtilities.invokeLater(() -> showGame(msg));
    }

    @Override
    public void onRemoteTankUpdate(String id, Team team, double x, double y,
                                   double angle, int health, boolean alive) {
        if (gamePanel != null) gamePanel.onRemoteTankUpdate(id, team, x, y, angle, health, alive);
    }

    @Override
    public void onRemoteBullet(double x, double y, double angle, String ownerId, Team team) {
        if (gamePanel != null) gamePanel.onRemoteBullet(x, y, angle, ownerId, team);
    }

    @Override
    public void onRemoteDeath(String id) {
        if (gamePanel != null) gamePanel.onRemoteDeath(id);
    }

    @Override
    public void onRemoteDisconnect(String id) {
        if (gamePanel != null) gamePanel.onRemoteDisconnect(id);
    }

    @Override
    public void onScoreUpdate(int red, int blue, int green, int yellow) {
        if (gamePanel != null) gamePanel.onScoreUpdate(red, blue, green, yellow);
    }

    @Override
    public void onPowerUpCollected(int index) {
        if (gamePanel != null) gamePanel.onRemotePowerUpCollected(index);
    }

    @Override
    public void onPowerUpRespawn(int batchIndex) {
        if (gamePanel != null) gamePanel.onPowerUpRespawn(batchIndex);
    }

    @Override
    public void onRoundEnd(int round, int total, String winner,
                           int redWins, int blueWins, int greenWins, int yellowWins) {
        if (gamePanel != null)
            gamePanel.onRoundEnd(round, total, winner, redWins, blueWins, greenWins, yellowWins);
    }

    @Override
    public void onRoundStart(int round, int total, String mapResource, long seed,
                             int redWins, int blueWins, int greenWins, int yellowWins) {
        if (gamePanel != null)
            gamePanel.onRoundStart(round, total, mapResource, seed,
                    redWins, blueWins, greenWins, yellowWins);
    }

    // ---- Helpers ----

    private String loadEnvOrDefault(String key, String def) {
        java.nio.file.Path p = java.nio.file.Path.of(".env");
        if (!java.nio.file.Files.exists(p)) return def;
        try {
            for (String line : java.nio.file.Files.readAllLines(p)) {
                line = line.trim();
                if (line.startsWith(key + "=")) return line.substring(key.length() + 1).trim();
            }
        } catch (java.io.IOException ignored) {}
        return def;
    }
}
