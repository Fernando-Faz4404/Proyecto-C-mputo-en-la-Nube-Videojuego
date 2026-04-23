package entidad;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;


public class Entidad {

	// Coordenadas de la entidad en el mapa del mundo (no en la pantalla).
	protected int mundoX, mundoY;
	// Velocidad de movimiento de la entidad en píxeles por fotograma.
	protected int velocidad;

	// Imágenes (sprites) para las animaciones de movimiento.
	// Se usan dos fotogramas por dirección para crear una animación simple de
	// caminar.
	protected BufferedImage arriba1, arriba2, abajo1, abajo2, izquierda1, izquierda2, derecha1, derecha2;

	// Dirección actual a la que mira o se mueve la entidad (ej: "arriba", "abajo").
	protected String direccion;

	// --- VARIABLES PARA LA ANIMACIÓN DE SPRITES ---

	// Contador que se incrementa en cada fotograma para controlar el tiempo de la
	// animación.
	protected int contadorSprites = 0;
	// Indica qué número de sprite se está mostrando actualmente (ej: 1 o 2).
	protected int numeroSprites = 1;
	// Define la velocidad de la animación. El sprite cambiará cada 'cambiaSprite'
	// fotogramas.
	protected int cambiaSprite = 10;

	// --- VARIABLES DE COLISIÓN ---

	// Define el rectángulo de colisión (hitbox) de la entidad.
	protected Rectangle areaSolida;
	// Bandera que indica si una colisión ha sido detectada.
	protected boolean colisionActivada = false;

	/**
	 * Establece el estado de la bandera de colisión. * @param valor El nuevo estado
	 * de la colisión ({@code true} si hay colisión, {@code false} si no).
	 */
	public void setColisionActivada(boolean valor) {
		// Asigna el valor booleano proporcionado a la variable de estado de colisión.
		this.colisionActivada = valor;
	}
}
