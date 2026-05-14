package client.game;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyHandler implements KeyListener {

    public boolean up, down, left, right, shoot, respawn;

    @Override public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W     -> up = true;
            case KeyEvent.VK_S     -> down = true;
            case KeyEvent.VK_A     -> left = true;
            case KeyEvent.VK_D     -> right = true;
            case KeyEvent.VK_SPACE -> shoot = true;
            case KeyEvent.VK_R     -> respawn = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_W     -> up = false;
            case KeyEvent.VK_S     -> down = false;
            case KeyEvent.VK_A     -> left = false;
            case KeyEvent.VK_D     -> right = false;
            case KeyEvent.VK_SPACE -> shoot = false;
            case KeyEvent.VK_R     -> respawn = false;
        }
    }
}

