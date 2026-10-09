package Servidor;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import Juego.Juego;
import Protocolo.Protocolo;
import dominio.Banco;
import dominio.Jugador;
import dominio.OperacionInvalidaException;
import dominio.Propiedad;
import dominio.ResultadoEfecto;

/**
 * Servidor principal del juego Monopoly (el "banco" de la partida).
 *
 * Abre UN solo puerto TCP (Protocolo.PUERTO = 6000). Por ahi entran los
 * clientes del juego (mensajes con "|") y el modulo electronico, que se
 * reconoce porque habla JSON (la linea empieza con "{").
 *
 * Aqui vive la coordinacion entre el juego y el modulo electronico:
 *   - dado: si hay modulo conectado, TIRAR_DADOS activa el dado fisico y la
 *     tirada se resuelve cuando la Pico manda RESULTADO_DADO;
 *   - tarjeta: compras y cobros obligatorios (alquiler, impuesto) esperan a
 *     que el jugador acerque su tarjeta RFID; el servidor identifica al
 *     jugador, valida, cobra y genera la transaccion.
 * Si no hay modulo conectado, el juego funciona solo por software.
 */
public class Servidor {

    // Archivo donde se exporta el historial (punto 13).
    public static final String RUTA_REPORTE = "reporte_transacciones.txt";

    private static final String ESPERA_DADO = "DADO";
    private static final String ESPERA_PAGO = "RFID_PAGO";
    private static final String ESPERA_COMPRA = "RFID_COMPRA";

    // Puerto utilizado por el servidor.
    private final int puerto;

    // Socket principal que escucha las conexiones.
    private ServerSocket servidorSocket;

    // Indica si el servidor esta activo.
    private volatile boolean activo;

    // Se utiliza como partida compartida por todos los clientes.
    private final Juego juego;

    // Manejadores de los clientes conectados (los jugadores).
    // Se deja un lugar extra para el modulo electronico mientras se identifica.
    private final ManejadorCliente[] clientes;

    private int cantidadClientes;

    // Modulo electronico (Pico W) conectado, o null.
    private ManejadorCliente dispositivo;

    // Que operacion espera al modulo electronico: null, DADO, RFID_PAGO o RFID_COMPRA.
    private String esperando;

    // Jugador al que se le esta esperando la tirada o la tarjeta.
    private String jugadorEsperado;

    // Evita anunciar dos veces el fin de la partida.
    private boolean finAnunciado;

    // Cuantas transacciones del historial ya se mostraron a los clientes.
    private int transaccionesAnunciadas;

    public Servidor(int puerto) {
        this(puerto, Banco.MAX_TURNOS_POR_DEFECTO);
    }

    /**
     * @param puerto    puerto TCP.
     * @param maxTurnos limite configurable de turnos (punto 18).
     */
    public Servidor(int puerto, int maxTurnos) {

        this.puerto = puerto;
        this.activo = false;
        this.juego = new Juego(maxTurnos);
        this.clientes = new ManejadorCliente[Juego.MAX_JUGADORES + 1];
        this.cantidadClientes = 0;
    }

    //  CONEXIONES

    /**
     * Se inicia el servidor y se comienza a esperar clientes.
     */
    public void iniciar() {

        try {

            servidorSocket = new ServerSocket(puerto);
            activo = true;

            System.out.println("Servidor iniciado.");
            System.out.println("Puerto: " + puerto);
            System.out.println("Esperando jugadores...");

            while (activo) {

                Socket clienteSocket = servidorSocket.accept();

                System.out.println("Cliente conectado desde: "
                        + clienteSocket.getInetAddress().getHostAddress());

                manejarCliente(clienteSocket);
            }

        } catch (IOException e) {

            if (activo) {
                System.out.println("Error en el servidor: " + e.getMessage());
            }
        }
    }

    private synchronized void manejarCliente(Socket clienteSocket) {

        System.out.println("Conexiones: " + cantidadClientes + "/" + clientes.length);

        if (cantidadClientes >= clientes.length) {

            try {

                PrintWriter salidaTemporal =
                        new PrintWriter(clienteSocket.getOutputStream(), true);

                salidaTemporal.println("ERROR|SERVIDOR_LLENO");
                clienteSocket.close();

            } catch (IOException e) {
                System.out.println("Error al rechazar cliente: " + e.getMessage());
            }

            return;
        }

        ManejadorCliente manejador = new ManejadorCliente(clienteSocket, juego, this);

        clientes[cantidadClientes] = manejador;
        cantidadClientes++;

        new Thread(manejador).start();
    }

    public synchronized void eliminarManejador(ManejadorCliente manejador) {

        for (int i = 0; i < cantidadClientes; i++) {

            if (clientes[i] == manejador) {

                for (int j = i; j < cantidadClientes - 1; j++) {
                    clientes[j] = clientes[j + 1];
                }

                clientes[cantidadClientes - 1] = null;
                cantidadClientes--;
                break;
            }
        }
    }

    public synchronized void enviarATodos(String mensaje) {

        for (int i = 0; i < cantidadClientes; i++) {

            if (clientes[i] != null) {
                clientes[i].enviarMensaje(mensaje);
            }
        }
    }

    /**
     * Envia un mensaje solo al cliente del jugador indicado.
     */
    public synchronized void enviarAJugador(String idJugador, String mensaje) {

        for (int i = 0; i < cantidadClientes; i++) {

            if (clientes[i] != null
                    && clientes[i].getJugador() != null
                    && clientes[i].getJugador().getId().equals(idJugador)) {

                clientes[i].enviarMensaje(mensaje);
            }
        }
    }

    public void detener() {

        activo = false;

        try {

            if (servidorSocket != null && !servidorSocket.isClosed()) {
                servidorSocket.close();
            }

            System.out.println("Servidor detenido.");

        } catch (IOException e) {
            System.out.println("Error al cerrar el servidor: " + e.getMessage());
        }
    }

    //  MODULO ELECTRONICO

    /**
     * Registra al modulo electronico. Deja de ser un "jugador" para los
     * mensajes a todos y, desde ahora, los pagos exigen tarjeta.
     */
    public synchronized boolean registrarDispositivo(ManejadorCliente manejador) {

        if (dispositivo != null && dispositivo != manejador) {
            return false;
        }

        eliminarManejador(manejador);
        dispositivo = manejador;
        juego.setExigirTarjeta(true);

        System.out.println("Modulo electronico conectado.");
        enviarATodos(Protocolo.EVENTO + "|Modulo electronico conectado");

        return true;
    }

    public synchronized boolean hayDispositivo() {
        return dispositivo != null;
    }

    /**
     * El modulo se desconecto: la partida sigue por software. Lo que estaba
     * esperando se resuelve solo para que nadie quede trabado.
     */
    public synchronized void dispositivoDesconectado(ManejadorCliente manejador) {

        if (dispositivo != manejador) {
            return;
        }

        dispositivo = null;
        juego.setExigirTarjeta(false);

        String queEsperaba = esperando;
        String id = jugadorEsperado;
        esperando = null;
        jugadorEsperado = null;

        System.out.println("Modulo electronico desconectado.");
        enviarATodos(Protocolo.EVENTO + "|Modulo electronico desconectado: se juega por software");

        try {

            if (ESPERA_DADO.equals(queEsperaba)) {

                Jugador jugador = juego.buscarJugador(id);
                resolverResultado(jugador, juego.tirarDados(id), true);

            } else if (ESPERA_PAGO.equals(queEsperaba)) {

                completarPago(juego.buscarJugador(id));
            }

        } catch (OperacionInvalidaException e) {
            enviarAJugador(id, "ERROR|" + e.getMessage());
        }
    }

    private void enviarADispositivo(String json) {

        if (dispositivo != null) {
            dispositivo.enviarMensaje(json);
        }
    }

    private void liberarEspera() {

        if (esperando != null) {
            esperando = null;
            jugadorEsperado = null;
            enviarADispositivo(Json.construir("DESACTIVAR"));
        }
    }

    private void validarLibre() {

        if (esperando != null) {
            throw new OperacionInvalidaException(
                "Hay una operacion esperando al modulo electronico");
        }
    }

    //  ACCIONES DE JUEGO (las piden los clientes)

    /**
     * TIRAR_DADOS. Con modulo electronico se activa el dado fisico; sin el,
     * el servidor sortea.
     */
    public synchronized void tirarDados(Jugador jugador) {

        validarLibre();
        juego.validarPuedeTirar(jugador.getId());

        if (dispositivo == null) {
            resolverResultado(jugador, juego.tirarDados(jugador.getId()), true);
            return;
        }

        esperando = ESPERA_DADO;
        jugadorEsperado = jugador.getId();

        enviarATodos(Protocolo.EVENTO + "|" + jugador.getNombre()
                + " debe pulsar el boton del dado electronico");

        enviarADispositivo(Json.construir("ACTIVAR_DADOS",
                "jugador", jugador.getId(), "nombre", jugador.getNombre()));
    }

    /**
     * La Pico mando el resultado del dado fisico.
     */
    public synchronized void recibirDado(int dado1, int dado2) {

        if (!ESPERA_DADO.equals(esperando)) {
            enviarADispositivo(Json.construir("DADO_ERROR", "mensaje", "No se espera ninguna tirada"));
            return;
        }

        String id = jugadorEsperado;
        Jugador jugador = juego.buscarJugador(id);

        try {

            ResultadoEfecto resultado = juego.tirarDados(id, dado1, dado2);

            esperando = null;
            jugadorEsperado = null;

            resolverResultado(jugador, resultado, true);

        } catch (OperacionInvalidaException e) {

            // Resultado invalido: se vuelve a pedir la tirada.
            enviarADispositivo(Json.construir("DADO_ERROR", "mensaje", e.getMessage()));
            enviarADispositivo(Json.construir("ACTIVAR_DADOS",
                    "jugador", id, "nombre", jugador.getNombre()));
            enviarAJugador(id, "ERROR|" + e.getMessage());
        }
    }

    public synchronized void comprar(Jugador jugador) {

        validarLibre();

        Propiedad propiedad = juego.getCompraPendiente();

        if (propiedad == null) {
            throw new OperacionInvalidaException("No hay ninguna propiedad para comprar");
        }

        if (!juego.esTurnoDe(jugador.getId())) {
            throw new OperacionInvalidaException("No es el turno de " + jugador.getNombre());
        }

        if (dispositivo == null) {
            completarCompra(jugador);
            return;
        }

        if (!jugador.puedePagar(propiedad.getPrecio())) {
            throw new OperacionInvalidaException("Saldo insuficiente: " + jugador.getNombre()
                    + " tiene " + jugador.getSaldo() + " y " + propiedad.getNombre()
                    + " cuesta " + propiedad.getPrecio());
        }

        esperando = ESPERA_COMPRA;
        jugadorEsperado = jugador.getId();

        enviarATodos(Protocolo.EVENTO + "|" + jugador.getNombre()
                + " debe acercar su tarjeta para comprar " + propiedad.getNombre());

        enviarADispositivo(Json.construir("LEER_TARJETA",
                "jugador", jugador.getId(), "nombre", jugador.getNombre(),
                "motivo", "Compra de " + propiedad.getNombre(),
                "monto", String.valueOf(propiedad.getPrecio())));
    }

    public synchronized void noComprar(Jugador jugador) {

        // Si estaba esperando la tarjeta para comprar, se cancela la espera.
        if (ESPERA_COMPRA.equals(esperando) && jugador.getId().equals(jugadorEsperado)) {
            liberarEspera();
        }

        validarLibre();

        Propiedad propiedad = juego.getCompraPendiente();

        juego.rechazarCompra(jugador.getId());

        enviarATodos(Protocolo.OFERTA_RECHAZADA + "|" + jugador.getId());
        enviarATodos(Protocolo.EVENTO + "|" + jugador.getNombre()
                + " no compro " + propiedad.getNombre());

        difundirEstado();
    }

    public synchronized void terminarTurno(Jugador jugador) {

        validarLibre();

        juego.terminarTurno(jugador.getId());

        if (!anunciarFinSiTermino()) {
            enviarATodos(Protocolo.TURNO + "|" + juego.obtenerJugadorActual().getId());
        }

        difundirEstado();
    }

    /**
     * EXPORTAR_TRANSACCIONES (punto 13).
     *
     * @return ruta absoluta del archivo generado.
     */
    public synchronized String exportarReporte() {

        juego.getBanco().exportarTransacciones(RUTA_REPORTE);

        return new java.io.File(RUTA_REPORTE).getAbsolutePath();
    }

    /**
     * Un jugador se desconecto (cerro la ventana o se cayo la red).
     */
    public synchronized void jugadorDesconectado(Jugador jugador) {

        if (jugador == null) {
            return;
        }

        boolean eraTurno = juego.estaIniciado() && juego.esTurnoDe(jugador.getId());

        if (esperando != null && jugador.getId().equals(jugadorEsperado)) {
            liberarEspera();
        }

        boolean eliminado = juego.retirarJugador(jugador.getId());

        if (!juego.estaIniciado()) {

            enviarATodos(Protocolo.EVENTO + "|" + jugador.getNombre()
                    + " salio de la sala. Jugadores: " + juego.getCantidadJugadores());
            return;
        }

        if (!eliminado) {
            return;
        }

        enviarATodos(Protocolo.EVENTO + "|" + jugador.getNombre() + " se desconecto");
        enviarATodos(Protocolo.ELIMINADO + "|" + jugador.getId());

        if (!anunciarFinSiTermino() && eraTurno) {
            enviarATodos(Protocolo.TURNO + "|" + juego.obtenerJugadorActual().getId());
        }

        difundirEstado();
    }

    //  TARJETA RFID

    /**
     * La Pico leyo una tarjeta. El servidor identifica al jugador, valida la
     * operacion, modifica el saldo, genera la transaccion y la muestra.
     */
    public synchronized void recibirTarjeta(String uid) {

        if (!ESPERA_PAGO.equals(esperando) && !ESPERA_COMPRA.equals(esperando)) {
            enviarADispositivo(Json.construir("RFID_ERROR", "mensaje", "No se espera ninguna tarjeta"));
            return;
        }

        String id = jugadorEsperado;

        try {

            Jugador jugador = juego.identificarTarjeta(uid, id);

            String que = esperando;
            esperando = null;
            jugadorEsperado = null;

            if (ESPERA_PAGO.equals(que)) {
                completarPago(jugador);
            } else {
                completarCompra(jugador);
            }

            enviarADispositivo(Json.construir("RFID_OK", "mensaje", "Operacion aprobada para " + jugador.getNombre()));

        } catch (OperacionInvalidaException e) {

            // Tarjeta equivocada: la Pico deja de leer tras cada tarjeta, asi que
            // se le vuelve a pedir la lectura para la correcta.
            enviarADispositivo(Json.construir("RFID_ERROR", "mensaje", e.getMessage()));
            Jugador esperado = juego.buscarJugador(id);
            enviarADispositivo(Json.construir("LEER_TARJETA",
                    "jugador", id, "nombre", esperado.getNombre(),
                    "motivo", "Tarjeta incorrecta, intenta de nuevo",
                    "monto", String.valueOf(juego.hayPagoPendiente() ? juego.montoPendiente() : 0)));
            enviarAJugador(id, "ERROR|" + e.getMessage());
        }
    }

    private void completarCompra(Jugador jugador) {

        Propiedad propiedad = juego.getCompraPendiente();

        juego.comprarPropiedadPendiente(jugador.getId());

        enviarATodos(Protocolo.COMPRA + "|" + jugador.getId() + "|" + propiedad.getId()
                + "|" + propiedad.getNombre() + "|" + propiedad.getPrecio()
                + "|" + jugador.getSaldo());

        anunciarTransaccionesNuevas();
        difundirEstado();
    }

    private void completarPago(Jugador jugador) {

        ResultadoEfecto resultado = juego.confirmarPagoPendiente(jugador.getId());

        resolverResultado(jugador, resultado, false);
    }

    //  RESULTADOS

    /**
     * Informa a todos lo que paso despues de una tirada o de un pago.
     *
     * @param conMovimiento true si hay que anunciar DADOS y MOVIMIENTO.
     */
    private void resolverResultado(Jugador jugador, ResultadoEfecto resultado, boolean conMovimiento) {

        if (conMovimiento) {

            enviarATodos(Protocolo.DADOS + "|" + jugador.getId()
                    + "|" + juego.getDado().getValor1()
                    + "|" + juego.getDado().getValor2()
                    + "|" + juego.getDado().getTotal());

            enviarATodos(Protocolo.MOVIMIENTO + "|" + jugador.getId()
                    + "|" + jugador.getCasillaActual().getId()
                    + "|" + jugador.getCasillaActual().getNombre());
        }

        enviarATodos(Protocolo.EVENTO + "|" + resultado.getMensaje());

        if (juego.hayPagoPendiente()) {

            // Hay que cobrar: se activa el lector RFID para ese jugador.
            esperando = ESPERA_PAGO;
            jugadorEsperado = jugador.getId();

            enviarADispositivo(Json.construir("LEER_TARJETA",
                    "jugador", jugador.getId(), "nombre", jugador.getNombre(),
                    "motivo", resultado.getMensaje(),
                    "monto", String.valueOf(juego.montoPendiente())));
        }

        if (resultado.hayPropiedadEnVenta()) {

            Propiedad propiedad = resultado.getPropiedadEnVenta();

            // Solo el jugador en turno decide si compra.
            enviarAJugador(jugador.getId(), Protocolo.OFERTA_COMPRA + "|" + propiedad.getId()
                    + "|" + propiedad.getNombre() + "|" + propiedad.getPrecio());
        }

        anunciarTransaccionesNuevas();

        if (resultado.jugadorEliminado()) {

            enviarATodos(Protocolo.ELIMINADO + "|" + jugador.getId());

            if (!anunciarFinSiTermino()) {
                enviarATodos(Protocolo.TURNO + "|" + juego.obtenerJugadorActual().getId());
            }
        }

        difundirEstado();
    }

    /**
     * Se muestran a todos las transacciones nuevas del historial (paso 8 del
     * enunciado): compras, alquileres, premios, eventos.
     */
    private void anunciarTransaccionesNuevas() {

        int total = juego.getBanco().getHistorial().getCantidad();

        if (total <= transaccionesAnunciadas) {
            return;
        }

        int[] contador = { 0 };

        juego.getBanco().getHistorial().recorrerDesdeLaMasAntigua(t -> {

            contador[0]++;

            if (contador[0] > transaccionesAnunciadas) {
                enviarATodos(Protocolo.EVENTO + "|Transaccion #" + t.getId() + ": "
                        + t.getTipo().getDescripcion() + ", " + t.getOrigen()
                        + " -> " + t.getDestino() + ", monto " + t.getMonto());
            }
        });

        transaccionesAnunciadas = total;
    }

    public synchronized void difundirEstado() {

        String estado = juego.mensajeEstado();

        if (estado != null) {
            enviarATodos(estado);
        }
    }

    /**
     * Si la partida termino, anuncia al ganador y exporta el reporte TXT.
     *
     * @return true si la partida habia terminado.
     */
    private boolean anunciarFinSiTermino() {

        if (!juego.estaIniciado() || !juego.getBanco().partidaTerminada()) {
            return false;
        }

        if (finAnunciado) {
            return true;
        }

        finAnunciado = true;
        liberarEspera();

        try {

            Jugador ganador = juego.getBanco().getGanador();

            enviarATodos(Protocolo.FIN_PARTIDA + "|" + ganador.getId());

        } catch (OperacionInvalidaException e) {
            System.out.println("Partida terminada sin jugadores.");
        }

        try {

            String ruta = exportarReporte();

            enviarATodos(Protocolo.EVENTO + "|Reporte de transacciones exportado en el servidor: " + ruta);

        } catch (RuntimeException e) {
            System.out.println("No se pudo exportar el reporte: " + e.getMessage());
        }

        return true;
    }

    //  CONSULTAS

    public boolean estaActivo() {
        return activo;
    }

    public int getPuerto() {
        return puerto;
    }

    public Juego getJuego() {
        return juego;
    }

    /**
     * Uso: java Servidor.Servidor [puerto] [maxTurnos]
     */
    public static void main(String[] args) {

        int puerto = Protocolo.PUERTO;
        int maxTurnos = Banco.MAX_TURNOS_POR_DEFECTO;

        try {

            if (args.length > 0) {
                puerto = Integer.parseInt(args[0]);
            }

            if (args.length > 1) {
                maxTurnos = Integer.parseInt(args[1]);
            }

        } catch (NumberFormatException e) {
            System.out.println("Uso: java Servidor.Servidor [puerto] [maxTurnos]");
            return;
        }

        System.out.println("Limite de turnos: " + maxTurnos);

        new Servidor(puerto, maxTurnos).iniciar();
    }
}
