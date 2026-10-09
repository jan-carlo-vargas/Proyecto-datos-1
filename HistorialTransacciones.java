package dominio;

import estructuras.ListaDoble;
import estructuras.Visitante;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * HistorialTransacciones: el registro completo de la partida.
 * Cubre entero el punto 12 del enunciado y genera el reporte del punto 13.
 *
 * ¿POR QUE ListaDoble Y NO OTRA ESTRUCTURA?
 * El enunciado pide seis operaciones: agregar, recorrer desde la mas antigua,
 * recorrer desde la mas reciente, buscar por jugador, buscar por tipo e
 * imprimir todas.
 *
 * Una cola no sirve porque para leer habria que ir sacando elementos, y el
 * historial no se puede vaciar. Una lista simple tampoco, porque con una sola
 * flecha "recorrer desde la mas reciente" obliga a recorrer todo al reves.
 * Una lista circular menos todavia: el historial tiene un principio y un final
 * reales, y en un anillo los recorridos no terminarian nunca.
 * La ListaDoble tiene flecha anterior y siguiente, asi que los dos recorridos
 * cuestan lo mismo y llegar al final es directo por el campo fin.
 *
 * ¿POR QUE UNA CLASE APARTE Y NO METODOS SUELTOS EN Banco?
 * Porque el Banco ya tiene bastante con validar turnos, cobrar y manejar
 * bancarrotas. Aca el punto 12 queda completo en un solo archivo. El Banco
 * expone los mismos metodos del diagrama UML delegando en esta clase.
 */
public class HistorialTransacciones {

    private final ListaDoble<Transaccion> transacciones;

    public HistorialTransacciones() {
        this.transacciones = new ListaDoble<>();
    }

    /**
     * Agrega una transaccion al final, que es donde va la mas reciente.
     * Nunca se borra nada: un historial que se pueda editar no sirve de
     * registro.
     */
    public void agregar(Transaccion transaccion) {
        if (transaccion == null) {
            throw new IllegalArgumentException("No se puede agregar una transaccion nula al historial");
        }
        transacciones.agregarAlFinal(transaccion);
    }

    //  RECORRIDOS (punto 12)

    /**
     * Recorre de la mas antigua a la mas reciente. Es el orden del reporte.
     */
    public void recorrerDesdeLaMasAntigua(Visitante<Transaccion> visitante) {
        validarVisitante(visitante);
        transacciones.recorrerDesdeInicio(visitante);
    }

    /**
     * Recorre de la mas reciente a la mas antigua. Es el orden natural para
     * mostrar "lo ultimo que paso" en la interfaz.
     * Solo es posible porque los nodos tienen flecha anterior.
     */
    public void recorrerDesdeLaMasReciente(Visitante<Transaccion> visitante) {
        validarVisitante(visitante);
        transacciones.recorrerDesdeFin(visitante);
    }

    //  BUSQUEDAS (punto 12)

    /**
     * Todas las transacciones donde aparece el jugador, sea pagando o cobrando.
     *
     * La lambda que se le pasa a buscar() es la condicion; la lista solo sabe
     * caminar por sus nodos y preguntarle a cada dato si cumple. Por eso una
     * misma ListaDoble sirve para el historial y para las propiedades.
     *
     * Devuelve una lista NUEVA: el historial original queda intacto.
     */
    public ListaDoble<Transaccion> buscarPorJugador(String idJugador) {
        if (idJugador == null || idJugador.trim().isEmpty()) {
            throw new IllegalArgumentException("Hay que indicar el id del jugador a buscar");
        }

        String id = idJugador.trim();
        return transacciones.buscar(transaccion -> transaccion.involucraA(id));
    }

    /**
     * Todas las transacciones de un tipo. Se comparan los tipos con == y no con
     * equals() porque de cada valor de un enum existe una sola instancia en
     * toda la aplicacion.
     */
    public ListaDoble<Transaccion> buscarPorTipo(TipoTransaccion tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("Hay que indicar el tipo de transaccion a buscar");
        }

        return transacciones.buscar(transaccion -> transaccion.getTipo() == tipo);
    }

    /**
     * Busca una transaccion por su numero. Devuelve null si no existe.
     */
    public Transaccion buscarPorId(int id) {
        return transacciones.buscarPrimero(transaccion -> transaccion.getId() == id);
    }

    //  REPORTE (punto 13)

    /**
     * Arma el reporte completo como un solo texto.
     *
     * Es el UNICO lugar donde se construye el reporte. imprimirTodas() y
     * exportarTxt() lo reutilizan, y el servidor tambien lo puede usar para
     * responder CONSULTAR_TRANSACCIONES. Si cada uno armara su propio texto,
     * tarde o temprano se desincronizarian y el archivo no coincidiria con lo
     * que muestra la pantalla.
     *
     * StringBuilder y no concatenar con +, porque en Java cada + crea un String
     * nuevo; con cientos de transacciones eso se vuelve lento.
     *
     * La lambda puede usar sb porque nunca se le reasigna otra referencia: es
     * "efectivamente final". Llamarle metodos a un objeto si esta permitido; lo
     * prohibido es apuntar la variable a otro objeto.
     */
    public String generarTexto() {
        StringBuilder sb = new StringBuilder();
        String salto = System.lineSeparator();

        sb.append("HISTORIAL DE TRANSACCIONES - MONOPOLY DISTRIBUIDO").append(salto);
        sb.append(Transaccion.encabezado()).append(salto);

        if (transacciones.estaVacia()) {
            sb.append("(todavia no hay transacciones)").append(salto);
        } else {
            transacciones.recorrerDesdeInicio(
                    transaccion -> sb.append(transaccion.toTexto()).append(salto));
        }

        sb.append(salto);
        sb.append("Total de transacciones: ").append(transacciones.getCantidad());

        return sb.toString();
    }

    /**
     * Imprime todas las transacciones en consola (punto 12).
     */
    public void imprimirTodas() {
        System.out.println(generarTexto());
    }

    /**
     * Exporta el historial a un archivo TXT (punto 13).
     *
     * try-with-resources: el PrintWriter se declara entre parentesis y Java lo
     * cierra solo al terminar, incluso si salta una excepcion. Sin esto, un
     * error a media escritura dejaria el archivo abierto y probablemente vacio,
     * porque lo escrito se queda en el buffer hasta que se cierra.
     */
    public void exportarTxt(String ruta) {
        if (ruta == null || ruta.trim().isEmpty()) {
            throw new IllegalArgumentException("Hay que indicar la ruta del archivo de reporte");
        }

        try (PrintWriter salida = new PrintWriter(new FileWriter(ruta))) {
            salida.println(generarTexto());
        } catch (IOException e) {
            // Se traduce el error tecnico de Java a uno que diga que archivo
            // fallo. Se conserva la causa original por si hay que depurar.
            throw new RuntimeException("No se pudo escribir el reporte en: " + ruta
                    + " (" + e.getMessage() + ")", e);
        }
    }

    //  CONSULTAS SIMPLES

    /**
     * La lista interna, por si el servidor necesita recorrerla a su manera.
     */
    public ListaDoble<Transaccion> getTransacciones() {
        return transacciones;
    }

    public Transaccion getMasReciente() {
        return transacciones.getUltimo();
    }

    public int getCantidad() {
        return transacciones.getCantidad();
    }

    public boolean estaVacio() {
        return transacciones.estaVacia();
    }

    //  VALIDACIONES INTERNAS

    private void validarVisitante(Visitante<Transaccion> visitante) {
        if (visitante == null) {
            throw new IllegalArgumentException("Hay que indicar que hacer con cada transaccion del recorrido");
        }
    }

    @Override
    public String toString() {
        return "Historial con " + transacciones.getCantidad() + " transacciones";
    }
}
