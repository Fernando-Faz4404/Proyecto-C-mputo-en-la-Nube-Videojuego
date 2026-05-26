package json;

import java.util.List;

public class JSON_GameMessage {
    public String type;
    public String playerId;
    public String team;
    public Double x, y, angle;
    public Integer health;
    public Boolean alive;
    public Integer redScore, blueScore, greenScore, yellowScore;

    // Lobby / game-start
    public Integer teamCount;
    public String status;
    public String mapResource;
    public Integer minPlayers;
    public Integer countdownSeconds;
    public Long seed;
    public Integer powerUpIndex;
    public Integer powerUpRespawnBatch;
    public List<LobbyPlayer> players;

    // Round fields
    public Integer roundNumber;
    public Integer totalRounds;
    public String roundWinner;
    public Integer redWins, blueWins, greenWins, yellowWins;

    public static class LobbyPlayer {
        public String playerId;
        public String team;
        public LobbyPlayer() {}
        public LobbyPlayer(String p, String t) { playerId = p; team = t; }
    }

    // ---- Factory methods ----

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

    public static JSON_GameMessage scoreUpdate(int red, int blue, int green, int yellow) {
        JSON_GameMessage m = new JSON_GameMessage();
        m.type = "SCORE_UPDATE";
        m.redScore = red; m.blueScore = blue;
        m.greenScore = green; m.yellowScore = yellow;
        return m;
    }

    public static JSON_GameMessage lobbyState(int teamCount, String status,
                                              int minPlayers, Integer countdownSeconds,
                                              List<LobbyPlayer> players) {
        JSON_GameMessage m = new JSON_GameMessage();
        m.type = "LOBBY_STATE";
        m.teamCount = teamCount;
        m.status = status;
        m.minPlayers = minPlayers;
        m.countdownSeconds = countdownSeconds;
        m.players = players;
        return m;
    }

    public static JSON_GameMessage gameStart(int teamCount, String mapResource,
                                             long seed, List<LobbyPlayer> players) {
        JSON_GameMessage m = new JSON_GameMessage();
        m.type = "GAME_START";
        m.teamCount = teamCount;
        m.mapResource = mapResource;
        m.seed = seed;
        m.players = players;
        return m;
    }

    public static JSON_GameMessage powerUpCollected(int index) {
        JSON_GameMessage m = new JSON_GameMessage();
        m.type = "POWERUP_COLLECTED";
        m.powerUpIndex = index;
        return m;
    }

    public static JSON_GameMessage powerUpRespawn(int batchIndex) {
        JSON_GameMessage m = new JSON_GameMessage();
        m.type = "POWERUP_RESPAWN";
        m.powerUpRespawnBatch = batchIndex;
        return m;
    }

    public static JSON_GameMessage roundEnd(int round, int total, String winner,
                                            int rw, int bw, int gw, int yw) {
        JSON_GameMessage m = new JSON_GameMessage();
        m.type = "ROUND_END";
        m.roundNumber = round; m.totalRounds = total;
        m.roundWinner = winner;
        m.redWins = rw; m.blueWins = bw; m.greenWins = gw; m.yellowWins = yw;
        return m;
    }

    public static JSON_GameMessage roundStart(int round, int total, String map, long seed,
                                              int rw, int bw, int gw, int yw) {
        JSON_GameMessage m = new JSON_GameMessage();
        m.type = "ROUND_START";
        m.roundNumber = round; m.totalRounds = total;
        m.mapResource = map; m.seed = seed;
        m.redWins = rw; m.blueWins = bw; m.greenWins = gw; m.yellowWins = yw;
        return m;
    }
}
