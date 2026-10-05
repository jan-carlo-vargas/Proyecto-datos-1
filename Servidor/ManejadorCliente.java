package Servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import Protocolo.Protocolo;

/**
 * Se encarga de manejar la comunicacion con un cliente
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
     * Se inicia la comunicacion con el cliente.
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

            // Se confirma que la conexion TCP fue establecida.
            enviarMensaje(Protocolo.CONEXION_OK);

            String mensaje;

            // Se reciben mensajes mientras el cliente continue conectado.
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

        switch (mensaje) {

            case Protocolo.CONECTAR:

                enviarMensaje(
                    Protocolo.CONECTADO
                );

                break;

            case Protocolo.TIRAR_DADOS:

                // *

                break;

            case Protocolo.COMPRAR_PROPIEDAD:

                // *

                break;

            case Protocolo.NO_COMPRAR:

                // *

                break;

            case Protocolo.TERMINAR_TURNO:

                // *

                break;

            case Protocolo.CONSULTAR_ESTADO:

                // *

                break;

            case Protocolo.CONSULTAR_TRANSACCIONES:

                // *

                break;

            default:

                enviarMensaje(
                    Protocolo.ERROR_COMANDO
                );

                break;
        }
    }

    /**
     * Se envia un mensaje al cliente.
     *
     * @param mensaje mensaje que se enviara.
     */
    public void enviarMensaje(String mensaje) {

        if (salida != null) {
            salida.println(mensaje);
        }
    }

    /**
     * Se cierra la conexion del cliente.
     */
    private void cerrarConexion() {

        try {

            // Se verifica que el socket continue abierto.
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
