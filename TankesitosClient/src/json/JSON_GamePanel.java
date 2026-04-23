package json;

public class JSON_GamePanel {
    public String packetType;
    
    // --- CONFIGURACIÓN DE PANTALLA Y MUNDO ---
    // Se establecen como finales para crear constantes y aumentar la claridad.

    // Tamaño original de los mosaicos (tiles) en píxeles (16x16).
    public int originalTileSize; // constante de configuración
    // Escala a la que se dibujarán los elementos para hacerlos más grandes.
    public int scale; // constante de configuración
    // Tamaño final del mosaico (16 * 3 = 48 píxeles).
    public int tileSize; // constante de configuración

    // Dimensiones de la pantalla en términos de mosaicos.
    public int maxRowScreen; // Filas (renglones) visibles. //constante de configuración
    public int maxColScreen; // Columnas visibles. //constante de configuración

    // Dimensiones de la pantalla en píxeles, calculadas a partir de los mosaicos.
    public int screenWidth; // 48 * 26 = 1248 píxeles
    public int screenHeight; // 48 * 15 = 720 píxeles

    // Dimensiones del mapa del mundo completo en términos de mosaicos.
    public int maxRowWorld; // constante de configuración
    public int maxColWorld; // constante de configuración

    // Dimensiones del mundo en píxeles (no se usan directamente pero son útiles
    // para referencia).
    public int worldWidth;
    public int worldHeight;

    // Fotogramas por segundo (Frames Per Second) a los que se ejecutará el juego.
    public int fps;
    
    public String mapRoute;
    

    // Matriz 2D que representa la estructura del mapa. Cada celda contiene un
    // código que corresponde a un índice en 'arregloTiles'.
//    public int[][] tilesMapCode;
    
}
