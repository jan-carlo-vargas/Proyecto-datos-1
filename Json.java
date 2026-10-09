package Servidor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilidades minimas para los mensajes JSON de una linea que intercambia el
 * servidor con el modulo electronico (Raspberry Pi Pico W).
 *
 * No se usa una libreria: los mensajes son planos (sin anidar), asi que basta
 * con construirlos a mano y leer campos con expresiones regulares.
 */
public final class Json {

    private Json() {
    }

    /**
     * Construye {"accion":"X","clave":"valor",...}. Los pares se pasan como
     * clave1, valor1, clave2, valor2... y todos los valores salen como texto.
     */
    public static String construir(String accion, String... pares) {

        StringBuilder sb = new StringBuilder();
        sb.append("{\"accion\":\"").append(escapar(accion)).append('"');

        for (int i = 0; i + 1 < pares.length; i += 2) {
            sb.append(",\"").append(escapar(pares[i])).append("\":\"")
              .append(escapar(pares[i + 1])).append('"');
        }

        return sb.append('}').toString();
    }

    /**
     * Lee el valor de una clave, venga como texto ("uid":"AB") o como numero
     * ("total":7). Devuelve null si la clave no esta.
     */
    public static String leer(String json, String clave) {

        Pattern patron = Pattern.compile(
            "\"" + Pattern.quote(clave) + "\"\\s*:\\s*(?:\"((?:[^\"\\\\]|\\\\.)*)\"|(-?[0-9]+(?:\\.[0-9]+)?|true|false))");

        Matcher m = patron.matcher(json);

        if (!m.find()) {
            return null;
        }

        return m.group(1) != null ? m.group(1) : m.group(2);
    }

    /**
     * Lee un entero; devuelve -1 si la clave no esta o no es un numero.
     */
    public static int leerEntero(String json, String clave) {

        String valor = leer(json, clave);

        if (valor == null) {
            return -1;
        }

        try {
            return (int) Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", " ").replace("\r", " ");
    }
}
