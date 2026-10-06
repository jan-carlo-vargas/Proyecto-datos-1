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
    private volatile boolean conectado;

    // Se utiliza para escuchar mensajes del servidor.
    private Thread hiloReceptor;

    /**
     * Se crea un cliente con la direccion y el puerto indicados.
     *
     * @param direccionServidor direccion IP del servidor.
     * @param puerto            puerto TCP utilizado por el servidor.
     */
    public Cliente(
            String direccionServidor,
            int puerto) {

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

            socket = new Socket(
                    direccionServidor,
                    puerto);

            entrada = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()));

            salida = new PrintWriter(
                    socket.getOutputStream(),
                    true);

            conectado = true;

            System.out.println(
                    "Conexion establecida con el servidor.");

            return true;

        } catch (IOException e) {

            System.out.println(
                    "No se pudo conectar con el servidor: "
                            + e.getMessage());

            conectado = false;

            return false;
        }
    }

    /**
     * Se inicia el hilo encargado de escuchar al servidor.
     */
    public void iniciarReceptor() {

        if (!conectado
                || entrada == null) {

            return;
        }

        hiloReceptor = new Thread(
                new Runnable() {

                    @Override
                    public void run() {

                        escucharServidor();
                    }
                });

        hiloReceptor.start();
    }

    /**
     * Se escuchan continuamente los mensajes enviados
     * por el servidor.
     */
    private void escucharServidor() {

        try {

            String mensaje;

            while (conectado
                    && (mensaje = entrada.readLine()) != null) {

                System.out.println(
                        "Servidor: "
                                + mensaje);
            }

        } catch (IOException e) {

            if (conectado) {

                System.out.println(
                        "Se perdio la conexion con el servidor: "
                                + e.getMessage());
            }

        } finally {

            conectado = false;
        }
    }

    /**
     * Se envia un mensaje al servidor.
     *
     * @param mensaje mensaje que se enviara.
     */
    public void enviarMensaje(
            String mensaje) {

        if (conectado
                && salida != null) {

            salida.println(
                    mensaje);

        } else {

            System.out.println(
                    "No existe una conexion con el servidor.");
        }
    }

    /**
     * Se cierra la conexion con el servidor.
     */
    public void desconectar() {

        conectado = false;

        try {

            if (socket != null
                    && !socket.isClosed()) {

                socket.close();
            }

            System.out.println(
                    "Cliente desconectado.");

        } catch (IOException e) {

            System.out.println(
                    "Error al cerrar la conexion: "
                            + e.getMessage());
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
     * Se muestran los comandos disponibles para la prueba.
     */
    private static void mostrarComandos() {

        System.out.println();
        System.out.println(
                "TERMINAR_TURNO");

        System.out.println(
                "CONSULTAR_ESTADO");

        System.out.println(
                "CONSULTAR_TRANSACCIONES");

        System.out.println(
                "SALIR");
    }

    /**
     * Se ejecuta el cliente de prueba.
     *
     * @param args argumentos de ejecucion.
     */
    public static void main(String[] args) throws IOException {

        BufferedReader teclado = new BufferedReader(
                new InputStreamReader(
                        System.in));
        System.out.print("Ingrese la IP del servidor: ");
        String ipServidor = teclado.readLine().trim();

        if (ipServidor.isEmpty()) {
            ipServidor = "localhost";
        }
        Cliente cliente = new Cliente(
                ipServidor,
                5000);

        if (!cliente.conectar()) {
            return;
        }

        try {

            // Se recibe la confirmacion inicial de la conexion.
            String respuestaInicial = cliente.entrada.readLine();

            System.out.println(
                    "Servidor: "
                            + respuestaInicial);
            if (!Protocolo.CONEXION_OK.equals(
                    respuestaInicial)) {

                cliente.desconectar();
                return;
            }

            System.out.print(
                    "Ingrese el ID del jugador: ");

            String id = teclado.readLine();

            System.out.print(
                    "Ingrese el nombre del jugador: ");

            String nombre = teclado.readLine();

            // Se inicia la escucha permanente del servidor.
            cliente.iniciarReceptor();

            // Se envia la solicitud de registro.
            cliente.enviarMensaje(
                    Protocolo.CONECTAR
                            + "|"
                            + id
                            + "|"
                            + nombre);

            mostrarComandos();

            String comando;

            // Se reciben comandos desde la consola.
            while (cliente.estaConectado()
                    && (comando = teclado.readLine()) != null) {

                comando = comando.trim();

                if (comando.equalsIgnoreCase(
                        "SALIR")) {

                    break;
                }
                if (comando.equalsIgnoreCase(
                        Protocolo.TERMINAR_TURNO)) {

                    cliente.enviarMensaje(
                            Protocolo.TERMINAR_TURNO);

                } else if (comando.equalsIgnoreCase(
                        Protocolo.CONSULTAR_ESTADO)) {

                    cliente.enviarMensaje(
                            Protocolo.CONSULTAR_ESTADO);

                } else if (comando.equalsIgnoreCase(
                        Protocolo.CONSULTAR_TRANSACCIONES)) {

                    cliente.enviarMensaje(
                            Protocolo.CONSULTAR_TRANSACCIONES);

                } else if (!comando.isEmpty()) {

                    System.out.println(
                            "Comando no disponible.");
                }
            }

        } catch (IOException e) {

            System.out.println(
                    "Error al leer la entrada: "
                            + e.getMessage());

        } finally {

            cliente.desconectar();
        }
    }
}
