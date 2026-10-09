package dominio;

import java.util.Random;

/**
 * Dado: los dos dados electronicos del juego
 *
 * DOS FORMAS DE OBTENER EL RESULTADO
 *   tirar():         sortea con numeros aleatorios.
 *   fijarValores():  el resultado viene del dado fisico (7 segmentos de la protoboard), El modulo de hardware
 *                      manda los dos numeros y el servidor los registra aqui
 * 
 *
 * Esta clase solo guarda y sortea valores. 
 *
 * 
 */
public class Dado {

    public static final int CARAS = 6;

    private final Random azar;

    // 0 significa que el dado no se ha tirado
    private int valor1;
    private int valor2;

    public Dado() {
        this.azar = new Random();
    }

    /**
     * esto sirve para que funcione automaticamente ya que se repiten resultados
     */
    public Dado(long semilla) {
        this.azar = new Random(semilla);
    }

    /**
     * aqui da la suma de los dados tirados que dice cuanto avanza
     */
    public int tirar() {
        valor1 = azar.nextInt(CARAS) + 1;
        valor2 = azar.nextInt(CARAS) + 1;
        return getTotal();
    }

    /**
     * aqui no funciona lo de los dados virtuales, sino el fisico, entocnes aqui es para que si dan numeros no validos como 7, que no lo tome como valido
     */
    public void fijarValores(int primero, int segundo) {
        validarCara(primero);
        validarCara(segundo);
        this.valor1 = primero;
        this.valor2 = segundo;
    }

    public int getValor1() {
        return valor1;
    }

    public int getValor2() {
        return valor2;
    }

    /** Suma de los dos dados, 0 si todavia no se ha tirado */
    public int getTotal() {
        return valor1 + valor2;
    }

    /** true si salieron dos numeros iguales. */
    public boolean esDoble() {
        return valor1 != 0 && valor1 == valor2;
    }

    public boolean seHaTirado() {
        return valor1 != 0;
    }

    private void validarCara(int valor) {
        if (valor < 1 || valor > CARAS) {
            throw new IllegalArgumentException("Un dado tiene caras de 1 a " + CARAS + ", se recibio: " + valor);
        }
    }

    @Override
    public String toString() {
        if (!seHaTirado()) {
            return "(dados sin tirar)";
        }
        return valor1 + " + " + valor2 + " = " + getTotal();
    }
}
