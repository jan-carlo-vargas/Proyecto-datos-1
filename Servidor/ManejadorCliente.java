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
 * Se encarga de manejar la comunicacion con UNA conexion del servidor.
 *
 * La conexion puede ser de dos tipos y se distingue por el primer mensaje:
 *   - cliente del juego: mensajes de texto separados por "|";
 *   - modulo electronico (Pico W): mensajes JSON, la linea empieza con "{".
 *
 * Aqui solo se interpretan los mensajes; las reglas viven en Juego y Banco, y
 * la coordinacion con el modulo electronico en Servidor.
 */
public class ManejadorCliente implements Runnable {

    private final Socket socketCliente;

    private final Juego juego;

    private final Servidor servidor;

    private BufferedReader entrada;

    private PrintWriter salida;

    // Jugador asociado con este cliente (null si es el modulo o no se registro).
    private volatile Jugador jugador;

    // true si esta conexion es el modulo electronico.
    private volatile boolean esDispositivo;

    public ManejadorCliente(Socket socketCliente, Juego juego, Servidor servidor) {

        this.socketCliente = socketCliente;
        this.juego = juego;
        this.servidor = servidor;
        this.jugador = null;
        this.esDispositivo = false;
    }

    public Jugador getJugador() {
        return jugador;
    }

    @Override
    public void run() {

        try {

            entrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
            salida = new PrintWriter(socketCliente.getOutputStream(), true);

            enviarMensaje(Protocolo.CONEXION_OK);

            String mensaje;

            while ((mensaje = entrada.readLine()) != null) {

                System.out.println("Mensaje recibido: " + mensaje);

                try {

                    if (mensaje.trim().startsWith("{")) {
                        procesarDispositivo(mensaje.trim());
                    } else {
                        procesarMensaje(mensaje);
                    }

                } catch (RuntimeException e) {

                    // Un error inesperado no debe matar la conexion del jugador.
                    System.out.println("Error procesando '" + mensaje + "': " + e);
                    enviarMensaje("ERROR|ERROR_INTERNO");
                }
            }

        } catch (IOException e) {

            System.out.println("Cliente desconectado: " + e.getMessage());

        } finally {

            cerrarConexion();
        }
    }

    //  MODULO ELECTRONICO (JSON)

    private void procesarDispositivo(String json) {

        String accion = Json.leer(json, "accion");

        if (accion == null) {
            enviarMensaje(Json.construir("ERROR", "mensaje", "Falta la accion"));
            return;
        }

        if (jugador != null) {
            enviarMensaje(Json.construir("ERROR", "mensaje", "Esta conexion es de un jugador"));
            return;
        }

        // Cualquier mensaje JSON identifica a la conexion como el modulo.
        if (!esDispositivo) {

            if (!servidor.registrarDispositivo(this)) {
                enviarMensaje(Json.construir("ERROR", "mensaje", "Ya hay un modulo electronico conectado"));
                return;
            }

            esDispositivo = true;
            enviarMensaje(Json.construir("DISPOSITIVO_OK"));
        }

        switch (accion) {

            case Protocolo.DISP_CONECTAR:
                break;

            case Protocolo.DISP_IDENTIFICAR:
                String uid = Json.leer(json, "uid");
                servidor.recibirTarjeta(uid);
                break;

            case Protocolo.DISP_RESULTADO_DADO:
                int d1 = Json.leerEntero(json, "dado1");
                int d2 = Json.leerEntero(json, "dado2");

                // Si solo llega el total (2 a 12) se reparte en dos dados validos.
                if (d1 < 0 || d2 < 0) {
                    int total = Json.leerEntero(json, "valor");
                    if (total < 0) {
                        total = Json.leerEntero(json, "total");
                    }
                    d1 = total / 2;
                    d2 = total - d1;
                }

                servidor.recibirDado(d1, d2);
                break;

            default:
                enviarMensaje(Json.construir("ERROR", "mensaje", "Accion desconocida: " + accion));
                break;
        }
    }

    //  CLIENTES DEL JUEGO

    private void procesarMensaje(String mensaje) {

        String[] partes = mensaje.split("\\|");

        if (partes.length == 0) {
            enviarMensaje(Protocolo.ERROR_COMANDO);
            return;
        }

        String comando = partes[0];

        switch (comando) {

            case Protocolo.CONECTAR:
                procesarConexion(partes);
                break;

            case Protocolo.TIRAR_DADOS:
                accionDeJuego(comando);
                break;

            case Protocolo.COMPRAR_PROPIEDAD:
                accionDeJuego(comando);
                break;

            case Protocolo.NO_COMPRAR:
                accionDeJuego(comando);
                break;

            case Protocolo.TERMINAR_TURNO:
                accionDeJuego(comando);
                break;

            case Protocolo.CONSULTAR_ESTADO:
                procesarConsultarEstado();
                break;

            case Protocolo.CONSULTAR_TABLERO:

                if (juego.estaIniciado()) {
                    enviarMensaje(juego.mensajeTablero());
                    enviarMensaje(juego.mensajeEstado());
                } else {
                    enviarMensaje("ERROR|PARTIDA_NO_INICIADA");
                }
                break;

            case Protocolo.CONSULTAR_TRANSACCIONES:
                procesarConsultarTransacciones();
                break;

            case Protocolo.EXPORTAR_TRANSACCIONES:
                procesarExportar();
                break;

            default:
                enviarMensaje(Protocolo.ERROR_COMANDO);
                break;
        }
    }

    /**
     * Las cuatro acciones que cambian el estado de la partida. El servidor
     * valida todo (turno, saldo, dados, fin de partida); el cliente solo pide.
     */
    private void accionDeJuego(String comando) {

        if (!validarJugadorEnPartida()) {
            return;
        }

        try {

            switch (comando) {

                case Protocolo.TIRAR_DADOS:
                    servidor.tirarDados(jugador);
                    break;

                case Protocolo.COMPRAR_PROPIEDAD:
                    servidor.comprar(jugador);
                    break;

                case Protocolo.NO_COMPRAR:
                    servidor.noComprar(jugador);
                    break;

                default:
                    servidor.terminarTurno(jugador);
                    break;
            }

        } catch (OperacionInvalidaException e) {

            enviarMensaje("ERROR|" + e.getMessage());
        }
    }

    private void procesarConexion(String[] partes) {

        if (jugador != null) {
            enviarMensaje("ERROR|CLIENTE_YA_REGISTRADO");
            return;
        }

        if (esDispositivo) {
            enviarMensaje("ERROR|CONEXION_DE_DISPOSITIVO");
            return;
        }

        if (partes.length != 3) {
            enviarMensaje("ERROR|FORMATO_CONECTAR");
            return;
        }

        String id = partes[1].trim();
        String nombre = partes[2].trim();

        if (id.isEmpty() || nombre.isEmpty()) {
            enviarMensaje("ERROR|DATOS_JUGADOR");
            return;
        }

        try {

            jugador = juego.registrarJugador(id, nombre);

            enviarMensaje(Protocolo.CONECTADO + "|" + jugador.getId() + "|" + jugador.getNombre());

            System.out.println("Jugador registrado: " + jugador.getId() + " - " + jugador.getNombre());
            System.out.println("Jugadores en la partida: "
                    + juego.getCantidadJugadores() + "/" + Juego.MAX_JUGADORES);

            // Se inicia la partida cuando se registran cuatro jugadores.
            if (juego.getCantidadJugadores() == Juego.MAX_JUGADORES && !juego.estaIniciado()) {
                iniciarPartida();
            }

        } catch (OperacionInvalidaException e) {

            enviarMensaje("ERROR|" + e.getMessage());
        }
    }

    private void iniciarPartida() {

        try {

            juego.iniciarPartida();

            Jugador jugadorActual = juego.obtenerJugadorActual();

            System.out.println("Partida iniciada.");
            System.out.println("Primer turno: " + jugadorActual.getId());

            servidor.enviarATodos(Protocolo.PARTIDA_INICIADA);
            servidor.enviarATodos(Protocolo.TURNO + "|" + jugadorActual.getId());
            servidor.enviarATodos(juego.mensajeTablero());
            servidor.difundirEstado();

        } catch (OperacionInvalidaException e) {

            enviarMensaje("ERROR|" + e.getMessage());
        }
    }

    private boolean validarJugadorEnPartida() {

        if (jugador == null) {
            enviarMensaje("ERROR|JUGADOR_NO_REGISTRADO");
            return false;
        }

        if (!juego.estaIniciado()) {
            enviarMensaje("ERROR|PARTIDA_NO_INICIADA");
            return false;
        }

        return true;
    }

    private void procesarConsultarEstado() {

        if (jugador == null) {
            enviarMensaje("ERROR|JUGADOR_NO_REGISTRADO");
            return;
        }

        try {

            dominio.Banco banco = juego.getBanco();

            String turnoActual = "NINGUNO";

            if (juego.estaIniciado() && !banco.partidaTerminada()) {
                turnoActual = juego.obtenerJugadorActual().getId();
            }

            String estado = "ESTADO"
                    + "|INICIADA=" + juego.estaIniciado()
                    + "|NUMERO_TURNO=" + banco.getNumeroTurno()
                    + "|TURNO=" + turnoActual
                    + "|JUGADORES=" + juego.getCantidadJugadores()
                    + "|JUGADORES_ACTIVOS=" + banco.jugadoresActivos()
                    + "|ID=" + jugador.getId()
                    + "|NOMBRE=" + jugador.getNombre()
                    + "|SALDO=" + jugador.getSaldo()
                    + "|PROPIEDADES=" + jugador.getCantidadPropiedades()
                    + "|ACTIVO=" + jugador.estaActivo()
                    + "|CASILLA=" + (jugador.getCasillaActual() == null
                            ? "NINGUNA"
                            : jugador.getCasillaActual().getId());

            enviarMensaje(estado);

        } catch (OperacionInvalidaException e) {

            enviarMensaje("ERROR|" + e.getMessage());
        }
    }

    private void procesarConsultarTransacciones() {

        if (jugador == null) {
            enviarMensaje("ERROR|JUGADOR_NO_REGISTRADO");
            return;
        }

        String historial = juego.getBanco().getHistorial().generarTexto();

        if (historial == null || historial.trim().isEmpty()) {
            enviarMensaje("TRANSACCIONES|SIN_TRANSACCIONES");
            return;
        }

        enviarMensaje("TRANSACCIONES|" + historial.replace("\r", "").replace("\n", " ; "));
    }

    /**
     * EXPORTAR_TRANSACCIONES: genera el TXT del punto 13 en el servidor.
     */
    private void procesarExportar() {

        if (jugador == null) {
            enviarMensaje("ERROR|JUGADOR_NO_REGISTRADO");
            return;
        }

        try {

            enviarMensaje(Protocolo.EXPORTADO + "|" + servidor.exportarReporte());

        } catch (RuntimeException e) {

            enviarMensaje("ERROR|No se pudo exportar: " + e.getMessage());
        }
    }

    public void enviarMensaje(String mensaje) {

        if (salida != null) {
            salida.println(mensaje);
        }
    }

    private void cerrarConexion() {

        servidor.eliminarManejador(this);

        if (esDispositivo) {
            servidor.dispositivoDesconectado(this);
        }

        if (jugador != null) {
            servidor.jugadorDesconectado(jugador);
        }

        try {

            if (socketCliente != null && !socketCliente.isClosed()) {
                socketCliente.close();
            }

        } catch (IOException e) {
            System.out.println("Error al cerrar cliente: " + e.getMessage());
        }
    }
}
