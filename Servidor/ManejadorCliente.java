package Servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Se encarga de manejar la comunicación con un cliente
 * conectado al servidor.
 *
 * Cada cliente se atiende mediante un hilo independiente.
 */
public class ManejadorCliente implements Runnable {

    // Socket correspondiente al cliente conectado.
    private final Socket socketCliente;

    // Se utiliza para recibir mensajes del cliente.
    private BufferedReader entrada;

    // Se utiliza para enviar mensajes al cliente.
    private PrintWriter salida;

    /**
     * Se crea un manejador para el cliente recibido.
     *
     * @param socketCliente socket correspondiente al cliente.
     */
    public ManejadorCliente(Socket socketCliente) {
        this.socketCliente = socketCliente;
    }

    /**
     * Se inicia la comunicación con el cliente.
     */
    @Override
    public void run() {

        try {

            // Se prepara la entrada de mensajes.
            entrada = new BufferedReader(
                new InputStreamReader(
                    socketCliente.getInputStream()
                )
            );

            // Se prepara la salida de mensajes.
            salida = new PrintWriter(
                socketCliente.getOutputStream(),
                true
            );

            // Se confirma que la conexión fue establecida.
            salida.println("CONEXION_OK");

            String mensaje;

            // Se reciben mensajes mientras el cliente continúe conectado.
            while ((mensaje = entrada.readLine()) != null) {

                System.out.println(
                    "Mensaje recibido: " + mensaje
                );

                procesarMensaje(mensaje);
            }

        } catch (IOException e) {

            System.out.println(
                "Cliente desconectado: "
                + e.getMessage()
            );

        } finally {

            cerrarConexion();
        }
    }

    /**
     * Se procesa el mensaje recibido desde el cliente.
     *
     * @param mensaje mensaje recibido.
     */
    private void procesarMensaje(String mensaje) {

        // *

        switch (mensaje) {

            case "CONECTAR":
                enviarMensaje("CONECTADO");
                break;

            case "CONSULTAR_ESTADO":
                enviarMensaje("ESTADO_PENDIENTE");
                break;

            case "CONSULTAR_TRANSACCIONES":
                enviarMensaje("TRANSACCIONES_PENDIENTES");
                break;

            default:
                enviarMensaje("ERROR|COMANDO_DESCONOCIDO");
                break;
        }
    }

    /**
     * Se envía un mensaje al cliente.
     *
     * @param mensaje mensaje que se enviará.
     */
    public void enviarMensaje(String mensaje) {

        if (salida != null) {
            salida.println(mensaje);
        }
    }

    /**
     * Se cierra la conexión del cliente.
     */
    private void cerrarConexion() {

        try {

            // Se verifica que el socket continúe abierto.
            if (socketCliente != null &&
                !socketCliente.isClosed()) {

                socketCliente.close();
            }

        } catch (IOException e) {

            System.out.println(
                "Error al cerrar cliente: "
                + e.getMessage()
            );
        }
    }
}