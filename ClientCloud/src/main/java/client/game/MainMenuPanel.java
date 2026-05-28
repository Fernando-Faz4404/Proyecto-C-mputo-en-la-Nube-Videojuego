package client.game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.function.BiConsumer;

public class MainMenuPanel extends JPanel {

    // ---- Colors ----
    private static final Color BG_BLOCK_BASE = new Color(30, 26, 22);
    private static final Color TITLE_MAIN = new Color(255, 215, 20);
    private static final Color TITLE_SHADOW = new Color(63, 53, 5);
    private static final Color TEXT_WHITE = new Color(255, 255, 255);
    private static final Color TEXT_GRAY = new Color(180, 180, 180);
    private static final Color TEXT_YELLOW = new Color(255, 230, 50);
    private static final Color FIELD_BG = new Color(0, 0, 0, 200);
    private static final Color FIELD_BORDER = new Color(140, 140, 140);

    // ---- Fonts ----
    private static final Font F_TITLE = new Font("Monospaced", Font.BOLD, 72);
    private static final Font F_HEAD = new Font("Monospaced", Font.BOLD, 16);
    private static final Font F_BODY = new Font("Monospaced", Font.PLAIN, 13);
    private static final Font F_SMALL = new Font("Monospaced", Font.PLAIN, 11);

    private static final int BLOCK_SZ = 16;

    private BufferedImage bgCache;
    private int cachedW = -1, cachedH = -1;

    private final JTextField nameField;
    private final JComboBox<String> teamBox;
    private final JLabel statusLabel;

    public MainMenuPanel(BiConsumer<String, Integer> onJoin, Runnable onTestMode) {
        setOpaque(false);
        setLayout(null);

        nameField = createField("Player" + (int)(Math.random() * 900 + 100));
        add(nameField);

        teamBox = createCombo("2 equipos", "3 equipos", "4 equipos");
        add(teamBox);

        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(F_BODY);
        statusLabel.setForeground(new Color(255, 90, 90));
        add(statusLabel);

        MinecraftButton joinBtn = new MinecraftButton("UNIRSE A PARTIDA",
                new Color(80, 80, 80), new Color(255, 255, 100));
        joinBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) { statusLabel.setText("¡Ingresa un nombre!"); return; }
            int tc = teamBox.getSelectedIndex() + 2;
            onJoin.accept(name, tc);
        });
        add(joinBtn);

        MinecraftButton tutorialBtn = new MinecraftButton("TUTORIAL",
                new Color(50, 50, 50), new Color(160, 220, 255));
        tutorialBtn.addActionListener(e -> showTutorial());
        add(tutorialBtn);

        // ---- Skip button (top-right, for local testing) ----
        JButton skipBtn = new JButton("▶ MODO PRUEBA") {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                boolean hover = getModel().isRollover();
                boolean press = getModel().isPressed();
                // Dark translucent background
                g2.setColor(hover ? new Color(60, 0, 0, 200) : new Color(30, 0, 0, 180));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                // Border
                g2.setColor(new Color(180, 50, 50, hover ? 220 : 140));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 4, 4);
                // Text
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.setColor(new Color(0, 0, 0, 120));
                g2.drawString(getText(), tx + 1, ty + 1);
                g2.setColor(press ? new Color(200, 80, 80) : new Color(255, 120, 120));
                g2.drawString(getText(), tx, ty);
            }
        };
        skipBtn.setContentAreaFilled(false);
        skipBtn.setBorderPainted(false);
        skipBtn.setFocusPainted(false);
        skipBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        skipBtn.setFont(new Font("Monospaced", Font.BOLD, 11));
        skipBtn.setToolTipText("Saltar al juego sin servidor (solo pruebas)");
        skipBtn.addActionListener(e -> onTestMode.run());
        add(skipBtn);

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { doLayout(); }
        });
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing())
                doLayout();
        });

        putClientProperty("joinBtn", joinBtn);
        putClientProperty("tutorialBtn", tutorialBtn);
        putClientProperty("skipBtn", skipBtn);
    }

    // ---- Layout ----

    @Override
    public void doLayout() {
        super.doLayout();
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        int cx = w / 2;
        int fieldW = 280, fieldH = 32;

        // Form centered vertically below title
        int formY = (int)(h * 0.60);

        nameField.setBounds(cx - fieldW / 2, formY, fieldW, fieldH);
        teamBox.setBounds(cx - fieldW / 2, formY + 42, fieldW, fieldH);

        Component joinBtn = (Component) getClientProperty("joinBtn");
        if (joinBtn != null) joinBtn.setBounds(cx - fieldW / 2, formY + 90, fieldW, 38);

        Component tutBtn = (Component) getClientProperty("tutorialBtn");
        if (tutBtn != null) tutBtn.setBounds(cx - fieldW / 2, formY + 138, fieldW, 32);

        statusLabel.setBounds(cx - 200, formY + 180, 400, 20);

        // Skip button — fixed top-right corner
        Component skipBtn = (Component) getClientProperty("skipBtn");
        if (skipBtn != null) skipBtn.setBounds(w - 152, 10, 142, 26);
    }

    // ---- Background ----

    @Override
    protected void paintComponent(Graphics g) {
        int w = getWidth(), h = getHeight();
        Graphics2D g2 = (Graphics2D) g;

        if (w != cachedW || h != cachedH) {
            bgCache = buildBackground(w, h);
            cachedW = w; cachedH = h;
        }
        g2.drawImage(bgCache, 0, 0, null);

        drawTitle(g2, w, h);
        drawFormLabels(g2, w, h);
        drawVersion(g2, w, h);
    }

    private BufferedImage buildBackground(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();

        for (int y = 0; y < h; y += BLOCK_SZ) {
            for (int x = 0; x < w; x += BLOCK_SZ) {
                int hash = ((x * 31) ^ (y * 37)) & 0xFF;
                int r = BG_BLOCK_BASE.getRed() + (hash % 14);
                int gv = BG_BLOCK_BASE.getGreen() + ((hash >> 2) % 10) - 2;
                int b = BG_BLOCK_BASE.getBlue() - (hash % 6);
                g.setColor(new Color(clamp(r), clamp(gv), clamp(b)));
                g.fillRect(x, y, BLOCK_SZ, BLOCK_SZ);
                g.setColor(new Color(0, 0, 0, 55));
                g.drawLine(x, y, x + BLOCK_SZ - 1, y);
                g.drawLine(x, y, x, y + BLOCK_SZ - 1);
            }
        }

        float cx = w / 2f, cy = h / 2f;
        RadialGradientPaint vignette = new RadialGradientPaint(
                cx, cy, Math.max(w, h) * 0.75f,
                new float[]{ 0f, 1f },
                new Color[]{ new Color(0,0,0,0), new Color(0,0,0,160) });
        g.setPaint(vignette);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    private void drawTitle(Graphics2D g2, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2.setFont(F_TITLE);
        FontMetrics fm = g2.getFontMetrics();

        String line1 = "TANK", line2 = "WARS";
        int totalH = fm.getHeight() * 2 - 8;
        int startY = (int)(h * 0.22) + fm.getAscent();

        drawShadowText(g2, line1, w / 2 - fm.stringWidth(line1) / 2, startY,
                TITLE_MAIN, TITLE_SHADOW, 4);
        drawShadowText(g2, line2, w / 2 - fm.stringWidth(line2) / 2, startY + fm.getHeight() - 8,
                new Color(255, 120, 20), TITLE_SHADOW, 4);
    }

    private void drawFormLabels(Graphics2D g2, int w, int h) {
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);

        int cx = w / 2;
        int formY = (int)(h * 0.60);
        int panelW = 330, panelH = 170;

        drawMcPanel(g2, cx - panelW / 2, formY - 22, panelW, panelH);

        g2.setFont(F_HEAD);
        drawShadowText(g2, "UNIRSE A PARTIDA",
                cx - g2.getFontMetrics().stringWidth("UNIRSE A PARTIDA") / 2,
                formY - 6, TEXT_YELLOW, new Color(63, 53, 0), 2);

        g2.setFont(F_BODY);
        drawShadowText(g2, "Nombre:", cx - 140, formY + 23, TEXT_WHITE, Color.BLACK, 1);
        drawShadowText(g2, "Equipos:", cx - 140, formY + 65, TEXT_WHITE, Color.BLACK, 1);
    }

    private void drawVersion(Graphics2D g2, int w, int h) {
        g2.setFont(F_SMALL);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        drawShadowText(g2, "Tank Wars 2026 — Proyecto Cómputo en la Nube",
                8, h - 8, TEXT_GRAY, Color.BLACK, 1);
    }

    // ---- Tutorial dialog ----

    private void showTutorial() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog((Frame)(owner instanceof Frame ? owner : null), "Tutorial", true);
        dlg.setUndecorated(true);
        dlg.setBackground(new Color(0, 0, 0, 0));

        JPanel content = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                int w = getWidth(), h = getHeight();

                // Minecraft panel background
                g2.setColor(new Color(12, 10, 8));
                g2.fillRect(0, 0, w, h);

                // Border
                g2.setColor(new Color(255, 255, 255, 60));
                g2.fillRect(0, 0, w, 3);
                g2.fillRect(0, 0, 3, h);
                g2.setColor(new Color(0, 0, 0, 120));
                g2.fillRect(0, h - 3, w, 3);
                g2.fillRect(w - 3, 0, 3, h);
                g2.setColor(new Color(255, 255, 255, 20));
                g2.fillRect(3, 3, w - 6, 2);
                g2.fillRect(3, 3, 2, h - 6);
            }
        };
        content.setLayout(new BorderLayout(0, 0));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(20, 28, 20, 28));

        // Title
        JLabel title = new JLabel("CÓMO JUGAR", SwingConstants.CENTER) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                FontMetrics fm = g2.getFontMetrics();
                int tx = (getWidth() - fm.stringWidth(getText())) / 2;
                int ty = fm.getAscent();
                g2.setColor(new Color(63, 53, 0));
                g2.drawString(getText(), tx + 2, ty + 2);
                g2.setColor(TEXT_YELLOW);
                g2.drawString(getText(), tx, ty);
            }
        };
        title.setFont(new Font("Monospaced", Font.BOLD, 22));
        title.setPreferredSize(new Dimension(0, 36));
        content.add(title, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
                g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
                FontMetrics fm = g2.getFontMetrics();

                Object[][] rows = {
                    { "MOVIMIENTO", null },
                    { " W / S", "Avanzar / Retroceder" },
                    { " A / D", "Rotar izquierda / derecha" },
                    { " ESPACIO", "Disparar" },
                    { " R", "Reaparecer" },
                    { "", null },
                    { "POWER-UPS", null },
                    { " Cura", "+40 HP (verde)" },
                    { " Escudo", "Inmunidad 4s (azul)" },
                    { " Velocidad", "x2 vel. 5s (dorado)" },
                    { " Bala esp.", "Disparo rápido 8s (naranja)" },
                    { "", null },
                    { "REGLAS", null },
                    { " Equipos", "2 a 4 equipos" },
                    { " Min/Max", "2 / 3 jugadores por equipo" },
                    { " Items", "Un solo uso, sin reaparición" },
                };

                int lh = fm.getHeight() + 3;
                int y = fm.getAscent() + 4;
                for (Object[] row : rows) {
                    String left = (String) row[0];
                    String right = (String) row[1];
                    if (left.isEmpty()) { y += 6; continue; }

                    boolean isHeader = right == null;
                    if (isHeader) {
                        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
                        fm = g2.getFontMetrics();
                        // Shadow
                        g2.setColor(new Color(63, 53, 0));
                        g2.drawString(left, 1 + 1, y + 1);
                        g2.setColor(TEXT_YELLOW);
                        g2.drawString(left, 1, y);
                        g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
                        fm = g2.getFontMetrics();
                    } else {
                        g2.setColor(new Color(0, 0, 0, 120));
                        g2.drawString(left, 1 + 1, y + 1);
                        g2.setColor(new Color(180, 220, 180));
                        g2.drawString(left, 1, y);

                        g2.setColor(new Color(0, 0, 0, 120));
                        g2.drawString(right, 181, y + 1);
                        g2.setColor(TEXT_WHITE);
                        g2.drawString(right, 180, y);
                    }
                    y += lh;
                }
            }
        };
        body.setOpaque(false);
        body.setPreferredSize(new Dimension(430, 320));
        content.add(body, BorderLayout.CENTER);

        // Close button
        MinecraftButton closeBtn = new MinecraftButton("CERRAR",
                new Color(100, 30, 30), new Color(255, 180, 180));
        closeBtn.setPreferredSize(new Dimension(0, 34));
        closeBtn.addActionListener(e -> dlg.dispose());
        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 0));
        south.add(closeBtn);
        content.add(south, BorderLayout.SOUTH);

        dlg.setContentPane(content);
        dlg.pack();
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }

    // ---- Drawing utilities ----

    private void drawMcPanel(Graphics2D g2, int x, int y, int w, int h) {
        g2.setColor(new Color(0, 0, 0, 175));
        g2.fillRect(x, y, w, h);
        g2.setColor(new Color(255, 255, 255, 70));
        g2.fillRect(x, y, w, 2);
        g2.fillRect(x, y, 2, h);
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(x, y + h - 2, w, 2);
        g2.fillRect(x + w - 2, y, 2, h);
        g2.setColor(new Color(255, 255, 255, 30));
        g2.fillRect(x + 2, y + 2, w - 4, 2);
        g2.fillRect(x + 2, y + 2, 2, h - 4);
    }

    private void drawShadowText(Graphics2D g2, String text, int x, int y,
                                Color main, Color shadow, int offset) {
        g2.setColor(shadow);
        g2.drawString(text, x + offset, y + offset);
        g2.setColor(main);
        g2.drawString(text, x, y);
    }

    // ---- Component factories ----

    private JTextField createField(String placeholder) {
        JTextField tf = new JTextField(placeholder) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(FIELD_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRect(0, 0, getWidth(), 1);
                g2.fillRect(0, 0, 1, getHeight());
                g2.setColor(new Color(0, 0, 0, 100));
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                g2.fillRect(getWidth() - 1, 0, 1, getHeight());
                super.paintComponent(g);
            }
        };
        tf.setFont(F_BODY);
        tf.setForeground(TEXT_WHITE);
        tf.setBackground(new Color(0, 0, 0, 0));
        tf.setOpaque(false);
        tf.setCaretColor(Color.WHITE);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FIELD_BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        return tf;
    }

    private JComboBox<String> createCombo(String... items) {
        JComboBox<String> cb = new JComboBox<>(items) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(FIELD_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());
                super.paintComponent(g);
            }
        };
        cb.setFont(F_BODY);
        cb.setForeground(TEXT_WHITE);
        cb.setBackground(new Color(40, 40, 40));
        cb.setOpaque(false);
        cb.setBorder(BorderFactory.createLineBorder(FIELD_BORDER, 1));
        cb.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v,
                    int idx, boolean sel, boolean focus) {
                super.getListCellRendererComponent(l, v, idx, sel, focus);
                setBackground(sel ? new Color(70, 70, 70) : new Color(30, 30, 30));
                setForeground(TEXT_WHITE);
                setFont(F_BODY);
                setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
                return this;
            }
        });
        return cb;
    }

    private int clamp(int v) { return Math.min(255, Math.max(0, v)); }

    public void setStatus(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(msg));
    }

    // ---- Minecraft button ----

    public static class MinecraftButton extends JButton {
        private static final Color BTN_BASE = new Color(85, 85, 85);
        private static final Color BTN_HOVER = new Color(110, 110, 110);
        private static final Color BTN_PRESS = new Color(65, 65, 65);
        private static final Color BTN_HIGH = new Color(190, 190, 190);
        private static final Color BTN_SHADOW = new Color(35, 35, 35);

        private final Color textColor;

        public MinecraftButton(String text, Color base, Color textColor) {
            super(text);
            this.textColor = textColor;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(new Font("Monospaced", Font.BOLD, 15));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
            int w = getWidth(), h = getHeight();
            boolean hover = getModel().isRollover();
            boolean press = getModel().isPressed();

            g2.setColor(new Color(20, 20, 20));
            g2.fillRect(0, 0, w, h);

            Color face = press ? BTN_PRESS : hover ? BTN_HOVER : BTN_BASE;
            g2.setColor(face);
            g2.fillRect(2, 2, w - 4, h - 4);

            g2.setColor(BTN_HIGH);
            g2.fillRect(2, 2, w - 4, 2);
            g2.fillRect(2, 4, 2, h - 6);

            g2.setColor(BTN_SHADOW);
            g2.fillRect(4, h - 4, w - 6, 2);
            g2.fillRect(w - 4, 4, 2, h - 6);

            g2.setColor(new Color(0, 0, 0, 40));
            g2.fillRect(2, h / 2, w - 4, 1);

            FontMetrics fm = g2.getFontMetrics();
            int tx = (w - fm.stringWidth(getText())) / 2;
            int ty = (h + fm.getAscent() - fm.getDescent()) / 2 - (press ? 0 : 1);

            g2.setColor(new Color(0, 0, 0, 180));
            g2.drawString(getText(), tx + 2, ty + 2);
            g2.setColor(press ? textColor.darker() : textColor);
            g2.drawString(getText(), tx, ty);
        }
    }
}
