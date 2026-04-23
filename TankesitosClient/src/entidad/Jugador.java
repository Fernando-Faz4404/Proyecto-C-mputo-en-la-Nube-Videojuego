package entidad;
import com.google.gson.Gson;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import json.JSON_Player;
import main.GamePanel;
import main.ManejadorTeclas;

/**
 * @author Los Ratones
 * @version 0.5
 * @since 11 de julio de 2025, 09:00 horas (horario de la Ciudad de México) *
 *        Representa al personaje principal controlado por el usuario. Hereda
 *        las propiedades de la clase Entidad y añade la lógica para responder a
 *        las entradas del teclado y dibujarse en el centro de la pantalla.
 */
public class Jugador extends Entidad {

	// --- REFERENCIAS A COMPONENTES PRINCIPALES ---
	// Se establecen como finales para asegurar la "Inmutabilidad de la Referencia".
	// El objeto Jugador nace con una conexión a un panel específico, y esa conexión
	// no debe romperse.
	private final GamePanel gP;
	// El objeto Jugador nace con una conexión a un controlador de teclas
	// específico, y esa conexión no debe romperse.
	private final ManejadorTeclas mT;

	// --- COORDENADAS EN PANTALLA ---
	// Coordenadas FINALES y FIJAS del jugador en la PANTALLA.
	// El jugador siempre se dibuja en el centro; es el mapa el que se mueve.
	private final int pantallaX;
	private final int pantallaY;

	/**
	 * Constructor de la clase Jugador. * @param gP Referencia al {@link GamePanel}
	 * principal.
	 * 
	 * @param mT Referencia al {@link ManejadorTeclas} para la entrada del usuario.
	 */
	public Jugador(GamePanel gP, ManejadorTeclas mT) {
		// Asigna la referencia del GamePanel principal.
		this.gP = gP;
		// Asigna la referencia del ManejadorTeclas.
		this.mT = mT;

		// Calcula la posición X central en la pantalla.
		// Se resta la mitad del tamaño del mosaico para que el CENTRO del jugador quede
		// en el centro de la pantalla.
		this.pantallaX = gP.getAnchoPantalla() / 2 - (gP.getTamanioTile() / 2);
		// Calcula la posición Y central en la pantalla.
		this.pantallaY = gP.getAltoPantalla() / 2 - (gP.getTamanioTile() / 2);

		// Define el área sólida (hitbox) del jugador. new Rectangle(x, y, ancho, alto)
		// relativo a la esquina superior izquierda del sprite.
		this.areaSolida = new Rectangle(8, 16, 32, 32);

		// Llama a los métodos para establecer los valores iniciales y cargar los
		// gráficos.
		this.configuracionInicial(); // Establece la posición, velocidad, etc.
		this.getSpritesJugador(); // Carga las imágenes del personaje.
	}

	/**
	 * Establece los valores por defecto del jugador al iniciar el juego.
	 */
	public void configuracionInicial() {
		// Posición inicial del jugador en el mapa del MUNDO (coordenadas X).
		this.mundoX = gP.getTamanioTile() * 22;
		// Posición inicial del jugador en el mapa del MUNDO (coordenadas Y).
		this.mundoY = gP.getTamanioTile() * 30;
		// Velocidad de movimiento del jugador en píxeles por fotograma.
		this.velocidad = 4;
		// Dirección inicial a la que mira el jugador.
		this.direccion = "abajo";
	}

	/**
	 * Carga las imágenes (sprites) del jugador desde la carpeta de recursos.
	 */
	public void getSpritesJugador() {
		try {
			// getClass().getResourceAsStream() es la forma estándar de acceder a recursos
			// dentro del proyecto.
			// Carga el primer sprite para el movimiento hacia arriba.
			this.arriba1 = ImageIO.read(getClass().getResourceAsStream("/spritesjugador/moverArriba1.png"));
			// Carga el segundo sprite para el movimiento hacia arriba.
			this.arriba2 = ImageIO.read(getClass().getResourceAsStream("/spritesjugador/moverArriba2.png"));
			// Carga el primer sprite para el movimiento hacia abajo.
			this.abajo1 = ImageIO.read(getClass().getResourceAsStream("/spritesjugador/moverAbajo1.png"));
			// Carga el segundo sprite para el movimiento hacia abajo.
			this.abajo2 = ImageIO.read(getClass().getResourceAsStream("/spritesjugador/moverAbajo2.png"));
			// Carga el primer sprite para el movimiento hacia la izquierda.
			this.izquierda1 = ImageIO.read(getClass().getResourceAsStream("/spritesjugador/moverIzquierda1.png"));
			// Carga el segundo sprite para el movimiento hacia la izquierda.
			this.izquierda2 = ImageIO.read(getClass().getResourceAsStream("/spritesjugador/moverIzquierda2.png"));
			// Carga el primer sprite para el movimiento hacia la derecha.
			this.derecha1 = ImageIO.read(getClass().getResourceAsStream("/spritesjugador/moverDerecha1.png"));
			// Carga el segundo sprite para el movimiento hacia la derecha.
			this.derecha2 = ImageIO.read(getClass().getResourceAsStream("/spritesjugador/moverDerecha2.png"));
		} catch (IOException e) {
			// Si ocurre un error al cargar las imágenes (ej: archivo no encontrado), se
			// imprime el error.
			e.printStackTrace();
		}
	}

	/**
	 * Actualiza el estado del jugador en cada fotograma. Gestiona el movimiento y
	 * la animación.
	 */
	public void update() {
		// Solo procesa el movimiento y la animación si alguna tecla de dirección está
		// pulsada.
		if (mT.getTeclaArriba() || mT.getTeclaAbajo() || mT.getTeclaIzquierda() || mT.getTeclaDerecha()) {

			// Comprueba la tecla pulsada y establece la dirección correspondiente.
			if (this.mT.getTeclaArriba()) {
				// Establece la dirección a "arriba". El movimiento se gestiona más adelante.
				this.direccion = "arriba";
			} else if (this.mT.getTeclaAbajo()) {
				// Establece la dirección a "abajo".
				this.direccion = "abajo";
			} else if (this.mT.getTeclaIzquierda()) {
				// Establece la dirección a "izquierda".
				this.direccion = "izquierda";
			} else if (this.mT.getTeclaDerecha()) {
				// Establece la dirección a "derecha".
				this.direccion = "derecha";
			}

			// Reinicia la bandera de colisión antes de cada comprobación.
			this.colisionActivada = false;
			// Llama al detector de colisiones para que revise si hay un obstáculo en la
			// dirección actual.
			this.gP.getDetectorColisiones().revisaTile(this);

			// Si la bandera de colisión NO fue activada por el detector...
			if (this.colisionActivada == false) {
				// ... permite que el jugador se mueva.
				switch (this.getDireccion()) {
				case "arriba":
					// Mueve al jugador hacia arriba en el mapa del mundo.
					this.setMundoY(this.getMundoY() - this.getVelocidad());
					break;
				case "abajo":
					// Mueve al jugador hacia abajo en el mapa del mundo.
					this.setMundoY(this.getMundoY() + this.getVelocidad());
					break;
				case "izquierda":
					// Mueve al jugador hacia la izquierda en el mapa del mundo.
					this.setMundoX(this.getMundoX() - this.getVelocidad());
					break;
				case "derecha":
					// Mueve al jugador hacia la derecha en el mapa del mundo.
					this.setMundoX(this.getMundoX() + this.getVelocidad());
					break;
				default:
					break; // Caso por defecto, no hace nada.
				}
			}

			// Incrementa el contador de sprites para la animación en cada fotograma de
			// movimiento.
			this.contadorSprites++;
			// Lógica para cambiar el fotograma de la animación cada cierto número de
			// updates.
			if (this.contadorSprites > this.cambiaSprite) {
				// Si el sprite actual es el 1, cambia al 2.
				if (this.numeroSprites == 1) {
					this.numeroSprites = 2;
				} else { // Si es el 2 (o cualquier otro valor), vuelve al 1.
					this.numeroSprites = 1;
				}
				// Reinicia el contador para el próximo cambio de sprite.
				this.contadorSprites = 0;
			}
                        
                    actualizarJugadorAlServidor();
		}
	}

	/**
	 * Dibuja al jugador en la pantalla. * @param g2 El contexto gráfico
	 * {@link Graphics2D} para dibujar.
	 */
	public void draw(Graphics2D g2) {
		// Declara una variable para almacenar el sprite que se va a dibujar.
		BufferedImage sprite = null;

		// Selecciona el sprite correcto basado en la dirección y el número de sprite
		// actual.
		switch (this.direccion) {
		case "arriba":
			// Usa el operador ternario para elegir entre el sprite 1 y 2 de "arriba".
			sprite = (this.numeroSprites == 1) ? this.arriba1 : this.arriba2;
			break;
		case "abajo":
			// Elige entre el sprite 1 y 2 de "abajo".
			sprite = (this.numeroSprites == 1) ? this.abajo1 : this.abajo2;
			break;
		case "izquierda":
			// Elige entre el sprite 1 y 2 de "izquierda".
			sprite = (this.numeroSprites == 1) ? this.izquierda1 : this.izquierda2;
			break;
		case "derecha":
			// Elige entre el sprite 1 y 2 de "derecha".
			sprite = (this.numeroSprites == 1) ? this.derecha1 : this.derecha2;
			break;
		}

		// Dibuja el sprite seleccionado en las coordenadas FIJAS de la pantalla.
		// El jugador siempre está en el centro; el mapa se mueve a su alrededor.
		g2.drawImage(sprite, this.pantallaX, this.pantallaY, this.gP.getTamanioTile(), this.gP.getTamanioTile(), null);
	}
        
        public void actualizarJugadorAlServidor(){
            JSON_Player jp = new JSON_Player();
     
            jp.id = this.gP.getCliente().getIdCliente();
            jp.posX = this.mundoX;
            jp.posY= this.mundoY;
            jp.direccion = this.direccion;
            
//            jp.playerName =this.gP.getCliente().getNombreCliente();
            
            String json = new Gson().toJson(jp);
            gP.getCliente().enviaMensaje(json);
        }
        

	// --- GETTERS Y SETTERS ---
	// Proporcionan acceso controlado a las propiedades del jugador.

	/** @return La coordenada X del jugador en el mundo. */
	public int getMundoX() {
		return this.mundoX;
	}

	/** @param valor La nueva coordenada X del jugador en el mundo. */
	public void setMundoX(int valor) {
		this.mundoX = valor;
	}

	/** @return La coordenada Y del jugador en el mundo. */
	public int getMundoY() {
		return this.mundoY;
	}

	/** @param valor La nueva coordenada Y del jugador en el mundo. */
	public void setMundoY(int valor) {
		this.mundoY = valor;
	}

	/** @return La velocidad de movimiento del jugador. */
	public int getVelocidad() {
		return this.velocidad;
	}

	/** @return La coordenada X fija del jugador en la pantalla. */
	public int getPantallaX() {
		return this.pantallaX;
	}

	/** @return La coordenada Y fija del jugador en la pantalla. */
	public int getPantallaY() {
		return this.pantallaY;
	}

	/** @return La coordenada X del área sólida (hitbox) relativa al sprite. */
	public int getAreaSolidaX() {
		return this.areaSolida.x;
	}

	/** @return La coordenada Y del área sólida (hitbox) relativa al sprite. */
	public int getAreaSolidaY() {
		return this.areaSolida.y;
	}

	/** @return El ancho del área sólida (hitbox). */
	public int getAreaSolidaAncho() {
		return this.areaSolida.width;
	}

	/** @return El alto del área sólida (hitbox). */
	public int getAreaSolidaAlto() {
		return this.areaSolida.height;
	}

	/** @return La dirección actual del jugador como un String. */
	public String getDireccion() {
		return this.direccion;
	}
        
}
