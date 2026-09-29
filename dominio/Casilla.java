package dominio;

/**
 * Casilla: clase base de todas las casillas del tablero (punto 6 del enunciado).
 *
 * CONTRATO ACORDADO CON EL ENCARGADO DEL TABLERO
 * Esta version tiene lo minimo que necesitan Jugador y Banco para funcionar.
 * Quien programe CasillaEvento, CasillaEspecial y el Tablero puede agregarle
 * todo lo que le haga falta, pero no debe cambiar las firmas que ya estan aqui,
 * porque el banco las usa.
 *
 * Es ABSTRACTA porque una casilla "generica" no existe en el tablero: toda
 * casilla es una propiedad, un evento o una especial. Declararla abstracta
 * impide que alguien escriba new Casilla(...) por error.
 *
 * El metodo ejecutarEfecto() tampoco tiene cuerpo aqui: cada subclase lo
 * resuelve a su manera. Eso es el polimorfismo que pide el punto 6. El tablero
 * puede llamar casilla.ejecutarEfecto(jugador) sin preguntar de que tipo es;
 * Java elige sola la version correcta.
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
