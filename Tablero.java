package dominio;

import dominio.Casilla;
import dominio.CasillaEspecial;
import dominio.Jugador;
import dominio.Propiedad;
import estructuras.Criterio;
import estructuras.ListaCircularDoble;
import estructuras.NodoDoble;
import estructuras.Visitante;

/**
 * Tablero: el anillo de casillas por donde se mueven los jugadores
 *
 * aqui esto es para que cuando un jugador va a avanzar, la posicion donde esta se marque desde donde esta, y kno que la vuelva a buscar desde el inicio 
 *
 * 
 * moverJugador() avisa si el jugador paso por Inicio, pero NO paga el premio:
 * quien paga es el Banco (Banco.pagarPremioPorInicio), porque toda operacion
 * economica debe pasar por el banco y generar su transaccion
 */
public class Tablero {

    /** Minimo de casillas que exige el enunciado. */
    public static final int CANTIDAD_MINIMA = 24;

    private final ListaCircularDoble<Casilla> casillas;

    public Tablero(ListaCircularDoble<Casilla> casillas) {
        if (casillas == null) {
            throw new IllegalArgumentException("El tablero necesita una lista de casillas");
        }
        if (casillas.getCantidad() < CANTIDAD_MINIMA) {
            throw new IllegalArgumentException("El tablero necesita al menos " + CANTIDAD_MINIMA
                    + " casillas, se recibieron: " + casillas.getCantidad());
        }
        for (int i = 0; i < casillas.getCantidad(); i++) {
            if (casillas.obtener(i).getId() != i) {
                throw new IllegalArgumentException("La casilla en la posicion " + i
                        + " debe tener id " + i + " pero tiene " + casillas.obtener(i).getId());
            }
        }

        this.casillas = casillas;
    }

        //  TABLERO POR DEFECTO

    /**
     * Tablero de 24 casillas con tematica de Costa Rica:
     * 13 propiedades, 5 sorpresas y 6 especiales. Las sorpresas comparten un
     * unico mazo. Para otra tematica basta con cambiar nombres y precios aqui.
     */
    public static Tablero crearPorDefecto() {
        ListaCircularDoble<Casilla> lista = new ListaCircularDoble<>();
        estructuras.ColaCircular<CartaEvento> mazo = CartaEvento.crearMazoPorDefecto();

        lista.agregar(new CasillaEspecial(0, "Inicio", CasillaEspecial.Tipo.INICIO));
        lista.agregar(new Propiedad(1, "Cartago", 60, 6));
        lista.agregar(new CasillaEvento(2, "Sorpresa", mazo));
        lista.agregar(new Propiedad(3, "Turrialba", 80, 8));
        lista.agregar(new CasillaEspecial(4, "Impuesto de ventas", CasillaEspecial.Tipo.IMPUESTO, 100));
        lista.agregar(new Propiedad(5, "Heredia", 100, 10));
        lista.agregar(new Propiedad(6, "Alajuela", 100, 10));
        lista.agregar(new CasillaEvento(7, "Sorpresa", mazo));
        lista.agregar(new Propiedad(8, "Puntarenas", 120, 12));
        lista.agregar(new CasillaEspecial(9, "Parque La Sabana", CasillaEspecial.Tipo.DESCANSO));
        lista.agregar(new Propiedad(10, "Liberia", 140, 14));
        lista.agregar(new Propiedad(11, "Monteverde", 160, 16));
        lista.agregar(new CasillaEvento(12, "Sorpresa", mazo));
        lista.agregar(new Propiedad(13, "Tamarindo", 180, 18));
        lista.agregar(new CasillaEspecial(14, "Reten", CasillaEspecial.Tipo.CARCEL));
        lista.agregar(new Propiedad(15, "Manuel Antonio", 200, 20));
        lista.agregar(new CasillaEvento(16, "Sorpresa", mazo));
        lista.agregar(new Propiedad(17, "Arenal", 220, 22));
        lista.agregar(new CasillaEspecial(18, "Ir al reten", CasillaEspecial.Tipo.IR_A_CARCEL));
        lista.agregar(new Propiedad(19, "Tortuguero", 240, 24));
        lista.agregar(new CasillaEvento(20, "Sorpresa", mazo));
        lista.agregar(new Propiedad(21, "Cahuita", 260, 26));
        lista.agregar(new CasillaEspecial(22, "Impuesto de lujo", CasillaEspecial.Tipo.IMPUESTO, 150));
        lista.agregar(new Propiedad(23, "Volcan Poas", 300, 30));

        return new Tablero(lista);
    }

    //  COLOCAR Y MOVER FICHAS

    /**
     * Pone al jugador en la casilla de Inicio. El Juego lo llama una vez por jugador al comenzar la partida
     */
    public void colocarEnInicio(Jugador jugador) {
        validarJugador(jugador);
        jugador.moverA(casillas.getInicio());
    }

    /**
     * Mueve al jugador pasos casillas hacia ADELANTE.
     * Devuelve true si en el camino paso por Inici
     * el Juego usa ese true para pedirle al Banco el premio
     */
    public boolean moverJugador(Jugador jugador, int pasos) {
        NodoDoble<Casilla> origen = posicionDe(jugador);
        validarPasos(pasos);

        boolean paso = casillas.pasaPorInicio(origen, pasos);
        jugador.moverA(casillas.avanzar(origen, pasos));
        return paso;
    }

    /**
     * Mueve al jugador paso casillas hacia ATRAS. Retroceder nunca cobra el
     * premio de inicio, por eso no devuelve nada
     */
    public void retrocederJugador(Jugador jugador, int pasos) {
        NodoDoble<Casilla> origen = posicionDe(jugador);
        validarPasos(pasos);

        jugador.moverA(casillas.retroceder(origen, pasos));
    }

    /**
     * Mueve al jugador HACIA ADELANTE hasta la casilla con ese id (carta "ve a
     * la casilla X"). Devuelve true si pasa por Inicio en el camino
     * Si ya esta parado en esa casilla se queda ahi y no cobra nada
     */
    public boolean moverACasilla(Jugador jugador, int idCasilla) {
        NodoDoble<Casilla> origen = posicionDe(jugador);
        NodoDoble<Casilla> destino = casillas.obtenerNodo(idCasilla);

        int pasos = casillas.pasosHasta(origen, destino);
        boolean paso = pasos > 0 && casillas.pasaPorInicio(origen, pasos);

        jugador.moverA(destino);
        return paso;
    }

    /**
     * Coloca al jugador en una casilla SIN recorrer el camino y sin premio de
     * inicio, Es el "teletransporte" de ir a la carcel
     */
    public void enviarACasilla(Jugador jugador, int idCasilla) {
        validarJugador(jugador);
        jugador.moverA(casillas.obtenerNodo(idCasilla));
    }

    //  CONSULTAS

    public Casilla getCasilla(int id) {
        return casillas.obtener(id);
    }

    public Casilla getInicio() {
        return casillas.getInicio().getDato();
    }

    public int getCantidadCasillas() {
        return casillas.getCantidad();
    }

    /**
     * Primera casilla que cumple el criterio, o null. 
     */
    public Casilla buscarCasilla(Criterio<Casilla> criterio) {
        NodoDoble<Casilla> nodo = casillas.buscarNodo(criterio);
        return nodo == null ? null : nodo.getDato();
    }

    /** Recorre una vuelta completa desde Inicio. Lo usa la interfaz para dibujar. */
    public void recorrer(Visitante<Casilla> visitante) {
        casillas.recorrer(visitante);
    }

    //  VALIDACIONES INTERNAS

    private void validarJugador(Jugador jugador) {
        if (jugador == null) {
            throw new IllegalArgumentException("El jugador recibido es nulo");
        }
    }

    private NodoDoble<Casilla> posicionDe(Jugador jugador) {
        validarJugador(jugador);
        if (jugador.getPosicionActual() == null) {
            throw new IllegalStateException(jugador.getNombre() + " todavia no esta en el tablero; "
                    + "falta llamar a colocarEnInicio()");
        }
        return jugador.getPosicionActual();
    }

    private void validarPasos(int pasos) {
        if (pasos <= 0) {
            throw new IllegalArgumentException("Los pasos deben ser mayores que cero, se recibio: " + pasos);
        }
    }

    @Override
    public String toString() {
        return casillas.toString();
    }
}