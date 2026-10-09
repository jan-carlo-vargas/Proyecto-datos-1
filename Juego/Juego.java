package Juego;

import Protocolo.Protocolo;
import dominio.Banco;
import dominio.Casilla;
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

    // true cuando hay un modulo electronico conectado: los pagos obligatorios
    // (alquiler, impuesto) esperan a que el jugador acerque su tarjeta RFID.
    private boolean exigirTarjeta;

    // true si el jugador en turno cayo en una casilla que cobra y todavia no
    // acerco su tarjeta. Hasta entonces no puede terminar el turno.
    private boolean pagoPendiente;

    /**
     * Se crea una nueva partida.
     */
    public Juego() {
        this(Banco.MAX_TURNOS_POR_DEFECTO);
    }

    /**
     * Se crea una partida con un limite de turnos configurable (punto 18).
     *
     * @param maxTurnos cantidad maxima de turnos antes de decidir por patrimonio.
     */
    public Juego(int maxTurnos) {

        banco = new Banco(maxTurnos, Banco.PREMIO_POR_INICIO_POR_DEFECTO);

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

        validarPartidaActiva();

        Jugador jugador =
            obtenerJugador(idJugador);

        if (banco.esSuTurno(jugador)) {

            if (pagoPendiente) {
                throw new OperacionInvalidaException(
                    "Debe pagar con su tarjeta antes de terminar el turno"
                );
            }

            if (!banco.yaLanzoDados()) {
                throw new OperacionInvalidaException(
                    "Debe tirar los dados antes de terminar el turno"
                );
            }
        }

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

        // Se validan las caras ANTES de gastar el lanzamiento del turno.
        if (valor1 < 1 || valor1 > Dado.CARAS
                || valor2 < 1 || valor2 > Dado.CARAS) {
            throw new OperacionInvalidaException(
                "Resultado de dados invalido: " + valor1 + " y " + valor2
            );
        }

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

        validarPartidaActiva();

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

        validarPartidaActiva();

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

        validarPartidaActiva();

        if (pagoPendiente) {
            throw new OperacionInvalidaException(
                "Hay un pago pendiente"
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

        Casilla casilla = jugador.getCasillaActual();

        // Con modulo electronico, los cobros obligatorios se validan con la
        // tarjeta RFID: se detiene aqui hasta confirmarPagoPendiente().
        if (exigirTarjeta && exigePago(casilla, jugador)) {

            pagoPendiente = true;
            compraPendiente = null;

            return ResultadoEfecto.informativo(
                jugador.getNombre() + " cayo en " + casilla.getNombre()
                + " y debe pagar " + montoPendiente()
                + ": acerca tu tarjeta RFID"
            );
        }

        return resolverCasilla(jugador);
    }

    /**
     * Ejecuta el efecto de la casilla donde esta parado el jugador.
     */
    private ResultadoEfecto resolverCasilla(Jugador jugador) {

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
     * true si caer en la casilla obliga a pagar (alquiler de otro jugador o
     * impuesto). Las cartas de evento se resuelven solas.
     */
    private boolean exigePago(Casilla casilla, Jugador jugador) {

        if (casilla instanceof Propiedad) {

            Propiedad propiedad = (Propiedad) casilla;

            return propiedad.getPropietario() != null
                && !propiedad.esDe(jugador)
                && propiedad.getAlquiler() > 0;
        }

        if (casilla instanceof dominio.CasillaEspecial) {

            return ((dominio.CasillaEspecial) casilla).getTipo()
                == dominio.CasillaEspecial.Tipo.IMPUESTO;
        }

        return false;
    }

    /**
     * Monto del pago que espera la tarjeta (0 si no hay pago pendiente).
     */
    public synchronized int montoPendiente() {

        if (!pagoPendiente) {
            return 0;
        }

        Casilla casilla = banco.jugadorActual().getCasillaActual();

        if (casilla instanceof Propiedad) {
            return ((Propiedad) casilla).getAlquiler();
        }

        if (casilla instanceof dominio.CasillaEspecial) {
            return ((dominio.CasillaEspecial) casilla).getMonto();
        }

        return 0;
    }

    /**
     * El jugador acerco su tarjeta y el servidor la valido: se cobra.
     */
    public synchronized ResultadoEfecto confirmarPagoPendiente(String idJugador) {

        validarPartidaActiva();

        Jugador jugador = obtenerJugador(idJugador);

        if (!banco.esSuTurno(jugador)) {
            throw new OperacionInvalidaException(
                "No es el turno de " + jugador.getNombre()
            );
        }

        if (!pagoPendiente) {
            throw new OperacionInvalidaException(
                "No hay ningun pago pendiente"
            );
        }

        pagoPendiente = false;

        return resolverCasilla(jugador);
    }

    public synchronized boolean hayPagoPendiente() {
        return pagoPendiente;
    }

    public synchronized void setExigirTarjeta(boolean exigir) {
        exigirTarjeta = exigir;
    }

    /**
     * Valida una tarjeta RFID para el jugador que debe operar.
     *
     * La primera vez que un jugador acerca una tarjeta que nadie usa, queda
     * asociada a el. Despues solo esa tarjeta lo identifica. La tarjeta no
     * guarda saldo: solo dice quien es.
     */
    public synchronized Jugador identificarTarjeta(String uid, String idEsperado) {

        if (uid == null || uid.trim().isEmpty()) {
            throw new OperacionInvalidaException("Tarjeta sin UID");
        }

        String limpio = uid.trim().toUpperCase();
        Jugador esperado = obtenerJugador(idEsperado);

        for (int i = 0; i < cantidadJugadores; i++) {

            if (limpio.equals(jugadores[i].getUidTarjeta())) {

                if (jugadores[i].equals(esperado)) {
                    return esperado;
                }

                throw new OperacionInvalidaException(
                    "Esa tarjeta pertenece a " + jugadores[i].getNombre()
                    + ", se esperaba a " + esperado.getNombre()
                );
            }
        }

        if (esperado.getUidTarjeta() != null) {
            throw new OperacionInvalidaException(
                "Tarjeta no reconocida para " + esperado.getNombre()
            );
        }

        esperado.setUidTarjeta(limpio);

        return esperado;
    }

    /**
     * Se verifica, sin cambiar nada, que el jugador pueda tirar ahora.
     * Sirve para no activar el dado fisico si la tirada seria rechazada.
     */
    public synchronized void validarPuedeTirar(String idJugador) {

        if (!iniciado) {
            throw new OperacionInvalidaException(
                "La partida no ha sido iniciada"
            );
        }

        validarPartidaActiva();

        Jugador jugador = obtenerJugador(idJugador);

        if (!banco.esSuTurno(jugador)) {
            throw new OperacionInvalidaException(
                "No es el turno de " + jugador.getNombre()
            );
        }

        if (banco.yaLanzoDados()) {
            throw new OperacionInvalidaException(
                jugador.getNombre() + " ya lanzo los dados en este turno"
            );
        }

        if (pagoPendiente) {
            throw new OperacionInvalidaException(
                "Hay un pago pendiente"
            );
        }
    }

    /**
     * Un jugador se desconecto. Antes de iniciar, simplemente sale del
     * vestibulo (otro puede tomar su lugar). Durante la partida queda
     * eliminado y sus propiedades quedan libres.
     *
     * @return true si fue eliminado de una partida en curso.
     */
    public synchronized boolean retirarJugador(String idJugador) {

        Jugador jugador = buscarJugador(idJugador);

        if (jugador == null) {
            return false;
        }

        if (!iniciado) {

            banco.quitarJugador(jugador);

            for (int i = 0; i < cantidadJugadores; i++) {

                if (jugadores[i] == jugador) {

                    for (int j = i; j < cantidadJugadores - 1; j++) {
                        jugadores[j] = jugadores[j + 1];
                    }

                    jugadores[cantidadJugadores - 1] = null;
                    cantidadJugadores--;
                    break;
                }
            }

            return false;
        }

        if (!jugador.estaActivo() || banco.partidaTerminada()) {
            return false;
        }

        if (banco.esSuTurno(jugador)) {
            compraPendiente = null;
            pagoPendiente = false;
        }

        banco.eliminarJugador(jugador);

        saltarTurnosPerdidos();

        return true;
    }

    /**
     * Despues de terminada la partida nadie puede seguir jugando.
     */
    private void validarPartidaActiva() {

        if (iniciado && banco.partidaTerminada()) {
            throw new OperacionInvalidaException(
                "La partida ya termino"
            );
        }
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
