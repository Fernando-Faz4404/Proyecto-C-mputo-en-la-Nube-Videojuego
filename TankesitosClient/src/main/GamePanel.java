package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import javax.swing.JPanel;
import entidad.Jugador;
import entidad.JugadorSecundario;
import tile.ManejadorTiles;

import json.JSON_GamePanel;

import java.util.concurrent.ConcurrentHashMap;

public class GamePanel extends JPanel implements Runnable {

	// Identificador único para la serialización de la clase, requerido por JPanel.
	private static final long serialVersionUID = 6872273934923133050L;

	// --- CONFIGURACIÓN DE PANTALLA Y MUNDO ---
	// Se establecen como finales para crear constantes y aumentar la claridad.

	// Tamaño original de los mosaicos (tiles) en píxeles (16x16).
	private int tamanioOriginalTile; // constante de configuración
	// Escala a la que se dibujarán los elementos para hacerlos más grandes.
	private int escala; // constante de configuración
	// Tamaño final del mosaico (16 * 3 = 48 píxeles).
	private int tamanioTile; // constante de configuración

	// Dimensiones de la pantalla en términos de mosaicos.
	private int maxRenPantalla; // Filas (renglones) visibles. //constante de configuración
	private int maxColPantalla; // Columnas visibles. //constante de configuración

	// Dimensiones de la pantalla en píxeles, calculadas a partir de los mosaicos.
	private int anchoPantalla; // 48 * 26 = 1248 píxeles
	private int altoPantalla; // 48 * 15 = 720 píxeles

	// Dimensiones del mapa del mundo completo en términos de mosaicos.
	private int maxRenMundo ; // constante de configuración
	private int maxColMundo; // constante de configuración

	// Dimensiones del mundo en píxeles (no se usan directamente pero son útiles
	// para referencia).
	private int anchoMundo;
	private int altoMundo;

	// Fotogramas por segundo (Frames Per Second) a los que se ejecutará el juego.
	private int FPS; // constante de configuración

	// --- COMPONENTES DEL JUEGO ---
	// Se establecen como finales para prevenir reasignaciones accidentales.

	// El hilo principal que ejecutará el bucle del juego (game loop).
	private Thread hebraJuego;
	// Instancia del manejador de teclado para procesar la entrada del usuario.
	private ManejadorTeclas mT = new ManejadorTeclas();
	// Instancia del manejador de colisiones.
	private DetectorColisiones dC;
        
        //Mapa para contener a tods los jugadores        
        // Llave: ID del jugador (int), Valor: Objeto con sus datos (x, y, nombre, sprite)
        public ConcurrentHashMap<Integer, JugadorSecundario> otrosJugadores = new ConcurrentHashMap<>();
        
	// Instancia del jugador, el personaje principal.
	private Jugador jugador;
	// Instancia del manejador de mosaicos, que gestiona el mapa.
	private ManejadorTiles mTi;
        
        
//      Clase Cliente para mandar a llamar funciones del cliente
        private Cliente cliente;

	/**
	 * Constructor de GamePanel. Configura las propiedades iniciales del panel.
	 */
	public GamePanel() {
            // Establece un color de fondo, visible si algo no se dibuja correctamente.
            this.setBackground(Color.BLACK);
            // Activa el doble búfer para un renderizado más suave y sin parpadeos
            // (flickering).
            this.setDoubleBuffered(true);
            // Añade el manejador de teclas como "oyente" de eventos de teclado en este
            // panel.
            this.addKeyListener(mT);
            // Permite que el panel reciba el "foco" del sistema para poder capturar teclas.
            this.setFocusable(true);
	}
        
        public void setearValoresGamePanel(JSON_GamePanel data){
            this.tamanioOriginalTile = data.originalTileSize;
            this.escala = data.scale;
            this.tamanioTile = data.tileSize;
            this.maxRenPantalla = data.maxRowScreen;
            this.maxColPantalla = data.maxColScreen;
            this.anchoPantalla = data.screenWidth;
            this.altoPantalla = data.screenHeight;
            this.maxRenMundo = data.maxRowWorld;
            this.maxColMundo = data.maxColWorld;
            this.altoMundo = data.worldHeight;
            this.anchoMundo = data.worldWidth;
            
            this.FPS = data.fps;
            
//          Teniendo la ruta del mapa desde el servidor ahora si lo genero
            this.mTi = new ManejadorTiles(this, data.mapRoute);
            // Instancia del manejador de colisiones.
            this.dC = new DetectorColisiones(this);
            // Instancia del jugador, el personaje principal.
            this.jugador = new Jugador(this, this.mT);
            
            // Establece las dimensiones preferidas del panel.
            this.setPreferredSize(new Dimension(this.anchoPantalla, this.altoPantalla));
            
        }

	/**
	 * Crea e inicia el hilo principal del juego.
	 */
	public void iniciaHebraJuego() {
            // Crea una nueva instancia de Thread, pasando este panel (que es un Runnable).
            this.hebraJuego = new Thread(this);
            // Inicia la ejecución del hilo, que llamará automáticamente al método run().
            this.hebraJuego.start();
	}

	/**
	 * El bucle principal del juego (Game Loop). Se ejecuta en un hilo separado para
	 * controlar la actualización y el redibujado a una velocidad constante (FPS).
	 */
	@Override
	public void run() {
            // Calcula cada cuánto tiempo debe ocurrir un fotograma en nanosegundos.
            double intervaloDibujo = 1000000000.0 / this.FPS; // 1 segundo = 1,000,000,000 nanosegundos.
            // Delta time: acumulador para controlar cuándo actualizar.
            double delta = 0;
            // Almacena el tiempo del último ciclo para calcular el tiempo transcurrido.
            long ultimaVez = System.nanoTime();
            // Almacenará el tiempo en el ciclo actual.
            long tiempoActual;

            // Bucle principal que se ejecuta mientras el hilo del juego exista (no sea
            // null).
            while (this.hebraJuego != null) {
                    // Captura el tiempo actual al inicio del ciclo.
                    tiempoActual = System.nanoTime();
                    // Acumula la proporción de tiempo transcurrido respecto al intervalo de dibujo.
                    delta += (tiempoActual - ultimaVez) / intervaloDibujo;
                    // Actualiza 'ultimaVez' para el próximo ciclo.
                    ultimaVez = tiempoActual;

                    // Si ha pasado suficiente tiempo para al menos un fotograma...
                    if (delta >= 1) {
                            // 1. Actualiza la lógica del juego (movimiento, colisiones, IA, etc.).
                            this.update();
                            // 2. Vuelve a dibujar todos los elementos en pantalla (llama a paintComponent).
                            this.repaint();
                            // Reduce el delta en 1, manteniendo cualquier fracción para el siguiente ciclo.
                            delta--;
                    }
            }
	}

	/**
	 * Actualiza el estado de todos los elementos del juego. Se llama una vez por
	 * cada fotograma desde el bucle del juego.
	 */
	public void update() {
		// Llama al método update del jugador para actualizar su estado.
		this.jugador.update();
		// En el futuro, aquí se actualizarían enemigos, NPCs, etc.
	}

	/**
	 * Dibuja todos los componentes del juego en el panel. Este método es llamado
	 * automáticamente por Swing cada vez que se invoca repaint(). * @param g El
	 * contexto gráfico proporcionado por Swing para dibujar.
	 */
	@Override
	public void paintComponent(Graphics g) {
            // Llama al método original de JPanel para limpiar el panel antes de dibujar.
            super.paintComponent(g);
            // Convierte el objeto Graphics a Graphics2D para tener más control y
            // herramientas avanzadas.
            Graphics2D g2 = (Graphics2D) g;

            // Dibuja el mapa primero para que sirva de fondo.
            this.mTi.draw(g2);

            // Dibuja al jugador después, para que aparezca sobre el mapa.
            this.jugador.draw(g2);
            
            // En el paintComponent de GamePanel.java
            for (JugadorSecundario p : otrosJugadores.values()) {
                // 1. Calcular posición relativa a mi cámara
                int screenX = p.posX - jugador.getMundoX() + jugador.getPantallaX();
                int screenY = p.posY - jugador.getMundoY() + jugador.getPantallaY();

                // 2. Solo dibujar si está dentro de lo que veo (Culling)
                if (screenX + getTamanioTile() > 0 && screenX < getAnchoPantalla() &&
                    screenY + getTamanioTile() > 0 && screenY < getAltoPantalla()) {

                    // 3. Pintar el sprite (puedes usar un cuadro por ahora para probar)
                    g2.setColor(Color.RED); 
                    g2.fillRect(screenX, screenY, getTamanioTile(), getTamanioTile());

                    // Opcional: Dibujar su nombre arriba
                    g2.setColor(Color.WHITE);
                    g2.drawString(p.playerName, screenX, screenY - 5);
                }
            }

            // Libera los recursos del sistema utilizados por el objeto Graphics2D. Es una
            // buena práctica.
            g2.dispose();
	}
        
//      ===SETTERS
        public void setCliente(Cliente cliente) {
            this.cliente = cliente;
        }

	// --- GETTERS ---
	// Métodos públicos para que otras clases puedan acceder a las configuraciones
	// del panel.

	/** @return El tamaño final de un mosaico en píxeles. */
	public int getTamanioTile() {
		return this.tamanioTile;
	}

	/** @return El número máximo de filas de mosaicos visibles en pantalla. */
	public int getMaxRenPantalla() {
		return this.maxRenPantalla;
	}

	/** @return El número máximo de columnas de mosaicos visibles en pantalla. */
	public int getMaxColPantalla() {
		return this.maxColPantalla;
	}

	/** @return El ancho total de la pantalla en píxeles. */
	public int getAnchoPantalla() {
		return this.anchoPantalla;
	}

	/** @return El alto total de la pantalla en píxeles. */
	public int getAltoPantalla() {
		return this.altoPantalla;
	}

	/** @return El número máximo de filas de mosaicos en el mundo. */
	public int getMaxRenMundo() {
		return this.maxRenMundo;
	}

	/** @return El número máximo de columnas de mosaicos en el mundo. */
	public int getMaxColMundo() {
		return this.maxColMundo;
	}

	/** @return La instancia del objeto Jugador. */
	public Jugador getJugador() {
		return this.jugador;
	}

	/** @return El ancho total del mundo en píxeles. */
	public int getAnchoMundo() {
		return this.anchoMundo;
	}

	/** @return El alto total del mundo en píxeles. */
	public int getAltoMundo() {
		return this.altoMundo;
	}

	/** @return La instancia del DetectorColisiones. */
	public DetectorColisiones getDetectorColisiones() {
		return this.dC;
	}

	/** @return La instancia del ManejadorTiles. */
	public ManejadorTiles getManejadorTiles() {
		return this.mTi;
	}
        
        /** @return La instancia de Cliente. */
	public Cliente getCliente() {
		return this.cliente;
	}
}