package dominio;

/**
 * TipoTransaccion: los tipos de movimiento economico que reconoce el banco.
 * Son los siete que exige el punto 11 del enunciado, ni uno mas ni uno menos.
 *
 * ¿POR QUE UN ENUM Y NO UN String?
 * Un enum es una lista cerrada de valores posibles. Si alguien escribe
 * TipoTransaccion.ALQILER el programa NO compila. Con un String, el error
 * "ALQILER" solo se descubre en plena demostracion, cuando la busqueda por
 * tipo del punto 12 devuelve una lista vacia y nadie entiende por que.
 *
 * Cada tipo lleva ademas un texto legible, para mostrarlo en pantalla sin
 * que el jugador tenga que leer MAYUSCULAS_CON_GUIONES.
 *
 * Para el protocolo de texto del servidor se usa name() al enviar y
 * desdeTexto() al recibir.
 */
public enum TipoTransaccion {

    COMPRA_PROPIEDAD("Compra de propiedad"),
    PAGO_ALQUILER("Pago de alquiler"),
    PAGO_AL_BANCO("Pago al banco"),
    PAGO_ENTRE_JUGADORES("Pago entre jugadores"),
    GANANCIA_POR_EVENTO("Ganancia por evento"),
    PERDIDA_POR_EVENTO("Perdida por evento"),
    PREMIO_POR_INICIO("Premio por pasar por inicio");

    // Texto para mostrarle al usuario. El valor tecnico sigue siendo name().
    private final String descripcion;

    /**
     * El constructor de un enum es siempre privado: no se pueden crear
     * tipos nuevos desde afuera, solo existen los siete de arriba.
     */
    TipoTransaccion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    /**
     * Convierte el texto que llega por el socket en un tipo real.
     *
     * valueOf() lanza IllegalArgumentException con un mensaje en ingles poco
     * util. Lo envolvemos para devolver un error claro y en el mismo formato
     * que el resto de las validaciones del banco.
     */
    public static TipoTransaccion desdeTexto(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            throw new OperacionInvalidaException("Falta el tipo de transaccion");
        }

        try {
            return TipoTransaccion.valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new OperacionInvalidaException("Tipo de transaccion desconocido: " + texto);
        }
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
