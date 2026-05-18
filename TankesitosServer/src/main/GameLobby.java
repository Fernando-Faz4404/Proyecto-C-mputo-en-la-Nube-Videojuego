package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameLobby {

    public enum State { WAITING, IN_GAME }

    private static final String[] TEAM_NAMES = { "RED", "BLUE", "GREEN", "YELLOW" };
    private static final int MAX_PER_TEAM = 3;
    private static final int MIN_PER_TEAM = 2;

    public static final int TOTAL_ROUNDS = 3;
    public static final String[] ROUND_MAPS = {
        "/maps/bigBattleMap.txt",
        "/maps/mapaVolcanico.txt",
        "/maps/mapaHielo.txt"
    };

    private State state = State.WAITING;
    private int teamCount = 2;
    private int currentRound = 0; // 0 = not started; 1..TOTAL_ROUNDS during game
    private final int[] roundWins = new int[4]; // 0=RED,1=BLUE,2=GREEN,3=YELLOW

    private final CopyOnWriteArrayList<LobbyEntry> entries = new CopyOnWriteArrayList<>();

    public static class LobbyEntry {
        public final String playerId;
        public final String team;
        public LobbyEntry(String p, String t) { playerId = p; team = t; }
    }

    /** Returns assigned team, or null if lobby is full or game already started. */
    public synchronized String addPlayer(String playerId, int preferredTeamCount) {
        if (state == State.IN_GAME) return null;
        if (entries.isEmpty())
            teamCount = Math.max(2, Math.min(4, preferredTeamCount));
        String team = assignTeam();
        if (team == null) return null;
        entries.add(new LobbyEntry(playerId, team));
        return team;
    }

    public synchronized void removePlayer(String playerId) {
        entries.removeIf(e -> e.playerId.equals(playerId));
    }

    public synchronized boolean isReadyToStart() {
        if (state == State.IN_GAME) return false;
        int[] counts = teamCounts();
        for (int i = 0; i < teamCount; i++) {
            if (counts[i] < MIN_PER_TEAM) return false;
        }
        return true;
    }

    public synchronized void startGame() {
        state = State.IN_GAME;
        currentRound = 1;
    }

    public synchronized void reset() {
        state = State.WAITING;
        entries.clear();
        teamCount = 2;
        currentRound = 0;
        for (int i = 0; i < roundWins.length; i++) roundWins[i] = 0;
    }

    public String getCurrentMapResource() {
        int idx = Math.max(0, Math.min(currentRound - 1, ROUND_MAPS.length - 1));
        return ROUND_MAPS[idx];
    }

    /**
     * Records a round win for the given team and advances the round counter.
     * Returns true if more rounds remain.
     */
    public synchronized boolean advanceRound(String winnerTeam) {
        for (int i = 0; i < TEAM_NAMES.length; i++) {
            if (TEAM_NAMES[i].equals(winnerTeam)) { roundWins[i]++; break; }
        }
        currentRound++;
        return currentRound <= TOTAL_ROUNDS;
    }

    private String assignTeam() {
        int[] counts = teamCounts();
        int minCount = Integer.MAX_VALUE, minIdx = -1;
        for (int i = 0; i < teamCount; i++) {
            if (counts[i] < MAX_PER_TEAM && counts[i] < minCount) {
                minCount = counts[i];
                minIdx = i;
            }
        }
        return minIdx >= 0 ? TEAM_NAMES[minIdx] : null;
    }

    private int[] teamCounts() {
        int[] counts = new int[teamCount];
        for (LobbyEntry e : entries) {
            for (int i = 0; i < teamCount; i++) {
                if (TEAM_NAMES[i].equals(e.team)) { counts[i]++; break; }
            }
        }
        return counts;
    }

    public State getState() { return state; }
    public int getTeamCount() { return teamCount; }
    public int getMinPlayers() { return teamCount * MIN_PER_TEAM; }
    public int getMaxPlayers() { return teamCount * MAX_PER_TEAM; }
    public int getCurrentPlayers() { return entries.size(); }
    public int getCurrentRound() { return currentRound; }
    public int[] getRoundWins() { return roundWins.clone(); }

    /** Kept for backward-compat; returns the map for the current round. */
    public String getMapResource() { return getCurrentMapResource(); }

    public List<LobbyEntry> getEntries() { return new ArrayList<>(entries); }
}
