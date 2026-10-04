package Servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor principal del juego Monopoly.
 *
 * Se encarga de abrir el servidor TCP, aceptar conexiones
 * y crear un manejador independiente para cada cliente.
 */
public class Servidor {

    // Puerto utilizado por el servidor.
    private final int puerto;

    // Socket principal que escucha las conexiones.
    private ServerSocket servidorSocket;

    // Indica si el servidor está activo.
    private boolean activo;

    /**
     * Se crea un servidor utilizando el puerto indicado.
     *
     * @param puerto puerto TCP que utilizará el servidor.
     */
    public Servidor(int puerto) {
        this.puerto = puerto;
        this.activo = false;
    }

    /**
     * Se inicia el servidor y se comienza a esperar clientes.
     */
    public void iniciar() {

        try {

            // Se crea el servidor en el puerto indicado.
            servidorSocket = new ServerSocket(puerto);

            activo = true;

            System.out.println("Servidor iniciado.");
            System.out.println("Puerto: " + puerto);
            System.out.println("Esperando jugadores...");

            // Se aceptan conexiones mientras el servidor esté activo.
            while (activo) {

                // Se espera la conexión de un cliente.
                Socket clienteSocket = servidorSocket.accept();

                System.out.println(
                    "Cliente conectado desde: "
                    + clienteSocket.getInetAddress().getHostAddress()
                );

                // Se envía la conexión al manejador correspondiente.
                manejarCliente(clienteSocket);
            }

        } catch (IOException e) {

            // Se muestra el error únicamente si el servidor seguía activo.
            if (activo) {

                System.out.println(
                    "Error en el servidor: "
                    + e.getMessage()
                );
            }
        }
    }

    /**
     * Se crea un manejador independiente para cada cliente conectado.
     *
     * @param clienteSocket socket del cliente.
     */
    private void manejarCliente(Socket clienteSocket) {

        // Se crea el manejador del cliente.
        ManejadorCliente manejador =
            new ManejadorCliente(clienteSocket);

        // Se crea un hilo independiente para el cliente.
        Thread hiloCliente =
            new Thread(manejador);

        // Se inicia el hilo.
        hiloCliente.start();
    }

    /**
     * Se detiene el servidor.
     */
    public void detener() {

        activo = false;

        try {

            // Se verifica que el socket exista y continúe abierto.
            if (servidorSocket != null &&
                !servidorSocket.isClosed()) {

                servidorSocket.close();
            }

            System.out.println("Servidor detenido.");

        } catch (IOException e) {

            System.out.println(
                "Error al cerrar el servidor: "
                + e.getMessage()
            );
        }
    }

    /**
     * Se consulta si el servidor se encuentra activo.
     *
     * @return true si el servidor está activo.
     */
    public boolean estaActivo() {
        return activo;
    }

    /**
     * Se obtiene el puerto utilizado por el servidor.
     *
     * @return puerto TCP.
     */
    public int getPuerto() {
        return puerto;
    }

    /**
     * Se ejecuta el servidor para realizar pruebas.
     */
    public static void main(String[] args) {

        // Se crea el servidor utilizando el puerto 5000.
        Servidor servidor =
            new Servidor(5000);

        // Se inicia el servidor.
        servidor.iniciar();
    }
}