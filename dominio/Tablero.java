package dominio;

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