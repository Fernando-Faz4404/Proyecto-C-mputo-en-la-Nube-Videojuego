package client.game;

import client.net.GameMessage;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
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

    private static final Color BG = new Color(30, 26, 22);
    private static final Color ACCENT = new Color(255, 215, 20);
    private static final Color FG = new Color(220, 220, 220);
    private static final Color PANEL_BG = new Color(20, 16, 12);
    private static final Font TITLE = new Font("Monospaced", Font.BOLD, 38);
    private static final Font HEADER = new Font("Monospaced", Font.BOLD, 16);
    private static final Font BODY = new Font("Monospaced", Font.PLAIN, 14);

    private static final Color[] TEAM_COLORS = {
        new Color(220, 60, 60), // RED
        new Color(60, 80, 220), // BLUE
        new Color(50, 180, 50), // GREEN
        new Color(200, 180, 0), // YELLOW
    };
    private static final String[] TEAM_NAMES = { "RED", "BLUE", "GREEN", "YELLOW" };
    private static final String[] TEAM_LABELS = { "ROJO", "AZUL", "VERDE", "AMARILLO" };

    private final JLabel statusLabel;
    private final JLabel countLabel;
    private final JPanel teamGrid;
    private final String localPlayerId;
    private int teamCount = 2;

    public LobbyPanel(String localPlayerId, int initialTeamCount) {
        this.localPlayerId = localPlayerId;
        this.teamCount = initialTeamCount;
        setOpaque(false);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // Title
        JLabel title = new JLabel("SALA DE ESPERA", SwingConstants.CENTER);
        title.setFont(TITLE); title.setForeground(ACCENT);
        add(title, BorderLayout.NORTH);

        // Center: team grid (layout is set dynamically in rebuildGrid)
        teamGrid = new JPanel();
        teamGrid.setOpaque(false);
        add(teamGrid, BorderLayout.CENTER);

        // Bottom: status
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBackground(BG);

        countLabel = new JLabel("Esperando jugadores...", SwingConstants.CENTER);
        countLabel.setFont(HEADER); countLabel.setForeground(FG);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(BODY); statusLabel.setForeground(new Color(150, 150, 150));

        bottom.add(countLabel, BorderLayout.NORTH);
        bottom.add(statusLabel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        rebuildGrid(initialTeamCount, List.of());
    }

    public void updateFromMessage(GameMessage msg) {
        SwingUtilities.invokeLater(() -> {
            if (msg.teamCount != null) teamCount = msg.teamCount;
            List<GameMessage.LobbyPlayer> players = msg.players != null ? msg.players : List.of();
            rebuildGrid(teamCount, players);

            int current = players.size();
            int needed = msg.minPlayers != null ? msg.minPlayers : teamCount * 2;
            int[] counts = new int[teamCount];
            for (GameMessage.LobbyPlayer lp : players) {
                if (lp.team == null) continue;
                for (int i = 0; i < teamCount; i++) {
                    if (TEAM_NAMES[i].equals(lp.team)) {
                        counts[i]++;
                        break;
                    }
                }
            }

            List<String> missingByTeam = new ArrayList<>();
            for (int i = 0; i < teamCount; i++) {
                int miss = Math.max(0, 2 - counts[i]);
                if (miss > 0) missingByTeam.add(TEAM_LABELS[i] + " " + miss);
            }

            if ("STARTING".equals(msg.status)) {
                int seconds = msg.countdownSeconds != null ? msg.countdownSeconds : 1;
                countLabel.setText("Iniciando en " + seconds + "s...");
                countLabel.setForeground(new Color(100, 255, 100));
            } else {
                String waitingText = missingByTeam.isEmpty()
                        ? "Todos los equipos listos"
                        : "Esperando: " + String.join(", ", missingByTeam);
                countLabel.setText(current + " / " + needed + " jugadores - " + waitingText);
                countLabel.setForeground(FG);
            }
            statusLabel.setText(teamCount + " equipos | min " + (teamCount * 2)
                    + " jugadores | max " + (teamCount * 3) + " jugadores");
        });
    }

    private JPanel buildTeamPanel(int i, List<GameMessage.LobbyPlayer> players) {
        String tName = TEAM_NAMES[i];
        Color tColor = TEAM_COLORS[i];

        JPanel col = new JPanel();
        col.setBackground(PANEL_BG);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(tColor, 2, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JLabel header = new JLabel(TEAM_LABELS[i]);
        header.setFont(HEADER); header.setForeground(tColor);
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(header);
        col.add(Box.createVerticalStrut(8));

        int teamPlayers = 0;
        for (GameMessage.LobbyPlayer lp : players) {
            if (lp.team != null && tName.equals(lp.team)) {
                teamPlayers++;
                boolean isLocal = lp.playerId != null && lp.playerId.equals(localPlayerId);
                String display = isLocal ? "► " + lp.playerId : " " + lp.playerId;
                JLabel pl = new JLabel(display);
                pl.setFont(BODY);
                pl.setForeground(isLocal ? ACCENT : FG);
                pl.setAlignmentX(Component.LEFT_ALIGNMENT);
                col.add(pl);
            }
        }

        // Empty slots up to max (3)
        for (int s = teamPlayers; s < 3; s++) {
            JLabel empty = new JLabel(" [vacio]");
            empty.setFont(BODY);
            empty.setForeground(new Color(80, 80, 100));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            col.add(empty);
        }

        col.add(Box.createVerticalGlue());
        return col;
    }

    private void rebuildGrid(int tc, List<GameMessage.LobbyPlayer> players) {
        teamGrid.removeAll();

        if (tc == 4) {
            // 2×2 grid for 4 teams
            teamGrid.setLayout(new GridLayout(2, 2, 12, 12));
            for (int i = 0; i < 4; i++) teamGrid.add(buildTeamPanel(i, players));

        } else if (tc == 3) {
            // Top row: teams 0 & 1 side by side; bottom row: team 2 centered
            teamGrid.setLayout(new BorderLayout(0, 12));

            JPanel topRow = new JPanel(new GridLayout(1, 2, 12, 0));
            topRow.setOpaque(false);
            topRow.add(buildTeamPanel(0, players));
            topRow.add(buildTeamPanel(1, players));

            JPanel bottomRow = new JPanel(new GridBagLayout());
            bottomRow.setOpaque(false);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.weightx = 0.5; gbc.fill = GridBagConstraints.BOTH;
            bottomRow.add(buildTeamPanel(2, players), gbc);

            teamGrid.add(topRow, BorderLayout.NORTH);
            teamGrid.add(bottomRow, BorderLayout.CENTER);

        } else {
            // 1 row for 1 or 2 teams
            teamGrid.setLayout(new GridLayout(1, Math.max(1, tc), 12, 0));
            for (int i = 0; i < tc; i++) teamGrid.add(buildTeamPanel(i, players));
        }

        teamGrid.revalidate();
        teamGrid.repaint();
    }
}
