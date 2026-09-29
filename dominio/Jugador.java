package dominio;

import estructuras.ListaDoble;
import estructuras.NodoDoble;

/**
 * Jugador: un participante de la partida (punto 4 del enunciado).
 *
 * Tiene los seis datos minimos que pide el enunciado: identificador, nombre,
 * saldo, posicion actual, estado activo/inactivo y propiedades adquiridas.
 *
 * LA POSICION NO ES UN NUMERO, ES UN NODO.
 * El tablero es una ListaCircularDoble, asi que guardar "estoy en la casilla
 * 17" obligaria a recorrer 17 nodos cada vez que hay que saber donde esta.
 * Guardando el NodoDoble directamente, moverse es solo seguir las flechas
 * desde donde ya estaba parado.
 *
 * LOS METODOS DE ESTA CLASE NO VALIDAN REGLAS DEL JUEGO.
 * comprarPropiedad() no revisa si la propiedad tiene duenio, y debitar() no
 * decide si el jugador quiebra. Eso lo hace el Banco, porque el punto 3 del
 * enunciado exige que sea el banco quien valide antes de modificar el estado.
 * Jugador es el que guarda los datos; Banco es el que manda.
 * El servidor nunca debe llamar estos metodos de forma directa.
 */
public class Jugador {

    // El id no cambia nunca: es la identidad del jugador y con el se compara
    // en equals(). Tambien es lo que manda la tarjeta RFID.
    private final String id;

    private final String nombre;

    private int saldo;

    // Nodo del tablero donde esta parado. Arranca en null porque el tablero
    // todavia no existe cuando se crea el jugador; el Juego lo coloca en la
    // casilla de salida al iniciar la partida.
    private NodoDoble<Casilla> posicionActual;

    // false cuando queda eliminado por bancarrota (punto 18).
    private boolean activo;

    // La lista es final: la referencia no cambia, pero su contenido si.
    private final ListaDoble<Propiedad> propiedades;

    public Jugador(String id, String nombre, int saldoInicial) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("El jugador necesita un identificador");
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El jugador " + id + " necesita un nombre");
        }
        if (saldoInicial < 0) {
            throw new IllegalArgumentException("El saldo inicial no puede ser negativo: " + saldoInicial);
        }

        this.id = id.trim();
        this.nombre = nombre.trim();
        this.saldo = saldoInicial;
        this.posicionActual = null;
        this.activo = true;
        this.propiedades = new ListaDoble<>();
    }

    //  DINERO

    /**
     * Le suma dinero al jugador.
     */
    public void acreditar(int monto) {
        validarMonto(monto);
        saldo += monto;
    }

    /**
     * Le resta dinero al jugador.
     *
     * Lanza excepcion si no alcanza, en vez de dejar el saldo en negativo.
     * El Banco pregunta antes con puedePagar(), asi que si esta excepcion se
     * dispara es porque alguien salteo la validacion.
     */
    public void debitar(int monto) {
        validarMonto(monto);

        if (!puedePagar(monto)) {
            throw new OperacionInvalidaException(
                    "Saldo insuficiente: " + nombre + " tiene " + saldo + " y se le quieren cobrar " + monto);
        }

        saldo -= monto;
    }

    /**
     * Dice si el jugador puede cubrir un monto. El Banco lo consulta SIEMPRE
     * antes de cobrar, para decidir entre cobrar normal o declarar bancarrota.
     */
    public boolean puedePagar(int monto) {
        return saldo >= monto;
    }

    //  PROPIEDADES

    /**
     * Paga la propiedad y la agrega a su lista.
     *
     * Se agrega AL INICIO y no al final porque la compra mas reciente es la que
     * mas interesa mostrar en la interfaz, y agregarAlInicio() es un solo paso.
     *
     * No valida si la propiedad esta disponible ni si el saldo alcanza: de eso
     * se encarga Banco.comprarPropiedad() antes de llamar aqui.
     */
    public void comprarPropiedad(Propiedad propiedad) {
        validarPropiedad(propiedad);

        debitar(propiedad.getPrecio());
        propiedades.agregarAlInicio(propiedad);
        propiedad.setPropietario(this);
    }

    /**
     * Le quita una propiedad y la deja libre. Pasa cuando el jugador quiebra
     * debiendole al banco (punto 18).
     *
     * Esto es lo que hace obligatorio el equals() de Propiedad: ListaDoble
     * .eliminar() compara con equals(), y la referencia que llega puede venir
     * del tablero y no ser el mismo objeto en memoria.
     */
    public void perderPropiedad(Propiedad propiedad) {
        validarPropiedad(propiedad);

        if (!propiedades.eliminar(propiedad)) {
            throw new OperacionInvalidaException(
                    "La propiedad " + propiedad.getNombre() + " no le pertenece a " + nombre);
        }

        propiedad.setPropietario(null);
    }

    /**
     * Recibe una propiedad sin pagarla. Es el otro lado de perderPropiedad:
     * cuando un jugador quiebra debiendole a otro jugador, sus propiedades
     * pasan al acreedor.
     */
    public void recibirPropiedad(Propiedad propiedad) {
        validarPropiedad(propiedad);

        propiedades.agregarAlInicio(propiedad);
        propiedad.setPropietario(this);
    }

    /**
     * Patrimonio = saldo + valor de las propiedades (punto 18).
     * Con esto se decide el ganador cuando se agota el limite de turnos.
     *
     * ¿POR QUE UN ARREGLO DE UN SOLO ELEMENTO?
     * recorrerDesdeInicio() recibe una lambda, y una lambda de Java no puede
     * modificar una variable local del metodo que la rodea: la variable tendria
     * que ser "efectivamente final". Con un arreglo el truco funciona porque lo
     * que no puede cambiar es la REFERENCIA al arreglo, no su contenido.
     * total[0] = otra cosa es valido; total = otro arreglo no lo seria.
     */
    public int calcularPatrimonio() {
        int[] total = { saldo };

        propiedades.recorrerDesdeInicio(propiedad -> total[0] += propiedad.getPrecio());

        return total[0];
    }

    //  POSICION EN EL TABLERO

    /**
     * Coloca al jugador en un nodo del tablero.
     * Quien calcula a que nodo llega es el Tablero con avanzar() o retroceder();
     * aqui solo se guarda el resultado.
     */
    public void moverA(NodoDoble<Casilla> destino) {
        if (destino == null) {
            throw new IllegalArgumentException("No se puede mover a " + nombre + " a una casilla nula");
        }
        this.posicionActual = destino;
    }

    public NodoDoble<Casilla> getPosicionActual() {
        return posicionActual;
    }

    /**
     * La casilla donde esta parado, ya sin el nodo de por medio.
     * Devuelve null si todavia no se le asigno posicion.
     */
    public Casilla getCasillaActual() {
        return posicionActual == null ? null : posicionActual.getDato();
    }

    //  ESTADO Y CONSULTAS

    public boolean estaActivo() {
        return activo;
    }

    /**
     * Lo llama Banco.eliminarJugador() cuando el jugador quiebra.
     */
    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public String getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public int getSaldo() {
        return saldo;
    }

    public ListaDoble<Propiedad> getPropiedades() {
        return propiedades;
    }

    public int getCantidadPropiedades() {
        return propiedades.getCantidad();
    }

    //  VALIDACIONES INTERNAS

    private void validarMonto(int monto) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero, se recibio: " + monto);
        }
    }

    private void validarPropiedad(Propiedad propiedad) {
        if (propiedad == null) {
            throw new IllegalArgumentException("La propiedad recibida es nula");
        }
    }

    //  IDENTIDAD

    /**
     * Dos jugadores son el mismo si tienen el mismo id.
     *
     * Esto NO es cosmetico, es obligatorio para que el juego funcione.
     * ColaCircular.eliminar() y ListaDoble.eliminar() comparan con equals().
     * Sin sobrescribirlo, Java compara direcciones de memoria y sacar a un
     * jugador en bancarrota de la cola de turnos falla en silencio: el juego
     * le seguiria dando turnos a alguien que ya perdio.
     */
    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Jugador)) {
            return false;
        }
        return this.id.equals(((Jugador) otro).id);
    }

    /**
     * Si dos objetos son equals, Java exige que su hashCode coincida.
     * Como equals compara por id, el hashCode sale del id.
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        String casilla = (getCasillaActual() == null) ? "sin ubicar" : getCasillaActual().getNombre();
        String estado = activo ? "activo" : "eliminado";

        return nombre + " (" + id + ") saldo " + saldo
                + ", " + propiedades.getCantidad() + " propiedades"
                + ", en " + casilla
                + ", " + estado;
    }
}
