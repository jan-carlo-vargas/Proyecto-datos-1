package Protocolo;

/**
 * Se definen los comandos utilizados para la comunicacion
 * entre el cliente y el servidor.
 */
public final class Protocolo {

    // Se utiliza cuando un cliente solicita conectarse.
    public static final String CONECTAR =
        "CONECTAR";

    // Se utiliza cuando un jugador solicita lanzar los dados.
    public static final String TIRAR_DADOS =
        "TIRAR_DADOS";

    // Se utiliza cuando un jugador solicita comprar una propiedad.
    public static final String COMPRAR_PROPIEDAD =
        "COMPRAR_PROPIEDAD";

    // Se utiliza cuando un jugador decide no comprar una propiedad.
    public static final String NO_COMPRAR =
        "NO_COMPRAR";

    // Se utiliza cuando un jugador solicita terminar su turno.
    public static final String TERMINAR_TURNO =
        "TERMINAR_TURNO";

    // Se utiliza para consultar el estado actual de la partida.
    public static final String CONSULTAR_ESTADO =
        "CONSULTAR_ESTADO";

    // Se utiliza para consultar el historial de transacciones.
    public static final String CONSULTAR_TRANSACCIONES =
        "CONSULTAR_TRANSACCIONES";

    // Se utiliza para confirmar la conexion TCP.
    public static final String CONEXION_OK =
        "CONEXION_OK";

    // Se utiliza para confirmar que el cliente fue conectado.
    public static final String CONECTADO =
        "CONECTADO";

    // Se utiliza cuando se recibe un comando no reconocido.
    public static final String ERROR_COMANDO =
        "ERROR|COMANDO_DESCONOCIDO";

    /**
     * Se evita crear objetos de esta clase.
     */
    private Protocolo() {
    }
}
