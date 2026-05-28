package json;


public class JSON_NewPlayer {
    private String type = "NEW_PLAYER";
    private int playerID;
    private String playerName;
    private int posX;
    private int posY;
    
    
    public JSON_NewPlayer(int id, String name, int x, int y){ 
        this.playerID = id; this.playerName = name; this.posX=x; this.posY=y;
    }
    
}
