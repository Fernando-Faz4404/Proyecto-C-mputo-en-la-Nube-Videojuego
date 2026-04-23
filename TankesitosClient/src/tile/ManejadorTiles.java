/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tile;


import java.awt.Graphics2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.imageio.ImageIO;
import main.GamePanel;

/**
 * @author Los Ratones
 * @version 0.5
 * @since 11 de julio de 2025, 09:00 horas (horario de la Ciudad de México) *
 *        Gestiona todo lo relacionado con el mapa del juego. Carga las imágenes
 *        de los mosaicos, lee el archivo del mapa y se encarga de dibujar el
 *        mapa en la pantalla.
 */
public class ManejadorTiles {
	// Se establece como final para Prevenir Errores al asignar por error una nueva
	// instancia.
	// El objeto nace con una conexión a un panel específico y no debe romperse.
	private final GamePanel gP;
	// Número máximo de tipos diferentes de mosaicos que el juego puede manejar.
	private final int maxTiles = 10; // constante de configuración

	// El array (catálogo) de mosaicos. 'final' impide reasignar el array, pero su
	// contenido puede cambiar.
	private final Tile[] arregloTiles;
	// Matriz 2D que representa la estructura del mapa. Cada celda contiene un
	// código que corresponde a un índice en 'arregloTiles'.
	private final int[][] codigosMapaTiles;

	/**
	 * Constructor del ManejadorTiles. * @param gP Referencia al {@link GamePanel}
	 * principal.
	 */
	public ManejadorTiles(GamePanel gP, String ruta) {
		// Asigna la referencia del GamePanel.
		this.gP = gP;
		// Inicializa el array para almacenar los tipos de mosaicos, con el tamaño
		// máximo definido.
		this.arregloTiles = new Tile[this.maxTiles];
		// Inicializa la matriz del mapa con las dimensiones del mundo definidas en
		// GamePanel.
		this.codigosMapaTiles = new int[gP.getMaxRenMundo()][gP.getMaxColMundo()];

		// Carga las imágenes de los mosaicos en la paleta.
		getImagenesTile();
		// Carga la estructura del mapa desde un archivo de texto.
		cargaMapa(ruta);
                
//                "/mapas/mundo01.txt"
	}

	/**
	 * Carga las imágenes de cada tipo de mosaico y las almacena en el
	 * 'arregloTiles', definiendo también sus propiedades como la colisión.
	 */
	public void getImagenesTile() {
		try {
			// El índice (0, 1, 2...) es el código que se usará en el archivo de mapa.

			// Mosaico 0: Agua (con colisión)
			this.arregloTiles[0] = new Tile(); // Crea una nueva instancia de Tile.
			this.arregloTiles[0].setImagen(ImageIO.read(getClass().getResourceAsStream("/tiles/agua.png"))); // Carga la
																												// imagen.
			this.arregloTiles[0].setColision(true); // Establece que este mosaico es sólido.

			// Mosaico 1: Árbol (con colisión)
			this.arregloTiles[1] = new Tile(); // Crea una nueva instancia de Tile.
			this.arregloTiles[1].setImagen(ImageIO.read(getClass().getResourceAsStream("/tiles/arbol.png"))); // Carga
																												// la
																												// imagen.
			this.arregloTiles[1].setColision(true); // Establece que este mosaico es sólido.

			// Mosaico 2: Arena (sin colisión)
			this.arregloTiles[2] = new Tile(); // Crea una nueva instancia de Tile.
			this.arregloTiles[2].setImagen(ImageIO.read(getClass().getResourceAsStream("/tiles/arena.png"))); // Carga
																												// la
																												// imagen.

			// Mosaico 3: Muro (con colisión)
			this.arregloTiles[3] = new Tile(); // Crea una nueva instancia de Tile.
			this.arregloTiles[3].setImagen(ImageIO.read(getClass().getResourceAsStream("/tiles/muro.png"))); // Carga la
																												// imagen.
			this.arregloTiles[3].setColision(true); // Establece que este mosaico es sólido.

			// Mosaico 4: Pasto (sin colisión)
			this.arregloTiles[4] = new Tile(); // Crea una nueva instancia de Tile.
			this.arregloTiles[4].setImagen(ImageIO.read(getClass().getResourceAsStream("/tiles/pasto.png"))); // Carga
																												// la
																												// imagen.

			// Mosaico 5: Suelo (sin colisión)
			this.arregloTiles[5] = new Tile(); // Crea una nueva instancia de Tile.
			this.arregloTiles[5].setImagen(ImageIO.read(getClass().getResourceAsStream("/tiles/suelo.png"))); // Carga
																												// la
																												// imagen.

		} catch (IOException e) {
			// Si ocurre un error al cargar las imágenes, se imprime la traza del error en
			// la consola.
			e.printStackTrace();
		}
	}

	/**
	 * Lee un archivo de texto que define el mapa y carga los códigos en la matriz
	 * 'codigosMapaTiles'. * @param ruta La ruta al archivo del mapa dentro de los
	 * recursos del proyecto (ej: "/mapas/mapa.txt").
	 */
	public void cargaMapa(String ruta) {
		try {
			// Abre el archivo de mapa como un flujo de entrada.
			InputStream mapa = getClass().getResourceAsStream(ruta);
			// Envuelve el flujo en un BufferedReader para leer texto de manera eficiente.
			BufferedReader br = new BufferedReader(new InputStreamReader(mapa));
			// Inicializa los contadores de fila y columna.
			int ren = 0, col = 0;

			// Recorre las filas y columnas del mapa.
			while (ren < gP.getMaxRenMundo() && col < gP.getMaxColMundo()) {
				// Lee una línea completa del archivo (ej: "4 4 4 5 5 2...").
				String renglonDatos = br.readLine();
				// Recorre las columnas de la línea leída.
				while (col < gP.getMaxColMundo()) {
					// Divide la línea en números individuales usando el espacio como separador.
					String[] codigos = renglonDatos.split(" ");
					// Convierte el código de tipo String a un entero.
					int codigo = Integer.parseInt(codigos[col]);
					// Asigna el código a la celda correspondiente en la matriz del mapa.
					this.codigosMapaTiles[ren][col] = codigo;
					// Avanza a la siguiente columna.
					col++;
				}
				// Si terminamos una fila, pasamos a la siguiente y reiniciamos el contador de
				// columnas.
				if (col == gP.getMaxColMundo()) {
					// Avanza a la siguiente fila.
					ren++;
					// Reinicia el contador de columnas a 0.
					col = 0;
				}
			}
			// Cierra el lector de archivo para liberar recursos.
			br.close();
		} catch (IOException e) {
			// Si ocurre un error de entrada/salida, imprime la traza del error.
			e.printStackTrace();
		}
	}

	/**
	 * Dibuja la porción visible del mapa en la pantalla, optimizado para dibujar
	 * únicamente los mosaicos dentro del campo de visión de la cámara. * @param g2
	 * El contexto gráfico {@link Graphics2D} para dibujar.
	 */
	public void draw(Graphics2D g2) {
		// Inicializa las variables para recorrer la matriz del mapa del mundo.
		int renMundo = 0, colMundo = 0;

		// Inicia un bucle que recorrerá cada celda de la matriz del mapa.
		while (renMundo < this.gP.getMaxRenMundo() && colMundo < this.gP.getMaxColMundo()) {

			// Obtiene el número (código) que identifica el tipo de mosaico para la posición
			// actual.
			int codigoTile = this.codigosMapaTiles[renMundo][colMundo];

			// Calcula la coordenada X del mosaico en el mapa del MUNDO completo.
			int mundoX = colMundo * gP.getTamanioTile();
			// Calcula la coordenada Y del mosaico en el mapa del MUNDO completo.
			int mundoY = renMundo * gP.getTamanioTile();

			// --- CÁLCULO DE LA CÁMARA ---
			// Calcula la coordenada X donde se debe dibujar el mosaico en la PANTALLA.
			// Esto crea el efecto de que la cámara sigue al jugador.
			int pantallaX = mundoX - gP.getJugador().getMundoX() + gP.getJugador().getPantallaX();
			// Calcula la coordenada Y donde se debe dibujar el mosaico en la PANTALLA.
			int pantallaY = mundoY - gP.getJugador().getMundoY() + gP.getJugador().getPantallaY();

			// --- CONDICIÓN DE OPTIMIZACIÓN (Culling) ---
			// Comprueba si el mosaico actual está dentro del área visible de la cámara
			// antes de dibujarlo.
			if (mundoX + gP.getTamanioTile() > gP.getJugador().getMundoX() - gP.getJugador().getPantallaX() && // El
																												// lado
																												// derecho
																												// del
																												// mosaico
																												// es
																												// visible.
					mundoX - gP.getTamanioTile() < gP.getJugador().getMundoX() + gP.getJugador().getPantallaX() && // El
																													// lado
																													// izquierdo
																													// del
																													// mosaico
																													// es
																													// visible.
					mundoY + gP.getTamanioTile() > gP.getJugador().getMundoY() - gP.getJugador().getPantallaY() && // El
																													// lado
																													// inferior
																													// del
																													// mosaico
																													// es
																													// visible.
					mundoY - gP.getTamanioTile() < gP.getJugador().getMundoY() + gP.getJugador().getPantallaY() // El
																												// lado
																												// superior
																												// del
																												// mosaico
																												// es
																												// visible.
			) {
				// Si el mosaico está visible, lo dibuja en las coordenadas de pantalla
				// calculadas.
				g2.drawImage(this.arregloTiles[codigoTile].getImagen(), pantallaX, pantallaY, this.gP.getTamanioTile(),
						this.gP.getTamanioTile(), null);
			}

			// Avanza a la siguiente columna del mapa.
			colMundo++;

			// Si se llega al final de una fila...
			if (colMundo == this.gP.getMaxColMundo()) {
				// ...se resetea el contador de columnas a 0...
				colMundo = 0;
				// ...y se avanza a la siguiente fila.
				renMundo++;
			}
		}
	}

	/**
	 * Obtiene el código de un mosaico en una posición específica del mapa.
	 * 
	 * @param ren La fila (renglón) del mapa.
	 * @param col La columna del mapa.
	 * @return El código entero del mosaico en esa posición.
	 */
	public int getCodigoMapaTiles(int ren, int col) {
		// Devuelve el valor almacenado en la matriz de códigos del mapa.
		return this.codigosMapaTiles[ren][col];
	}

	/**
	 * Verifica si un tipo de mosaico, identificado por su índice, tiene colisión.
	 * 
	 * @param index El índice (código) del mosaico en el {@code arregloTiles}.
	 * @return {@code true} si el mosaico en ese índice tiene colisión,
	 *         {@code false} de lo contrario.
	 */
	public boolean getColisionDeTile(int index) {
		// Devuelve la propiedad de colisión del mosaico correspondiente al índice.
		return this.arregloTiles[index].getColision();
	}
}