/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tile;

import java.awt.image.BufferedImage;

/**
 * @author Los Ratones
 * @version 0.5
 * @since 11 de julio de 2025, 09:00 horas (horario de la Ciudad de México) *
 *        Clase de datos que representa un único tipo de mosaico (tile) en el
 *        mapa. Contiene la información esencial de un mosaico: su imagen y si
 *        es sólido.
 */
public class Tile {

	// La imagen que se usará para dibujar el mosaico.
	private BufferedImage imagen;
	// Bandera que indica si el mosaico tiene colisión (es sólido).
	// 'true' = no se puede atravesar; 'false' = se puede atravesar.
	private boolean colision = false;

	// --- GETTERS Y SETTERS ---

	/**
	 * Obtiene la imagen del mosaico. * @return La {@link BufferedImage} que
	 * representa visualmente este mosaico.
	 */
	public BufferedImage getImagen() {
		// Devuelve la imagen almacenada.
		return this.imagen;
	}

	/**
	 * Establece la imagen para este mosaico. * @param imagen La
	 * {@link BufferedImage} que se asignará a este mosaico.
	 */
	public void setImagen(BufferedImage imagen) {
		// Asigna la imagen proporcionada al campo 'imagen' de este objeto.
		this.imagen = imagen;
	}

	/**
	 * Comprueba si el mosaico es sólido (tiene colisión). * @return {@code true} si
	 * el mosaico es sólido, {@code false} en caso contrario.
	 */
	public boolean getColision() {
		// Devuelve el valor booleano de la propiedad de colisión.
		return this.colision;
	}

	/**
	 * Establece el estado de colisión del mosaico. * @param valor El valor booleano
	 * a establecer: {@code true} para sólido, {@code false} para no sólido.
	 */
	public void setColision(boolean valor) {
		// Asigna el valor proporcionado al campo 'colision' de este objeto.
		this.colision = valor;
	}
}
