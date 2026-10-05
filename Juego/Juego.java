package Juego;

import dominio.Banco;
import dominio.Jugador;
import dominio.OperacionInvalidaException;

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

        // *

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

        banco.terminarTurno(jugador);
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
     * Se obtiene el banco de la partida.
     *
     * @return banco utilizado.
     */
    public Banco getBanco() {
        return banco;
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
