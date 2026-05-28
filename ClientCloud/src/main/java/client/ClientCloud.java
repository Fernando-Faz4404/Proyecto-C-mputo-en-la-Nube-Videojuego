package client;

import client.game.GameWindow;

import javax.swing.SwingUtilities;

public class ClientCloud {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameWindow::new);
    }
}
