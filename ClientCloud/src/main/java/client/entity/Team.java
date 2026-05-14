package client.entity;

import java.awt.Color;

public enum Team {
    RED("Red Team", new Color(200, 50, 50), new Color(255, 100, 100)),
    BLUE("Blue Team", new Color(50, 50, 200), new Color(100, 100, 255));

    public final String displayName;
    public final Color bodyColor;
    public final Color turretColor;

    Team(String displayName, Color bodyColor, Color turretColor) {
        this.displayName = displayName;
        this.bodyColor = bodyColor;
        this.turretColor = turretColor;
    }
}

