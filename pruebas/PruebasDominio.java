package pruebas;

import dominio.Banco;
import dominio.Casilla;
import dominio.HistorialTransacciones;
import dominio.Jugador;
import dominio.OperacionInvalidaException;
import dominio.Propiedad;
import dominio.TipoTransaccion;
import dominio.Transaccion;

import estructuras.ColaCircular;
import estructuras.ListaCircularDoble;
import estructuras.ListaDoble;
import estructuras.NodoDoble;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Pruebas automaticas de las clases de dominio: Jugador, Propiedad, Transaccion,
 * HistorialTransacciones y Banco.
 *
 * Sin librerias externas, igual que las pruebas de las estructuras: una clase
 * con main que imprime OK o FALLO por cada comprobacion y termina con un
 * resumen. Si algo falla, el programa sale con codigo 1, asi el error no pasa
 * desapercibido entre tanta linea de consola.
 *
 * Cada bloque de pruebas dice a que punto del enunciado corresponde, para poder
 * ubicarlo rapido durante la defensa del proyecto.
 */
public class PruebasDominio {

    private static final String RUTA_REPORTE = "reporte_transacciones.txt";

    private static int totales = 0;
    private static int fallos = 0;

    public static void main(String[] args) {
        titulo("JUGADOR: saldo, propiedades y patrimonio (punto 4)");
        pruebasJugador();

        titulo("IDENTIDAD: equals y hashCode por id");
        pruebasIdentidad();

        titulo("TURNOS: cola circular y validaciones (puntos 8 y 17)");
        pruebasTurnos();

        titulo("COMPRA DE PROPIEDAD (puntos 7 y 17)");
        pruebasCompra();

        titulo("ALQUILER: las tres reglas del punto 7");
        pruebasAlquiler();

        titulo("BANCARROTA: regla de eliminacion (punto 18)");
        pruebasBancarrota();

        titulo("HISTORIAL: recorridos y busquedas (punto 12)");
        pruebasHistorial();

        titulo("REPORTE TXT (punto 13)");
        pruebasReporte();

        titulo("FIN DE PARTIDA (punto 18)");
        pruebasFinDePartida();

        titulo("TURNOS DESPUES DE UNA ELIMINACION (puntos 8, 17 y 18)");
        pruebasTurnosTrasEliminacion();

        titulo("IDS DE TRANSACCION (punto 11)");
        pruebasIdsDeTransaccion();

        titulo("MOVIMIENTO: pasos invalidos (punto 9)");
        pruebasPasosInvalidos();

        resumen();
    }

    //  JUGADOR

    private static void pruebasJugador() {
        Jugador ana = new Jugador("J1", "Ana", 1000);

        revisarIgual("saldo inicial", 1000, ana.getSaldo());
        revisar("arranca activo", ana.estaActivo());
        revisarIgual("arranca sin propiedades", 0, ana.getCantidadPropiedades());
        revisarIgual("patrimonio sin propiedades es el saldo", 1000, ana.calcularPatrimonio());

        ana.acreditar(200);
        revisarIgual("acreditar suma al saldo", 1200, ana.getSaldo());

        ana.debitar(500);
        revisarIgual("debitar resta del saldo", 700, ana.getSaldo());

        Propiedad lab = new Propiedad(1, "Laboratorio", 200, 50);
        ana.comprarPropiedad(lab);
        revisarIgual("comprar descuenta el precio", 500, ana.getSaldo());
        revisarIgual("comprar agrega a la lista de propiedades", 1, ana.getCantidadPropiedades());
        revisar("comprar deja la propiedad a nombre del jugador", lab.esDe(ana));
        revisarIgual("patrimonio = saldo + valor de propiedades", 700, ana.calcularPatrimonio());

        Propiedad biblio = new Propiedad(3, "Biblioteca", 300, 75);
        ana.comprarPropiedad(biblio);
        revisarIgual("patrimonio con dos propiedades", 700, ana.calcularPatrimonio());

        ana.perderPropiedad(lab);
        revisarIgual("perder una propiedad la saca de la lista", 1, ana.getCantidadPropiedades());
        revisar("la propiedad perdida queda disponible", lab.estaDisponible());

        revisarLanza("debitar mas de lo que se tiene",
                () -> new Jugador("J9", "Prueba", 100).debitar(500));

        // La posicion es un nodo del tablero, no un numero.
        ListaCircularDoble<Casilla> tablero = tableroDePrueba();
        ana.moverA(tablero.getInicio());
        revisar("la posicion apunta a un nodo del tablero",
                "Salida".equals(ana.getCasillaActual().getNombre()));

        NodoDoble<Casilla> destino = tablero.avanzar(ana.getPosicionActual(), 3);
        ana.moverA(destino);
        revisar("avanzar 3 desde Salida cae en Biblioteca",
                "Biblioteca".equals(ana.getCasillaActual().getNombre()));
    }

    //  IDENTIDAD

    /**
     * Estas pruebas son las que justifican haber escrito equals() y hashCode().
     * Sin ellos, las estructuras comparan direcciones de memoria y no encuentran
     * nada cuando la referencia viene de otro lado del juego.
     */
    private static void pruebasIdentidad() {
        Jugador original = new Jugador("J1", "Ana", 1000);
        Jugador mismaIdentidad = new Jugador("J1", "Ana desde otro lado", 50);
        Jugador otro = new Jugador("J2", "Bruno", 1000);

        revisar("dos Jugador con el mismo id son equals", original.equals(mismaIdentidad));
        revisar("si son equals, el hashCode coincide", original.hashCode() == mismaIdentidad.hashCode());
        revisar("dos ids distintos no son equals", !original.equals(otro));

        ColaCircular<Jugador> cola = new ColaCircular<>();
        cola.encolar(original);
        cola.encolar(otro);
        revisar("ColaCircular.eliminar busca por id, no por referencia", cola.eliminar(mismaIdentidad));
        revisarIgual("la cola de turnos quedo con un jugador", 1, cola.getCantidad());

        Propiedad gimnasio = new Propiedad(5, "Gimnasio", 100, 25);
        Propiedad gimnasioCopia = new Propiedad(5, "Gimnasio", 100, 25);
        revisar("dos Propiedad con el mismo id son equals", gimnasio.equals(gimnasioCopia));

        ListaDoble<Propiedad> propiedades = new ListaDoble<>();
        propiedades.agregarAlFinal(gimnasio);
        revisar("ListaDoble.eliminar busca por id, no por referencia",
                propiedades.eliminar(gimnasioCopia));

        Transaccion tx = new Transaccion(1, 1, TipoTransaccion.PAGO_AL_BANCO,
                "J1", Transaccion.BANCO, 50, "prueba");
        Transaccion txMismoId = new Transaccion(1, 5, TipoTransaccion.PAGO_ALQUILER,
                "J2", "J3", 900, "otra");
        revisar("dos Transaccion con el mismo id son equals", tx.equals(txMismoId));
    }

    //  TURNOS

    private static void pruebasTurnos() {
        Banco banco = new Banco();
        Jugador ana = new Jugador("J1", "Ana", 1000);
        Jugador bruno = new Jugador("J2", "Bruno", 1000);
        Jugador carla = new Jugador("J3", "Carla", 1000);

        banco.agregarJugador(ana);
        banco.agregarJugador(bruno);
        banco.agregarJugador(carla);

        revisarIgual("tres jugadores en la partida", 3, banco.jugadoresActivos());
        revisar("el primero en entrar juega primero", banco.jugadorActual().equals(ana));
        revisarIgual("la partida arranca en el turno 1", 1, banco.getNumeroTurno());

        revisarLanza("no se puede agregar dos veces al mismo jugador",
                () -> banco.agregarJugador(ana));
        revisarLanza("jugar fuera de turno",
                () -> banco.registrarLanzamientoDados(bruno));

        banco.registrarLanzamientoDados(ana);
        revisar("queda anotado que ya lanzo los dados", banco.yaLanzoDados());
        revisarLanza("lanzar los dados dos veces en el mismo turno",
                () -> banco.registrarLanzamientoDados(ana));
        revisarLanza("terminar el turno de otro jugador",
                () -> banco.terminarTurno(carla));

        banco.terminarTurno(ana);
        revisar("el turno pasa al siguiente de la cola", banco.jugadorActual().equals(bruno));
        revisarIgual("el contador de turnos avanza", 2, banco.getNumeroTurno());
        revisar("los dados se reinician en el turno nuevo", !banco.yaLanzoDados());

        banco.terminarTurno(bruno);
        banco.terminarTurno(carla);
        revisar("la cola da la vuelta y vuelve al primero", banco.jugadorActual().equals(ana));
    }

    //  COMPRA DE PROPIEDAD

    private static void pruebasCompra() {
        Banco banco = new Banco();
        Jugador ana = new Jugador("J1", "Ana", 500);
        Jugador bruno = new Jugador("J2", "Bruno", 100);
        banco.agregarJugador(ana);
        banco.agregarJugador(bruno);

        Propiedad lab = new Propiedad(1, "Laboratorio", 200, 50);
        Propiedad torre = new Propiedad(2, "Torre de computo", 400, 100);

        banco.comprarPropiedad(ana, lab);
        revisarIgual("la compra descuenta el precio", 300, ana.getSaldo());
        revisar("la propiedad queda a nombre del comprador", lab.esDe(ana));
        revisarIgual("la compra genera una transaccion", 1, banco.getHistorial().getCantidad());

        Transaccion tx = banco.getHistorial().getMasReciente();
        revisar("la transaccion es de tipo COMPRA_PROPIEDAD",
                tx.getTipo() == TipoTransaccion.COMPRA_PROPIEDAD);
        revisar("el origen es el comprador", "J1".equals(tx.getOrigen()));
        revisar("el destino es el banco", Transaccion.BANCO.equals(tx.getDestino()));
        revisarIgual("el monto es el precio de la propiedad", 200, tx.getMonto());
        revisarIgual("la transaccion guarda el turno", 1, tx.getTurno());

        revisarLanza("comprar fuera de turno",
                () -> banco.comprarPropiedad(bruno, torre));
        revisarLanza("comprar una propiedad que ya tiene duenio",
                () -> banco.comprarPropiedad(ana, lab));
        revisarLanza("comprar sin saldo suficiente",
                () -> banco.comprarPropiedad(ana, torre));

        // La compra es un cobro VOLUNTARIO: si se rechaza no pasa nada mas.
        revisar("el rechazo no elimina al jugador", ana.estaActivo());
        revisarIgual("el rechazo no genera transaccion", 1, banco.getHistorial().getCantidad());
        revisarIgual("el rechazo no toca el saldo", 300, ana.getSaldo());
    }

    //  ALQUILER

    private static void pruebasAlquiler() {
        Banco banco = new Banco();
        Jugador ana = new Jugador("J1", "Ana", 1000);
        Jugador bruno = new Jugador("J2", "Bruno", 1000);
        banco.agregarJugador(ana);
        banco.agregarJugador(bruno);

        Propiedad libre = new Propiedad(1, "Soda", 150, 40);
        Propiedad deAna = new Propiedad(2, "Laboratorio", 200, 50);

        banco.comprarPropiedad(ana, deAna);
        int antes = banco.getHistorial().getCantidad();

        banco.pagarAlquiler(bruno, libre);
        revisarIgual("caer en una propiedad sin duenio no cobra nada", 1000, bruno.getSaldo());
        revisarIgual("y no genera transaccion", antes, banco.getHistorial().getCantidad());

        banco.pagarAlquiler(ana, deAna);
        revisarIgual("caer en la propiedad propia no cobra nada", 800, ana.getSaldo());
        revisarIgual("y tampoco genera transaccion", antes, banco.getHistorial().getCantidad());

        boolean sigue = banco.pagarAlquiler(bruno, deAna);
        revisar("el inquilino sobrevive al pago", sigue);
        revisarIgual("el inquilino paga el alquiler", 950, bruno.getSaldo());
        revisarIgual("el duenio lo cobra", 850, ana.getSaldo());
        revisarIgual("el alquiler genera una transaccion", antes + 1, banco.getHistorial().getCantidad());

        Transaccion tx = banco.getHistorial().getMasReciente();
        revisar("la transaccion es PAGO_ALQUILER", tx.getTipo() == TipoTransaccion.PAGO_ALQUILER);
        revisar("va de inquilino a duenio",
                "J2".equals(tx.getOrigen()) && "J1".equals(tx.getDestino()));

        // Propiedad acepta alquiler 0; caer en una ajena no puede tumbar el juego.
        Propiedad sinAlquiler = new Propiedad(3, "Jardin", 100, 0);
        banco.comprarPropiedad(ana, sinAlquiler);
        int antesSinAlquiler = banco.getHistorial().getCantidad();
        revisar("caer en una propiedad ajena con alquiler 0 no falla",
                banco.pagarAlquiler(bruno, sinAlquiler));
        revisarIgual("y no cobra nada", 950, bruno.getSaldo());
        revisarIgual("ni genera transaccion", antesSinAlquiler, banco.getHistorial().getCantidad());
    }

    //  BANCARROTA

    private static void pruebasBancarrota() {
        Banco banco = new Banco();
        Jugador ana = new Jugador("J1", "Ana", 1000);
        Jugador bruno = new Jugador("J2", "Bruno", 200);
        Jugador carla = new Jugador("J3", "Carla", 1000);
        banco.agregarJugador(ana);
        banco.agregarJugador(bruno);
        banco.agregarJugador(carla);

        Propiedad deAna = new Propiedad(1, "Laboratorio", 200, 500);
        Propiedad deBruno = new Propiedad(2, "Soda", 150, 40);

        banco.comprarPropiedad(ana, deAna);
        banco.terminarTurno(ana);
        banco.comprarPropiedad(bruno, deBruno);
        revisarIgual("Bruno queda con 50", 50, bruno.getSaldo());
        revisarIgual("y con una propiedad", 1, bruno.getCantidadPropiedades());

        // Bruno cae en la propiedad de Ana: alquiler 500 y solo tiene 50.
        boolean sigue = banco.pagarAlquiler(bruno, deAna);

        revisar("un pago obligatorio sin saldo no se rechaza, quiebra al jugador", !sigue);
        revisar("el jugador queda inactivo", !bruno.estaActivo());
        revisarIgual("el jugador queda en cero", 0, bruno.getSaldo());
        revisarIgual("el acreedor recibe el pago parcial", 850, ana.getSaldo());
        revisarIgual("las propiedades pasan al acreedor", 2, ana.getCantidadPropiedades());
        revisarIgual("el deudor se queda sin propiedades", 0, bruno.getCantidadPropiedades());
        revisar("la propiedad transferida cambio de duenio", deBruno.esDe(ana));
        revisarIgual("quedan dos jugadores en la cola de turnos", 2, banco.jugadoresActivos());
        revisar("la bancarrota queda registrada en el historial",
                banco.getHistorial().getMasReciente().getDescripcion().contains("BANCARROTA"));

        // Caso borde: quebro en su propio turno, asi que el frente de la cola
        // ya avanzo solo y terminarTurno() no debe rotar de nuevo.
        revisar("al eliminarlo el turno pasa al siguiente", banco.jugadorActual().equals(carla));
        banco.terminarTurno(bruno);
        revisar("cerrar el turno del eliminado no saltea a Carla",
                banco.jugadorActual().equals(carla));
        banco.terminarTurno(carla);
        revisar("despues de Carla le toca a Ana", banco.jugadorActual().equals(ana));

        // Quebrar contra el banco deja las propiedades libres, no las hereda nadie.
        Banco otroBanco = new Banco();
        Jugador dario = new Jugador("J4", "Dario", 100);
        Jugador elena = new Jugador("J5", "Elena", 1000);
        otroBanco.agregarJugador(dario);
        otroBanco.agregarJugador(elena);

        Propiedad deDario = new Propiedad(9, "Cafeteria", 80, 20);
        otroBanco.comprarPropiedad(dario, deDario);

        boolean sigueDario = otroBanco.pagarAlBanco(dario, 500, "Impuesto de matricula");
        revisar("quiebra pagandole al banco", !sigueDario);
        revisar("sus propiedades quedan disponibles otra vez", deDario.estaDisponible());
        revisarIgual("queda un solo jugador", 1, otroBanco.jugadoresActivos());

        // Un eliminado ya no participa. Reingresarlo trabaria la cola de
        // turnos, porque terminarTurno() no rota con jugadores inactivos.
        int transaccionesAntes = otroBanco.getHistorial().getCantidad();
        revisarLanza("un eliminado no puede volver a la partida",
                () -> otroBanco.agregarJugador(dario));
        revisarLanza("no se le puede pagar a un eliminado",
                () -> otroBanco.pagarAJugador(elena, dario, 100, "Regalo"));
        revisarLanza("no se le puede cobrar a un eliminado",
                () -> otroBanco.pagarAlBanco(dario, 10, "Multa"));
        revisarLanza("un eliminado no cobra premio por inicio",
                () -> otroBanco.pagarPremioPorInicio(dario));
        revisarIgual("el dinero de Elena no se toca", 1000, elena.getSaldo());
        revisarIgual("y ninguno de los rechazos queda en el historial",
                transaccionesAntes, otroBanco.getHistorial().getCantidad());
    }

    //  HISTORIAL

    private static void pruebasHistorial() {
        HistorialTransacciones historial = new HistorialTransacciones();
        revisar("el historial arranca vacio", historial.estaVacio());

        historial.agregar(new Transaccion(1, 1, TipoTransaccion.COMPRA_PROPIEDAD,
                "J1", Transaccion.BANCO, 200, "Ana compro Laboratorio"));
        historial.agregar(new Transaccion(2, 1, TipoTransaccion.PAGO_ALQUILER,
                "J2", "J1", 50, "Bruno paga alquiler a Ana"));
        historial.agregar(new Transaccion(3, 2, TipoTransaccion.PAGO_ALQUILER,
                "J3", "J1", 50, "Carla paga alquiler a Ana"));
        historial.agregar(new Transaccion(4, 2, TipoTransaccion.PREMIO_POR_INICIO,
                Transaccion.BANCO, "J2", 200, "Bruno paso por inicio"));

        revisarIgual("guarda las cuatro transacciones", 4, historial.getCantidad());

        StringBuilder antiguas = new StringBuilder();
        historial.recorrerDesdeLaMasAntigua(t -> antiguas.append(t.getId()));
        revisar("recorrer desde la mas antigua da 1234", "1234".equals(antiguas.toString()));

        StringBuilder recientes = new StringBuilder();
        historial.recorrerDesdeLaMasReciente(t -> recientes.append(t.getId()));
        revisar("recorrer desde la mas reciente da 4321", "4321".equals(recientes.toString()));

        ListaDoble<Transaccion> deAna = historial.buscarPorJugador("J1");
        revisarIgual("buscar por jugador encuentra origen y destino", 3, deAna.getCantidad());

        ListaDoble<Transaccion> alquileres = historial.buscarPorTipo(TipoTransaccion.PAGO_ALQUILER);
        revisarIgual("buscar por tipo", 2, alquileres.getCantidad());

        revisarIgual("buscar no modifica el historial original", 4, historial.getCantidad());
        revisar("buscar por numero de transaccion",
                historial.buscarPorId(3).getDescripcion().contains("Carla"));
        revisar("la mas reciente es la ultima agregada", historial.getMasReciente().getId() == 4);
    }

    //  REPORTE TXT

    private static void pruebasReporte() {
        Banco banco = new Banco();
        Jugador ana = new Jugador("J1", "Ana", 1000);
        Jugador bruno = new Jugador("J2", "Bruno", 1000);
        banco.agregarJugador(ana);
        banco.agregarJugador(bruno);

        Propiedad lab = new Propiedad(1, "Laboratorio", 200, 50);
        banco.comprarPropiedad(ana, lab);
        banco.pagarPremioPorInicio(ana);
        banco.terminarTurno(ana);
        banco.pagarAlquiler(bruno, lab);

        banco.exportarTransacciones(RUTA_REPORTE);

        File archivo = new File(RUTA_REPORTE);
        revisar("el archivo TXT se creo", archivo.exists() && archivo.length() > 0);

        // Se lee el archivo de vuelta, no el texto en memoria: lo que hay que
        // comprobar es lo que quedo escrito en el disco.
        String contenido = leerArchivo(archivo);

        revisar("el reporte trae todas las columnas del punto 13",
                contenido.contains("NUM") && contenido.contains("TURNO")
                        && contenido.contains("TIPO") && contenido.contains("ORIGEN")
                        && contenido.contains("DESTINO") && contenido.contains("MONTO")
                        && contenido.contains("DESCRIPCION"));
        revisar("el reporte trae los tres movimientos",
                contenido.contains("COMPRA_PROPIEDAD")
                        && contenido.contains("PREMIO_POR_INICIO")
                        && contenido.contains("PAGO_ALQUILER"));
        revisar("el reporte cierra con el total", contenido.contains("Total de transacciones: 3"));

        System.out.println();
        System.out.println(contenido.trim());
    }

    //  FIN DE PARTIDA

    private static void pruebasFinDePartida() {
        // Caso 1: queda un unico jugador activo.
        Banco banco = new Banco();
        Jugador ana = new Jugador("J1", "Ana", 1000);
        Jugador bruno = new Jugador("J2", "Bruno", 10);
        banco.agregarJugador(ana);
        banco.agregarJugador(bruno);

        revisar("con dos jugadores la partida sigue", !banco.partidaTerminada());

        banco.pagarAlBanco(bruno, 500, "Impuesto de matricula");
        revisar("al quedar uno solo la partida termina", banco.partidaTerminada());
        revisar("gana el ultimo que queda en pie", banco.getGanador().equals(ana));

        // Caso 2: se agota el limite de turnos, gana el de mayor patrimonio.
        Banco corto = new Banco(3, 200);
        Jugador uno = new Jugador("A", "Uno", 300);
        Jugador dos = new Jugador("B", "Dos", 450);
        Jugador tres = new Jugador("C", "Tres", 400);
        corto.agregarJugador(uno);
        corto.agregarJugador(dos);
        corto.agregarJugador(tres);

        corto.terminarTurno(uno);
        corto.comprarPropiedad(dos, new Propiedad(1, "Torre de computo", 400, 90));
        revisarIgual("Dos queda con el saldo mas bajo de los tres", 50, dos.getSaldo());
        revisarIgual("pero con el patrimonio mas alto", 450, dos.calcularPatrimonio());

        corto.terminarTurno(dos);
        revisar("todavia no se alcanza el limite", !corto.partidaTerminada());

        corto.terminarTurno(tres);
        revisar("la partida termina al pasar el limite de turnos", corto.partidaTerminada());
        revisar("gana el de mayor patrimonio, no el de mayor saldo",
                corto.getGanador().equals(dos));
    }

    //  TURNOS DESPUES DE UNA ELIMINACION

    /**
     * Sacar a un jugador de la cola mueve el frente por su cuenta, asi que
     * quien elimina tambien tiene que cerrar el turno. Estas pruebas cubren los
     * tres caminos por los que se puede perder un jugador, y en los tres el
     * turno tiene que seguir en la mano correcta.
     */
    private static void pruebasTurnosTrasEliminacion() {

        // 1. Quiebra en su propio turno, despues de haber lanzado los dados.
        //    El que hereda el frente tiene que poder lanzar: si se le arrastran
        //    los dados del que quebro, se queda trabado y la partida no avanza.
        Banco banco = new Banco();
        Jugador ana = new Jugador("J1", "Ana", 50);
        Jugador bruno = new Jugador("J2", "Bruno", 1000);
        Jugador carla = new Jugador("J3", "Carla", 1000);
        banco.agregarJugador(ana);
        banco.agregarJugador(bruno);
        banco.agregarJugador(carla);

        banco.registrarLanzamientoDados(ana);
        banco.pagarAlBanco(ana, 500, "Impuesto de matricula");

        revisar("al quebrar, el turno pasa al siguiente", banco.jugadorActual().equals(bruno));
        revisar("los dados quedan libres para el que hereda el turno", !banco.yaLanzoDados());
        revisarIgual("el turno del que quebro se cierra al eliminarlo", 2, banco.getNumeroTurno());

        banco.registrarLanzamientoDados(bruno);
        revisar("el siguiente jugador si puede lanzar los dados", banco.yaLanzoDados());

        banco.terminarTurno(bruno);
        revisar("Bruno cierra su turno y le toca a Carla", banco.jugadorActual().equals(carla));

        // Cerrar el turno de un jugador ya eliminado no debe mover nada.
        banco.terminarTurno(ana);
        revisar("cerrar el turno de un eliminado no rota la cola", banco.jugadorActual().equals(carla));
        revisarIgual("ni adelanta el contador de turnos", 3, banco.getNumeroTurno());

        // 2. Eliminacion del jugador del frente por fuera de una bancarrota,
        //    por ejemplo una desconexion. El turno se cierra igual, y el
        //    siguiente jugador no puede perder su rotacion por eso.
        Banco porDesconexion = new Banco();
        Jugador dario = new Jugador("J4", "Dario", 900);
        Jugador elena = new Jugador("J5", "Elena", 900);
        Jugador fabio = new Jugador("J6", "Fabio", 900);
        porDesconexion.agregarJugador(dario);
        porDesconexion.agregarJugador(elena);
        porDesconexion.agregarJugador(fabio);

        porDesconexion.eliminarJugador(dario);
        revisar("al eliminar al del frente le toca al siguiente",
                porDesconexion.jugadorActual().equals(elena));

        porDesconexion.terminarTurno(elena);
        revisar("Elena juega su turno completo y despues le toca a Fabio",
                porDesconexion.jugadorActual().equals(fabio));

        // 3. Quiebra de un jugador que NO es el del frente, como en una carta
        //    de evento donde todos le pagan al que tiro. El turno no se mueve.
        Banco porEvento = new Banco();
        Jugador gina = new Jugador("J7", "Gina", 900);
        Jugador hugo = new Jugador("J8", "Hugo", 10);
        Jugador ivan = new Jugador("J9", "Ivan", 900);
        porEvento.agregarJugador(gina);
        porEvento.agregarJugador(hugo);
        porEvento.agregarJugador(ivan);

        porEvento.pagarAJugador(hugo, gina, 500, "Todos le pagan a Gina");

        revisar("quebrar fuera de turno no le quita el turno al que jugaba",
                porEvento.jugadorActual().equals(gina));
        revisarIgual("el contador de turnos no se mueve", 1, porEvento.getNumeroTurno());
        revisarIgual("pero el eliminado si sale de la cola", 2, porEvento.jugadoresActivos());

        porEvento.terminarTurno(gina);
        revisar("Gina cierra su turno y le toca a Ivan", porEvento.jugadorActual().equals(ivan));
    }

    //  IDS DE TRANSACCION

    /**
     * Los ids tienen que ser unicos dentro de una partida: buscarPorId()
     * devuelve la primera coincidencia, asi que un id repetido esconde una
     * transaccion para siempre y el reporte del punto 13 deja de cuadrar.
     */
    private static void pruebasIdsDeTransaccion() {
        Banco banco = new Banco();
        Jugador ana = new Jugador("J1", "Ana", 1000);
        banco.agregarJugador(ana);

        banco.pagarPremioPorInicio(ana);
        revisarIgual("la primera transaccion lleva el id 1", 1,
                banco.getHistorial().getMasReciente().getId());

        // Una transaccion armada por fuera, como al recuperar una partida.
        banco.registrarTransaccion(new Transaccion(
                7, 1, TipoTransaccion.PAGO_AL_BANCO, "J1", Transaccion.BANCO, 10, "Recuperada"));

        banco.pagarPremioPorInicio(ana);
        revisarIgual("el banco sigue numerando despues del id que entro por fuera", 8,
                banco.getHistorial().getMasReciente().getId());

        revisar("la transaccion recuperada sigue siendo visible",
                banco.getHistorial().buscarPorId(7) != null);
        revisar("y la nueva tambien", banco.getHistorial().buscarPorId(8) != null);
        revisarIgual("el historial tiene las tres", 3, banco.getHistorial().getCantidad());
    }

    //  MOVIMIENTO CON PASOS INVALIDOS

    /**
     * Un numero de pasos negativo no rompia nada: los ciclos de avanzar() y
     * retroceder() no daban ninguna vuelta y la ficha se quedaba quieta sin que
     * nadie se enterara. Es peor que un error, porque no se ve.
     */
    private static void pruebasPasosInvalidos() {
        ListaCircularDoble<Casilla> tablero = tableroDePrueba();
        NodoDoble<Casilla> salida = tablero.getInicio();

        revisarLanzaArgumento("avanzar una cantidad negativa de pasos",
                () -> tablero.avanzar(salida, -3));
        revisarLanzaArgumento("retroceder una cantidad negativa de pasos",
                () -> tablero.retroceder(salida, -3));
        revisarLanzaArgumento("preguntar por el inicio con pasos negativos",
                () -> tablero.pasaPorInicio(salida, -3));

        revisar("avanzar cero pasos deja al jugador donde estaba",
                tablero.avanzar(salida, 0) == salida);
        revisar("dar la vuelta completa vuelve a la salida",
                tablero.avanzar(salida, tablero.getCantidad()) == salida);
    }

    //  AYUDAS

    /**
     * Casilla concreta para armar un tablero de prueba.
     *
     * Casilla es abstracta y no se puede instanciar, asi que hace falta una
     * subclase. De paso comprueba que el contrato sirva para que el encargado
     * del tablero cree las suyas de la misma manera.
     */
    private static class CasillaDePrueba extends Casilla {

        CasillaDePrueba(int id, String nombre) {
            super(id, nombre);
        }

        @Override
        public void ejecutarEfecto(Jugador jugador) {
            // Las casillas no mueven dinero: eso lo hace el Banco.
        }
    }

    private static ListaCircularDoble<Casilla> tableroDePrueba() {
        ListaCircularDoble<Casilla> tablero = new ListaCircularDoble<>();
        tablero.agregar(new CasillaDePrueba(0, "Salida"));
        tablero.agregar(new Propiedad(1, "Laboratorio", 200, 50));
        tablero.agregar(new CasillaDePrueba(2, "Suerte"));
        tablero.agregar(new Propiedad(3, "Biblioteca", 300, 75));
        return tablero;
    }

    private static String leerArchivo(File archivo) {
        try {
            return Files.readString(archivo.toPath());
        } catch (IOException e) {
            return "";
        }
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("== " + texto + " ==");
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
            System.out.println("  FALLO " + descripcion + " -> esperado " + esperado + ", obtenido " + obtenido);
        }
    }

    /**
     * Comprueba que el banco RECHACE una accion.
     *
     * Aqui el exito es que salte la excepcion: si la accion se ejecuta sin
     * problemas, significa que falto una validacion del punto 17.
     *
     * Runnable es una interfaz de Java que solo tiene un metodo sin parametros
     * y sin retorno, asi que se le puede pasar una lambda como
     * () -> banco.comprarPropiedad(ana, lab).
     */
    private static void revisarLanza(String descripcion, Runnable accion) {
        totales++;
        try {
            accion.run();
            fallos++;
            System.out.println("  FALLO " + descripcion + " -> no lanzo excepcion");
        } catch (OperacionInvalidaException e) {
            System.out.println("  OK    " + descripcion + " -> " + e.getMessage());
        }
    }

    /**
     * Igual que revisarLanza(), pero para los errores de programacion.
     *
     * Se separan a proposito: OperacionInvalidaException es "el jugador no
     * puede hacer eso" y se le muestra en pantalla; IllegalArgumentException es
     * "quien programo esto se equivoco" y nunca deberia llegarle al jugador.
     */
    private static void revisarLanzaArgumento(String descripcion, Runnable accion) {
        totales++;
        try {
            accion.run();
            fallos++;
            System.out.println("  FALLO " + descripcion + " -> no lanzo excepcion");
        } catch (IllegalArgumentException e) {
            System.out.println("  OK    " + descripcion + " -> " + e.getMessage());
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
