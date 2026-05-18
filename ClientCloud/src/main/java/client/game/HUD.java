package client.game;

import client.entity.PowerUp;
import client.entity.Tank;
import client.entity.Team;

import java.awt.*;
import java.util.Map;

public class HUD {

    private static final Font FONT_LARGE = new Font("Monospaced", Font.BOLD, 16);
    private static final Font FONT_SMALL = new Font("Monospaced", Font.PLAIN, 13);
    private static final Font FONT_TITLE = new Font("Monospaced", Font.BOLD, 48);

    private static final String[] MAP_NAMES = { "Gran Batalla", "Volcánico", "Ártico" };

    public void draw(Graphics2D g2, Tank localTank, Map<String, Tank> allTanks,
                     int redScore, int blueScore, int greenScore, int yellowScore,
                     int redWins, int blueWins, int greenWins, int yellowWins,
                     int teamCount, int currentRound, int totalRounds,
                     int viewW, int viewH,
                     long speedUntil, long immunityUntil, long ammoUntil) {

        drawTopBar(g2, redScore, blueScore, greenScore, yellowScore,
                   redWins, blueWins, greenWins, yellowWins,
                   teamCount, currentRound, totalRounds, viewW);
        drawLocalInfo(g2, localTank, viewH);
        drawActiveEffects(g2, speedUntil, immunityUntil, ammoUntil, viewW, viewH);
        drawControls(g2, viewW, viewH);
    }

    // ---- Top bar: RONDA X/Y  |  TEAM ●●○ kills  | ... ----

    private void drawTopBar(Graphics2D g2,
                            int rKills, int bKills, int gKills, int yKills,
                            int rWins,  int bWins,  int gWins,  int yWins,
                            int teamCount, int currentRound, int totalRounds, int viewW) {

        Team[]   teams  = { Team.RED,  Team.BLUE,  Team.GREEN,  Team.YELLOW };
        int[]    kills  = { rKills, bKills, gKills, yKills };
        int[]    wins   = { rWins,  bWins,  gWins,  yWins  };

        // ── measure widths ──────────────────────────────────────────────────
        g2.setFont(FONT_LARGE);
        FontMetrics fm = g2.getFontMetrics();

        // Round label
        String mapLabel  = currentRound >= 1 && currentRound <= MAP_NAMES.length
                           ? MAP_NAMES[currentRound - 1] : "";
        String roundText = "RONDA " + currentRound + "/" + totalRounds;
        int roundW = fm.stringWidth(roundText);
        int mapW   = 0;
        if (!mapLabel.isEmpty()) {
            g2.setFont(FONT_SMALL);
            mapW = g2.getFontMetrics().stringWidth(mapLabel);
            g2.setFont(FONT_LARGE);
        }
        int leftBlockW = Math.max(roundW, mapW) + 20;

        // Team segments
        int dotSize = 10, dotGap = 4;
        int dotsW   = totalRounds * (dotSize + dotGap) - dotGap;
        int segPad  = 16;
        int[] segWidths = new int[teamCount];
        for (int i = 0; i < teamCount; i++) {
            String label = teams[i].name() + "  " + kills[i];
            segWidths[i] = segPad + dotsW + 8 + fm.stringWidth(label) + segPad;
        }
        int totalTeamW = 0;
        for (int w : segWidths) totalTeamW += w;

        int totalBarW = leftBlockW + 12 + totalTeamW + 12;
        int barH      = 38;
        int barX      = viewW / 2 - totalBarW / 2;
        int barY      = 6;

        // ── background ──────────────────────────────────────────────────────
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRoundRect(barX, barY, totalBarW, barH, 8, 8);
        // subtle top highlight
        g2.setColor(new Color(255, 255, 255, 18));
        g2.fillRoundRect(barX, barY, totalBarW, 2, 8, 8);

        // ── round block ─────────────────────────────────────────────────────
        int cx = barX + leftBlockW / 2;
        g2.setFont(FONT_LARGE);
        fm = g2.getFontMetrics();
        // shadow
        g2.setColor(new Color(80, 60, 0));
        g2.drawString(roundText, cx - fm.stringWidth(roundText) / 2 + 1, barY + 16 + 1);
        // gold text
        g2.setColor(new Color(255, 215, 20));
        g2.drawString(roundText, cx - fm.stringWidth(roundText) / 2, barY + 16);

        if (!mapLabel.isEmpty()) {
            g2.setFont(FONT_SMALL);
            FontMetrics sfm = g2.getFontMetrics();
            g2.setColor(new Color(160, 160, 160));
            g2.drawString(mapLabel, cx - sfm.stringWidth(mapLabel) / 2, barY + 30);
        }

        // divider after round block
        g2.setColor(new Color(255, 255, 255, 40));
        g2.fillRect(barX + leftBlockW + 4, barY + 6, 1, barH - 12);

        // ── team segments ───────────────────────────────────────────────────
        int sx = barX + leftBlockW + 12;
        g2.setFont(FONT_LARGE);
        fm = g2.getFontMetrics();

        for (int i = 0; i < teamCount; i++) {
            Color tc = teams[i].bodyColor;
            int midY  = barY + barH / 2;

            // Dot indicators (round wins)
            int dotStartX = sx + segPad;
            int dotY      = midY - dotSize / 2;
            for (int d = 0; d < totalRounds; d++) {
                int dx = dotStartX + d * (dotSize + dotGap);
                if (d < wins[i]) {
                    // filled = won
                    g2.setColor(tc);
                    g2.fillOval(dx, dotY, dotSize, dotSize);
                    g2.setColor(new Color(255, 255, 255, 60));
                    g2.fillOval(dx + 2, dotY + 1, 4, 3); // small shine
                } else {
                    // empty = not yet won
                    g2.setColor(new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), 60));
                    g2.fillOval(dx, dotY, dotSize, dotSize);
                    g2.setColor(new Color(tc.getRed(), tc.getGreen(), tc.getBlue(), 120));
                    g2.drawOval(dx, dotY, dotSize - 1, dotSize - 1);
                }
            }

            // Kill count
            String killStr = teams[i].name() + "  " + kills[i];
            int textX = dotStartX + dotsW + 8;
            // shadow
            g2.setColor(new Color(0, 0, 0, 150));
            g2.drawString(killStr, textX + 1, midY + fm.getAscent() / 2 + 1);
            // colored text
            g2.setColor(tc);
            g2.drawString(killStr, textX, midY + fm.getAscent() / 2);

            sx += segWidths[i];

            // divider between teams
            if (i < teamCount - 1) {
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRect(sx - 8, barY + 6, 1, barH - 12);
            }
        }
    }

    // ── Bottom-left: local player info ──────────────────────────────────────

    private void drawLocalInfo(Graphics2D g2, Tank localTank, int viewH) {
        if (localTank == null) return;
        g2.setFont(FONT_SMALL);
        int y = viewH - 80;   // raised so both lines are fully inside the viewport
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(8, y - 6, 260, 50, 6, 6);
        g2.setColor(localTank.getTeam().turretColor);
        g2.drawString("▶  " + localTank.getTeam().displayName
                + " | " + localTank.getPlayerId(), 16, y + 12);
        g2.setColor(new Color(210, 210, 210));
        g2.drawString("HP: " + localTank.getHealth() + "/" + Tank.MAX_HEALTH
                + "   Kills: " + localTank.getScore(), 16, y + 30);
    }

    // ── Bottom-right: controls hint ──────────────────────────────────────────

    private void drawControls(Graphics2D g2, int viewW, int viewH) {
        g2.setFont(FONT_SMALL);
        g2.setColor(new Color(180, 180, 180, 140));
        g2.drawString("WASD=Mover  SPACE=Disparar  R=Reaparecer", viewW - 360, viewH - 16);
    }

    // ── Right side: active power-up bars ────────────────────────────────────

    private void drawActiveEffects(Graphics2D g2, long speedUntil, long immunityUntil,
                                   long ammoUntil, int viewW, int viewH) {
        long now = System.currentTimeMillis();
        PowerUp.Type[] types  = { PowerUp.Type.SPEED, PowerUp.Type.IMMUNITY, PowerUp.Type.AMMO };
        long[]         untils = { speedUntil, immunityUntil, ammoUntil };

        int x = viewW - 148;
        int y = viewH - 86;

        for (int i = 0; i < types.length; i++) {
            long remaining = untils[i] - now;
            if (remaining <= 0) continue;
            Color c    = types[i].color();
            float secs = remaining / 1000f;

            g2.setColor(new Color(0, 0, 0, 160));
            g2.fillRoundRect(x - 4, y - 14, 134, 18, 5, 5);
            int barW = (int) (124 * Math.min(1f, secs / maxDuration(types[i])));
            g2.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 190));
            g2.fillRoundRect(x, y - 11, barW, 12, 3, 3);
            g2.setColor(Color.WHITE);
            g2.setFont(FONT_SMALL);
            g2.drawString(types[i].label() + "  " + String.format("%.1fs", secs), x + 2, y);
            y -= 22;
        }
    }

    private float maxDuration(PowerUp.Type type) {
        return switch (type) {
            case SPEED    -> 5f;
            case IMMUNITY -> 4f;
            case AMMO     -> 8f;
            default       -> 5f;
        };
    }

    // ── Death screen ─────────────────────────────────────────────────────────

    public void drawDeathScreen(Graphics2D g2, int viewW, int viewH) {
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, viewW, viewH);
        g2.setFont(FONT_TITLE);
        g2.setColor(Color.RED);
        String msg = "DESTRUIDO";
        int w = g2.getFontMetrics().stringWidth(msg);
        g2.drawString(msg, viewW / 2 - w / 2, viewH / 2 - 10);
        g2.setFont(FONT_LARGE);
        g2.setColor(Color.WHITE);
        String sub = "Presiona R para reaparecer";
        int w2 = g2.getFontMetrics().stringWidth(sub);
        g2.drawString(sub, viewW / 2 - w2 / 2, viewH / 2 + 32);
    }
}
