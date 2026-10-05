package Servidor;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import Juego.Juego;

/**
 * Servidor principal del juego Monopoly.
 *
 * Se encarga de abrir el servidor TCP, aceptar conexiones
 * y administrar los clientes conectados.
 */
public class Servidor {

    // Puerto utilizado por el servidor.
    private final int puerto;

    // Socket principal que escucha las conexiones.
    private ServerSocket servidorSocket;

    // Indica si el servidor esta activo.
    private boolean activo;

    // Se utiliza como partida compartida por todos los clientes.
    private final Juego juego;

    // Se almacenan los manejadores conectados al servidor.
    private final ManejadorCliente[] clientes;

    // Cantidad de manejadores almacenados.
    private int cantidadClientes;

    /**
     * Se crea un servidor utilizando el puerto indicado.
     *
     * @param puerto puerto TCP que utilizara el servidor.
     */
    public Servidor(int puerto) {

        this.puerto = puerto;
        this.activo = false;

        // Se crea una unica partida para el servidor.
        this.juego = new Juego();

        // Se reserva espacio para los clientes de la partida.
        this.clientes =
            new ManejadorCliente[Juego.MAX_JUGADORES];

        this.cantidadClientes = 0;
    }

    /**
     * Se inicia el servidor y se comienza a esperar clientes.
     */
    public void iniciar() {

        try {

            // Se crea el servidor en el puerto indicado.
            servidorSocket =
                new ServerSocket(puerto);

            activo = true;

            System.out.println(
                "Servidor iniciado."
            );

            System.out.println(
                "Puerto: " + puerto
            );

            System.out.println(
                "Esperando jugadores..."
            );

            // Se aceptan conexiones mientras el servidor este activo.
            while (activo) {

                // Se espera la conexion de un cliente.
                Socket clienteSocket =
                    servidorSocket.accept();

                System.out.println(
                    "Cliente conectado desde: "
                    + clienteSocket
                        .getInetAddress()
                        .getHostAddress()
                );

                manejarCliente(
                    clienteSocket
                );
            }

        } catch (IOException e) {

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
    private void manejarCliente(
        Socket clienteSocket
    ) {

        ManejadorCliente manejador =
            new ManejadorCliente(
                clienteSocket,
                juego,
                this
            );

        registrarManejador(
            manejador
        );

        Thread hiloCliente =
            new Thread(manejador);

        hiloCliente.start();
    }

    /**
     * Se registra un manejador conectado al servidor.
     *
     * @param manejador manejador que se registrara.
     */
    private synchronized void registrarManejador(
        ManejadorCliente manejador
    ) {

        if (
            cantidadClientes
            < clientes.length
        ) {

            clientes[cantidadClientes] =
                manejador;

            cantidadClientes++;
        }
    }

    /**
     * Se elimina un manejador del registro del servidor.
     *
     * @param manejador manejador que se eliminara.
     */
    public synchronized void eliminarManejador(
        ManejadorCliente manejador
    ) {

        for (
            int i = 0;
            i < cantidadClientes;
            i++
        ) {

            if (clientes[i] == manejador) {

                for (
                    int j = i;
                    j < cantidadClientes - 1;
                    j++
                ) {

                    clientes[j] =
                        clientes[j + 1];
                }

                clientes[cantidadClientes - 1] =
                    null;

                cantidadClientes--;

                break;
            }
        }
    }

    /**
     * Se envia un mensaje a todos los clientes conectados.
     *
     * @param mensaje mensaje que se enviara.
     */
    public synchronized void enviarATodos(
        String mensaje
    ) {

        for (
            int i = 0;
            i < cantidadClientes;
            i++
        ) {

            if (clientes[i] != null) {

                clientes[i].enviarMensaje(
                    mensaje
                );
            }
        }
    }

    /**
     * Se detiene el servidor.
     */
    public void detener() {

        activo = false;

        try {

            if (
                servidorSocket != null
                && !servidorSocket.isClosed()
            ) {

                servidorSocket.close();
            }

            System.out.println(
                "Servidor detenido."
            );

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
     * @return true si el servidor esta activo.
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
     * Se obtiene la partida utilizada por el servidor.
     *
     * @return partida actual.
     */
    public Juego getJuego() {
        return juego;
    }

    /**
     * Se ejecuta el servidor para realizar pruebas.
     *
     * @param args argumentos de ejecucion.
     */
    public static void main(String[] args) {

        Servidor servidor =
            new Servidor(5000);

        servidor.iniciar();
    }
}
