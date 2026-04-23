package main;

import entidad.Entidad;
import entidad.Jugador;
import main.GamePanel;


public class DetectorColisiones {
	// Referencia al panel principal del juego (GamePanel) para acceder a sus
	// componentes y configuraciones.
	private GamePanel gP;

	/**
	 * Constructor de la clase DetectorColisiones. * @param gP La instancia de
	 * {@link GamePanel} a la que este detector estará asociado.
	 */
	public DetectorColisiones(GamePanel gP) {
		// Asigna la referencia del GamePanel recibida a la variable local.
		this.gP = gP;
	}

	/**
	 * Revisa si la entidad colisionará con un mosaico sólido en su próxima
	 * posición. *
	 * <p>
	 * <b>ADVERTENCIA DE IMPLEMENTACIÓN:</b> Este método presenta una falla lógica
	 * conocida como "tunneling" o "snagging". La revisión de colisión se basa
	 * únicamente en la comprobación de <b>dos puntos</b> (las esquinas) del área de
	 * colisión del jugador. Si un obstáculo de un solo mosaico se encuentra entre
	 * estos dos puntos, la colisión no será detectada, permitiendo que el jugador
	 * lo atraviese.
	 * </p>
	 * <p>
	 * Una solución robusta implicaría: <br>
	 * 1. Separar la detección y movimiento del eje X y del eje Y. <br>
	 * 2. Comprobar un rango de puntos a lo largo de todo el borde del área de
	 * colisión, no solo las esquinas.
	 * </p>
	 * * @param entidad La {@link Entidad} (específicamente un {@link Jugador}) que
	 * se va a verificar.
	 */
	public void revisaTile(Entidad entidad) {
		// Verifica si la entidad proporcionada es una instancia de la clase Jugador.
		if (entidad instanceof Jugador) {
			// Calcula la coordenada X del lado izquierdo del área de colisión de la entidad
			// en el mundo.
			int izquierdaEntidadMundoX = ((Jugador) entidad).getMundoX() + ((Jugador) entidad).getAreaSolidaX();
			// Calcula la coordenada X del lado derecho del área de colisión sumando el
			// ancho.
			int derechaEntidadMundoX = izquierdaEntidadMundoX + ((Jugador) entidad).getAreaSolidaAncho();
			// Calcula la coordenada Y del lado superior del área de colisión de la entidad
			// en el mundo.
			int arribaEntidadMundoY = ((Jugador) entidad).getMundoY() + ((Jugador) entidad).getAreaSolidaY();
			// Calcula la coordenada Y del lado inferior del área de colisión sumando el
			// alto.
			int abajoEntidadMundoY = arribaEntidadMundoY + ((Jugador) entidad).getAreaSolidaAlto();

			// Convierte la coordenada X izquierda del mundo a la columna del mapa de
			// mosaicos.
			int colIzquierdaEntidad = izquierdaEntidadMundoX / this.gP.getTamanioTile();
			// Convierte la coordenada X derecha del mundo a la columna del mapa de
			// mosaicos.
			int colDerechaEntidad = derechaEntidadMundoX / this.gP.getTamanioTile();
			// Convierte la coordenada Y superior del mundo a la fila del mapa de mosaicos.
			int renArribaEntidad = arribaEntidadMundoY / this.gP.getTamanioTile();
			// Convierte la coordenada Y inferior del mundo a la fila del mapa de mosaicos.
			int renAbajoEntidad = abajoEntidadMundoY / this.gP.getTamanioTile();

			// Declara dos variables para almacenar los códigos de los mosaicos que se van a
			// revisar.
			int numTile1 = 0, numTile2 = 0;

			// Utiliza una estructura switch para verificar la colisión según la dirección
			// de la entidad.
			switch (((Jugador) entidad).getDireccion()) {
			case "arriba":
				// Calcula la fila del mosaico superior al que la entidad se moverá.
				renArribaEntidad = (arribaEntidadMundoY - ((Jugador) entidad).getVelocidad())
						/ this.gP.getTamanioTile();
				// Obtiene el código del mosaico en la esquina superior izquierda del área de
				// colisión.
				numTile1 = this.gP.getManejadorTiles().getCodigoMapaTiles(renArribaEntidad, colIzquierdaEntidad);
				// Obtiene el código del mosaico en la esquina superior derecha del área de
				// colisión.
				numTile2 = this.gP.getManejadorTiles().getCodigoMapaTiles(renArribaEntidad, colDerechaEntidad);
				break; // Termina el caso "arriba".
			case "abajo":
				// Calcula la fila del mosaico inferior al que la entidad se moverá.
				renAbajoEntidad = (abajoEntidadMundoY + ((Jugador) entidad).getVelocidad()) / this.gP.getTamanioTile();
				// Obtiene el código del mosaico en la esquina inferior izquierda del área de
				// colisión.
				numTile1 = this.gP.getManejadorTiles().getCodigoMapaTiles(renAbajoEntidad, colIzquierdaEntidad);
				// Obtiene el código del mosaico en la esquina inferior derecha del área de
				// colisión.
				numTile2 = this.gP.getManejadorTiles().getCodigoMapaTiles(renAbajoEntidad, colDerechaEntidad);
				break; // Termina el caso "abajo".
			case "izquierda":
				// Calcula la columna del mosaico izquierdo al que la entidad se moverá.
				colIzquierdaEntidad = (izquierdaEntidadMundoX - ((Jugador) entidad).getVelocidad())
						/ this.gP.getTamanioTile();
				// Obtiene el código del mosaico en la esquina superior izquierda del área de
				// colisión.
				numTile1 = this.gP.getManejadorTiles().getCodigoMapaTiles(renArribaEntidad, colIzquierdaEntidad);
				// Obtiene el código del mosaico en la esquina inferior izquierda del área de
				// colisión.
				numTile2 = this.gP.getManejadorTiles().getCodigoMapaTiles(renAbajoEntidad, colIzquierdaEntidad);
				break; // Termina el caso "izquierda".
			case "derecha":
				// Calcula la columna del mosaico derecho al que la entidad se moverá.
				colDerechaEntidad = (derechaEntidadMundoX + ((Jugador) entidad).getVelocidad())
						/ this.gP.getTamanioTile();
				// Obtiene el código del mosaico en la esquina superior derecha del área de
				// colisión.
				numTile1 = this.gP.getManejadorTiles().getCodigoMapaTiles(renArribaEntidad, colDerechaEntidad);
				// Obtiene el código del mosaico en la esquina inferior derecha del área de
				// colisión.
				numTile2 = this.gP.getManejadorTiles().getCodigoMapaTiles(renAbajoEntidad, colDerechaEntidad);
				break; // Termina el caso "derecha".
			default:
				// Bloque por defecto en caso de que la dirección no coincida con ninguna de las
				// anteriores.
				break;
			}

			// Comprueba si alguno de los dos mosaicos a revisar tiene la propiedad de
			// colisión activada.
			if (this.gP.getManejadorTiles().getColisionDeTile(numTile1)
					|| this.gP.getManejadorTiles().getColisionDeTile(numTile2)) {
				// Si hay colisión con al menos uno de los mosaicos, activa la bandera de
				// colisión en la entidad.
				((Jugador) entidad).setColisionActivada(true);
			}
		}
	}
}
