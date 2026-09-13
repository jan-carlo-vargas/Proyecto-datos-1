package estructuras; // organización de clases en paquetes, para no mezclar con otras

public class ListaCircularDoble<T> { // <T> indica que es una lista generica, puede contener cualquier tipo de dato
 
    private NodoDoble<T> inicio; // referencia al primer nodo del anillo. Si es null, la lista esta vacia.
    private int cantidad;// cantidad de nodos que hay en el anillo. Se mantiene actualizada al agregar o quitar nodos.
 
    // Constructor: la lista arranca vacia.
     public ListaCircularDoble() {
        this.inicio = null;
        this.cantidad = 0;
    }
 
    //  AGREGAR
    public void agregar(T dato) {
        NodoDoble<T> nuevo = new NodoDoble<>(dato); //construimos un nodo nuevo con el dato recibido.
 
        if (inicio == null) {
            // primer nodo del anillo.
            inicio = nuevo;
            nuevo.setSiguiente(nuevo); // se apunta a si mismo
            nuevo.setAnterior(nuevo);  // en ambos sentidos
        } else {
            //el "ultimo" es el que esta justo antes del inicio.
            // Como es circular, llegar al ultimo es gratis: inicio.anterior.
            NodoDoble<T> ultimo = inicio.getAnterior();
 
            // Enganchamos el nodo nuevo entre 'ultimo' e 'inicio':
            ultimo.setSiguiente(nuevo);   // 1. ultimo ahora apunta al nuevo
            nuevo.setAnterior(ultimo);    // 2. nuevo mira hacia atras al ultimo
            nuevo.setSiguiente(inicio);   // 3. nuevo cierra el anillo hacia el inicio
            inicio.setAnterior(nuevo);    // 4. el inicio mira hacia atras al nuevo
        }
        cantidad++; // aumentamos la cantidad de nodos en el anillo
    }
 
    // Inserta un dato en una posicion especifica (0 = al inicio).
    public void insertarEn(int posicion, T dato) { 
        if (posicion < 0 || posicion > cantidad) { // || es un or logico. Si la posicion es negativa o mayor a la cantidad, es invalida.
            throw new IndexOutOfBoundsException("Posicion invalida: " + posicion); 
            /* 
            * throw hace que el programa se detenga y muestre un error.
            * new IndexOutOfBoundsException crea un objeto de error que indica que el indice esta fuera de rango.
            * El mensaje entre comillas se muestra junto con el error.
            */ 
            
        }
 
        // Insertar al final es exactamente lo que ya hace agregar().
        if (posicion == cantidad) {
            agregar(dato);
            return;
        }
 
        NodoDoble<T> nuevo = new NodoDoble<>(dato); //creamos el nodo nuevo con el dato recibido. sin enlaces todavia.
        NodoDoble<T> actual = obtenerNodo(posicion); //obtenemos el nodo que actualmente esta en la posicion indicada. Lo vamos a "empujar" hacia adelante.
        NodoDoble<T> previo = actual.getAnterior(); //leemos el nodo que esta justo antes del actual. Ese es el que va a apuntar al nuevo.
 
        // se usan el previo y el actual que creamos
        previo.setSiguiente(nuevo);
        nuevo.setAnterior(previo);
        nuevo.setSiguiente(actual);
        actual.setAnterior(nuevo);
 
        // Si insertamos en la posicion 0, el nodo nuevo pasa a ser el inicio.
        if (posicion == 0) {
            inicio = nuevo;
        }
 
        cantidad++;
    }
 
  
    //  MOVIMIENTO POR EL TABLERO
    /**
     * Devuelve el nodo que esta 'pasos' casillas ADELANTE de 'actual'.
     * Esto es literalmente "mover la ficha" cuando se tiran los dados.
     *
     * No hace falta revisar si nos pasamos del final: como el anillo esta
     * cerrado, seguir avanzando simplemente da otra vuelta.
     */

    // devuelven NodoDoble<T> para que el servidor pueda saber en que casilla quedo el jugador, y si paso por inicio.
    public NodoDoble<T> avanzar(NodoDoble<T> actual, int pasos) { // nodoDoble<T> actual: el nodo desde el cual se empieza a contar los pasos. int pasos: la cantidad de pasos a avanzar.
        validarNoVacia(); // 
        validarNodo(actual);
 
        NodoDoble<T> nodo = actual; // NodoDoble<T> nodo: variable temporal para recorrer la lista sin modificar el nodo actual.
        for (int i = 0; i < pasos; i++) {
            nodo = nodo.getSiguiente(); // un salto por cada paso
        }
        return nodo;
    }
 
    /**
     * Igual que avanzar pero hacia atras.
     * Lo usan las cartas de evento del tipo "retroceda N casillas".
     */
    public NodoDoble<T> retroceder(NodoDoble<T> actual, int pasos) {
        validarNoVacia();
        validarNodo(actual);
 
        NodoDoble<T> nodo = actual;
        for (int i = 0; i < pasos; i++) {
            nodo = nodo.getAnterior();
        }
        return nodo;
    }
 
    /**
     * Dice si moverse 'pasos' hacia adelante desde 'actual' hace que el
     * jugador PASE POR LA CASILLA DE INICIO.
     *
     * Sirve para el "premio por pasar por inicio" que pide el punto 11
     * del enunciado (tipo de transaccion obligatorio).
     *
     * Como funciona: damos los pasos de uno en uno y, cada vez que caemos
     * sobre el nodo inicio, contamos que pasamos por ahi.
     */
    public boolean pasaPorInicio(NodoDoble<T> actual, int pasos) {
        validarNoVacia();
        validarNodo(actual);
 
        NodoDoble<T> nodo = actual;
        for (int i = 0; i < pasos; i++) {
            nodo = nodo.getSiguiente();
            if (nodo == inicio) {
                return true;
            }
        }
        return false;
    }
 
    /**
     * Cuenta cuantos pasos hacia adelante hay entre dos nodos.
     * Util para cartas del tipo "vaya a la casilla X": el servidor puede
     * saber cuantas casillas recorrio el jugador y si paso por inicio.
     */
    public int pasosHasta(NodoDoble<T> origen, NodoDoble<T> destino) {
        validarNoVacia();
        validarNodo(origen);
        validarNodo(destino);
 
        int pasos = 0;
        NodoDoble<T> nodo = origen;
        while (nodo != destino) {
            nodo = nodo.getSiguiente();
            pasos++;
            //Seguro contra bucle infinito: si el destino no pertenece
            //a esta lista, nunca lo encontrariamos.
            if (pasos > cantidad) {
                throw new IllegalArgumentException("El nodo destino no pertenece a esta lista");
            }
        }
        return pasos;
    }
 
    //  CONSULTAS
    
 
    /**
     * Devuelve el nodo que esta en la posicion indicada (0, 1, 2...).
     *
     * OJO: aqui esta la gran diferencia con un arreglo. En un arreglo
     * tablero[17] es inmediato. Aqui hay que CAMINAR 17 nodos desde el
     * inicio. Para un Monopoly no importa, porque el jugador siempre se
     * mueve paso a paso desde donde esta, nunca salta a una posicion arbitraria
     */
    public NodoDoble<T> obtenerNodo(int posicion) { // metodo privado que devuelve el nodo en la posicion indicada, para no repetir codigo en obtener() y buscarNodo()
        validarNoVacia();   
        if (posicion < 0 || posicion >= cantidad) {
            throw new IndexOutOfBoundsException("Posicion invalida: " + posicion);
        }
 
        NodoDoble<T> nodo = inicio;
        for (int i = 0; i < posicion; i++) {
            nodo = nodo.getSiguiente();
        }
        return nodo;
    }
 
    /**
     * Devuelve el DATO que esta en la posicion indicada.
     * Version comoda de obtenerNodo() para cuando no interesa el nodo.
     */
    public T obtener(int posicion) { // T es el tipo generico de dato que contiene la lista. Se usa para devolver el dato del nodo en la posicion indicada.
        return obtenerNodo(posicion).getDato();
    }
 
    /**
     * Busca el primer nodo cuyo dato cumpla la condicion dada.
     * Devuelve null si ninguno cumple.
     *
     * Ejemplo en el juego:
     *   tablero.buscarNodo(c -> c.getNombre().equals("Carcel"));
     */
    public NodoDoble<T> buscarNodo(Criterio<T> criterio) {
        if (estaVacia()) {
            return null;
        }
 
        NodoDoble<T> nodo = inicio;
        // Recorremos exactamente 'cantidad' veces. Si usaramos un
        // while (nodo != null) nunca terminaria: en un anillo jamas hay null.
        for (int i = 0; i < cantidad; i++) {
            if (criterio.cumple(nodo.getDato())) {
                return nodo;
            }
            nodo = nodo.getSiguiente();
        }
        return null;
    }
 
    /**
     * Recorre el anillo una sola vuelta completa, desde el inicio,
     * aplicando el visitante a cada dato.
     */
    public void recorrer(Visitante<T> visitante) {
        if (estaVacia()) {
            return;
        }
 
        NodoDoble<T> nodo = inicio;
        for (int i = 0; i < cantidad; i++) {
            visitante.visitar(nodo.getDato());
            nodo = nodo.getSiguiente();
        }
    }
 
    public NodoDoble<T> getInicio() {
        return inicio;
    }
 
    public int getCantidad() {
        return cantidad;
    }
 
    public boolean estaVacia() {
        return inicio == null;
    }
 
    //  VALIDACIONES INTERNAS
    private void validarNoVacia() { // metodo privado que lanza una excepcion si la lista esta vacia, para no repetir codigo en varios metodos
        if (inicio == null) {
            throw new IllegalStateException("La lista circular esta vacia"); 
            // IllegalStateException es un error que indica que el objeto no esta en un estado valido para la operacion solicitada.
        }
    }
 
    private void validarNodo(NodoDoble<T> nodo) { // metodo privado que lanza una excepcion si el nodo recibido es null, para no repetir codigo en varios metodos
        if (nodo == null) {
            throw new IllegalArgumentException("El nodo recibido es null");
            // IllegalArgumentException es un error que indica que el argumento pasado a un metodo es invalido.
        }
    }
 
   
    //  IMPRESION
  
    /**
     * Arma un texto con todos los datos en orden.
     * Sirve para revisar a simple vista que el anillo quedo bien armado.
     *
     * Usamos StringBuilder y no "texto = texto + algo" porque en Java
     * cada + crea un String nuevo; con muchos elementos eso se vuelve lento.
     */
    // toString es un metodo que viene de la clase Object, que es la superclase de todas las clases.
    @Override // sobreescribimos el metodo toString para que devuelva el dato del nodo, en vez de la referencia al objeto.
    public String toString() {
        if (estaVacia()) {
            return "(lista vacia)";
        }
 
        StringBuilder sb = new StringBuilder();
        NodoDoble<T> nodo = inicio;
        for (int i = 0; i < cantidad; i++) {
            sb.append(nodo.getDato()).append(" -> ");
            nodo = nodo.getSiguiente();
        }
        sb.append("(vuelve al inicio)");
        return sb.toString();
    }
}
