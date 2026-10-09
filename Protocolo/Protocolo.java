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

    // Se utiliza para confirmar el registro de un jugador.
    public static final String CONECTADO =
        "CONECTADO";

    // Se utiliza para informar que la partida fue iniciada.
    public static final String PARTIDA_INICIADA =
        "PARTIDA_INICIADA";

    // Se utiliza para informar el jugador que posee el turno.
    public static final String TURNO =
        "TURNO";

    // Se envia a todos con el resultado de los dados:
    // DADOS|idJugador|dado1|dado2|total
    public static final String DADOS =
        "DADOS";

    // Se envia a todos cuando una ficha cae en una casilla:
    // MOVIMIENTO|idJugador|idCasilla|nombreCasilla
    public static final String MOVIMIENTO =
        "MOVIMIENTO";

    // Se envia a todos con lo que ocurrio al caer en una casilla o carta:
    // EVENTO|mensaje
    public static final String EVENTO =
        "EVENTO";

    // Se envia solo al jugador en turno cuando puede comprar la propiedad:
    // OFERTA_COMPRA|idPropiedad|nombre|precio
    public static final String OFERTA_COMPRA =
        "OFERTA_COMPRA";

    // Se envia a todos cuando el jugador rechaza una oferta de compra:
    // OFERTA_RECHAZADA|idJugador
    public static final String OFERTA_RECHAZADA =
        "OFERTA_RECHAZADA";

    // Se envia a todos cuando alguien compra una propiedad:
    // COMPRA|idJugador|idPropiedad|nombre|precio|saldoNuevo
    public static final String COMPRA =
        "COMPRA";

    // Se envia a todos cuando un jugador queda eliminado:
    // ELIMINADO|idJugador
    public static final String ELIMINADO =
        "ELIMINADO";

    // Se envia a todos cuando termina la partida:
    // FIN_PARTIDA|idGanador
    public static final String FIN_PARTIDA =
        "FIN_PARTIDA";

    // Se utiliza cuando se recibe un comando no reconocido.
    public static final String ERROR_COMANDO =
        "ERROR|COMANDO_DESCONOCIDO";

    /**
     * Se evita crear objetos de esta clase.
     */
    private Protocolo() {
    }

    // TABLERO|id:nombre:tipo:precio;id:nombre:tipo:precio;...
    // tipo: PROPIEDAD, EVENTO o ESPECIAL. Se envia al iniciar la partida.
    public static final String TABLERO =
        "TABLERO";

    // ESTADO_JUEGO|idTurno|numeroTurno|maxTurnos|jugadores|duenos
    // jugadores: id:nombre:saldo:idCasilla:activo(1/0) separados por ;
    // duenos: idCasilla=idPropietario separados por , (puede ir vacio)
    public static final String ESTADO_JUEGO =
        "ESTADO_JUEGO";

    // El cliente lo envia para pedir TABLERO y ESTADO_JUEGO (GUI que entra tarde).
    public static final String CONSULTAR_TABLERO =
        "CONSULTAR_TABLERO";

    // Puerto TCP unico del servidor: lo usan los clientes del juego y el
    // modulo electronico (Raspberry Pi Pico W).
    public static final int PUERTO = 6000;

    // El cliente pide exportar el historial a TXT (punto 13).
    // El servidor responde: EXPORTADO|rutaDelArchivo
    public static final String EXPORTAR_TRANSACCIONES =
        "EXPORTAR_TRANSACCIONES";

    public static final String EXPORTADO =
        "EXPORTADO";

    // ---- Modulo electronico (mensajes JSON, una linea por mensaje) ----
    // Pico -> servidor:
    //   {"accion":"CONECTAR_DISPOSITIVO"}                      (al conectarse)
    //   {"accion":"RESULTADO_DADOS","valor":7}                 (suma 2 a 12)
    //   {"accion":"IDENTIFICAR_JUGADOR","uid":"a1b2c3d4"}
    // Servidor -> Pico:
    //   {"accion":"ACTIVAR_DADOS","jugador":"1","nombre":"Ana"}
    //   {"accion":"LEER_TARJETA","jugador":"1","nombre":"Ana","motivo":"...","monto":"60"}
    //   {"accion":"DESACTIVAR"}                                (cancela la espera)
    //   {"accion":"RFID_OK"|"RFID_ERROR"|"DADO_ERROR","mensaje":"..."} (informativos)
    public static final String DISP_CONECTAR = "CONECTAR_DISPOSITIVO";
    public static final String DISP_IDENTIFICAR = "IDENTIFICAR_JUGADOR";
    public static final String DISP_RESULTADO_DADO = "RESULTADO_DADOS";

}
