package client.net;

/**
 * JSON message format sent over the socket via DataOutputStream.writeUTF().
 * Fields are optional depending on the message type.
 */
public class GameMessage {

    public MessageType type;
    public String playerId;
    public String team;
    public Double x, y, angle;
    public Integer health;
    public Boolean alive;
    public Integer redScore, blueScore;

    public static GameMessage move(String id, String team, double x, double y,
                                   double angle, int hp, boolean alive) {
        GameMessage m = new GameMessage();
        m.type = MessageType.MOVE;
        m.playerId = id;
        m.team = team;
        m.x = x; m.y = y; m.angle = angle;
        m.health = hp;
        m.alive = alive;
        return m;
    }

    public static GameMessage shoot(String id, String team, double x, double y, double angle) {
        GameMessage m = new GameMessage();
        m.type = MessageType.SHOOT;
        m.playerId = id;
        m.team = team;
        m.x = x; m.y = y; m.angle = angle;
        return m;
    }

    public static GameMessage join(String id, String team) {
        GameMessage m = new GameMessage();
        m.type = MessageType.JOIN;
        m.playerId = id;
        m.team = team;
        return m;
    }

    public static GameMessage death(String id) {
        GameMessage m = new GameMessage();
        m.type = MessageType.DEATH;
        m.playerId = id;
        return m;
    }
}

