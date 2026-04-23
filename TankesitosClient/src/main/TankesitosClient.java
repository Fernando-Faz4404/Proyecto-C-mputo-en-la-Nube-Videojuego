package main;

import java.net.Socket;
import java.io.IOException;
import javax.swing.JFrame;

public class TankesitosClient {

    
   
    public static void main(String[] args) {
        
        try{
            //A partir de los 1500 se puede usar el que quieran
            Socket socket = new Socket("localhost", 5555);
            
            // 3. Crear una instancia del panel principal del juego, donde ocurre toda la
            // acción.
            GamePanel panelJuego = new GamePanel();
            
            JFrame ventana = new JFrame();
            
            Cliente cliente = new Cliente(socket, panelJuego);
            
            cliente.ingresarVideojuego();
           
            panelJuego.setCliente(cliente);

            // 2. Configurar el comportamiento de la ventana.
            // Asegura que el programa termine cuando se cierre la ventana.
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            // Impide que el usuario cambie el tamaño de la ventana.
            ventana.setResizable(false);
            // Establece el título que aparecerá en la barra de la ventana.
            ventana.setTitle("The Game");

            // 3. Crear una instancia del panel principal del juego, donde ocurre toda la
            // acción.
            
            // Añadir el panel a la ventana para que sea visible.
            ventana.add(panelJuego);

            // 4. Ajustar el tamaño de la ventana al tamaño preferido del panel de juego.
            ventana.pack();

            // 5. Configurar la visualización de la ventana.
            // Centra la ventana en la pantalla (null como referencia significa el centro).
            ventana.setLocationRelativeTo(null);
            // Hace la ventana visible para el usuario.
            ventana.setVisible(true);

            // 6. Iniciar el hilo de ejecución del juego.
            panelJuego.iniciaHebraJuego();  
            
        }catch(IOException e){
        
        }   
        
    }
    
}
