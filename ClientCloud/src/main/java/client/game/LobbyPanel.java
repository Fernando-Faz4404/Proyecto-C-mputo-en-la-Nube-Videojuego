package client.game;

import client.net.GameMessage;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LobbyPanel extends JPanel {

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        int w = getWidth(), h = getHeight(), bs = 16;
        for (int y = 0; y < h; y += bs) {
            for (int x = 0; x < w; x += bs) {
                int hash = ((x * 31) ^ (y * 37)) & 0xFF;
                int r = 30 + (hash % 14), gv = 26 + ((hash >> 2) % 10) - 2, b = 22 - (hash % 6);
                g2.setColor(new Color(Math.min(255,r), Math.min(255,Math.max(0,gv)), Math.max(0,b)));
                g2.fillRect(x, y, bs, bs);
                g2.setColor(new Color(0,0,0,55));
                g2.drawLine(x,y,x+bs-1,y); g2.drawLine(x,y,x,y+bs-1);
            }
        }
        float cx2 = w/2f, cy2 = h/2f;
        java.awt.RadialGradientPaint vg = new java.awt.RadialGradientPaint(cx2, cy2,
                Math.max(w,h)*0.75f, new float[]{0f,1f},
                new Color[]{new Color(0,0,0,0), new Color(0,0,0,150)});
        g2.setPaint(vg); g2.fillRect(0,0,w,h);
        super.paintComponent(g);
    }

    private static final Color BG       = new Color(30, 26, 22);
    private static final Color ACCENT   = new Color(255, 215, 20);
    private static final Color FG       = new Color(220, 220, 220);
    private static final Color PANEL_BG = new Color(20, 16, 12);
    private static final Font  TITLE    = new Font("Monospaced", Font.BOLD, 38);
    private static final Font  HEADER   = new Font("Monospaced", Font.BOLD, 16);
    private static final Font  BODY     = new Font("Monospaced", Font.PLAIN, 14);

    private static final Color[] TEAM_COLORS = {
        new Color(220, 60, 60),   // RED
        new Color(60, 80, 220),   // BLUE
        new Color(50, 180, 50),   // GREEN
        new Color(200, 180, 0),   // YELLOW
    };
    private static final String[] TEAM_NAMES = { "RED", "BLUE", "GREEN", "YELLOW" };

    private final JLabel statusLabel;
    private final JLabel countLabel;
    private final JPanel teamGrid;
    private final String localPlayerId;
    private int teamCount = 2;

    public LobbyPanel(String localPlayerId) {
        this.localPlayerId = localPlayerId;
        setOpaque(false);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // Title
        JLabel title = new JLabel("WAITING ROOM", SwingConstants.CENTER);
        title.setFont(TITLE); title.setForeground(ACCENT);
        add(title, BorderLayout.NORTH);

        // Center: team grid
        teamGrid = new JPanel(new GridLayout(1, 4, 12, 0));
        teamGrid.setBackground(BG);
        add(teamGrid, BorderLayout.CENTER);

        // Bottom: status
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(BG);

        countLabel = new JLabel("Waiting for players...", SwingConstants.CENTER);
        countLabel.setFont(HEADER); countLabel.setForeground(FG);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(BODY); statusLabel.setForeground(new Color(150, 150, 150));

        bottom.add(countLabel, BorderLayout.NORTH);
        bottom.add(statusLabel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        rebuildGrid(2, List.of());
    }

    public void updateFromMessage(GameMessage msg) {
        SwingUtilities.invokeLater(() -> {
            if (msg.teamCount != null) teamCount = msg.teamCount;
            List<GameMessage.LobbyPlayer> players = msg.players != null ? msg.players : List.of();
            rebuildGrid(teamCount, players);

            int current = players.size();
            int needed  = msg.minPlayers != null ? msg.minPlayers : teamCount * 2;
            int missing = Math.max(0, needed - current);

            if ("STARTING".equals(msg.status)) {
                countLabel.setText("Starting game...");
                countLabel.setForeground(new Color(100, 255, 100));
            } else {
                countLabel.setText(current + " / " + needed + " players  — Need " + missing + " more");
                countLabel.setForeground(FG);
            }
            statusLabel.setText(teamCount + " teams  |  min " + (teamCount * 2)
                    + " players  |  max " + (teamCount * 3) + " players");
        });
    }

    private void rebuildGrid(int tc, List<GameMessage.LobbyPlayer> players) {
        teamGrid.removeAll();
        teamGrid.setLayout(new GridLayout(1, tc, 12, 0));

        for (int i = 0; i < tc; i++) {
            String tName = TEAM_NAMES[i];
            Color  tColor = TEAM_COLORS[i];

            JPanel col = new JPanel();
            col.setBackground(PANEL_BG);
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
            col.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(tColor, 2, true),
                    BorderFactory.createEmptyBorder(10, 14, 10, 14)));

            JLabel header = new JLabel(tName);
            header.setFont(HEADER); header.setForeground(tColor);
            header.setAlignmentX(Component.CENTER_ALIGNMENT);
            col.add(header);
            col.add(Box.createVerticalStrut(8));

            int teamPlayers = 0;
            for (GameMessage.LobbyPlayer lp : players) {
                if (tName.equals(lp.team)) {
                    teamPlayers++;
                    boolean isLocal = localPlayerId.equals(lp.playerId);
                    String display = isLocal ? "► " + lp.playerId : "  " + lp.playerId;
                    JLabel pl = new JLabel(display);
                    pl.setFont(BODY);
                    pl.setForeground(isLocal ? ACCENT : FG);
                    pl.setAlignmentX(Component.LEFT_ALIGNMENT);
                    col.add(pl);
                }
            }

            // Empty slots
            for (int s = teamPlayers; s < 3; s++) {
                JLabel empty = new JLabel("  [empty]");
                empty.setFont(BODY);
                empty.setForeground(new Color(80, 80, 100));
                empty.setAlignmentX(Component.LEFT_ALIGNMENT);
                col.add(empty);
            }

            col.add(Box.createVerticalGlue());
            teamGrid.add(col);
        }

        teamGrid.revalidate();
        teamGrid.repaint();
    }
}
