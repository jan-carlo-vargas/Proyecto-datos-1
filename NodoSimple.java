package estructuras;
 
/**
 * NodoSimple: igual que NodoDoble pero con UNA sola flecha (siguiente).
 * Lo usa: ColaCircular (turnos de jugadores y cartas de evento).
 */
public class NodoSimple<T> {
 
    // El dato que este nodo guarda (un Jugador, una CartaEvento, etc).
    private T dato; 
 
    // Referencia al siguiente nodo de la cola.
    private NodoSimple<T> siguiente;
 
    public NodoSimple(T dato) {
        this.dato = dato;
        this.siguiente = null;
    }
 
    public T getDato() {
        return dato;
    }
 
    public void setDato(T dato) {
        this.dato = dato;
    }
 
    public NodoSimple<T> getSiguiente() {
        return siguiente;
    }
 
    public void setSiguiente(NodoSimple<T> siguiente) {
        this.siguiente = siguiente;
    }
 
    @Override
    public String toString() {
        return String.valueOf(dato);
    }
}
 