package Cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import Protocolo.Protocolo;

/**
 * Se encarga de establecer la comunicacion con el servidor
 * mediante una conexion TCP.
 */
public class Cliente {

    // Direccion del servidor al que se realizara la conexion.
    private final String direccionServidor;

    // Puerto utilizado por el servidor.
    private final int puerto;

    // Socket utilizado para mantener la conexion con el servidor.
    private Socket socket;

    // Se utiliza para recibir mensajes del servidor.
    private BufferedReader entrada;

    // Se utiliza para enviar mensajes al servidor.
    private PrintWriter salida;

    // Indica si el cliente se encuentra conectado.
    private boolean conectado;

    /**
     * Se crea un cliente con la direccion y el puerto indicados.
     *
     * @param direccionServidor direccion IP del servidor.
     * @param puerto puerto TCP utilizado por el servidor.
     */
    public Cliente(String direccionServidor, int puerto) {
        this.direccionServidor = direccionServidor;
        this.puerto = puerto;
        this.conectado = false;
    }

    /**
     * Se establece la conexion con el servidor.
     *
     * @return true si la conexion se realizo correctamente.
     */
    public boolean conectar() {

        try {

            // Se establece la conexion TCP con el servidor.
            socket = new Socket(
                direccionServidor,
                puerto
            );

            // Se prepara la entrada de mensajes.
            entrada = new BufferedReader(
                new InputStreamReader(
                    socket.getInputStream()
                )
            );

            // Se prepara la salida de mensajes.
            salida = new PrintWriter(
                socket.getOutputStream(),
                true
            );

            conectado = true;

            System.out.println(
                "Conexion establecida con el servidor."
            );

            return true;

        } catch (IOException e) {

            System.out.println(
                "No se pudo conectar con el servidor: "
                + e.getMessage()
            );

            conectado = false;

            return false;
        }
    }

    /**
     * Se envia un mensaje al servidor.
     *
     * @param mensaje mensaje que se enviara.
     */
    public void enviarMensaje(String mensaje) {

        if (conectado && salida != null) {

            salida.println(mensaje);

        } else {

            System.out.println(
                "No existe una conexion con el servidor."
            );
        }
    }

    /**
     * Se recibe un mensaje enviado por el servidor.
     *
     * @return mensaje recibido.
     */
    public String recibirMensaje() {

        if (!conectado || entrada == null) {
            return null;
        }

        try {

            return entrada.readLine();

        } catch (IOException e) {

            System.out.println(
                "Error al recibir el mensaje: "
                + e.getMessage()
            );

            return null;
        }
    }

    /**
     * Se cierra la conexion con el servidor.
     */
    public void desconectar() {

        conectado = false;

        try {

            // Se verifica que el socket continue abierto.
            if (socket != null &&
                !socket.isClosed()) {

                socket.close();
            }

            System.out.println(
                "Cliente desconectado."
            );

        } catch (IOException e) {

            System.out.println(
                "Error al cerrar la conexion: "
                + e.getMessage()
            );
        }
    }

    /**
     * Se consulta si el cliente se encuentra conectado.
     *
     * @return true si existe una conexion.
     */
    public boolean estaConectado() {
        return conectado;
    }

    /**
     * Se ejecuta una prueba de conexion con el servidor.
     */
    public static void main(String[] args) {

        // Se crea un cliente conectado al equipo local.
        Cliente cliente =
            new Cliente("localhost", 5000);

        if (cliente.conectar()) {

            // Se recibe la confirmacion inicial del servidor.
            String respuesta =
                cliente.recibirMensaje();

            System.out.println(
                "Servidor: " + respuesta
            );

            // Se envia el comando de conexion.
            cliente.enviarMensaje(
                Protocolo.CONECTAR
            );

            // Se recibe la respuesta del servidor.
            respuesta =
                cliente.recibirMensaje();

            System.out.println(
                "Servidor: " + respuesta
            );

            System.out.println();

            System.out.println(
                "Cliente conectado."
            );

            System.out.println(
                "Presione ENTER para desconectarse."
            );

            // Se utiliza para esperar la entrada desde la consola.
            BufferedReader teclado =
                new BufferedReader(
                    new InputStreamReader(System.in)
                );

            try {

                // Se mantiene el cliente conectado hasta presionar ENTER.
                teclado.readLine();

            } catch (IOException e) {

                System.out.println(
                    "Error al leer la entrada: "
                    + e.getMessage()
                );
            }

            // Se cierra la conexion con el servidor.
            cliente.desconectar();
        }
    }
}
