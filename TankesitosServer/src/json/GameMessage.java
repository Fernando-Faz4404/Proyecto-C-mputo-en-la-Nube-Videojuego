package json;

import main.GamePanel;

/**
 * JSON message format sent over the socket via DataOutputStream.writeUTF().
 * Fields are optional depending on the message type.
 */
public class GameMessage {

    public MessageType type;
    public String playerID;
    public String team;
    public Double x, y, angle;
    public Integer health;
    public Boolean alive;
    public Integer redScore, blueScore;
    
    public String mapRoute;
    public Integer maxRowWorld;
    public Integer maxColWorld;
   
    public static GameMessage move(String id, String team, double x, double y,
                                   double angle, int hp, boolean alive) {
        GameMessage m = new GameMessage();
        m.type = MessageType.MOVE;
        m.playerID = id;
        m.team = team;
        m.x = x; m.y = y; m.angle = angle;
        m.health = hp;
        m.alive = alive;
        return m;
    }
    
    public static GameMessage mapInit(String route) {
        GameMessage msg = new GameMessage();
        msg.type = MessageType.MAP_INIT;
        msg.mapRoute = route;
//        msg.maxRowWorld = maxRow;
//        msg.maxColWorld = maxCol;
        return msg;
    }

    public static GameMessage shoot(String id, String team, double x, double y, double angle) {
        GameMessage m = new GameMessage();
        m.type = MessageType.SHOOT;
        m.playerID = id;
        m.team = team;
        m.x = x; m.y = y; m.angle = angle;
        return m;
    }

    public static GameMessage join(String id, String team) {
        GameMessage m = new GameMessage();
        m.type = MessageType.JOIN;
        m.playerID = id;
        m.team = team;
        return m;
    }

    public static GameMessage death(String id) {
        GameMessage m = new GameMessage();
        m.type = MessageType.DEATH;
        m.playerID = id;
        return m;
    }
}

