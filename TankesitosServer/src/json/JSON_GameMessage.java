package json;

/**
 * Mensaje JSON intercambiado con el cliente (ClientCloud).
 * Estructura espeja client.net.GameMessage para compatibilidad con Gson.
 */
public class JSON_GameMessage {
    public String type;
    public String playerId;
    public String team;
    public Double x, y, angle;
    public Integer health;
    public Boolean alive;
    public Integer redScore, blueScore;

    public static JSON_GameMessage stateUpdate(String id, String team, double x, double y,
                                               double angle, int health, boolean alive) {
        JSON_GameMessage m = new JSON_GameMessage();
        m.type = "STATE_UPDATE";
        m.playerId = id; m.team = team;
        m.x = x; m.y = y; m.angle = angle;
        m.health = health; m.alive = alive;
        return m;
    }

    public static JSON_GameMessage disconnect(String id) {
        JSON_GameMessage m = new JSON_GameMessage();
        m.type = "DISCONNECT";
        m.playerId = id;
        return m;
    }

    public static JSON_GameMessage scoreUpdate(int red, int blue) {
        JSON_GameMessage m = new JSON_GameMessage();
        m.type = "SCORE_UPDATE";
        m.redScore = red; m.blueScore = blue;
        return m;
    }
}
