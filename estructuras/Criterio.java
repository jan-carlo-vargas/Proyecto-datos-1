package estructuras;

/**
 * Criterio: una "condicion" que se le puede pasar a un metodo de busqueda.
 *
 * Una interfaz en Java es como una DESCRIPCION DE PUESTO: no hace nada
 * por si sola, solo dice "quien implemente esto promete tener este metodo".
 *
 * ¿Para que sirve aqui?
 * Nuestras listas necesitan poder buscar, pero NO deben saber que estamos
 * buscando. Si la lista supiera de jugadores o de tipos de transaccion,
 * dejaria de ser generica y solo serviria para un caso.
 *
 * Con Criterio, la lista solo sabe: "me dieron una condicion, se la
 * pregunto a cada dato y me quedo con los que dicen que si".
 * Quien llama decide cual es la condicion.
 */
public interface Criterio<T> {

    /**
     * Devuelve true si el dato cumple la condicion, false si no.
     */
    boolean cumple(T dato);
}