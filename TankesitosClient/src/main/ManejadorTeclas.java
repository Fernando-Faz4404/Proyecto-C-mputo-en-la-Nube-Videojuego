package main;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class ManejadorTeclas implements KeyListener {

	// Banderas (flags) booleanas que indican el estado de cada tecla de movimiento.
	// Son 'true' si la tecla está siendo presionada, 'false' en caso contrario.
	private boolean teclaArriba, teclaAbajo, teclaIzquierda, teclaDerecha;

	/**
	 * Este método se invoca cuando una tecla es presionada y luego soltada (un
	 * "click" de tecla). No se utiliza en este juego, ya que necesitamos saber el
	 * estado continuo de la tecla.
	 * 
	 * @param e El evento de teclado.
	 */
	@Override
	public void keyTyped(KeyEvent e) {
		// Sin implementación requerida para este juego.
	}

	/**
	 * Se invoca en el momento exacto en que una tecla es presionada. * @param e El
	 * evento de teclado que contiene información sobre la tecla pulsada.
	 */
	@Override
	public void keyPressed(KeyEvent e) {
		// Obtiene el código virtual de la tecla que generó el evento.
		int codigo = e.getKeyCode();

		// Comprueba qué tecla fue y actualiza la bandera correspondiente a 'true'.
		switch (codigo) {
		case KeyEvent.VK_W: // Si la tecla es 'W'.
			this.teclaArriba = true; // Activa la bandera de movimiento hacia arriba.
			break; // Termina el caso.
		case KeyEvent.VK_S: // Si la tecla es 'S'.
			this.teclaAbajo = true; // Activa la bandera de movimiento hacia abajo.
			break; // Termina el caso.
		case KeyEvent.VK_A: // Si la tecla es 'A'.
			this.teclaIzquierda = true; // Activa la bandera de movimiento hacia la izquierda.
			break; // Termina el caso.
		case KeyEvent.VK_D: // Si la tecla es 'D'.
			this.teclaDerecha = true; // Activa la bandera de movimiento hacia la derecha.
			break; // Termina el caso.
		default: // Para cualquier otra tecla.
			break; // No hace nada.
		}
	}

	/**
	 * Se invoca en el momento exacto en que una tecla es liberada. * @param e El
	 * evento de teclado que contiene información sobre la tecla liberada.
	 */
	@Override
	public void keyReleased(KeyEvent e) {
		// Obtiene el código virtual de la tecla que generó el evento.
		int codigo = e.getKeyCode();

		// Comprueba qué tecla fue y actualiza la bandera correspondiente a 'false'.
		switch (codigo) {
		case KeyEvent.VK_W: // Si la tecla liberada es 'W'.
			this.teclaArriba = false; // Desactiva la bandera de movimiento hacia arriba.
			break; // Termina el caso.
		case KeyEvent.VK_S: // Si la tecla liberada es 'S'.
			this.teclaAbajo = false; // Desactiva la bandera de movimiento hacia abajo.
			break; // Termina el caso.
		case KeyEvent.VK_A: // Si la tecla liberada es 'A'.
			this.teclaIzquierda = false; // Desactiva la bandera de movimiento hacia la izquierda.
			break; // Termina el caso.
		case KeyEvent.VK_D: // Si la tecla liberada es 'D'.
			this.teclaDerecha = false; // Desactiva la bandera de movimiento hacia la derecha.
			break; // Termina el caso.
		}
	}

	// --- GETTERS ---
	// Métodos públicos para que otras clases (como Jugador) puedan consultar el
	// estado de las teclas.

	/**
	 * @return {@code true} si la tecla de movimiento hacia arriba está presionada.
	 */
	public boolean getTeclaArriba() {
		return this.teclaArriba;
	}

	/**
	 * @return {@code true} si la tecla de movimiento hacia abajo está presionada.
	 */
	public boolean getTeclaAbajo() {
		return this.teclaAbajo;
	}

	/**
	 * @return {@code true} si la tecla de movimiento hacia la izquierda está
	 *         presionada.
	 */
	public boolean getTeclaIzquierda() {
		return this.teclaIzquierda;
	}

	/**
	 * @return {@code true} si la tecla de movimiento hacia la derecha está
	 *         presionada.
	 */
	public boolean getTeclaDerecha() {
		return this.teclaDerecha;
	}
}
