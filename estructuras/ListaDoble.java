package estructuras;
 
public class ListaDoble<T> {
 
    // Primer nodo (el dato mas antiguo). null si la lista esta vacia.
    private NodoDoble<T> inicio;
 
    // Ultimo nodo (el dato mas reciente). null si la lista esta vacia.
    private NodoDoble<T> fin;
 
    private int cantidad;
 
    public ListaDoble() {
        this.inicio = null;
        this.fin = null;
        this.cantidad = 0;
    }
 

    //  AGREGAR
    /**
     * Agrega un dato al final. Es la operacion normal del historial:
     */
    public void agregarAlFinal(T dato) {
        NodoDoble<T> nuevo = new NodoDoble<>(dato); //crea un nodo con el dato, apuntando a null por ambos lados.
 
        if (inicio == null) {
            // Lista vacia: el nodo nuevo es inicio y fin a la vez.
            inicio = nuevo;
            fin = nuevo;
        } else {
            fin.setSiguiente(nuevo); // el viejo fin apunta al nuevo
            nuevo.setAnterior(fin);  // el nuevo mira hacia atras
            fin = nuevo;             // el nuevo pasa a ser el fin
        }
 
        cantidad++;
    }
 
    /**
     * Agrega un dato al inicio. Es la operacion normal de las propiedades del jugador: cuando se compra una propiedad, pasa a ser la mas reciente.
     */
    public void agregarAlInicio(T dato) {
        NodoDoble<T> nuevo = new NodoDoble<>(dato);
 
        if (inicio == null) {
            inicio = nuevo;
            fin = nuevo;
        } else {
            nuevo.setSiguiente(inicio);
            inicio.setAnterior(nuevo);
            inicio = nuevo;
        }
 
        cantidad++;
    }
 
   
    //  ELIMINAR
 
    /**
     * Elimina la primera aparicion de un dato. Devuelve true si lo elimino.
     *
     * En el juego se usa para las propiedades del jugador: si una propiedad
     * cambia de dueño, hay que sacarla de la lista del dueño anterior.
     * (El historial de transacciones NO se borra nunca: es un registro.)
     *
     * Los casos borde son los extremos. Si el nodo a borrar es el inicio,
     * no tiene 'anterior' al cual reengancharlo; igual con el fin.
     */
    public boolean eliminar(T dato) {
        NodoDoble<T> actual = inicio;
 
        while (actual != null) {
            if (sonIguales(actual.getDato(), dato)) {
 
                if (actual.getAnterior() != null) {
                    // Hay alguien antes: que salte por encima de 'actual'.
                    actual.getAnterior().setSiguiente(actual.getSiguiente());
                } else {
                    // 'actual' era el inicio: el inicio pasa al siguiente.
                    inicio = actual.getSiguiente();
                }
 
                if (actual.getSiguiente() != null) {
                    // Hay alguien despues: que mire hacia atras al previo.
                    actual.getSiguiente().setAnterior(actual.getAnterior());
                } else {
                    // 'actual' era el fin: el fin pasa al anterior.
                    fin = actual.getAnterior();
                }
 
                cantidad--;
                return true;
            }
            actual = actual.getSiguiente();
        }
 
        return false; // no estaba
    }

    //  RECORRIDOS (punto 12 del enunciado)
    /**
     * Recorre desde la mas ANTIGUA hacia la mas reciente.
     * Este es el orden del reporte TXT (punto 13).
     */
    public void recorrerDesdeInicio(Visitante<T> visitante) {
        NodoDoble<T> actual = inicio;
        // Aqui si podemos usar while (actual != null) porque la lista NO es
        // circular: al llegar al final, siguiente es null y el ciclo termina.
        while (actual != null) {
            visitante.visitar(actual.getDato());
            actual = actual.getSiguiente();
        }
    }
 
    /**
     * Recorre desde la mas RECIENTE hacia la mas antigua.
     * Sirve para mostrar "lo ultimo que paso" en la interfaz.
     * Esto es posible SOLO porque los nodos tienen flecha 'anterior'.
     */
    public void recorrerDesdeFin(Visitante<T> visitante) {
        NodoDoble<T> actual = fin;
        while (actual != null) {
            visitante.visitar(actual.getDato());
            actual = actual.getAnterior();
        }
    }
 
  
    //  BUSQUEDAS (punto 12: buscar por jugador y por tipo)
    /**
     * Devuelve una NUEVA ListaDoble con todos los datos que cumplen el
     * criterio. La lista original no se toca.
     *
     * Un solo metodo cubre "buscar por jugador" y "buscar por tipo":
     * lo que cambia es la condicion que le pasa quien llama.
     *
     *   historial.buscar(t -> t.getOrigen().equals("J1"));
     *   historial.buscar(t -> t.getTipo().equals("ALQUILER"));
     */
    public ListaDoble<T> buscar(Criterio<T> criterio) {
        ListaDoble<T> resultados = new ListaDoble<>();
 
        NodoDoble<T> actual = inicio;
        while (actual != null) {
            if (criterio.cumple(actual.getDato())) {
                resultados.agregarAlFinal(actual.getDato());
            }
            actual = actual.getSiguiente();
        }
 
        return resultados;
    }
 
    /**
     * Devuelve el primer dato que cumple el criterio, o null si ninguno.
     */
    public T buscarPrimero(Criterio<T> criterio) {
        NodoDoble<T> actual = inicio;
        while (actual != null) {
            if (criterio.cumple(actual.getDato())) {
                return actual.getDato();
            }
            actual = actual.getSiguiente();
        }
        return null;
    }
 
    /**
     * Dice si un dato esta en la lista.
     */
    public boolean contiene(T dato) {
        NodoDoble<T> actual = inicio;
        while (actual != null) {
            if (sonIguales(actual.getDato(), dato)) {
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }
 
    /**
     * Devuelve el dato en la posicion indicada (0 = el mas antiguo).
     */
    public T obtener(int posicion) {
        if (posicion < 0 || posicion >= cantidad) {
            throw new IndexOutOfBoundsException("Posicion invalida: " + posicion);
        }
 
        NodoDoble<T> actual = inicio;
        for (int i = 0; i < posicion; i++) {
            actual = actual.getSiguiente();
        }
        return actual.getDato();
    }
 
    //  CONSULTAS SIMPLES
 
    public T getPrimero() {
        return inicio == null ? null : inicio.getDato(); // ? es el operador ternario: si inicio es null devuelve null, sino devuelve inicio.getDato().
    }
 
    public T getUltimo() {
        return fin == null ? null : fin.getDato();
    }
 
    public int getCantidad() {
        return cantidad;
    }
 
    public boolean estaVacia() {
        return inicio == null;
    }
 
    //  AUXILIARES

 
    private boolean sonIguales(T a, T b) {
        if (a == null) {
            return b == null;
        }
        return a.equals(b);
    }
 
    @Override
    public String toString() {
        if (estaVacia()) {
            return "(lista vacia)";
        }
 
        StringBuilder sb = new StringBuilder();
        NodoDoble<T> actual = inicio;
        while (actual != null) {
            sb.append(actual.getDato());
            if (actual.getSiguiente() != null) {
                sb.append(" <-> ");
            }
            actual = actual.getSiguiente();
        }
        return sb.toString();
    }
}
 