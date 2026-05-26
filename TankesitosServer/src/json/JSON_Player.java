package json;

public class JSON_Player {
// PLAYER_MOVE: utilizado para indicar que un jugador que no eres tu se ha movido
// OWNEDPLAYER_MOVE: utilizado para indicar que tu te has movido
// NEW_PLAYER: utilizado para indicar que un nuevo jugador se ha unido
    public String type = "PLAYER_MOVE";
    public int id;
    public int posX;
    public int posY;
// public String playerName;
    public String direccion;
}
