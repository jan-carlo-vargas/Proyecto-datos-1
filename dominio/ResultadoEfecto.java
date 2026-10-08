package dominio;

/**
 * ResultadoEfecto: que pasa cuando cae en una casilla 
 * 
 * 
 *
 * Dice cuatro cosas:
 *   - mensaje:          texto en espaniol listo para mostrar a todos los clientes
 *   - propiedadEnVenta: si no es null, el jugador puede comprarla ahora              
 *   - eliminado:        el jugador no pudo cubrir un pago obligatorio
 *   - pierdeTurno:      el jugador perdera su proximo turno
 *   - seMovio:          la casilla o carta movio al jugador, hay que redibujar
 *
 * 
 */
public class ResultadoEfecto {

    private final String mensaje;
    private final Propiedad propiedadEnVenta;
    private final boolean eliminado;
    private final boolean pierdeTurno;
    private final boolean seMovio;

    private ResultadoEfecto(String mensaje, Propiedad propiedadEnVenta, boolean eliminado,
                            boolean pierdeTurno, boolean seMovio) {
        if (mensaje == null) {
            throw new IllegalArgumentException("El resultado necesita un mensaje"); /** esto es para evitar que salgan cosas raras */
        }
        this.mensaje = mensaje;
        this.propiedadEnVenta = propiedadEnVenta;
        this.eliminado = eliminado;
        this.pierdeTurno = pierdeTurno;
        this.seMovio = seMovio;
    }

    //  FABRICAS

    /** en esta parte va conatnto la situacion */
    public static ResultadoEfecto informativo(String mensaje) {
        return new ResultadoEfecto(mensaje, null, false, false, false);
    }

    /** Cayo en una propiedad libre */
    public static ResultadoEfecto ofrecerCompra(Propiedad propiedad, String mensaje) {
        if (propiedad == null) {
            throw new IllegalArgumentException("No se puede ofrecer una propiedad nula");
        }
        return new ResultadoEfecto(mensaje, propiedad, false, false, false);
    }

    //  VARIANTES (devuelven un resultado nuevo)

    public ResultadoEfecto conEliminado() {
        return new ResultadoEfecto(mensaje, propiedadEnVenta, true, pierdeTurno, seMovio);
    }

    public ResultadoEfecto conPierdeTurno() {
        return new ResultadoEfecto(mensaje, propiedadEnVenta, eliminado, true, seMovio);
    }

    public ResultadoEfecto conMovimiento() {
        return new ResultadoEfecto(mensaje, propiedadEnVenta, eliminado, pierdeTurno, true);
    }

    /** Antepone un texto, por ejemplo el de la carta que provoco este efecto. */
    public ResultadoEfecto conMensajePrevio(String previo) {
        return new ResultadoEfecto(previo + " " + mensaje, propiedadEnVenta, eliminado, pierdeTurno, seMovio);
    }

    //  CONSULTAS

    public String getMensaje() {
        return mensaje;
    }

    public Propiedad getPropiedadEnVenta() {
        return propiedadEnVenta;
    }

    public boolean hayPropiedadEnVenta() {
        return propiedadEnVenta != null;
    }

    public boolean jugadorEliminado() {
        return eliminado;
    }

    public boolean pierdeTurno() {
        return pierdeTurno;
    }

    public boolean seMovio() {
        return seMovio;
    }

    @Override
    public String toString() {
        return mensaje;
    }
}
