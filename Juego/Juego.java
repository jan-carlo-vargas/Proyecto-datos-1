package Juego;

import Protocolo.Protocolo;
import dominio.Banco;
import dominio.Dado;
import dominio.Jugador;
import dominio.OperacionInvalidaException;
import dominio.Propiedad;
import dominio.ResultadoEfecto;
import dominio.Tablero;

/**
 * Se encarga de coordinar la partida de Monopoly.
 *
 * Se utiliza como intermediario entre el servidor y las
 * clases encargadas de mantener el estado oficial.
 */
public class Juego {

    // Cantidad maxima de jugadores permitidos.
    public static final int MAX_JUGADORES = 4;

    // Se utiliza para mantener el estado oficial de la partida.
    private final Banco banco;

    // Se utiliza para localizar jugadores por su identificador.
    private final Jugador[] jugadores;

    // Cantidad de jugadores registrados actualmente.
    private int cantidadJugadores;

    // Indica si la partida ya fue iniciada.
    private boolean iniciado;

    // Tablero de la partida. Se crea al iniciar, cuando ya estan los 4 jugadores.
    private Tablero tablero;

    // Los dos dados electronicos del juego.
    private final Dado dado = new Dado();

    // Propiedad que se le ofrecio al jugador en turno y todavia no decide
    // comprar o no. null si no hay ninguna oferta pendiente.
    private Propiedad compraPendiente;

    /**
     * Se crea una nueva partida.
     */
    public Juego() {

        // Se utiliza la configuracion predeterminada del banco.
        banco = new Banco();

        // Se reserva espacio para los cuatro jugadores.
        jugadores =
            new Jugador[MAX_JUGADORES];

        cantidadJugadores = 0;
        iniciado = false;
    }

    /**
     * Se registra un jugador en la partida.
     *
     * @param id identificador del jugador.
     * @param nombre nombre del jugador.
     * @return jugador registrado.
     */
    public synchronized Jugador registrarJugador(
        String id,
        String nombre
    ) {

        if (iniciado) {
            throw new OperacionInvalidaException(
                "La partida ya fue iniciada"
            );
        }

        if (cantidadJugadores >= MAX_JUGADORES) {
            throw new OperacionInvalidaException(
                "La partida ya tiene cuatro jugadores"
            );
        }

        if (buscarJugador(id) != null) {
            throw new OperacionInvalidaException(
                "Ya existe un jugador con el id " + id
            );
        }

        // Se crea el jugador utilizando el saldo definido por el banco.
        Jugador jugador =
            new Jugador(
                id,
                nombre,
                Banco.SALDO_INICIAL_POR_DEFECTO
            );

        // Se agrega el jugador a la cola oficial de turnos.
        banco.agregarJugador(jugador);

        // Se guarda la referencia para localizarlo posteriormente.
        jugadores[cantidadJugadores] =
            jugador;

        cantidadJugadores++;

        return jugador;
    }

    /**
     * Se busca un jugador mediante su identificador.
     *
     * @param id identificador buscado.
     * @return jugador encontrado o null.
     */
    public synchronized Jugador buscarJugador(String id) {

        if (id == null) {
            return null;
        }

        for (int i = 0; i < cantidadJugadores; i++) {

            if (jugadores[i].getId().equals(id)) {
                return jugadores[i];
            }
        }

        return null;
    }

    /**
     * Se inicia la partida.
     */
    public synchronized void iniciarPartida() {

        if (iniciado) {
            throw new OperacionInvalidaException(
                "La partida ya fue iniciada"
            );
        }

        if (cantidadJugadores != MAX_JUGADORES) {
            throw new OperacionInvalidaException(
                "Se necesitan cuatro jugadores para iniciar la partida"
            );
        }

        // Se crea el tablero y todos los jugadores salen de Inicio.
        tablero = Tablero.crearPorDefecto();

        for (int i = 0; i < cantidadJugadores; i++) {
            tablero.colocarEnInicio(jugadores[i]);
        }

        iniciado = true;
    }

    /**
     * Se obtiene el jugador que posee el turno actual.
     *
     * @return jugador actual.
     */
    public synchronized Jugador obtenerJugadorActual() {

        if (!iniciado) {
            throw new OperacionInvalidaException(
                "La partida no ha sido iniciada"
            );
        }

        return banco.jugadorActual();
    }

    /**
     * Se consulta si un jugador posee el turno.
     *
     * @param idJugador identificador del jugador.
     * @return true si corresponde su turno.
     */
    public synchronized boolean esTurnoDe(
        String idJugador
    ) {

        Jugador jugador =
            buscarJugador(idJugador);

        if (jugador == null) {
            return false;
        }

        return banco.esSuTurno(jugador);
    }

    /**
     * Se termina el turno de un jugador.
     *
     * @param idJugador identificador del jugador.
     */
    public synchronized void terminarTurno(
        String idJugador
    ) {

        Jugador jugador =
            obtenerJugador(idJugador);

        // Si no compro, la oferta se pierde al terminar el turno.
        compraPendiente = null;

        banco.terminarTurno(jugador);

        saltarTurnosPerdidos();
    }

    /**
     * Se lanzan los dados de un jugador (comando TIRAR_DADOS).
     *
     * El banco valida que sea su turno y que no haya tirado ya. Despues se
     * mueve la ficha, se paga el premio si paso por Inicio y se ejecuta el
     * efecto de la casilla donde cayo.
     *
     * @param idJugador identificador del jugador.
     * @return lo que ocurrio, listo para enviarse a todos los clientes. Si
     *         hayPropiedadEnVenta() es true, el jugador debe responder con
     *         COMPRAR_PROPIEDAD o NO_COMPRAR.
     */
    public synchronized ResultadoEfecto tirarDados(String idJugador) {

        Jugador jugador = prepararTirada(idJugador);

        dado.tirar();

        return moverYResolver(jugador);
    }

    /**
     * Igual que tirarDados(), pero con el resultado del dado fisico (los dos
     * displays de la protoboard) en vez de sortearlo aqui.
     */
    public synchronized ResultadoEfecto tirarDados(
        String idJugador,
        int valor1,
        int valor2
    ) {

        Jugador jugador = prepararTirada(idJugador);

        dado.fijarValores(valor1, valor2);

        return moverYResolver(jugador);
    }

    /**
     * Se compra la propiedad que se le ofrecio al jugador (COMPRAR_PROPIEDAD).
     * El banco valida turno, que no tenga duenio y que alcance el saldo; si
     * rechaza, la oferta sigue pendiente y el jugador puede elegir NO_COMPRAR.
     */
    public synchronized void comprarPropiedadPendiente(String idJugador) {

        Jugador jugador = obtenerJugador(idJugador);

        if (compraPendiente == null) {
            throw new OperacionInvalidaException(
                "No hay ninguna propiedad para comprar"
            );
        }

        banco.comprarPropiedad(jugador, compraPendiente);

        compraPendiente = null;
    }

    /**
     * El jugador decide no comprar la propiedad ofrecida (NO_COMPRAR).
     */
    public synchronized void rechazarCompra(String idJugador) {

        Jugador jugador = obtenerJugador(idJugador);

        if (!banco.esSuTurno(jugador)) {
            throw new OperacionInvalidaException(
                "No es el turno de " + jugador.getNombre()
            );
        }

        if (compraPendiente == null) {
            throw new OperacionInvalidaException(
                "No hay ninguna propiedad para rechazar"
            );
        }

        compraPendiente = null;
    }

    /**
     * Pasos comunes a las dos formas de tirar: la partida debe estar iniciada
     * y el banco debe aceptar el lanzamiento.
     */
    private Jugador prepararTirada(String idJugador) {

        if (!iniciado) {
            throw new OperacionInvalidaException(
                "La partida no ha sido iniciada"
            );
        }

        Jugador jugador = obtenerJugador(idJugador);

        banco.registrarLanzamientoDados(jugador);

        return jugador;
    }

    /**
     * Mueve la ficha con el resultado de los dados y ejecuta la casilla.
     */
    private ResultadoEfecto moverYResolver(Jugador jugador) {

        boolean pasoPorInicio =
            tablero.moverJugador(jugador, dado.getTotal());

        if (pasoPorInicio) {
            banco.pagarPremioPorInicio(jugador);
        }

        ResultadoEfecto resultado =
            jugador.getCasillaActual()
                .ejecutarEfecto(jugador, banco, tablero);

        compraPendiente = resultado.getPropiedadEnVenta();

        // Si quebro, el banco ya cerro su turno: hay que revisar si al
        // siguiente le toca perder el suyo.
        if (resultado.jugadorEliminado()) {
            compraPendiente = null;
            saltarTurnosPerdidos();
        }

        return resultado;
    }

    /**
     * Pasa de largo a los jugadores que deben perder el turno (carta de
     * evento o reten). Cada vuelta gasta un turno perdido, asi que el bucle
     * siempre termina.
     */
    private void saltarTurnosPerdidos() {

        while (!banco.partidaTerminada()
                && banco.jugadorActual().debePerderTurno()) {

            Jugador castigado = banco.jugadorActual();

            castigado.consumirTurnoPerdido();

            banco.terminarTurno(castigado);
        }
    }

    /**
     * Se obtiene un jugador existente.
     *
     * @param id identificador del jugador.
     * @return jugador encontrado.
     */
    private Jugador obtenerJugador(String id) {

        Jugador jugador =
            buscarJugador(id);

        if (jugador == null) {
            throw new OperacionInvalidaException(
                "No existe el jugador " + id
            );
        }

        return jugador;
    }

    /**
     * Mensaje TABLERO con las casillas (ver Protocolo.TABLERO).
     * Devuelve null si la partida no ha iniciado.
     */
    public synchronized String mensajeTablero() {

        if (tablero == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(Protocolo.TABLERO + "|");

        for (int i = 0; i < tablero.getCantidadCasillas(); i++) {

            dominio.Casilla c = tablero.getCasilla(i);
            String tipo = "ESPECIAL";
            int precio = 0;

            if (c instanceof Propiedad) {
                tipo = "PROPIEDAD";
                precio = ((Propiedad) c).getPrecio();
            } else if (c instanceof dominio.CasillaEvento) {
                tipo = "EVENTO";
            }

            if (i > 0) {
                sb.append(';');
            }

            sb.append(c.getId()).append(':')
              .append(c.getNombre()).append(':')
              .append(tipo).append(':')
              .append(precio);
        }

        return sb.toString();
    }

    /**
     * Mensaje ESTADO_JUEGO con turno, jugadores y duenos (ver Protocolo.ESTADO_JUEGO).
     * Devuelve null si la partida no ha iniciado.
     */
    public synchronized String mensajeEstado() {

        if (!iniciado) {
            return null;
        }

        String turno = "-";

        if (!banco.partidaTerminada()) {
            turno = obtenerJugadorActual().getId();
        }

        StringBuilder jug = new StringBuilder();
        StringBuilder duenos = new StringBuilder();

        for (int i = 0; i < cantidadJugadores; i++) {

            Jugador j = jugadores[i];

            if (i > 0) {
                jug.append(';');
            }

            jug.append(j.getId()).append(':')
               .append(j.getNombre()).append(':')
               .append(j.getSaldo()).append(':')
               .append(j.getCasillaActual().getId()).append(':')
               .append(j.estaActivo() ? 1 : 0);
        }

        for (int i = 0; i < tablero.getCantidadCasillas(); i++) {

            dominio.Casilla c = tablero.getCasilla(i);

            if (c instanceof Propiedad && ((Propiedad) c).getPropietario() != null) {

                if (duenos.length() > 0) {
                    duenos.append(',');
                }

                duenos.append(c.getId()).append('=')
                      .append(((Propiedad) c).getPropietario().getId());
            }
        }

        return Protocolo.ESTADO_JUEGO + "|" + turno
                + "|" + banco.getNumeroTurno()
                + "|" + banco.getMaxTurnos()
                + "|" + jug + "|" + duenos;
    }

    /**
     * Se obtiene el banco de la partida.
     *
     * @return banco utilizado.
     */
    public Banco getBanco() {
        return banco;
    }

    /**
     * Se obtiene el tablero (null si la partida no ha iniciado).
     */
    public synchronized Tablero getTablero() {
        return tablero;
    }

    /**
     * Se obtiene el dado, para consultar el ultimo resultado.
     */
    public Dado getDado() {
        return dado;
    }

    /**
     * Se obtiene la propiedad ofrecida y aun no decidida (o null).
     */
    public synchronized Propiedad getCompraPendiente() {
        return compraPendiente;
    }

    /**
     * Se obtiene la cantidad de jugadores registrados.
     *
     * @return cantidad de jugadores.
     */
    public synchronized int getCantidadJugadores() {
        return cantidadJugadores;
    }

    /**
     * Se consulta si la partida fue iniciada.
     *
     * @return true si la partida esta iniciada.
     */
    public synchronized boolean estaIniciado() {
        return iniciado;
    }
}
