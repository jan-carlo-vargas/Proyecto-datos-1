package dominio;

/**
 * OperacionInvalidaException: el banco rechazo una accion del jugador.
 *
 * Es la forma en que el banco dice "esto no se puede" sin devolver null ni
 * un false que alguien pueda ignorar por descuido. El mensaje ya viene
 * redactado en espaniol y listo para mostrarse, asi que el servidor solo
 * tiene que reenviarlo:
 *
 *   try {
 *       banco.comprarPropiedad(jugador, propiedad);
 *       return "OK|Propiedad comprada";
 *   } catch (OperacionInvalidaException e) {
 *       return "ERROR|" + e.getMessage();
 *   }
 *
 * ¿POR QUE EXTIENDE RuntimeException Y NO Exception?
 * Si extendiera Exception seria una excepcion "chequeada" y Java obligaria a
 * escribir throws en cada metodo de la cadena, desde el socket hasta el banco.
 * Al ser RuntimeException se atrapa en un solo lugar: donde el servidor arma
 * la respuesta del protocolo.
 *
 * Aqui van SOLO los rechazos por reglas del juego (punto 17 del enunciado).
 * Los errores de programacion, como pasar un null donde no corresponde, se
 * reportan con IllegalArgumentException porque no son culpa del jugador.
 */
public class OperacionInvalidaException extends RuntimeException {

    // Java lo pide para poder serializar excepciones. Sin este campo el
    // compilador avisa con -Xlint.
    private static final long serialVersionUID = 1L;

    public OperacionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
