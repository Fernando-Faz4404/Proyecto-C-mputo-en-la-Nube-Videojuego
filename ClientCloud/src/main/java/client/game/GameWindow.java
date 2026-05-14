package client.game;

import client.entity.Team;

import javax.swing.JFrame;

public class GameWindow {

    public GameWindow(String playerName, Team team, String serverIp, int serverPort, String mapResource) {
        JFrame frame = new JFrame("Tank Wars — " + team.displayName + " — " + playerName);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);

        GamePanel gamePanel = new GamePanel(playerName, team, mapResource);
        frame.add(gamePanel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // Connect to server if IP provided (null = offline mode)
        if (serverIp != null && !serverIp.isEmpty()) {
            gamePanel.connectToServer(serverIp, serverPort);
        }

        gamePanel.startGame();
        gamePanel.requestFocusInWindow();
    }
}

