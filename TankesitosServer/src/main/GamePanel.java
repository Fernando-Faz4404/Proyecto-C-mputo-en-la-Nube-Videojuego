package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class GamePanel{
    
    private final String type = "MAP_INIT";
    
    // --- CONFIGURACIÓN DE PANTALLA Y MUNDO ---
    // Se establecen como finales para crear constantes y aumentar la claridad.

    // Tamaño original de los mosaicos (tiles) en píxeles (16x16).
    private final int originalTileSize = 16; // constante de configuración
    // Escala a la que se dibujarán los elementos para hacerlos más grandes.
    private final int scale = 3; // constante de configuración
    // Tamaño final del mosaico (16 * 3 = 48 píxeles).
    private final int tileSize = this.originalTileSize * this.scale; // constante de configuración

    // Dimensiones de la pantalla en términos de mosaicos.
    private final int maxRowScreen = 15; // Filas (renglones) visibles. //constante de configuración
    private final int maxColScreen = 26; // Columnas visibles. //constante de configuración

    // Dimensiones de la pantalla en píxeles, calculadas a partir de los mosaicos.
    private final int screenWidth = this.tileSize * this.maxColScreen; // 48 * 26 = 1248 píxeles
    private final int screenHeight = this.tileSize * this.maxRowScreen; // 48 * 15 = 720 píxeles

    // Dimensiones del mapa del mundo completo en términos de mosaicos.
    private final int maxRowWorld = 50; // constante de configuración
    private final int maxColWorld = 50; // constante de configuración

    // Dimensiones del mundo en píxeles (no se usan directamente pero son útiles
    // para referencia).
    private final int worldWidth = this.tileSize * this.maxColWorld;
    private final int worldHeight = this.tileSize * this.maxRowWorld;

    // Fotogramas por segundo (Frames Per Second) a los que se ejecutará el juego.
    private final int fps = 60;

    // Matriz 2D que representa la estructura del mapa. Cada celda contiene un
    // código que corresponde a un índice en 'arregloTiles'.
    private final int[][] tilesMapCode;
    
    public String mapRoute = "/mapas/mundo01.txt";


  public GamePanel (){
    this.tilesMapCode = new int[this.maxRowWorld][this.maxColWorld];

    // Carga la estructura del mapa desde un archivo de texto.
    this.loadMap("/mapas/mundo01.txt");
  }


    public void loadMap(String ruta){
        try {
            // Abre el archivo de mapa como un flujo de entrada.
            InputStream mapa = getClass().getResourceAsStream(ruta);
            // Envuelve el flujo en un BufferedReader para leer texto de manera eficiente.
            BufferedReader br = new BufferedReader(new InputStreamReader(mapa));
            // Inicializa los contadores de fila y columna.
            int row = 0;

            while (row < this.maxRowWorld) {
                String dataRow = br.readLine();
                if (dataRow == null) break; // Seguridad por si el archivo es más corto

                String[] codes = dataRow.split(" "); // Split una sola vez por fila

                for (int col = 0; col < this.maxColWorld; col++) {
                    this.tilesMapCode[row][col] = Integer.parseInt(codes[col]);
                }
                row++;
            }
            // Cierra el lector de archivo para liberar recursos.
            br.close();
        } catch (IOException e) {
            // Si ocurre un error de entrada/salida, imprime la traza del error.
            e.printStackTrace();
        }
    }
    
    // Solo Getter para constantes (no pueden cambiar)
    public int getTileSize() {
        return tileSize;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    // Getter y Setter para la ruta (esta sí podría cambiar si cargas otro nivel)
    public String getMapRoute() {
        return mapRoute;
    }

    public void setMapRoute(String mapRoute) {
        this.mapRoute = mapRoute;
    }

}
