package dominio;

import estructuras.ColaCircular;

/**
 * CartaEvento: una carta del mazo de sorpresas
 *
 * Los seis tipos que pide el enunciado:
 *   RECIBIR_DINERO, PAGAR_DINERO, AVANZAR, RETROCEDER, PERDER_TURNO, IR_A_CASILLA
 */
public class CartaEvento {

    public enum Tipo {
        RECIBIR_DINERO,
        PAGAR_DINERO,
        AVANZAR,
        RETROCEDER,
        PERDER_TURNO,
        IR_A_CASILLA
    }

    private final String descripcion;
    private final Tipo tipo;

    // Segun el tipo: monto, cantidad de casillas o id de la casilla destino.
    // En PERDER_TURNO no se usa y vale 0.
    private final int valor;

    private CartaEvento(String descripcion, Tipo tipo, int valor) {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new IllegalArgumentException("La carta necesita una descripcion");
        }
        this.descripcion = descripcion.trim();
        this.tipo = tipo;
        this.valor = valor;
    }

    //  FABRICAS

    public static CartaEvento recibirDinero(String descripcion, int monto) {
        validarPositivo(monto, "El monto de la carta");
        return new CartaEvento(descripcion, Tipo.RECIBIR_DINERO, monto);
    }

    public static CartaEvento pagarDinero(String descripcion, int monto) {
        validarPositivo(monto, "El monto de la carta");
        return new CartaEvento(descripcion, Tipo.PAGAR_DINERO, monto);
    }

    public static CartaEvento avanzar(String descripcion, int casillas) {
        validarPositivo(casillas, "La cantidad de casillas a avanzar");
        return new CartaEvento(descripcion, Tipo.AVANZAR, casillas);
    }

    public static CartaEvento retroceder(String descripcion, int casillas) {
        validarPositivo(casillas, "La cantidad de casillas a retroceder");
        return new CartaEvento(descripcion, Tipo.RETROCEDER, casillas);
    }

    public static CartaEvento perderTurno(String descripcion) {
        return new CartaEvento(descripcion, Tipo.PERDER_TURNO, 0);
    }

    public static CartaEvento irACasilla(String descripcion, int idCasilla) {
        if (idCasilla < 0) {
            throw new IllegalArgumentException("El id de la casilla destino no puede ser negativo: " + idCasilla);
        }
        return new CartaEvento(descripcion, Tipo.IR_A_CASILLA, idCasilla);
    }

    //  EFECTO

    /**
     * Aplica la carta al jugador y cuenta que paso.
     *
     * Las cartas de movimiento terminan igual que una tirada de dados: si el
     * jugador cae en una propiedad o en una casilla especial, se ejecuta su
     * efecto (por ejemplo, se le ofrece comprar). Una excepcion deliberada: si
     * cae en OTRA casilla de evento NO se saca otra carta. Sin esa regla una
     * carta podria llevar a otra y esa a otra, y el turno no terminaria nunca.
     */
    public ResultadoEfecto aplicar(Jugador jugador, Banco banco, Tablero tablero) {
        if (jugador == null || banco == null || tablero == null) {
            throw new IllegalArgumentException("La carta necesita jugador, banco y tablero");
        }

        String texto = "Carta para " + jugador.getNombre() + ": " + descripcion;
        String detalle = jugador.getNombre() + " - " + descripcion;

        switch (tipo) {
            case RECIBIR_DINERO:
                banco.aplicarGananciaEvento(jugador, valor, detalle);
                return ResultadoEfecto.informativo(texto);

            case PAGAR_DINERO:
                boolean sigue = banco.aplicarPerdidaEvento(jugador, valor, detalle);
                ResultadoEfecto pago = ResultadoEfecto.informativo(texto);
                return sigue ? pago : pago.conEliminado();

            case AVANZAR:
                return terminarMovimiento(jugador, banco, tablero, texto,
                        tablero.moverJugador(jugador, valor));

            case IR_A_CASILLA:
                return terminarMovimiento(jugador, banco, tablero, texto,
                        tablero.moverACasilla(jugador, valor));

            case RETROCEDER:
                // Retroceder nunca cobra el premio de inicio.
                tablero.retrocederJugador(jugador, valor);
                return terminarMovimiento(jugador, banco, tablero, texto, false);

            case PERDER_TURNO:
                jugador.perderProximoTurno();
                return ResultadoEfecto.informativo(texto).conPierdeTurno();

            default:
                throw new IllegalStateException("Tipo de carta sin implementar: " + tipo);
        }
    }

    /**
     * Cierra una carta de movimiento: cobra el premio de inicio si lo paso y
     * ejecuta el efecto de la casilla donde cayo.
     */
    private ResultadoEfecto terminarMovimiento(Jugador jugador, Banco banco, Tablero tablero,
                                               String texto, boolean pasoPorInicio) {
        String mensaje = texto;

        if (pasoPorInicio && banco.getPremioPorInicio() > 0) {
            banco.pagarPremioPorInicio(jugador);
            mensaje += " (pasa por Inicio y cobra " + banco.getPremioPorInicio() + ")";
        }

        ResultadoEfecto base = ResultadoEfecto.informativo(mensaje).conMovimiento();

        Casilla destino = jugador.getCasillaActual();
        if (destino instanceof CasillaEvento) {
            return base;
        }

        return destino.ejecutarEfecto(jugador, banco, tablero)
                .conMensajePrevio(mensaje)
                .conMovimiento();
    }

    //  MAZO POR DEFECTO

    /**
     * El mazo de la partida: 10 cartas que cubren los seis tipos del enunciado.
     * Los ids de IR_A_CASILLA corresponden al Tablero.crearPorDefecto(): 13 es
     * Tamarindo y 0 es Inicio. Si cambian el tablero, ajusten esos dos numeros.
     */
    public static ColaCircular<CartaEvento> crearMazoPorDefecto() {
        ColaCircular<CartaEvento> mazo = new ColaCircular<>();

        mazo.encolar(recibirDinero("Ganas un concurso del TEC: recibes 100", 100));
        mazo.encolar(pagarDinero("Pagas el marchamo del carro: pagas 80", 80));
        mazo.encolar(avanzar("Atajo por la Interamericana: avanzas 3 casillas", 3));
        mazo.encolar(recibirDinero("Reembolso de impuestos: recibes 50", 50));
        mazo.encolar(retroceder("Derrumbe en la carretera: retrocedes 2 casillas", 2));
        mazo.encolar(pagarDinero("Multa de transito: pagas 60", 60));
        mazo.encolar(perderTurno("Huelga de buses: pierdes tu proximo turno"));
        mazo.encolar(irACasilla("Viaje sorpresa: vas a Tamarindo", 13));
        mazo.encolar(avanzar("Tapon en la Ruta 32: avanzas 5 casillas", 5));
        mazo.encolar(irACasilla("Regresas a casa: vas a Inicio y cobras", 0));

        return mazo;
    }

    //  CONSULTAS

    public String getDescripcion() {
        return descripcion;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public int getValor() {
        return valor;
    }

    private static void validarPositivo(int numero, String que) {
        if (numero <= 0) {
            throw new IllegalArgumentException(que + " debe ser mayor que cero, se recibio: " + numero);
        }
    }

    @Override
    public String toString() {
        return descripcion;
    }
}