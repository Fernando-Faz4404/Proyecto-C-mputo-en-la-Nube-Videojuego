/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package json;

import java.util.List;
import main.Player;

/**
 *
 * @author brian
 */
public class JSON_LoadPlayers {
    private String type = "LOAD_PLAYERS";
    private List<Player> players;
    
    public JSON_LoadPlayers(List<Player> players) {
        this.players = players;
    }
}
