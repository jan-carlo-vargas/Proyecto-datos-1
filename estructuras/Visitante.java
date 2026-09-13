package estructuras;

/**
 * Visitante: define "que hacer con cada dato" al recorrer una lista.
 *
 * Mismo razonamiento que Criterio: la lista sabe COMO caminar por sus
 * nodos, pero no debe saber QUE hacer con cada uno (¿imprimirlo?
 * ¿escribirlo en un archivo? ¿sumarlo?). Eso lo decide quien llama.
 *
 * Ejemplo de uso:
 *
 *     historial.recorrerDesdeInicio(t -> System.out.println(t));
 *
 * Esa linea significa: "mi metodo visitar(t) hace System.out.println(t)".
 *
 * Gracias a esto, el mismo metodo recorrerDesdeInicio() nos sirve para
 * imprimir en pantalla y para generar el archivo TXT del reporte.
 */
public interface Visitante<T> {

    /**
     * Se ejecuta una vez por cada dato de la lista, en orden.
     */
    void visitar(T dato);
}
