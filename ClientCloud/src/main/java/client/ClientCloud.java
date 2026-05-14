package client;

import client.entity.Team;
import client.game.GameWindow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ClientCloud {

    static final String DEFAULT_MAP = "/maps/map01.txt";

    public static void main(String[] args) {
        Map<String, String> env = loadEnv();
        String serverIp = env.getOrDefault("SERVER_IP", "10.103.162.37");
        int serverPort = Integer.parseInt(env.getOrDefault("SERVER_PORT", "2555"));

        String playerName = args.length > 0 ? args[0] : "Player" + (int) (Math.random() * 1000);
        Team team = args.length > 1 ? Team.valueOf(args[1].toUpperCase()) : Team.BLUE;
        String map = args.length > 2 ? args[2] : DEFAULT_MAP;

        System.out.println(">>> Conectando a " + serverIp + ":" + serverPort);
        new GameWindow(playerName, team, serverIp, serverPort, map);
    }

    /** Reads key=value pairs from .env file in the working directory. */
    private static Map<String, String> loadEnv() {
        Map<String, String> env = new HashMap<>();
        Path path = Path.of(".env");
        if (!Files.exists(path)) return env;
        try {
            for (String line : Files.readAllLines(path)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq > 0) {
                    env.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        } catch (IOException e) {
            System.out.println("Warning: could not read .env file: " + e.getMessage());
        }
        return env;
    }
}
