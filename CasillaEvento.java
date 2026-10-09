package dominio;

import estructuras.ColaCircular;

/**
 * CasillaEvento: casilla de "Sorpresa". Al caer, el jugador saca la siguiente
 * carta del mazo y se le aplica (puntos 6 y 10 del enunciado).
 *
 * EL MAZO ES UNA COLA CIRCULAR
 * Se usa rotar(): saca la carta del frente, la devuelve y la manda al final en
 * un solo paso. Por eso el mazo nunca se agota: la carta usada queda al fondo
 * y vuelve a salir cuando le toque la vuelta.
 *
 * Todas las casillas de evento del tablero por defecto comparten el MISMO
 * mazo (la misma ColaCircular), como en el Monopoly real, donde hay un solo
 * monton de cartas sin importar en que casilla de sorpresa caiga el jugador.
 */
public class CasillaEvento extends Casilla {

    private final ColaCircular<CartaEvento> cartas;

    public CasillaEvento(int id, String nombre, ColaCircular<CartaEvento> cartas) {
        super(id, nombre);

        if (cartas == null || cartas.estaVacia()) {
            throw new IllegalArgumentException("La casilla " + nombre + " necesita un mazo con al menos una carta");
        }

        this.cartas = cartas;
    }

    /**
     * Version de la firma original de Casilla. Una casilla de evento no puede
     * hacer nada util sin el Banco y el Tablero (una carta mueve dinero y
     * fichas), asi que el efecto real vive en la version de tres parametros.
     */
    @Override
    public void ejecutarEfecto(Jugador jugador) {
        // Sin efecto propio: ver ejecutarEfecto(jugador, banco, tablero).
    }

    @Override
    public ResultadoEfecto ejecutarEfecto(Jugador jugador, Banco banco, Tablero tablero) {
        // rotar() devuelve la carta que estaba al frente y ya la dejo al final.
        CartaEvento carta = cartas.rotar();
        return carta.aplicar(jugador, banco, tablero);
    }

    /**
     * La carta que saldra la proxima vez, sin sacarla. Sirve para pruebas y
     * para que la interfaz pueda mostrar el mazo.
     */
    public CartaEvento verSiguienteCarta() {
        return cartas.verFrente();
    }

    public int getCantidadCartas() {
        return cartas.getCantidad();
    }
}