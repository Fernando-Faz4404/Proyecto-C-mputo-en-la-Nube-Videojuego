package main;


public class Player {
    private int playerID;
    private String playerName;
    private int posX;
    private int posY;
    private String direction;
    
    public Player(int playerID, String playerName, int posX, int posY, String direction){
        this.playerID = playerID;
        this.playerName = playerName;
        this.posX=posX;
        this.posY = posY;
        this.direction = direction;
    }

    // Getter y Setter para playerID
    public int getPlayerID() {
        return playerID;
    }

    public void setPlayerID(int playerID) {
        this.playerID = playerID;
    }

    // Getter y Setter para playerName
    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    // Getter y Setter para posX
    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    // Getter y Setter para posY
    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    // Getter y Setter para direccion
    public String getDireccion() {
        return direction;
    }

    public void setDirection(String direccion) {
        this.direction = direccion;
    }
}
