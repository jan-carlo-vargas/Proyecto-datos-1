package Servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import Juego.Juego;
import Protocolo.Protocolo;
import dominio.Jugador;
import dominio.OperacionInvalidaException;

/**
 * Se encarga de manejar la comunicacion con un cliente
 * conectado al servidor.
 */
public class ManejadorCliente implements Runnable {

    // Socket correspondiente al cliente conectado.
    private final Socket socketCliente;

    // Se utiliza como partida compartida por los clientes.
    private final Juego juego;

    // Se utiliza para acceder al servidor principal.
    private final Servidor servidor;

    // Se utiliza para recibir mensajes del cliente.
    private BufferedReader entrada;

    // Se utiliza para enviar mensajes al cliente.
    private PrintWriter salida;

    // Se almacena el jugador asociado con este cliente.
    private Jugador jugador;

    /**
     * Se crea un manejador para el cliente recibido.
     *
     * @param socketCliente socket correspondiente al cliente.
     * @param juego         partida compartida por el servidor.
     * @param servidor      servidor principal.
     */
    public ManejadorCliente(
            Socket socketCliente,
            Juego juego,
            Servidor servidor) {

        this.socketCliente = socketCliente;

        this.juego = juego;

        this.servidor = servidor;

        this.jugador = null;
    }

    /**
     * Se inicia la comunicacion con el cliente.
     */
    @Override
    public void run() {

        try {

            entrada = new BufferedReader(
                    new InputStreamReader(
                            socketCliente.getInputStream()));

            salida = new PrintWriter(
                    socketCliente.getOutputStream(),
                    true);

            enviarMensaje(
                    Protocolo.CONEXION_OK);

            String mensaje;

            while ((mensaje = entrada.readLine()) != null) {

                System.out.println(
                        "Mensaje recibido: "
                                + mensaje);

                procesarMensaje(
                        mensaje);
            }

        } catch (IOException e) {

            System.out.println(
                    "Cliente desconectado: "
                            + e.getMessage());

        } finally {

            cerrarConexion();
        }
    }

    /**
     * Se procesa el mensaje recibido desde el cliente.
     *
     * @param mensaje mensaje recibido.
     */
    private void procesarMensaje(
            String mensaje) {

        String[] partes = mensaje.split("\\|");

        if (partes.length == 0) {

            enviarMensaje(
                    Protocolo.ERROR_COMANDO);

            return;
        }

        String comando = partes[0];

        switch (comando) {

            case Protocolo.CONECTAR:

                procesarConexion(
                        partes);

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

                procesarTerminarTurno();

                break;

            case Protocolo.CONSULTAR_ESTADO:

                procesarConsultarEstado();

                break;

            case Protocolo.CONSULTAR_TRANSACCIONES:

                procesarConsultarTransacciones();

                break;

            default:

                enviarMensaje(
                        Protocolo.ERROR_COMANDO);

                break;
        }
    }

    /**
     * Se procesa la solicitud de registro de un jugador.
     *
     * @param partes partes recibidas en el mensaje.
     */
    private void procesarConexion(
            String[] partes) {

        if (jugador != null) {

            enviarMensaje(
                    "ERROR|CLIENTE_YA_REGISTRADO");

            return;
        }

        if (partes.length != 3) {

            enviarMensaje(
                    "ERROR|FORMATO_CONECTAR");

            return;
        }

        String id = partes[1].trim();

        String nombre = partes[2].trim();

        if (id.isEmpty()
                || nombre.isEmpty()) {

            enviarMensaje(
                    "ERROR|DATOS_JUGADOR");

            return;
        }

        try {

            jugador = juego.registrarJugador(
                    id,
                    nombre);

            enviarMensaje(
                    Protocolo.CONECTADO
                            + "|"
                            + jugador.getId()
                            + "|"
                            + jugador.getNombre());

            System.out.println(
                    "Jugador registrado: "
                            + jugador.getId()
                            + " - "
                            + jugador.getNombre());

            System.out.println(
                    "Jugadores en la partida: "
                            + juego.getCantidadJugadores()
                            + "/"
                            + Juego.MAX_JUGADORES);

            // Se inicia la partida cuando se registran cuatro jugadores.
            if (juego.getCantidadJugadores() == Juego.MAX_JUGADORES
                    && !juego.estaIniciado()) {

                iniciarPartida();
            }

        } catch (OperacionInvalidaException e) {

            enviarMensaje(
                    "ERROR|"
                            + e.getMessage());
        }
    }

    /**
     * Se inicia la partida y se informa el primer turno.
     */
    private void iniciarPartida() {

        try {

            // Se inicia la partida compartida.
            juego.iniciarPartida();

            Jugador jugadorActual = juego.obtenerJugadorActual();

            System.out.println(
                    "Partida iniciada.");

            System.out.println(
                    "Primer turno: "
                            + jugadorActual.getId());

            // Se informa a todos que la partida fue iniciada.
            servidor.enviarATodos(
                    Protocolo.PARTIDA_INICIADA);

            // Se informa a todos cual jugador posee el turno.
            servidor.enviarATodos(
                    Protocolo.TURNO
                            + "|"
                            + jugadorActual.getId());

        } catch (OperacionInvalidaException e) {

            enviarMensaje(
                    "ERROR|"
                            + e.getMessage());
        }
    }

    /**
     * Se procesa la solicitud para terminar un turno.
     */
    private void procesarTerminarTurno() {

        if (jugador == null) {

            enviarMensaje(
                    "ERROR|JUGADOR_NO_REGISTRADO");

            return;
        }

        if (!juego.estaIniciado()) {

            enviarMensaje(
                    "ERROR|PARTIDA_NO_INICIADA");

            return;
        }

        try {

            // Se solicita terminar el turno del jugador.
            juego.terminarTurno(
                    jugador.getId());

            // Se obtiene el siguiente jugador de la cola.
            Jugador siguiente = juego.obtenerJugadorActual();

            System.out.println(
                    "Turno terminado por: "
                            + jugador.getId());

            System.out.println(
                    "Nuevo turno: "
                            + siguiente.getId());

            // Se informa el nuevo turno a todos los clientes.
            servidor.enviarATodos(
                    Protocolo.TURNO
                            + "|"
                            + siguiente.getId());

        } catch (OperacionInvalidaException e) {

            enviarMensaje(
                    "ERROR|"
                            + e.getMessage());
        }
    }

    /**
     * Se procesa la solicitud para consultar el estado
     * actual de la partida.
     */
    private void procesarConsultarEstado() {

        if (jugador == null) {

            enviarMensaje(
                    "ERROR|JUGADOR_NO_REGISTRADO");

            return;
        }

        try {

            // Se obtiene el banco que mantiene el estado oficial.
            dominio.Banco banco = juego.getBanco();

            String turnoActual = "NINGUNO";

            // Se obtiene el jugador actual cuando la partida esta iniciada.
            if (juego.estaIniciado()) {

                Jugador actual = juego.obtenerJugadorActual();

                turnoActual = actual.getId();
            }

            // Se construye la respuesta utilizando el estado oficial.
            String estado = "ESTADO"
                    + "|INICIADA="
                    + juego.estaIniciado()
                    + "|NUMERO_TURNO="
                    + banco.getNumeroTurno()
                    + "|TURNO="
                    + turnoActual
                    + "|JUGADORES="
                    + juego.getCantidadJugadores()
                    + "|JUGADORES_ACTIVOS="
                    + banco.jugadoresActivos()
                    + "|ID="
                    + jugador.getId()
                    + "|NOMBRE="
                    + jugador.getNombre()
                    + "|SALDO="
                    + jugador.getSaldo()
                    + "|PROPIEDADES="
                    + jugador.getCantidadPropiedades()
                    + "|ACTIVO="
                    + jugador.estaActivo();

            enviarMensaje(
                    estado);

        } catch (OperacionInvalidaException e) {

            enviarMensaje(
                    "ERROR|"
                            + e.getMessage());
        }
    }

    /**
     * Se procesa la solicitud para consultar
     * el historial de transacciones.
     */
    private void procesarConsultarTransacciones() {

        if (jugador == null) {

            enviarMensaje(
                    "ERROR|JUGADOR_NO_REGISTRADO");

            return;
        }

        // Se obtiene el historial oficial almacenado por el banco.
        String historial = juego.getBanco()
                .getHistorial()
                .generarTexto();

        // Se verifica si existen transacciones registradas.
        if (historial == null
                || historial.trim().isEmpty()) {

            enviarMensaje(
                    "TRANSACCIONES|SIN_TRANSACCIONES");

            return;
        }

        // Se envia el historial al cliente.
        enviarMensaje(
                "TRANSACCIONES|"
                        + historial.replace(
                                "\n",
                                " ; "));
    }

    /**
     * Se envia un mensaje al cliente.
     *
     * @param mensaje mensaje que se enviara.
     */
    public void enviarMensaje(
            String mensaje) {

        if (salida != null) {

            salida.println(
                    mensaje);
        }
    }

    /**
     * Se cierra la conexion del cliente.
     */
    private void cerrarConexion() {

        // Se elimina el manejador del registro del servidor.
        servidor.eliminarManejador(
                this);

        try {

            if (socketCliente != null
                    && !socketCliente.isClosed()) {

                socketCliente.close();
            }

        } catch (IOException e) {

            System.out.println(
                    "Error al cerrar cliente: "
                            + e.getMessage());
        }
    }
}
