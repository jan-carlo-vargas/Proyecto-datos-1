package estructuras;
 
/**
 * ColaCircular: cola (FIFO) implementada con nodos enlazados en anillo.
 * ESTRUCTURA DE LOS TURNOS (punto 8 del enunciado)
 * y tambien de las CARTAS DE EVENTO (punto 10).
 *
 * FIFO = First In, First Out = "el primero que entra es el primero que sale",
 * igual que la fila del banco.
 *
 * Guardamos dos referencias:
 *   frente -> a quien le toca ahora (de aqui se saca)
 *   ultimo -> el final de la fila   (aqui se mete)

 * ¿POR QUE LA MISMA CLASE SIRVE PARA TURNOS Y PARA CARTAS?
 * Porque el comportamiento que piden es identico:
 *   - Turnos:  el jugador juega y pasa al final de la fila.
 *   - Cartas:  la carta se usa y "debera pasar al final para reutilizarse".
 */
public class ColaCircular<T> {
 
    // Primer nodo: a quien le toca el turno / la carta que sigue.
    private NodoSimple<T> frente;
 
    // Ultimo nodo: donde se agregan los nuevos.
    private NodoSimple<T> ultimo;
 
    // Cuantos elementos hay en la cola.
    private int cantidad;
 
    public ColaCircular() {
        this.frente = null;
        this.ultimo = null;
        this.cantidad = 0;
    }
 
    //  OPERACIONES BASICAS DE COLA
 
    /**
     * Mete un dato al FINAL de la cola.
     *
     * CASO 1 - cola vacia: el nodo nuevo es frente y ultimo a la vez,
     *          y se apunta a si mismo para cerrar el anillo.
     * CASO 2 - ya hay elementos: se engancha despues del ultimo y se
     *          vuelve a cerrar el anillo apuntando al frente.
     */
    public void encolar(T dato) {
        NodoSimple<T> nuevo = new NodoSimple<>(dato);
 
        if (frente == null) {
            // CASO 1
            frente = nuevo;
            ultimo = nuevo;
            nuevo.setSiguiente(frente); // se apunta a si mismo
        } else {
            // CASO 2
            ultimo.setSiguiente(nuevo); // el viejo ultimo apunta al nuevo
            nuevo.setSiguiente(frente); // el nuevo cierra el anillo
            ultimo = nuevo;             // ahora el nuevo es el ultimo
        }
 
        cantidad++;
    }
 
    /**
     * Saca y devuelve el dato que esta al FRENTE.
     *
     * CASO 1 - queda un solo elemento: la cola se vacia por completo,
     *          hay que poner frente y ultimo en null.
     * CASO 2 - quedan mas: el frente avanza al siguiente, y el ultimo
     *          tiene que volver a apuntar al nuevo frente para no romper
     *          el anillo (este paso es el que mas se olvida).
     */
    public T decolar() {
        validarNoVacia();
 
        T dato = frente.getDato();
 
        if (frente == ultimo) {
            // CASO 1: era el unico
            frente = null;
            ultimo = null;
        } else {
            // CASO 2
            frente = frente.getSiguiente();
            ultimo.setSiguiente(frente); // recerrar el anillo
        }
 
        cantidad--;
        return dato;
    }
 
    /**
     * Devuelve quien esta al frente SIN sacarlo de la cola.
     *
     * En el juego, esto es "¿de quien es el turno?". El servidor lo usa
     * para rechazar acciones de jugadores fuera de turno (punto 17).
     */
    public T verFrente() {
        validarNoVacia();
        return frente.getDato();
    }
 
    /**
     * ROTAR: saca al del frente y lo vuelve a meter al final, en un paso.
     *
     * Es la operacion mas importante de esta clase:
     *   - "terminar el turno" -> el jugador actual pasa al final
     *   - "usar una carta"    -> la carta usada pasa al final
     *
     * Devuelve el dato que estaba al frente, por si hace falta usarlo
     * (por ejemplo, la carta que se acaba de aplicar).
     */
    public T rotar() {
        validarNoVacia();
        T dato = decolar();
        encolar(dato);
        return dato;
    }
 

    //  ELIMINAR (jugador en bancarrota)
 
    /**
     * Saca un elemento especifico de la cola, sin importar en que posicion
     * este. Devuelve true si lo encontro y lo saco.
     *
     * ¿POR QUE HACE FALTA?
     * El punto 18 del enunciado dice que un jugador queda ELIMINADO si no
     * puede cubrir un pago obligatorio. Ese jugador tiene que salir de la
     * cola de turnos, o el juego le seguiria dando turnos a alguien que ya
     * perdio.
     */
    public boolean eliminar(T dato) {
        if (estaVacia()) {
            return false;
        }
 
        NodoSimple<T> actual = frente;
        NodoSimple<T> previo = ultimo; // el anterior al frente es el ultimo
 
        for (int i = 0; i < cantidad; i++) {
            if (sonIguales(actual.getDato(), dato)) {
 
                if (cantidad == 1) {
                    // Era el unico: la cola queda vacia.
                    frente = null;
                    ultimo = null;
                } else {
                    // Se desengancha: el previo salta por encima de 'actual'.
                    previo.setSiguiente(actual.getSiguiente());
 
                    // Si borramos el frente o el ultimo, hay que reubicarlos.
                    if (actual == frente) {
                        frente = actual.getSiguiente();
                    }
                    if (actual == ultimo) {
                        ultimo = previo;
                    }
                }
 
                cantidad--;
                return true;
            }
 
            previo = actual;
            actual = actual.getSiguiente();
        }
 
        return false; // no estaba en la cola
    }
 
    /**
     * Dice si un dato esta en la cola.
     */
    public boolean contiene(T dato) {
        if (estaVacia()) {
            return false;
        }
 
        NodoSimple<T> nodo = frente;
        for (int i = 0; i < cantidad; i++) {
            if (sonIguales(nodo.getDato(), dato)) {
                return true;
            }
            nodo = nodo.getSiguiente();
        }
        return false;
    }
 

    //  CONSULTAS
    /**
     * Recorre la cola una vuelta completa desde el frente.
     * Sirve, por ejemplo, para mostrar el orden de turnos en la interfaz.
     */
    public void recorrer(Visitante<T> visitante) {
        if (estaVacia()) {
            return;
        }
 
        NodoSimple<T> nodo = frente;
        for (int i = 0; i < cantidad; i++) {
            visitante.visitar(nodo.getDato());
            nodo = nodo.getSiguiente();
        }
    }
 
    public boolean estaVacia() {
        return frente == null;
    }
 
    public int getCantidad() {
        return cantidad;
    }
 

    //  AUXILIARES INTERNOS
    /**
     * Compara dos datos con cuidado de los null.
     *
     * Usamos equals() y no == porque == compara si son EL MISMO objeto en
     * memoria, mientras que equals() compara si son iguales en contenido.
     * Ojo: para que esto funcione bien con nuestras clases (Jugador, etc.)
     * hay que sobrescribir equals() en ellas, o comparar por id.
     */
    private boolean sonIguales(T a, T b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
 
    private void validarNoVacia() {
        if (frente == null) {
            throw new IllegalStateException("La cola esta vacia");
        }
    }
 
    @Override
    public String toString() {
        if (estaVacia()) {
            return "(cola vacia)";
        }
 
        StringBuilder sb = new StringBuilder();
        NodoSimple<T> nodo = frente;
        for (int i = 0; i < cantidad; i++) {
            sb.append(nodo.getDato());
            if (i < cantidad - 1) {
                sb.append(" -> ");
            }
            nodo = nodo.getSiguiente();
        }
        sb.append("  (el ultimo vuelve al frente)");
        return sb.toString();
    }
}
 
