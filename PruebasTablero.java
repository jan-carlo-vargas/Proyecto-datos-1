package pruebas;

import dominio.Banco;
import dominio.CartaEvento;
import dominio.Casilla;
import dominio.CasillaEspecial;
import dominio.CasillaEvento;
import dominio.Dado;
import dominio.Jugador;
import dominio.Propiedad;
import dominio.ResultadoEfecto;
import dominio.Tablero;
import dominio.TipoTransaccion;

import estructuras.ColaCircular;
import estructuras.ListaCircularDoble;
import estructuras.NodoDoble;

/**
 * Pruebas automaticas de la parte del tablero: Dado, Tablero, Casilla y sus
 * subclases, CartaEvento y ResultadoEfecto.
 *
 * Mismo estilo que PruebasDominio: sin librerias externas, una clase con main
 * que imprime OK o FALLO por cada comprobacion y sale con codigo 1 si algo
 * falla. Cada bloque dice a que punto del enunciado corresponde.
 *
 * Se ejecuta desde la raiz del repositorio, despues de compilar:
 *   javac -d out $(find . -name "*.java")
 *   java -cp out pruebas.PruebasTablero
 */
public class PruebasTablero {

    private static int totales = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        titulo("DADO: dos dados de 6 caras (punto 9)");
        pruebasDado();

        titulo("TABLERO: estructura circular de 24 casillas (puntos 5 y 6)");
        pruebasEstructuraTablero();

        titulo("MOVIMIENTO: avanzar, retroceder, ir a casilla (puntos 5 y 9)");
        pruebasMovimiento();

        titulo("CARTAS DE EVENTO: los seis tipos (punto 10)");
        pruebasCartas();

        titulo("MAZO: la carta usada pasa al final (punto 10)");
        pruebasMazoCircular();

        titulo("PROPIEDAD: las tres reglas del punto 7");
        pruebasPropiedad();

        titulo("CASILLA ESPECIAL: impuesto, carcel y demas (punto 6)");
        pruebasCasillaEspecial();

        titulo("POLIMORFISMO: ejecutarEfecto en las 24 casillas (punto 6)");
        pruebasPolimorfismo();

        titulo("TURNOS PERDIDOS en Jugador");
        pruebasTurnosPerdidos();

        titulo("SIMULACION: 20 partidas completas de 4 jugadores");
        pruebasSimulacion();

        resumen();
    }

    // ESCENA REUTILIZABLE

    /**
     * Una partida minima para probar efectos: banco, tablero por defecto y dos
     * jugadores ya colocados en Inicio. Premio por inicio 200.
     */
    private static class Escena {
        final Banco banco = new Banco(100, 200);
        final Tablero tablero = Tablero.crearPorDefecto();
        final Jugador j1 = new Jugador("J1", "Ana", 1500);
        final Jugador j2 = new Jugador("J2", "Beto", 1500);

        Escena() {
            banco.agregarJugador(j1);
            banco.agregarJugador(j2);
            tablero.colocarEnInicio(j1);
            tablero.colocarEnInicio(j2);
        }

        /** Agrega un jugador con poco saldo, para probar la eliminacion. */
        Jugador agregarPobre(int saldo) {
            Jugador pobre = new Jugador("J3", "Pobre", saldo);
            banco.agregarJugador(pobre);
            tablero.colocarEnInicio(pobre);
            return pobre;
        }

        int transacciones(TipoTransaccion tipo) {
            return banco.transaccionesPorTipo(tipo).getCantidad();
        }

        int totalTransacciones() {
            return banco.getHistorial().getCantidad();
        }

        /** Coloca al jugador en una casilla sin cobrar premio, para armar el escenario. */
        void ubicar(Jugador j, int idCasilla) {
            tablero.enviarACasilla(j, idCasilla);
        }
    }

    // DADO

    private static void pruebasDado() {
        Dado dado = new Dado(7L);

        revisar("un dado nuevo todavia no se ha tirado", !dado.seHaTirado());
        revisarIgual("el total de un dado sin tirar es 0", 0, dado.getTotal());

        boolean todoEnRango = true;
        boolean sumaCorrecta = true;
        boolean vioUno = false;
        boolean vioSeis = false;
        for (int i = 0; i < 2000; i++) {
            int total = dado.tirar();
            int a = dado.getValor1();
            int b = dado.getValor2();
            if (a < 1 || a > 6 || b < 1 || b > 6) {
                todoEnRango = false;
            }
            if (total != a + b || total < 2 || total > 12) {
                sumaCorrecta = false;
            }
            if (a == 1 || b == 1) {
                vioUno = true;
            }
            if (a == 6 || b == 6) {
                vioSeis = true;
            }
        }
        revisar("2000 tiradas: cada dado siempre sale entre 1 y 6", todoEnRango);
        revisar("2000 tiradas: el total es la suma y esta entre 2 y 12", sumaCorrecta);
        revisar("2000 tiradas: salen los extremos 1 y 6", vioUno && vioSeis);
        revisar("despues de tirar, seHaTirado es true", dado.seHaTirado());

        Dado a = new Dado(99L);
        Dado b = new Dado(99L);
        boolean igual = true;
        for (int i = 0; i < 50; i++) {
            if (a.tirar() != b.tirar()) {
                igual = false;
            }
        }
        revisar("misma semilla produce la misma secuencia (pruebas repetibles)", igual);

        dado.fijarValores(3, 4);
        revisarIgual("fijarValores(3, 4) da total 7 (dado fisico)", 7, dado.getTotal());
        revisar("3 y 4 no es doble", !dado.esDoble());
        dado.fijarValores(5, 5);
        revisar("5 y 5 es doble", dado.esDoble());

        revisarLanza("fijarValores rechaza un 0", IllegalArgumentException.class, () -> dado.fijarValores(0, 3));
        revisarLanza("fijarValores rechaza un 7", IllegalArgumentException.class, () -> dado.fijarValores(3, 7));
        revisarLanza("fijarValores rechaza negativos", IllegalArgumentException.class, () -> dado.fijarValores(-1, 2));
    }

    // TABLERO: ESTRUCTURA

    private static void pruebasEstructuraTablero() {
        Tablero tablero = Tablero.crearPorDefecto();

        revisar("el tablero por defecto tiene al menos 24 casillas",
                tablero.getCantidadCasillas() >= Tablero.CANTIDAD_MINIMA);

        revisar("la casilla 0 es Inicio",
                tablero.getInicio() instanceof CasillaEspecial
                        && ((CasillaEspecial) tablero.getInicio()).getTipo() == CasillaEspecial.Tipo.INICIO);

        revisar("la casilla 13 es Tamarindo", tablero.getCasilla(13).getNombre().equals("Tamarindo"));

        int[] conteo = new int[3]; // propiedades, eventos, especiales
        boolean idsEnOrden = true;
        tablero.recorrer(c -> {
            if (c instanceof Propiedad) {
                conteo[0]++;
            } else if (c instanceof CasillaEvento) {
                conteo[1]++;
            } else if (c instanceof CasillaEspecial) {
                conteo[2]++;
            }
        });
        for (int i = 0; i < tablero.getCantidadCasillas(); i++) {
            if (tablero.getCasilla(i).getId() != i) {
                idsEnOrden = false;
            }
        }
        revisarIgual("hay 13 propiedades", 13, conteo[0]);
        revisarIgual("hay 5 casillas de evento", 5, conteo[1]);
        revisarIgual("hay 6 casillas especiales", 6, conteo[2]);
        revisar("el id de cada casilla coincide con su posicion", idsEnOrden);

        // El anillo se cierra en los dos sentidos.
        Jugador j = new Jugador("J1", "Ana", 1500);
        tablero.colocarEnInicio(j);
        NodoDoble<Casilla> nodoInicio = j.getPosicionActual();
        revisarIgual("el anterior a Inicio es la ultima casilla (23)", 23, nodoInicio.getAnterior().getDato().getId());
        revisarIgual("el siguiente a Inicio es la casilla 1", 1, nodoInicio.getSiguiente().getDato().getId());

        tablero.moverJugador(j, 24);
        revisar("dar la vuelta completa (24 pasos) vuelve a Inicio", j.getPosicionActual() == nodoInicio);

        // Las casillas de evento comparten un solo mazo.
        CasillaEvento e1 = (CasillaEvento) tablero.getCasilla(2);
        CasillaEvento e2 = (CasillaEvento) tablero.getCasilla(7);
        revisar("las casillas de evento comparten el mismo mazo", e1.verSiguienteCarta() == e2.verSiguienteCarta());

        // El constructor valida.
        revisarLanza("un tablero nulo se rechaza", IllegalArgumentException.class, () -> new Tablero(null));

        ListaCircularDoble<Casilla> corta = new ListaCircularDoble<>();
        for (int i = 0; i < 10; i++) {
            corta.agregar(new CasillaEspecial(i, "C" + i, CasillaEspecial.Tipo.DESCANSO));
        }
        revisarLanza("un tablero de 10 casillas se rechaza (minimo 24)", IllegalArgumentException.class,
                () -> new Tablero(corta));

        ListaCircularDoble<Casilla> idsMalos = new ListaCircularDoble<>();
        for (int i = 0; i < 24; i++) {
            idsMalos.agregar(new CasillaEspecial(i + 1, "C" + i, CasillaEspecial.Tipo.DESCANSO));
        }
        revisarLanza("un tablero con ids que no coinciden con la posicion se rechaza",
                IllegalArgumentException.class, () -> new Tablero(idsMalos));

        revisar("buscarCasilla encuentra por criterio",
                tablero.buscarCasilla(c -> c.getNombre().equals("Cahuita")).getId() == 21);
        revisar("buscarCasilla devuelve null si nadie cumple",
                tablero.buscarCasilla(c -> c.getNombre().equals("Atlantida")) == null);
    }

    // MOVIMIENTO

    private static void pruebasMovimiento() {
        Tablero tablero = Tablero.crearPorDefecto();
        Jugador j = new Jugador("J1", "Ana", 1500);

        revisarLanza("mover a un jugador sin colocar falla con un mensaje claro", IllegalStateException.class,
                () -> tablero.moverJugador(j, 3));

        tablero.colocarEnInicio(j);
        revisarIgual("colocarEnInicio deja al jugador en la casilla 0", 0, j.getCasillaActual().getId());

        boolean paso = tablero.moverJugador(j, 5);
        revisarIgual("avanzar 5 desde Inicio llega a la casilla 5", 5, j.getCasillaActual().getId());
        revisar("avanzar de 0 a 5 no pasa por Inicio", !paso);

        tablero.enviarACasilla(j, 22);
        paso = tablero.moverJugador(j, 5);
        revisarIgual("desde la 22 avanzar 5 da la vuelta y llega a la 3", 3, j.getCasillaActual().getId());
        revisar("dar la vuelta SI pasa por Inicio", paso);

        tablero.enviarACasilla(j, 20);
        paso = tablero.moverJugador(j, 4);
        revisarIgual("desde la 20 avanzar 4 cae justo en Inicio", 0, j.getCasillaActual().getId());
        revisar("caer justo en Inicio cuenta como pasar por Inicio", paso);

        tablero.enviarACasilla(j, 1);
        tablero.retrocederJugador(j, 3);
        revisarIgual("retroceder 3 desde la 1 llega a la 22", 22, j.getCasillaActual().getId());

        tablero.enviarACasilla(j, 20);
        paso = tablero.moverACasilla(j, 3);
        revisarIgual("moverACasilla(3) desde la 20 llega a la 3", 3, j.getCasillaActual().getId());
        revisar("ir de la 20 a la 3 pasa por Inicio", paso);

        paso = tablero.moverACasilla(j, 13);
        revisarIgual("moverACasilla(13) desde la 3 llega a la 13", 13, j.getCasillaActual().getId());
        revisar("ir de la 3 a la 13 no pasa por Inicio", !paso);

        paso = tablero.moverACasilla(j, 13);
        revisarIgual("ir a la casilla donde ya esta parado no lo mueve", 13, j.getCasillaActual().getId());
        revisar("y tampoco cobra premio", !paso);

        revisarLanza("avanzar 0 pasos se rechaza", IllegalArgumentException.class, () -> tablero.moverJugador(j, 0));
        revisarLanza("avanzar pasos negativos se rechaza", IllegalArgumentException.class,
                () -> tablero.moverJugador(j, -2));
        revisarLanza("retroceder 0 pasos se rechaza", IllegalArgumentException.class,
                () -> tablero.retrocederJugador(j, 0));
        revisarLanza("ir a una casilla que no existe se rechaza", IndexOutOfBoundsException.class,
                () -> tablero.moverACasilla(j, 99));
    }

    // CARTAS

    private static void pruebasCartas() {
        // RECIBIR_DINERO
        Escena e = new Escena();
        ResultadoEfecto r = CartaEvento.recibirDinero("Premio", 100).aplicar(e.j1, e.banco, e.tablero);
        revisarIgual("recibir dinero: el saldo sube 100", 1600, e.j1.getSaldo());
        revisarIgual("recibir dinero: genera una transaccion GANANCIA_POR_EVENTO", 1,
                e.transacciones(TipoTransaccion.GANANCIA_POR_EVENTO));
        revisar("recibir dinero: no mueve ni elimina", !r.seMovio() && !r.jugadorEliminado());

        // PAGAR_DINERO
        e = new Escena();
        CartaEvento.pagarDinero("Multa", 80).aplicar(e.j1, e.banco, e.tablero);
        revisarIgual("pagar dinero: el saldo baja 80", 1420, e.j1.getSaldo());
        revisarIgual("pagar dinero: genera una transaccion PERDIDA_POR_EVENTO", 1,
                e.transacciones(TipoTransaccion.PERDIDA_POR_EVENTO));

        // PAGAR_DINERO sin saldo: regla de eliminacion del punto 18
        e = new Escena();
        Jugador pobre = e.agregarPobre(50);
        r = CartaEvento.pagarDinero("Multa grande", 500).aplicar(pobre, e.banco, e.tablero);
        revisar("pagar sin saldo: el resultado marca eliminado", r.jugadorEliminado());
        revisar("pagar sin saldo: el jugador queda inactivo", !pobre.estaActivo());

        // AVANZAR sin pasar por inicio, cayendo en una propiedad libre
        e = new Escena();
        r = CartaEvento.avanzar("Atajo", 3).aplicar(e.j1, e.banco, e.tablero);
        revisarIgual("avanzar 3 desde Inicio deja en la casilla 3", 3, e.j1.getCasillaActual().getId());
        revisar("avanzar marca que se movio", r.seMovio());
        revisar("al caer en una propiedad libre se ofrece comprarla", r.hayPropiedadEnVenta());
        revisarIgual("la propiedad ofrecida es la de la casilla 3", 3, r.getPropiedadEnVenta().getId());
        revisarIgual("sin pasar por Inicio no hay premio", 0, e.transacciones(TipoTransaccion.PREMIO_POR_INICIO));
        revisarIgual("y el saldo no cambia", 1500, e.j1.getSaldo());

        // AVANZAR pasando por inicio
        e = new Escena();
        e.ubicar(e.j1, 22);
        r = CartaEvento.avanzar("Atajo", 3).aplicar(e.j1, e.banco, e.tablero);
        revisarIgual("desde la 22 avanzar 3 llega a la 1", 1, e.j1.getCasillaActual().getId());
        revisarIgual("pasar por Inicio con una carta suma el premio de 200", 1700, e.j1.getSaldo());
        revisarIgual("pasar por Inicio con una carta genera PREMIO_POR_INICIO", 1,
                e.transacciones(TipoTransaccion.PREMIO_POR_INICIO));
        revisar("el mensaje cuenta que cobro el premio", r.getMensaje().contains("cobra"));

        // RETROCEDER nunca cobra premio
        e = new Escena();
        e.ubicar(e.j1, 1);
        r = CartaEvento.retroceder("Derrumbe", 2).aplicar(e.j1, e.banco, e.tablero);
        revisarIgual("retroceder 2 desde la 1 llega a la 23", 23, e.j1.getCasillaActual().getId());
        revisarIgual("retroceder pasando por Inicio NO cobra premio", 0,
                e.transacciones(TipoTransaccion.PREMIO_POR_INICIO));
        revisarIgual("y el saldo no cambia", 1500, e.j1.getSaldo());
        revisar("cae en una propiedad libre y se ofrece", r.hayPropiedadEnVenta() && r.seMovio());

        // IR_A_CASILLA
        e = new Escena();
        r = CartaEvento.irACasilla("Viaje", 13).aplicar(e.j1, e.banco, e.tablero);
        revisarIgual("ir a la casilla 13 desde Inicio", 13, e.j1.getCasillaActual().getId());
        revisarIgual("sin pasar por Inicio no cobra premio", 0, e.transacciones(TipoTransaccion.PREMIO_POR_INICIO));

        e = new Escena();
        e.ubicar(e.j1, 5);
        r = CartaEvento.irACasilla("Regresa", 0).aplicar(e.j1, e.banco, e.tablero);
        revisarIgual("ir a Inicio desde la 5 llega a la 0", 0, e.j1.getCasillaActual().getId());
        revisarIgual("ir a Inicio cobra el premio", 1700, e.j1.getSaldo());

        // PERDER_TURNO
        e = new Escena();
        r = CartaEvento.perderTurno("Huelga").aplicar(e.j1, e.banco, e.tablero);
        revisar("perder turno: el jugador queda con un turno por perder", e.j1.debePerderTurno());
        revisar("perder turno: el resultado lo indica", r.pierdeTurno());
        revisarIgual("perder turno: no mueve dinero", 0, e.totalTransacciones());

        // Una carta de movimiento que cae en otra casilla de evento NO saca otra carta.
        e = new Escena();
        CasillaEvento sorpresa = (CasillaEvento) e.tablero.getCasilla(2);
        CartaEvento frente = sorpresa.verSiguienteCarta();
        r = CartaEvento.avanzar("Atajo", 2).aplicar(e.j1, e.banco, e.tablero);
        revisarIgual("avanzar 2 cae en la sorpresa de la casilla 2", 2, e.j1.getCasillaActual().getId());
        revisar("caer en otra sorpresa por una carta no saca otra carta", sorpresa.verSiguienteCarta() == frente);

        // Validaciones de las fabricas
        revisarLanza("recibirDinero con monto 0 se rechaza", IllegalArgumentException.class,
                () -> CartaEvento.recibirDinero("x", 0));
        revisarLanza("pagarDinero negativo se rechaza", IllegalArgumentException.class,
                () -> CartaEvento.pagarDinero("x", -5));
        revisarLanza("avanzar 0 casillas se rechaza", IllegalArgumentException.class,
                () -> CartaEvento.avanzar("x", 0));
        revisarLanza("retroceder negativo se rechaza", IllegalArgumentException.class,
                () -> CartaEvento.retroceder("x", -1));
        revisarLanza("irACasilla con id negativo se rechaza", IllegalArgumentException.class,
                () -> CartaEvento.irACasilla("x", -1));
        revisarLanza("una carta sin descripcion se rechaza", IllegalArgumentException.class,
                () -> CartaEvento.perderTurno("  "));

        Escena sinNada = new Escena();
        revisarLanza("aplicar sin banco se rechaza", IllegalArgumentException.class,
                () -> CartaEvento.perderTurno("x").aplicar(sinNada.j1, null, sinNada.tablero));
    }

    // MAZO CIRCULAR

    private static void pruebasMazoCircular() {
        ColaCircular<CartaEvento> mazo = new ColaCircular<>();
        mazo.encolar(CartaEvento.recibirDinero("A", 10));
        mazo.encolar(CartaEvento.recibirDinero("B", 20));
        mazo.encolar(CartaEvento.recibirDinero("C", 30));
        CasillaEvento casilla = new CasillaEvento(2, "Sorpresa", mazo);

        Escena e = new Escena();
        int[] salieron = new int[4];
        for (int i = 0; i < 4; i++) {
            salieron[i] = casilla.verSiguienteCarta().getValor();
            casilla.ejecutarEfecto(e.j1, e.banco, e.tablero);
        }
        revisarIgual("1a carta en salir: A (10)", 10, salieron[0]);
        revisarIgual("2a carta en salir: B (20)", 20, salieron[1]);
        revisarIgual("3a carta en salir: C (30)", 30, salieron[2]);
        revisarIgual("4a carta: A de nuevo, porque la usada paso al final", 10, salieron[3]);
        revisarIgual("el mazo nunca pierde cartas", 3, casilla.getCantidadCartas());
        revisarIgual("las cuatro cartas dieron 10+20+30+10 de saldo", 1500 + 70, e.j1.getSaldo());

        revisarLanza("una casilla de evento sin mazo se rechaza", IllegalArgumentException.class,
                () -> new CasillaEvento(2, "Sorpresa", null));
        revisarLanza("una casilla de evento con mazo vacio se rechaza", IllegalArgumentException.class,
                () -> new CasillaEvento(2, "Sorpresa", new ColaCircular<CartaEvento>()));

        ColaCircular<CartaEvento> porDefecto = CartaEvento.crearMazoPorDefecto();
        boolean[] tipos = new boolean[CartaEvento.Tipo.values().length];
        porDefecto.recorrer(c -> tipos[c.getTipo().ordinal()] = true);
        boolean todos = true;
        for (boolean visto : tipos) {
            if (!visto) {
                todos = false;
            }
        }
        revisar("el mazo por defecto incluye los seis tipos de carta del enunciado", todos);
    }

    // PROPIEDAD

    private static void pruebasPropiedad() {
        // Disponible: se ofrece, no se compra sola.
        Escena e = new Escena();
        Propiedad cartago = (Propiedad) e.tablero.getCasilla(1);
        e.ubicar(e.j1, 1);
        ResultadoEfecto r = cartago.ejecutarEfecto(e.j1, e.banco, e.tablero);
        revisar("propiedad libre: se ofrece comprarla", r.hayPropiedadEnVenta() && r.getPropiedadEnVenta() == cartago);
        revisar("propiedad libre: sigue sin duenio (no se compra sola)", cartago.estaDisponible());
        revisarIgual("propiedad libre: no genera transacciones", 0, e.totalTransacciones());
        revisarIgual("propiedad libre: el saldo no cambia", 1500, e.j1.getSaldo());

        // El jugador acepta: lo hace el Banco, que valida el turno.
        e.banco.comprarPropiedad(e.j1, r.getPropiedadEnVenta());
        revisar("tras COMPRAR_PROPIEDAD el jugador es el duenio", cartago.esDe(e.j1));
        revisarIgual("la compra genera una transaccion COMPRA_PROPIEDAD", 1,
                e.transacciones(TipoTransaccion.COMPRA_PROPIEDAD));
        revisarIgual("la compra cobra el precio (60)", 1440, e.j1.getSaldo());

        // Propia: no se paga nada.
        int antes = e.totalTransacciones();
        r = cartago.ejecutarEfecto(e.j1, e.banco, e.tablero);
        revisar("propiedad propia: no se ofrece ni se cobra", !r.hayPropiedadEnVenta() && !r.jugadorEliminado());
        revisarIgual("propiedad propia: no genera transacciones", antes, e.totalTransacciones());
        revisarIgual("propiedad propia: el saldo no cambia", 1440, e.j1.getSaldo());

        // De otro: paga alquiler.
        e.ubicar(e.j2, 1);
        r = cartago.ejecutarEfecto(e.j2, e.banco, e.tablero);
        revisarIgual("propiedad ajena: el inquilino paga 6 de alquiler", 1494, e.j2.getSaldo());
        revisarIgual("propiedad ajena: el duenio cobra 6", 1446, e.j1.getSaldo());
        revisarIgual("propiedad ajena: genera una transaccion PAGO_ALQUILER", 1,
                e.transacciones(TipoTransaccion.PAGO_ALQUILER));
        revisar("propiedad ajena: el que paga sigue en la partida", !r.jugadorEliminado() && e.j2.estaActivo());
        revisar("propiedad ajena: no se ofrece comprarla", !r.hayPropiedadEnVenta());

        // De otro y sin saldo: eliminacion.
        e = new Escena();
        Propiedad poas = (Propiedad) e.tablero.getCasilla(23);
        e.j1.comprarPropiedad(poas);
        Jugador pobre = e.agregarPobre(10);
        e.ubicar(pobre, 23);
        r = poas.ejecutarEfecto(pobre, e.banco, e.tablero);
        revisar("alquiler sin saldo: el resultado marca eliminado", r.jugadorEliminado());
        revisar("alquiler sin saldo: el jugador queda inactivo", !pobre.estaActivo());
        revisar("alquiler sin saldo: el duenio no pierde su propiedad", poas.esDe(e.j1));
    }

    // CASILLA ESPECIAL

    private static void pruebasCasillaEspecial() {
        // Impuesto
        Escena e = new Escena();
        e.ubicar(e.j1, 4);
        ResultadoEfecto r = e.tablero.getCasilla(4).ejecutarEfecto(e.j1, e.banco, e.tablero);
        revisarIgual("impuesto de ventas: cobra 100", 1400, e.j1.getSaldo());
        revisarIgual("impuesto: genera una transaccion PAGO_AL_BANCO", 1, e.transacciones(TipoTransaccion.PAGO_AL_BANCO));
        revisar("impuesto: el jugador sigue en la partida", !r.jugadorEliminado());

        e = new Escena();
        Jugador pobre = e.agregarPobre(40);
        e.ubicar(pobre, 4);
        r = e.tablero.getCasilla(4).ejecutarEfecto(pobre, e.banco, e.tablero);
        revisar("impuesto sin saldo: eliminado", r.jugadorEliminado() && !pobre.estaActivo());

        // Ir a la carcel
        e = new Escena();
        e.ubicar(e.j1, 17);
        e.tablero.moverJugador(e.j1, 1); // cae en la 18, Ir al reten
        r = e.j1.getCasillaActual().ejecutarEfecto(e.j1, e.banco, e.tablero);
        revisarIgual("ir al reten: la ficha queda en la casilla 14", 14, e.j1.getCasillaActual().getId());
        revisar("ir al reten: se movio y pierde turno", r.seMovio() && r.pierdeTurno());
        revisar("ir al reten: el jugador queda con un turno por perder", e.j1.debePerderTurno());
        revisarIgual("ir al reten no cobra premio de inicio", 0, e.transacciones(TipoTransaccion.PREMIO_POR_INICIO));

        // Especiales sin efecto economico
        for (int id : new int[] { 0, 9, 14 }) {
            Escena s = new Escena();
            s.ubicar(s.j1, id);
            ResultadoEfecto res = s.tablero.getCasilla(id).ejecutarEfecto(s.j1, s.banco, s.tablero);
            revisar(s.tablero.getCasilla(id).getNombre() + ": solo informa, sin dinero ni movimiento",
                    s.totalTransacciones() == 0 && s.j1.getSaldo() == 1500 && !res.seMovio()
                            && !res.pierdeTurno() && !res.getMensaje().isEmpty());
        }

        // Tablero sin carcel: no se cae el juego.
        ListaCircularDoble<Casilla> sinCarcel = new ListaCircularDoble<>();
        sinCarcel.agregar(new CasillaEspecial(0, "Inicio", CasillaEspecial.Tipo.INICIO));
        sinCarcel.agregar(new CasillaEspecial(1, "Ir al reten", CasillaEspecial.Tipo.IR_A_CARCEL));
        for (int i = 2; i < 24; i++) {
            sinCarcel.agregar(new CasillaEspecial(i, "D" + i, CasillaEspecial.Tipo.DESCANSO));
        }
        Tablero raro = new Tablero(sinCarcel);
        Banco banco = new Banco(100, 200);
        Jugador j = new Jugador("J1", "Ana", 1500);
        banco.agregarJugador(j);
        raro.colocarEnInicio(j);
        raro.moverJugador(j, 1);
        ResultadoEfecto sin = j.getCasillaActual().ejecutarEfecto(j, banco, raro);
        revisar("ir al reten sin carcel en el tablero: no falla ni mueve", !sin.seMovio() && !j.debePerderTurno());

        // Validaciones del constructor
        revisarLanza("un impuesto de monto 0 se rechaza", IllegalArgumentException.class,
                () -> new CasillaEspecial(4, "Impuesto", CasillaEspecial.Tipo.IMPUESTO, 0));
        revisarLanza("una casilla especial sin tipo se rechaza", IllegalArgumentException.class,
                () -> new CasillaEspecial(4, "Rara", null));
    }

    // POLIMORFISMO

    private static void pruebasPolimorfismo() {
        Escena e = new Escena();
        Jugador rico = new Jugador("R", "Rico", 1000000);
        e.banco.agregarJugador(rico);
        e.tablero.colocarEnInicio(rico);

        boolean todasResponden = true;
        for (int id = 0; id < e.tablero.getCantidadCasillas(); id++) {
            // Se llama por la clase base: Java elige solo la version correcta.
            Casilla casilla = e.tablero.getCasilla(id);
            e.ubicar(rico, id);
            try {
                ResultadoEfecto r = casilla.ejecutarEfecto(rico, e.banco, e.tablero);
                if (r == null || r.getMensaje().isEmpty()) {
                    todasResponden = false;
                }
            } catch (RuntimeException ex) {
                todasResponden = false;
                System.out.println("  FALLO casilla " + id + " lanzo " + ex);
            }
        }
        revisar("las 24 casillas ejecutan su efecto desde la clase base Casilla", todasResponden);

        // Compatibilidad: la firma original de Casilla sigue funcionando.
        Casilla propiedad = e.tablero.getCasilla(1);
        propiedad.ejecutarEfecto(rico);
        revisar("la firma original ejecutarEfecto(Jugador) sigue disponible", true);
    }

    // TURNOS PERDIDOS

    private static void pruebasTurnosPerdidos() {
        Jugador j = new Jugador("J1", "Ana", 1500);
        revisar("un jugador nuevo no debe perder turnos", !j.debePerderTurno());

        j.perderProximoTurno();
        j.perderProximoTurno();
        revisarIgual("dos penalizaciones se acumulan", 2, j.getTurnosPorPerder());

        j.consumirTurnoPerdido();
        revisar("tras consumir uno aun debe perder otro", j.debePerderTurno());
        j.consumirTurnoPerdido();
        revisar("tras consumir los dos ya no debe perder turnos", !j.debePerderTurno());

        revisarLanza("consumir sin turnos pendientes se rechaza", IllegalStateException.class,
                j::consumirTurnoPerdido);
    }

    // SIMULACION

    private static void pruebasSimulacion() {
        boolean todasBien = true;
        int turnosTotales = 0;

        for (long semilla = 1; semilla <= 20; semilla++) {
            String problema = simularPartida(semilla);
            if (problema != null) {
                todasBien = false;
                System.out.println("  FALLO semilla " + semilla + ": " + problema);
            }
        }
        revisar("20 partidas completas terminan sin errores y con estado consistente", todasBien);
    }

    /**
     * Juega una partida completa de 4 jugadores repitiendo lo que hara el
     * Juego/Servidor en cada turno:
     *   1. registrar el lanzamiento (valida turno y que no tire dos veces)
     *   2. tirar los dados y mover la ficha; pagar premio si paso por Inicio
     *   3. ejecutar el efecto de la casilla donde cayo
     *   4. si se ofrecio una propiedad y alcanza, comprarla
     *   5. terminar el turno y saltar a quien deba perder el suyo
     *
     * Devuelve null si todo salio bien, o la descripcion del problema.
     */
    private static String simularPartida(long semilla) {
        Banco banco = new Banco(200, 200);
        Tablero tablero = Tablero.crearPorDefecto();
        Dado dado = new Dado(semilla);

        Jugador[] jugadores = new Jugador[4];
        for (int i = 0; i < 4; i++) {
            jugadores[i] = new Jugador("J" + (i + 1), "Jugador" + (i + 1), Banco.SALDO_INICIAL_POR_DEFECTO);
            banco.agregarJugador(jugadores[i]);
            tablero.colocarEnInicio(jugadores[i]);
        }

        int guardia = 0;
        try {
            while (!banco.partidaTerminada() && guardia++ < 5000) {
                Jugador j = banco.jugadorActual();

                banco.registrarLanzamientoDados(j);
                int pasos = dado.tirar();
                boolean paso = tablero.moverJugador(j, pasos);
                if (paso) {
                    banco.pagarPremioPorInicio(j);
                }

                ResultadoEfecto r = j.getCasillaActual().ejecutarEfecto(j, banco, tablero);

                if (r.hayPropiedadEnVenta() && j.estaActivo() && j.puedePagar(r.getPropiedadEnVenta().getPrecio())) {
                    banco.comprarPropiedad(j, r.getPropiedadEnVenta());
                }

                banco.terminarTurno(j);

                while (!banco.partidaTerminada() && banco.jugadorActual().debePerderTurno()) {
                    Jugador castigado = banco.jugadorActual();
                    castigado.consumirTurnoPerdido();
                    banco.terminarTurno(castigado);
                }
            }
        } catch (RuntimeException ex) {
            return "excepcion inesperada: " + ex;
        }

        if (guardia >= 5000) {
            return "la partida no termino (posible ciclo infinito)";
        }
        if (!banco.partidaTerminada()) {
            return "el bucle salio pero la partida no esta terminada";
        }
        if (banco.getGanador() == null) {
            return "la partida termino sin ganador";
        }

        // Consistencia: cada propiedad con duenio esta en la lista de ese duenio,
        // y los jugadores eliminados no conservan propiedades.
        String[] error = { null };
        tablero.recorrer(c -> {
            if (c instanceof Propiedad) {
                Propiedad p = (Propiedad) c;
                if (p.getPropietario() != null && !p.getPropietario().getPropiedades().contiene(p)) {
                    error[0] = p.getNombre() + " tiene duenio pero no esta en su lista";
                }
            }
        });
        if (error[0] != null) {
            return error[0];
        }
        for (Jugador j : jugadores) {
            if (!j.estaActivo() && j.getCantidadPropiedades() != 0) {
                return j.getNombre() + " esta eliminado pero conserva propiedades";
            }
            if (j.getSaldo() < 0) {
                return j.getNombre() + " quedo con saldo negativo";
            }
        }
        return null;
    }

    // AUXILIARES

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("--- " + texto + " ---");
    }

    private static void revisar(String descripcion, boolean condicion) {
        totales++;
        if (condicion) {
            System.out.println("  OK    " + descripcion);
        } else {
            fallos++;
            System.out.println("  FALLO " + descripcion);
        }
    }

    private static void revisarIgual(String descripcion, int esperado, int obtenido) {
        totales++;
        if (esperado == obtenido) {
            System.out.println("  OK    " + descripcion);
        } else {
            fallos++;
            System.out.println("  FALLO " + descripcion + " -> esperado " + esperado + " pero fue " + obtenido);
        }
    }

    /** Comprueba que la accion lance exactamente el tipo de excepcion indicado. */
    private static void revisarLanza(String descripcion, Class<? extends RuntimeException> tipo, Runnable accion) {
        totales++;
        try {
            accion.run();
            fallos++;
            System.out.println("  FALLO " + descripcion + " -> no lanzo excepcion");
        } catch (RuntimeException e) {
            if (tipo.isInstance(e)) {
                System.out.println("  OK    " + descripcion + " -> " + e.getMessage());
            } else {
                fallos++;
                System.out.println("  FALLO " + descripcion + " -> lanzo " + e.getClass().getSimpleName()
                        + " en vez de " + tipo.getSimpleName());
            }
        }
    }

    private static void resumen() {
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("Pruebas: " + totales + "    Fallos: " + fallos);
        System.out.println(fallos == 0 ? "TODO CORRECTO" : "HAY PRUEBAS FALLANDO");
        System.out.println("=".repeat(70));

        if (fallos > 0) {
            System.exit(1);
        }
    }
}
