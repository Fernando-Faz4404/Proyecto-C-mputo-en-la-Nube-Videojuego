package client.game;

import client.entity.Tank;
import client.entity.Team;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.Map;

public class HUD {

    private static final Font FONT_LARGE = new Font("Monospaced", Font.BOLD, 18);
    private static final Font FONT_SMALL = new Font("Monospaced", Font.PLAIN, 14);
    private static final Font FONT_TITLE = new Font("Monospaced", Font.BOLD, 48);

    public void draw(Graphics2D g2, Tank localTank, Map<String, Tank> allTanks,
                     int redScore, int blueScore, int mapWidth, int mapHeight) {
        // Team scores top center
        g2.setFont(FONT_LARGE);
        String scoreText = "RED " + redScore + " - " + blueScore + " BLUE";
        int tw = g2.getFontMetrics().stringWidth(scoreText);
        int cx = mapWidth / 2 - tw / 2;
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRoundRect(cx - 10, 5, tw + 20, 28, 8, 8);
        g2.setColor(Team.RED.bodyColor);
        String redPart = "RED " + redScore;
        g2.drawString(redPart, cx, 25);
        g2.setColor(Color.WHITE);
        int redW = g2.getFontMetrics().stringWidth(redPart);
        g2.drawString(" - ", cx + redW, 25);
        g2.setColor(Team.BLUE.bodyColor);
        int midW = g2.getFontMetrics().stringWidth(" - ");
        g2.drawString(blueScore + " BLUE", cx + redW + midW, 25);

        // Local player info bottom-left
        if (localTank != null) {
            g2.setFont(FONT_SMALL);
            int y = mapHeight - 50;
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(8, y - 2, 200, 42, 8, 8);
            g2.setColor(localTank.getTeam().turretColor);
            g2.drawString(localTank.getTeam().displayName + " | " + localTank.getPlayerId(), 15, y + 14);
            g2.drawString("HP: " + localTank.getHealth() + "/" + Tank.MAX_HEALTH
                    + "  Kills: " + localTank.getScore(), 15, y + 32);
        }

        // Controls hint bottom-right
        g2.setFont(FONT_SMALL);
        g2.setColor(new Color(200, 200, 200, 150));
        g2.drawString("WASD=Move SPACE=Shoot R=Respawn", mapWidth - 320, mapHeight - 12);
    }

    public void drawDeathScreen(Graphics2D g2, int mapWidth, int mapHeight) {
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, mapWidth, mapHeight);
        g2.setFont(FONT_TITLE);
        g2.setColor(Color.RED);
        String msg = "DESTROYED";
        int w = g2.getFontMetrics().stringWidth(msg);
        g2.drawString(msg, mapWidth / 2 - w / 2, mapHeight / 2 - 10);
        g2.setFont(FONT_LARGE);
        g2.setColor(Color.WHITE);
        String sub = "Press R to respawn";
        int w2 = g2.getFontMetrics().stringWidth(sub);
        g2.drawString(sub, mapWidth / 2 - w2 / 2, mapHeight / 2 + 30);
    }
}

