package dominio;

/**
 * Casilla: clase base de todas las casillas del tablero
 */
public abstract class Casilla {

    // El id no cambia nunca una vez creada la casilla, por eso es final.
    protected final int id;

    protected final String nombre;

    /**
     * El constructor es protected y no public: solo lo pueden llamar las
     * subclases, con super(id, nombre). Refuerza que nadie cree una Casilla
     * suelta.
     */
    protected Casilla(int id, String nombre) {
        if (id < 0) {
            throw new IllegalArgumentException("El id de la casilla no puede ser negativo: " + id);
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("La casilla " + id + " necesita un nombre");
        }

        this.id = id;
        this.nombre = nombre.trim();
    }

    /**
     * Lo que pasa cuando un jugador cae en esta casilla.
     *
     * OJO, REGLA IMPORTANTE DEL PUNTO 3: una casilla NO mueve dinero.
     * El enunciado exige que el banco valide antes de modificar el estado, asi
     * que la casilla solo describe que corresponde hacer. Quien cobra, quien
     * valida el saldo y quien genera la transaccion es siempre el Banco.
     */
    public abstract void ejecutarEfecto(Jugador jugador);

    
    /**
     * Version completa del efecto, la que usa el Juego. Recibe el Banco y el
     * Tablero porque un efecto real necesita cobrar (Banco) y mover fichas
     * (Tablero), y la firma de un solo parametro no alcanza para eso.
     *
     */
    public ResultadoEfecto ejecutarEfecto(Jugador jugador, Banco banco, Tablero tablero) {
        ejecutarEfecto(jugador);
        return ResultadoEfecto.informativo(jugador.getNombre() + " cayo en " + nombre);
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public String toString() {
        return "[" + id + "] " + nombre;
    }
}
