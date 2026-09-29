package dominio;

/**
 * Propiedad: casilla que se puede comprar (punto 7 del enunciado).
 *
 * CONTRATO ACORDADO CON EL ENCARGADO DEL TABLERO
 * El banco consume getPrecio(), getAlquiler(), getPropietario() y
 * setPropietario(). Esas cuatro firmas no se deben cambiar. Todo lo demas
 * (colores, grupos, casas, hoteles) se puede agregar encima sin romper nada.
 *
 * Las tres reglas del punto 7 quedan repartidas asi:
 *   - disponible          -> el banco deja comprarla   (Banco.comprarPropiedad)
 *   - de otro jugador     -> se paga alquiler          (Banco.pagarAlquiler)
 *   - del mismo jugador   -> no se paga nada           (Banco.pagarAlquiler)
 * La propiedad solo responde de quien es; decidir y cobrar le toca al banco.
 */
public class Propiedad extends Casilla {

    // Precio de compra y alquiler no cambian durante la partida.
    private final int precio;
    private final int alquiler;

    // null significa "esta disponible, la tiene el banco".
    // Este si cambia: es lo unico mutable de la propiedad.
    private Jugador propietario;

    public Propiedad(int id, String nombre, int precio, int alquiler) {
        super(id, nombre);

        if (precio <= 0) {
            throw new IllegalArgumentException("La propiedad " + nombre + " necesita un precio positivo, se recibio: " + precio);
        }
        if (alquiler < 0) {
            throw new IllegalArgumentException("El alquiler de " + nombre + " no puede ser negativo, se recibio: " + alquiler);
        }

        this.precio = precio;
        this.alquiler = alquiler;
        this.propietario = null;
    }

    /**
     * Caer en una propiedad no mueve dinero por si solo (ver el comentario de
     * Casilla.ejecutarEfecto). El Juego lee de quien es la propiedad y le pide
     * al Banco que cobre el alquiler o que ofrezca la compra.
     *
     * Queda implementado pero vacio porque Casilla lo declara abstracto y sin
     * el la clase no compilaria.
     */
    @Override
    public void ejecutarEfecto(Jugador jugador) {
        // Sin efecto propio: el movimiento de dinero lo hace el Banco.
    }

    /**
     * true si todavia no tiene duenio y se puede comprar.
     */
    public boolean estaDisponible() {
        return propietario == null;
    }

    /**
     * true si esta propiedad le pertenece al jugador indicado.
     * Sirve para la tercera regla del punto 7: caer en lo propio no paga nada.
     */
    public boolean esDe(Jugador jugador) {
        return propietario != null && propietario.equals(jugador);
    }

    public int getPrecio() {
        return precio;
    }

    public int getAlquiler() {
        return alquiler;
    }

    public Jugador getPropietario() {
        return propietario;
    }

    /**
     * Cambia el duenio. Se pasa null para dejarla libre otra vez, que es lo que
     * ocurre cuando un jugador quiebra debiendole al banco.
     *
     * Lo llaman Jugador.comprarPropiedad() y Jugador.perderPropiedad(), nunca
     * el servidor directamente.
     */
    public void setPropietario(Jugador propietario) {
        this.propietario = propietario;
    }

    /**
     * Dos propiedades son la misma si tienen el mismo id.
     *
     * Hay que sobrescribirlo porque ListaDoble.eliminar() y contiene() comparan
     * con equals(). El equals que Java trae por defecto compara direcciones de
     * memoria, asi que sacar una propiedad de la lista de su duenio fallaria en
     * silencio cuando la referencia viene de otro lado, por ejemplo del tablero.
     */
    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Propiedad)) {
            return false;
        }
        return this.id == ((Propiedad) otro).id;
    }

    /**
     * Regla de Java: si dos objetos son equals, sus hashCode deben coincidir.
     * Como equals compara por id, el hashCode se calcula con el id.
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        String duenio = (propietario == null) ? "disponible" : "de " + propietario.getNombre();
        return "[" + id + "] " + nombre + " (precio " + precio + ", alquiler " + alquiler + ", " + duenio + ")";
    }
}
