package dominio;

import estructuras.ColaCircular;
import estructuras.ListaDoble;

/**
 * Banco: el duenio del estado oficial de la partida.
 *
 * Es la pieza central del punto 3 del enunciado: el cliente NO puede modificar
 * saldo, posicion, propiedades, turno, dados ni transacciones. Todo pasa por
 * aqui, y aqui se valida antes de tocar nada.
 *
 * El servidor lo usa asi:
 *
 *   try {
 *       banco.comprarPropiedad(jugador, propiedad);
 *       return "OK|Propiedad comprada";
 *   } catch (OperacionInvalidaException e) {
 *       return "ERROR|" + e.getMessage();
 *   }
 *
 * Los mensajes ya vienen redactados en espaniol, listos para mostrarse.
 *
 * DOS ESTRUCTURAS SOSTIENEN TODA LA CLASE:
 *   ColaCircular<Jugador> turnos    -> punto 8, el turno pasa al siguiente
 *   HistorialTransacciones historial -> puntos 11, 12 y 13
 *
 * UNA DISTINCION QUE VALE LA PENA ENTENDER: hay dos clases de cobro.
 *   - Cobros VOLUNTARIOS, como comprar una propiedad. Si no alcanza el saldo
 *     se rechaza la operacion y no pasa nada mas. El jugador sigue jugando.
 *   - Cobros OBLIGATORIOS, como un alquiler o una perdida por evento. Si no
 *     alcanza NO se rechazan: se cobra lo que haya y el jugador queda
 *     eliminado. Es la regla de eliminacion del punto 18.
 * Confundirlos es el error clasico: si la compra eliminara al jugador, bastaria
 * con querer comprar algo caro para perder la partida.
 */
public class Banco {

    /** Saldo con el que arranca cada jugador si no se indica otro. */
    public static final int SALDO_INICIAL_POR_DEFECTO = 1500;

    /** Premio por pasar por la casilla de inicio (punto 11). */
    public static final int PREMIO_POR_INICIO_POR_DEFECTO = 200;

    /** Limite de turnos configurable del punto 18. */
    public static final int MAX_TURNOS_POR_DEFECTO = 100;

    // Punto 8: los turnos van en una cola circular. El del frente es a quien le
    // toca; terminar el turno es rotar() y mandarlo al final.
    private final ColaCircular<Jugador> turnos;

    private final HistorialTransacciones historial;

    // Arranca en 1: el primer turno de la partida es el turno 1, no el 0.
    private int numeroTurno;

    private final int maxTurnos;
    private final int premioPorInicio;

    // Los ids de transaccion son consecutivos y los asigna solo esta clase.
    private int siguienteIdTransaccion;

    // Punto 17: impedir lanzar los dados varias veces en el mismo turno.
    // Se reinicia en cerrarTurno().
    private boolean dadosLanzados;

    public Banco() {
        this(MAX_TURNOS_POR_DEFECTO, PREMIO_POR_INICIO_POR_DEFECTO);
    }

    public Banco(int maxTurnos, int premioPorInicio) {
        if (maxTurnos <= 0) {
            throw new IllegalArgumentException("El limite de turnos debe ser positivo: " + maxTurnos);
        }
        if (premioPorInicio < 0) {
            throw new IllegalArgumentException("El premio por pasar por inicio no puede ser negativo: " + premioPorInicio);
        }

        this.turnos = new ColaCircular<>();
        this.historial = new HistorialTransacciones();
        this.numeroTurno = 1;
        this.maxTurnos = maxTurnos;
        this.premioPorInicio = premioPorInicio;
        this.siguienteIdTransaccion = 1;
        this.dadosLanzados = false;
    }

    //  TURNOS (punto 8)

    /**
     * Mete un jugador a la partida. El orden de entrada es el orden de turnos.
     */
    public void agregarJugador(Jugador jugador) {
        validarActivo(jugador);

        if (turnos.contiene(jugador)) {
            throw new OperacionInvalidaException("El jugador " + jugador.getId() + " ya esta en la partida");
        }

        turnos.encolar(jugador);
    }

    /**
     * Saca a un jugador de la cola ANTES de empezar la partida (se desconecto
     * en el vestibulo). Durante la partida se usa eliminarJugador().
     */
    public void quitarJugador(Jugador jugador) {
        validarJugador(jugador);
        turnos.eliminar(jugador);
    }

    /**
     * El jugador que sigue despues de este en la cola de turnos (para la carta
     * de pago entre jugadores). null si no esta en la cola.
     */
    public Jugador siguienteDe(Jugador jugador) {
        Jugador[] orden = new Jugador[turnos.getCantidad()];
        int[] n = { 0 };

        turnos.recorrer(j -> orden[n[0]++] = j);

        for (int i = 0; i < orden.length; i++) {
            if (orden[i].equals(jugador)) {
                return orden[(i + 1) % orden.length];
            }
        }
        return null;
    }

    /**
     * A quien le toca jugar ahora. Es verFrente(), que mira sin sacar de la
     * cola: el jugador tiene que seguir ahi hasta que termine su turno.
     */
    public Jugador jugadorActual() {
        if (turnos.estaVacia()) {
            throw new OperacionInvalidaException("No hay jugadores en la partida");
        }
        return turnos.verFrente();
    }

    /**
     * Consulta sin efectos secundarios: devuelve false en vez de lanzar, para
     * que el servidor pueda preguntar sin envolver todo en un try.
     */
    public boolean esSuTurno(Jugador jugador) {
        if (jugador == null || turnos.estaVacia()) {
            return false;
        }
        return turnos.verFrente().equals(jugador);
    }

    /**
     * Primera validacion del punto 17: nadie juega fuera de su turno.
     */
    private void validarTurno(Jugador jugador) {
        validarJugador(jugador);

        if (!esSuTurno(jugador)) {
            String deQuienEs = turnos.estaVacia() ? "nadie" : turnos.verFrente().getNombre();
            throw new OperacionInvalidaException(
                    "No es el turno de " + jugador.getNombre() + ", le toca a " + deQuienEs);
        }
    }

    /**
     * Anota que el jugador ya tiro los dados en este turno.
     *
     * Cubre dos validaciones del punto 17 de una sola vez: jugar fuera de turno
     * y lanzar los dados varias veces en el mismo turno. El servidor llama a
     * este metodo ANTES de mover la ficha; si lanza excepcion, no se mueve nada.
     */
    public void registrarLanzamientoDados(Jugador jugador) {
        validarTurno(jugador);

        if (dadosLanzados) {
            throw new OperacionInvalidaException(
                    jugador.getNombre() + " ya lanzo los dados en el turno " + numeroTurno);
        }

        dadosLanzados = true;
    }

    public boolean yaLanzoDados() {
        return dadosLanzados;
    }

    /**
     * Cierra el turno actual y se lo pasa al siguiente jugador.
     *
     * El caso borde es la bancarrota. Si el jugador quebro durante su propio
     * turno ya salio de la cola, y ColaCircular.eliminar() dejo el frente
     * apuntando al siguiente: ese turno ya se cerro dentro de
     * eliminarJugador(). Rotar aqui otra vez saltaria el turno de ese
     * siguiente jugador, que nunca llegaria a jugar.
     *
     * Por eso no se pregunta por una bandera sino por el estado real del
     * jugador: si esta inactivo, es que lo eliminaron y no hay turno suyo que
     * cerrar. Una bandera se queda armada hasta que alguien la consume, y
     * termina robandole la rotacion a un jugador que no tuvo nada que ver.
     */
    public void terminarTurno(Jugador jugador) {
        validarJugador(jugador);

        if (!jugador.estaActivo()) {
            return;
        }

        validarTurno(jugador);
        turnos.rotar();
        cerrarTurno();
    }

    /**
     * Cierra el turno vigente: los dados quedan libres para el que sigue y el
     * contador avanza.
     *
     * Lo llaman los dos unicos lugares donde un turno puede terminar:
     * terminarTurno(), cuando el jugador lo cierra normalmente, y
     * eliminarJugador(), cuando lo cierra la bancarrota.
     */
    private void cerrarTurno() {
        dadosLanzados = false;
        numeroTurno++;
    }

    //  DINERO (punto 11: toda operacion economica genera una transaccion)

    /**
     * Premio por pasar por la casilla de inicio.
     * Lo llama el Juego cuando ListaCircularDoble.pasaPorInicio() da true.
     */
    public void pagarPremioPorInicio(Jugador jugador) {
        validarActivo(jugador);

        if (premioPorInicio == 0) {
            return;
        }

        jugador.acreditar(premioPorInicio);
        registrar(TipoTransaccion.PREMIO_POR_INICIO, Transaccion.BANCO, jugador.getId(), premioPorInicio,
                jugador.getNombre() + " paso por la casilla de inicio");
    }

    /**
     * Compra de propiedad (puntos 7 y 17).
     *
     * Es un cobro VOLUNTARIO: si no alcanza el saldo se rechaza y listo, el
     * jugador no queda eliminado. Valida las tres cosas que pide el punto 17:
     * que sea su turno, que la propiedad no tenga duenio y que tenga saldo.
     */
    public void comprarPropiedad(Jugador jugador, Propiedad propiedad) {
        validarTurno(jugador);
        validarPropiedad(propiedad);

        if (!propiedad.estaDisponible()) {
            throw new OperacionInvalidaException("La propiedad " + propiedad.getNombre()
                    + " ya le pertenece a " + propiedad.getPropietario().getNombre());
        }

        if (!jugador.puedePagar(propiedad.getPrecio())) {
            throw new OperacionInvalidaException("Saldo insuficiente: " + jugador.getNombre()
                    + " tiene " + jugador.getSaldo() + " y " + propiedad.getNombre()
                    + " cuesta " + propiedad.getPrecio());
        }

        jugador.comprarPropiedad(propiedad);

        registrar(TipoTransaccion.COMPRA_PROPIEDAD, jugador.getId(), Transaccion.BANCO, propiedad.getPrecio(),
                jugador.getNombre() + " compro " + propiedad.getNombre());
    }

    /**
     * Pago de alquiler (punto 7).
     *
     * Las tres reglas del enunciado quedan resueltas aqui:
     *   sin duenio          -> no se paga nada (la puede comprar)
     *   del mismo jugador   -> no se paga nada
     *   de otro jugador     -> paga el alquiler
     *   alquiler en cero    -> no se paga nada
     *
     * Devuelve true si el jugador sigue en la partida, false si quebro pagando.
     */
    public boolean pagarAlquiler(Jugador inquilino, Propiedad propiedad) {
        validarJugador(inquilino);
        validarPropiedad(propiedad);

        Jugador duenio = propiedad.getPropietario();

        if (duenio == null || duenio.equals(inquilino)) {
            // No se genera transaccion porque no hubo movimiento de dinero.
            return true;
        }

        // Propiedad acepta alquiler 0. Sin esta salida, cobrarObligatorio()
        // rechaza el monto y el juego se cae cuando alguien cae en ella.
        if (propiedad.getAlquiler() == 0) {
            return true;
        }

        return cobrarObligatorio(inquilino, duenio, propiedad.getAlquiler(), TipoTransaccion.PAGO_ALQUILER,
                inquilino.getNombre() + " paga alquiler de " + propiedad.getNombre()
                        + " a " + duenio.getNombre());
    }

    /**
     * Pago obligatorio al banco, por ejemplo un impuesto de casilla especial.
     * Devuelve true si el jugador sigue en la partida.
     */
    public boolean pagarAlBanco(Jugador jugador, int monto, String descripcion) {
        return cobrarObligatorio(jugador, null, monto, TipoTransaccion.PAGO_AL_BANCO, descripcion);
    }

    /**
     * Pago obligatorio de un jugador a otro.
     * Devuelve true si quien paga sigue en la partida.
     */
    public boolean pagarAJugador(Jugador origen, Jugador destino, int monto, String descripcion) {
        validarJugador(origen);
        validarJugador(destino);

        if (origen.equals(destino)) {
            throw new OperacionInvalidaException(origen.getNombre() + " no se puede pagar a si mismo");
        }

        return cobrarObligatorio(origen, destino, monto, TipoTransaccion.PAGO_ENTRE_JUGADORES, descripcion);
    }

    /**
     * Carta de evento que da dinero (punto 10).
     */
    public void aplicarGananciaEvento(Jugador jugador, int monto, String descripcion) {
        validarActivo(jugador);
        validarMonto(monto);

        jugador.acreditar(monto);

        registrar(TipoTransaccion.GANANCIA_POR_EVENTO, Transaccion.BANCO, jugador.getId(), monto, descripcion);
    }

    /**
     * Carta de evento que cobra dinero (punto 10).
     *
     * Es obligatoria: el jugador no puede negarse, asi que si no alcanza queda
     * eliminado. Devuelve true si sigue en la partida.
     */
    public boolean aplicarPerdidaEvento(Jugador jugador, int monto, String descripcion) {
        return cobrarObligatorio(jugador, null, monto, TipoTransaccion.PERDIDA_POR_EVENTO, descripcion);
    }

    /**
     * EL NUCLEO DE TODOS LOS COBROS OBLIGATORIOS.
     *
     * Aqui vive la regla mas delicada del enunciado, la que cruza el punto 17
     * con el 18: un pago obligatorio sin saldo suficiente NO se rechaza, se
     * aplica la eliminacion correspondiente.
     *
     * Si el jugador puede pagar, se cobra normal. Si no puede:
     *   1. se le cobra todo lo que tenga (pago parcial),
     *   2. se registra igual la transaccion, aclarando cuanto debia,
     *   3. sus propiedades pasan al acreedor, o quedan libres si el acreedor
     *      es el banco,
     *   4. queda eliminado y sale de la cola de turnos.
     *
     * acreedor en null significa "el banco".
     * Devuelve true si el jugador sobrevivio al cobro.
     */
    private boolean cobrarObligatorio(Jugador deudor, Jugador acreedor, int monto,
                                      TipoTransaccion tipo, String descripcion) {
        validarActivo(deudor);
        if (acreedor != null) {
            validarActivo(acreedor);
        }
        validarMonto(monto);

        String idAcreedor = (acreedor == null) ? Transaccion.BANCO : acreedor.getId();

        if (deudor.puedePagar(monto)) {
            deudor.debitar(monto);
            if (acreedor != null) {
                acreedor.acreditar(monto);
            }
            registrar(tipo, deudor.getId(), idAcreedor, monto, descripcion);
            return true;
        }

        // Bancarrota (punto 18).
        int pagoParcial = deudor.getSaldo();

        // Se pregunta por cero porque debitar() exige monto positivo: un jugador
        // que ya estaba en cero no tiene nada que entregar.
        if (pagoParcial > 0) {
            deudor.debitar(pagoParcial);
            if (acreedor != null) {
                acreedor.acreditar(pagoParcial);
            }
        }

        registrar(tipo, deudor.getId(), idAcreedor, pagoParcial,
                descripcion + " | BANCARROTA: debia " + monto + " y solo tenia " + pagoParcial);

        eliminarJugador(deudor, acreedor);
        return false;
    }

    //  ELIMINACION Y FIN DE PARTIDA (punto 18)

    /**
     * Saca a un jugador de la partida y deja sus propiedades libres.
     */
    public void eliminarJugador(Jugador jugador) {
        eliminarJugador(jugador, null);
    }

    /**
     * Saca a un jugador de la partida.
     *
     * Si el acreedor es otro jugador, hereda las propiedades; si es el banco
     * (acreedor en null), quedan disponibles para que otro las compre. El
     * enunciado no lo especifica, se eligio lo que hace el Monopoly real.
     *
     * EL DETALLE QUE HAY QUE CUIDAR: si el eliminado era el del frente de la
     * cola, ColaCircular.eliminar() reubica el frente al siguiente por su
     * cuenta. Eso equivale a que el turno ya avanzo. Hay que anotarlo ANTES de
     * sacarlo, porque despues ya no habria forma de saberlo, y avisarle a
     * terminarTurno() para que no rote de nuevo.
     */
    private void eliminarJugador(Jugador jugador, Jugador acreedor) {
        validarJugador(jugador);

        boolean eraSuTurno = esSuTurno(jugador);

        transferirPropiedades(jugador, acreedor);

        jugador.setActivo(false);
        turnos.eliminar(jugador);

        if (eraSuTurno) {
            // Sacarlo de la cola ya dejo el frente en el siguiente jugador:
            // su turno termino aqui. Se cierra completo, para que quien hereda
            // el frente empiece con los dados libres y no arrastre el
            // "ya lanzo los dados" del que acaba de quebrar.
            cerrarTurno();
        }
    }

    /**
     * Mueve todas las propiedades del deudor al acreedor, o las deja libres si
     * el acreedor es el banco.
     *
     * Se saca primero una copia con buscar(p -> true), que devuelve una lista
     * nueva con las mismas referencias. Recorrer una lista mientras se le
     * borran nodos deja los enlaces a medio camino y el recorrido se pierde.
     */
    private void transferirPropiedades(Jugador deudor, Jugador acreedor) {
        ListaDoble<Propiedad> copia = deudor.getPropiedades().buscar(propiedad -> true);

        copia.recorrerDesdeInicio(propiedad -> {
            deudor.perderPropiedad(propiedad);
            if (acreedor != null) {
                acreedor.recibirPropiedad(propiedad);
            }
        });
    }

    /**
     * Cuantos jugadores siguen en juego. Los eliminados salen de la cola, asi
     * que la cantidad de la cola ya es la respuesta.
     */
    public int jugadoresActivos() {
        return turnos.getCantidad();
    }

    /**
     * La partida termina cuando queda un unico jugador activo o cuando se
     * alcanza el limite configurable de turnos (punto 18).
     */
    public boolean partidaTerminada() {
        return turnos.getCantidad() <= 1 || numeroTurno > maxTurnos;
    }

    /**
     * Quien gano.
     *
     * Si queda uno solo, es ese. Si se acabaron los turnos, gana el de mayor
     * patrimonio, que el enunciado define como saldo mas valor de propiedades.
     *
     * El arreglo de un elemento es el mismo truco de calcularPatrimonio(): una
     * lambda no puede reasignar una variable local, pero si puede cambiar el
     * contenido de un arreglo.
     */
    public Jugador getGanador() {
        if (turnos.estaVacia()) {
            throw new OperacionInvalidaException("No queda ningun jugador en la partida");
        }

        if (turnos.getCantidad() == 1) {
            return turnos.verFrente();
        }

        Jugador[] mejor = { null };

        turnos.recorrer(jugador -> {
            if (mejor[0] == null || jugador.calcularPatrimonio() > mejor[0].calcularPatrimonio()) {
                mejor[0] = jugador;
            }
        });

        return mejor[0];
    }

    //  HISTORIAL (puntos 11, 12 y 13)

    /**
     * Crea la transaccion, le pone el id consecutivo y el turno vigente, y la
     * guarda.
     *
     * Es privado a proposito: es el UNICO lugar de todo el proyecto donde nace
     * una transaccion. Si cada clase creara las suyas, los ids se repetirian y
     * el numero de turno podria quedar mal.
     */
    private Transaccion registrar(TipoTransaccion tipo, String origen, String destino,
                                  int monto, String descripcion) {
        Transaccion transaccion = new Transaccion(
                siguienteIdTransaccion, numeroTurno, tipo, origen, destino, monto, descripcion);

        siguienteIdTransaccion++;
        historial.agregar(transaccion);

        return transaccion;
    }

    /**
     * Entrada para transacciones armadas por fuera, por ejemplo al recuperar
     * una partida. En el juego normal las crea registrar().
     *
     * Hay que correr el contador por encima del id que entra, o la siguiente
     * transaccion que genere el banco nacera con un id ya usado. Dos
     * transacciones con el mismo numero rompen buscarPorId(), que devuelve la
     * primera que encuentra y esconde la otra para siempre.
     */
    public void registrarTransaccion(Transaccion transaccion) {
        if (transaccion == null) {
            throw new IllegalArgumentException("La transaccion recibida es nula");
        }

        historial.agregar(transaccion);

        if (transaccion.getId() >= siguienteIdTransaccion) {
            siguienteIdTransaccion = transaccion.getId() + 1;
        }
    }

    /**
     * Busqueda por jugador del punto 12. Delega en el historial.
     */
    public ListaDoble<Transaccion> transaccionesDe(String idJugador) {
        return historial.buscarPorJugador(idJugador);
    }

    /**
     * Busqueda por tipo del punto 12. Delega en el historial.
     */
    public ListaDoble<Transaccion> transaccionesPorTipo(TipoTransaccion tipo) {
        return historial.buscarPorTipo(tipo);
    }

    /**
     * Genera el archivo TXT del punto 13.
     */
    public void exportarTransacciones(String ruta) {
        historial.exportarTxt(ruta);
    }

    public HistorialTransacciones getHistorial() {
        return historial;
    }

    //  CONSULTAS

    public ColaCircular<Jugador> getTurnos() {
        return turnos;
    }

    public int getNumeroTurno() {
        return numeroTurno;
    }

    public int getMaxTurnos() {
        return maxTurnos;
    }

    public int getPremioPorInicio() {
        return premioPorInicio;
    }

    //  VALIDACIONES INTERNAS

    private void validarJugador(Jugador jugador) {
        if (jugador == null) {
            throw new IllegalArgumentException("El jugador recibido es nulo");
        }
    }

    /**
     * Un eliminado ya no juega: no vuelve a la cola, no cobra y no paga.
     *
     * Reingresarlo trabaria la partida, porque terminarTurno() no rota con
     * jugadores inactivos y la cola se quedaria parada en el. Pagarle haria
     * desaparecer el dinero, y cobrarle registraria una segunda bancarrota.
     */
    private void validarActivo(Jugador jugador) {
        validarJugador(jugador);

        if (!jugador.estaActivo()) {
            throw new OperacionInvalidaException(jugador.getNombre() + " ya fue eliminado de la partida");
        }
    }

    private void validarPropiedad(Propiedad propiedad) {
        if (propiedad == null) {
            throw new IllegalArgumentException("La propiedad recibida es nula");
        }
    }

    private void validarMonto(int monto) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero, se recibio: " + monto);
        }
    }

    @Override
    public String toString() {
        return "Banco: turno " + numeroTurno + " de " + maxTurnos
                + ", " + turnos.getCantidad() + " jugadores activos"
                + ", " + historial.getCantidad() + " transacciones";
    }
}
