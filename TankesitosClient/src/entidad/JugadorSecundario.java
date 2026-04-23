package entidad;


public class JugadorSecundario {
    public int playerID;
    public int posX;
    public int posY;
    
    public String playerName;
    
    public String direction;
    
    
    public void setPosX(int x){
        this.posX = x;
    }
    
    public void setPosY(int y){
        this.posY = y;
    }
    
    public void setDireccion(String direccion){
        this.direction = direccion;
    }
    
}
