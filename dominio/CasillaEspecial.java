package dominio;

/**
 * CasillaEspecial: casillas que no se compran ni sacan carta (punto 6).
 *
 * El diagrama UML guarda el tipo como String; aqui es un enum por la misma
 * razon que TipoTransaccion: si alguien escribe "IMPUETO" el programa no
 * compila, en vez de fallar callado durante la defensa.
 *
 * Los cinco tipos:
 *   INICIO       punto de partida. El premio por pasar lo paga el movimiento
 *                (Banco.pagarPremioPorInicio), no esta casilla.
 *   IMPUESTO     el jugador paga un monto al banco. Es un cobro OBLIGATORIO: si
 *                no alcanza queda eliminado (punto 18).
 *   DESCANSO     no pasa nada.
 *   CARCEL       zona de "solo de visita". Aqui llegan los enviados al reten.
 *   IR_A_CARCEL  manda al jugador a la casilla CARCEL SIN cobrar el premio de
 *                inicio y le hace perder su proximo turno.
 */
public class CasillaEspecial extends Casilla {

    public enum Tipo {
        INICIO,
        IMPUESTO,
        DESCANSO,
        CARCEL,
        IR_A_CARCEL
    }

    private final Tipo tipo;

    // Solo se usa en IMPUESTO. En los demas tipos vale 0.
    private final int monto;

    public CasillaEspecial(int id, String nombre, Tipo tipo) {
        this(id, nombre, tipo, 0);
    }

    public CasillaEspecial(int id, String nombre, Tipo tipo, int monto) {
        super(id, nombre);

        if (tipo == null) {
            throw new IllegalArgumentException("La casilla especial " + nombre + " necesita un tipo");
        }
        if (tipo == Tipo.IMPUESTO && monto <= 0) {
            throw new IllegalArgumentException("El impuesto de " + nombre + " necesita un monto positivo: " + monto);
        }

        this.tipo = tipo;
        this.monto = monto;
    }

    /**
     * Version de la firma original de Casilla. El efecto real necesita Banco y
     * Tablero (cobrar un impuesto, mover la ficha), asi que vive en la version
     * de tres parametros.
     */
    @Override
    public void ejecutarEfecto(Jugador jugador) {
        // Sin efecto propio: ver ejecutarEfecto(jugador, banco, tablero).
    }

    @Override
    public ResultadoEfecto ejecutarEfecto(Jugador jugador, Banco banco, Tablero tablero) {
        String quien = jugador.getNombre();

        switch (tipo) {
            case IMPUESTO:
                boolean sigue = banco.pagarAlBanco(jugador, monto, quien + " paga " + nombre);
                ResultadoEfecto impuesto = ResultadoEfecto.informativo(quien + " paga " + monto + " de " + nombre);
                return sigue ? impuesto : impuesto.conEliminado();

            case IR_A_CARCEL:
                Casilla carcel = tablero.buscarCasilla(
                        c -> c instanceof CasillaEspecial && ((CasillaEspecial) c).getTipo() == Tipo.CARCEL);

                if (carcel == null) {
                    // Un tablero sin carcel no es un error del jugador: no hay a donde enviarlo.
                    return ResultadoEfecto.informativo(quien + " cayo en " + nombre + ", pero no hay carcel");
                }

                tablero.enviarACasilla(jugador, carcel.getId());
                jugador.perderProximoTurno();
                return ResultadoEfecto.informativo(quien + " va al reten y pierde su proximo turno")
                        .conMovimiento()
                        .conPierdeTurno();

            case INICIO:
                return ResultadoEfecto.informativo(quien + " cayo en " + nombre);

            case CARCEL:
                return ResultadoEfecto.informativo(quien + " esta de visita en " + nombre);

            case DESCANSO:
                return ResultadoEfecto.informativo(quien + " descansa en " + nombre);

            default:
                throw new IllegalStateException("Tipo de casilla especial sin implementar: " + tipo);
        }
    }

    public Tipo getTipo() {
        return tipo;
    }

    public int getMonto() {
        return monto;
    }
}