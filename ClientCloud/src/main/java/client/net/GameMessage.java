package client.net;

import java.util.List;

public class GameMessage {

    public MessageType type;
    public String playerId;
    public String team;
    public Double x, y, angle;
    public Integer health;
    public Boolean alive;
    public Integer redScore, blueScore, greenScore, yellowScore;

    // Lobby / game-start fields
    public Integer teamCount;
    public String status; // "WAITING" | "STARTING"
    public String mapResource;
    public Integer minPlayers;
    public Long seed; // RNG seed for power-up positions
    public Integer powerUpIndex;
    public Integer powerUpRespawnBatch;
    public List<LobbyPlayer> players;

    // Round fields
    public Integer roundNumber;
    public Integer totalRounds;
    public String roundWinner;
    public Integer redWins, blueWins, greenWins, yellowWins;

    // ---- Inner types ----

    public static class LobbyPlayer {
        public String playerId;
        public String team;
        public LobbyPlayer() {}
        public LobbyPlayer(String p, String t) { playerId = p; team = t; }
    }

    // ---- Factory methods ----

    public static GameMessage move(String id, String team, double x, double y,
                                   double angle, int hp, boolean alive) {
        GameMessage m = new GameMessage();
        m.type = MessageType.MOVE;
        m.playerId = id; m.team = team;
        m.x = x; m.y = y; m.angle = angle;
        m.health = hp; m.alive = alive;
        return m;
    }

    public static GameMessage shoot(String id, String team, double x, double y, double angle) {
        GameMessage m = new GameMessage();
        m.type = MessageType.SHOOT;
        m.playerId = id; m.team = team;
        m.x = x; m.y = y; m.angle = angle;
        return m;
    }

    public static GameMessage join(String id, String team, int teamCount) {
        GameMessage m = new GameMessage();
        m.type = MessageType.JOIN;
        m.playerId = id; m.team = team;
        m.teamCount = teamCount;
        return m;
    }

    public static GameMessage death(String id) {
        GameMessage m = new GameMessage();
        m.type = MessageType.DEATH;
        m.playerId = id;
        return m;
    }

    public static GameMessage powerUpCollected(int index) {
        GameMessage m = new GameMessage();
        m.type = MessageType.POWERUP_COLLECTED;
        m.powerUpIndex = index;
        return m;
    }

    public static GameMessage powerUpRespawn(int batchIndex) {
        GameMessage m = new GameMessage();
        m.type = MessageType.POWERUP_RESPAWN;
        m.powerUpRespawnBatch = batchIndex;
        return m;
    }

    public static GameMessage roundEnd(int round, int total, String winner,
                                       int rw, int bw, int gw, int yw) {
        GameMessage m = new GameMessage();
        m.type = MessageType.ROUND_END;
        m.roundNumber = round; m.totalRounds = total;
        m.roundWinner = winner;
        m.redWins = rw; m.blueWins = bw; m.greenWins = gw; m.yellowWins = yw;
        return m;
    }

    public static GameMessage roundStart(int round, int total, String map, long seed,
                                         int rw, int bw, int gw, int yw) {
        GameMessage m = new GameMessage();
        m.type = MessageType.ROUND_START;
        m.roundNumber = round; m.totalRounds = total;
        m.mapResource = map; m.seed = seed;
        m.redWins = rw; m.blueWins = bw; m.greenWins = gw; m.yellowWins = yw;
        return m;
    }
}
