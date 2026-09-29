package dominio;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Transaccion: el registro de un movimiento economico (punto 11 del enunciado).
 *
 * Tiene los ocho datos minimos que pide el enunciado: identificador, fecha y
 * hora, numero de turno, tipo, jugador origen, jugador destino, monto y
 * descripcion.
 *
 * TODOS LOS CAMPOS SON final, NO HAY SETTERS.
 * Una transaccion es un hecho que ya ocurrio, como la linea de un estado de
 * cuenta. Si se pudiera editar, el historial dejaria de ser confiable. Cuando
 * algo se deshace no se corrige la transaccion vieja: se agrega una nueva.
 *
 * ORIGEN Y DESTINO SON ids, NO OBJETOS Jugador.
 * Asi el historial sigue siendo legible aunque el jugador ya este eliminado,
 * viaja tal cual por el socket sin tener que serializar objetos, y buscar por
 * jugador es una comparacion exacta de texto. Cuando el banco es una de las
 * dos puntas se usa la constante BANCO.
 * Los nombres legibles de las personas van en la descripcion.
 */
public class Transaccion {

    /** Valor que se usa en origen o destino cuando la contraparte es el banco. */
    public static final String BANCO = "BANCO";

    // static y final: el formateador se crea una sola vez y lo comparten todas
    // las transacciones, en vez de construir uno nuevo por cada movimiento.
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Plantilla unica de columnas. Al estar en un solo lugar, el encabezado y
    // las filas no se pueden desalinear entre si.
    private static final String PLANTILLA = "%-5s %-6s %-20s %-21s %-10s %-10s %10s  %s";

    private final int id;
    private final String fechaHora;
    private final int turno;
    private final TipoTransaccion tipo;
    private final String origen;
    private final String destino;
    private final int monto;
    private final String descripcion;

    /**
     * La fecha y hora no se reciben como parametro: se toman del reloj en el
     * momento exacto en que nace la transaccion. Se guarda ya formateada como
     * texto porque nunca hace falta hacer cuentas con ella, solo imprimirla.
     *
     * A este constructor lo llama unicamente Banco, que es quien lleva la
     * cuenta de los ids y del numero de turno.
     */
    public Transaccion(int id, int turno, TipoTransaccion tipo,
                       String origen, String destino, int monto, String descripcion) {

        if (id <= 0) {
            throw new IllegalArgumentException("El id de la transaccion debe ser positivo: " + id);
        }
        if (turno <= 0) {
            throw new IllegalArgumentException("El numero de turno debe ser positivo: " + turno);
        }
        if (tipo == null) {
            throw new IllegalArgumentException("La transaccion " + id + " necesita un tipo");
        }
        if (origen == null || origen.trim().isEmpty()) {
            throw new IllegalArgumentException("La transaccion " + id + " necesita un origen");
        }
        if (destino == null || destino.trim().isEmpty()) {
            throw new IllegalArgumentException("La transaccion " + id + " necesita un destino");
        }
        if (monto < 0) {
            throw new IllegalArgumentException("El monto no puede ser negativo: " + monto);
        }

        this.id = id;
        this.turno = turno;
        this.tipo = tipo;
        this.origen = origen.trim();
        this.destino = destino.trim();
        this.monto = monto;
        this.descripcion = (descripcion == null) ? "" : descripcion.trim();
        this.fechaHora = LocalDateTime.now().format(FORMATO_FECHA);
    }

    //  FORMATO DEL REPORTE (punto 13)

    /**
     * Los titulos de las columnas y la linea de separacion.
     *
     * Es static porque pertenece al formato del reporte, no a una transaccion
     * en particular: se llama con Transaccion.encabezado(), sin tener ninguna.
     */
    public static String encabezado() {
        String titulos = String.format(PLANTILLA,
                "NUM", "TURNO", "FECHA Y HORA", "TIPO", "ORIGEN", "DESTINO", "MONTO", "DESCRIPCION");

        return titulos + System.lineSeparator() + "-".repeat(titulos.length());
    }

    /**
     * La linea de esta transaccion dentro del reporte, con las columnas que
     * exige el punto 13: numero, turno, tipo, origen, destino, monto y
     * descripcion. La fecha y hora va de mas porque el punto 11 la pide.
     *
     * Se usa la misma PLANTILLA del encabezado, por eso todo queda alineado.
     * El guion en %-5s alinea a la izquierda; sin el, a la derecha, que es lo
     * que conviene para el monto porque deja las unidades una debajo de otra.
     */
    public String toTexto() {
        return String.format(PLANTILLA,
                id, turno, fechaHora, tipo.name(), origen, destino, monto, descripcion);
    }

    //  CONSULTAS

    public int getId() {
        return id;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public int getTurno() {
        return turno;
    }

    public TipoTransaccion getTipo() {
        return tipo;
    }

    public String getOrigen() {
        return origen;
    }

    public String getDestino() {
        return destino;
    }

    public int getMonto() {
        return monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Dice si esta transaccion involucra al jugador indicado, sea como origen o
     * como destino. Lo usa la busqueda por jugador del punto 12.
     */
    public boolean involucraA(String idJugador) {
        return origen.equals(idJugador) || destino.equals(idJugador);
    }

    //  IDENTIDAD

    /**
     * Dos transacciones son la misma si tienen el mismo id. El banco los asigna
     * consecutivos, asi que nunca se repiten dentro de una partida.
     */
    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Transaccion)) {
            return false;
        }
        return this.id == ((Transaccion) otro).id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    /**
     * Version corta para depurar. El formato de columnas del reporte vive en
     * toTexto(), que es mucho mas ancho de lo comodo para imprimir sueltas.
     */
    @Override
    public String toString() {
        return "TX#" + id + " turno " + turno + " " + tipo.name()
                + " " + origen + " -> " + destino + " monto " + monto;
    }
}
