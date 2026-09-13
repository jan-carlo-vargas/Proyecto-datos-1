package estructuras;
 
/**
 * NodoDoble: la pieza mas basica de las estructuras "dobles".
 *
 * Un nodo es una cajita que guarda TRES cosas:
 *   1. el dato en si (una Casilla, una Transaccion, lo que sea)
 *   2. una referencia al nodo ANTERIOR
 *   3. una referencia al nodo SIGUIENTE
 * Lo usan: ListaCircularDoble (tablero) y ListaDoble (historial, propiedades).
 */
public class NodoDoble<T> {
 
    private T dato; // El dato que este nodo guarda.

    private NodoDoble<T> anterior; // Referencia al nodo que esta ANTES de este. Si es null, no hay nadie antes.
 
    private NodoDoble<T> siguiente; // Referencia al nodo que esta DESPUES de este. Si es null, no hay nadie despues.
 
    // Constructor: al crear un nodo solo le damos el dato.

    public NodoDoble(T dato) {
        this.dato = dato;
        this.anterior = null;
        this.siguiente = null;
    }
 
    // Getters y setters
    // Los atributos son "private" para que nadie de afuera pueda romper
    // los enlaces por accidente: solo la lista dueña del nodo los modifica.
 
    public T getDato() {
        return dato;
    }
 
    public void setDato(T dato) {
        this.dato = dato;
    }
 
    public NodoDoble<T> getAnterior() {
        return anterior;
    }
 
    public void setAnterior(NodoDoble<T> anterior) {
        this.anterior = anterior;
    }
 
    public NodoDoble<T> getSiguiente() {
        return siguiente;
    }
 
    public void setSiguiente(NodoDoble<T> siguiente) {
        this.siguiente = siguiente;
    }
 
    // toString es un metodo que viene de la clase Object, que es la superclase de todas las clases.
    @Override // sobreescribimos el metodo toString para que devuelva el dato del nodo, en vez de la referencia al objeto.
    public String toString() {
        return String.valueOf(dato);
    }
}