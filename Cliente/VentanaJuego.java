package Cliente;

import Protocolo.Protocolo;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Interfaz grafica del cliente: tablero de Monopoly visto desde arriba
 *
 * 
 */
public class VentanaJuego extends JFrame {

    private static final int LADO = 7;
    private static final Color[] COLORES_JUGADOR = {
        new Color(0xD32F2F), new Color(0x1976D2), new Color(0x388E3C), new Color(0xF9A825)
    };

    // Conexion
    private Socket socket;
    private PrintWriter salida;
    private String miId;

    // Estado recibido del servidor
    private String[] nombresCasilla = new String[24];
    private String[] tiposCasilla = new String[24];
    private int[] preciosCasilla = new int[24];
    private String[] idsJugador = new String[0];
    private String[] nombresJugador = new String[0];
    private int[] saldos = new int[0];
    private int[] posiciones = new int[0];
    private boolean[] activos = new boolean[0];
    private String[] duenoDeCasilla = new String[24];
    private String turnoActual = "-";
    private int numeroTurno;
    private int maxTurnos;
    private int dado1;
    private int dado2;
    private boolean hayOferta;
    private boolean yaTiro;
    private boolean terminada;

    // Componentes
    private final PanelTablero panelTablero = new PanelTablero();
    private final JTextArea registro = new JTextArea();
    private final JLabel lblEstado = new JLabel("Esperando jugadores...");
    private final JButton btnTirar = new JButton("Tirar dados");
    private final JButton btnComprar = new JButton("Comprar");
    private final JButton btnNoComprar = new JButton("No comprar");
    private final JButton btnTerminar = new JButton("Terminar turno");
    private final JButton btnHistorial = new JButton("Historial");
    private final JButton btnTransacciones = new JButton("Transacciones");
    private final JButton btnExportar = new JButton("Exportar TXT");
    private final PanelJugadores panelJugadores = new PanelJugadores();
    private JScrollPane scrollRegistro;

    public VentanaJuego(String ip, int puerto, String id, String nombre) throws IOException {
        super("Monopoly Costa Rica - " + nombre);
        this.miId = id;

        socket = new Socket(ip, puerto);
        BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        salida = new PrintWriter(socket.getOutputStream(), true);

        construirInterfaz();
        enviar("CONECTAR|" + id + "|" + nombre);

        Thread receptor = new Thread(() -> {
            try {
                String linea;
                while ((linea = entrada.readLine()) != null) {
                    final String l = linea;
                    SwingUtilities.invokeLater(() -> procesar(l));
                }
            } catch (IOException e) {
                // conexion cerrada
            }
            SwingUtilities.invokeLater(() -> log("Conexion con el servidor cerrada."));
        });
        receptor.setDaemon(true);
        receptor.start();
    }

    //  INTERFAZ

    private void construirInterfaz() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(Color.WHITE);

        add(panelTablero, BorderLayout.CENTER);

        registro.setEditable(false);
        registro.setLineWrap(true);
        registro.setWrapStyleWord(true);
        registro.setFont(new Font("SansSerif", Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(registro);
        scroll.setPreferredSize(new Dimension(260, 200));
        scrollRegistro = scroll;

        JPanel botones = new JPanel(new GridLayout(7, 1, 4, 4));
        botones.add(btnTirar);
        botones.add(btnComprar);
        botones.add(btnNoComprar);
        botones.add(btnTerminar);
        botones.add(btnHistorial);
        botones.add(btnTransacciones);
        botones.add(btnExportar);

        lblEstado.setForeground(new Color(0x212121));
        lblEstado.setFont(new Font("SansSerif", Font.BOLD, 13));

        JPanel lateral = new JPanel(new BorderLayout(6, 6));
        lateral.setOpaque(false);
        lateral.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 8));
        lateral.add(lblEstado, BorderLayout.NORTH);
        JPanel centroLateral = new JPanel(new BorderLayout(0, 6));
        centroLateral.setOpaque(false);
        centroLateral.add(panelJugadores, BorderLayout.NORTH);
        centroLateral.add(scroll, BorderLayout.CENTER);
        lateral.add(centroLateral, BorderLayout.CENTER);
        lateral.add(botones, BorderLayout.SOUTH);
        add(lateral, BorderLayout.EAST);

        btnTirar.addActionListener(e -> { enviar("TIRAR_DADOS"); });
        btnComprar.addActionListener(e -> enviar("COMPRAR_PROPIEDAD"));
        btnNoComprar.addActionListener(e -> enviar("NO_COMPRAR"));
        btnTerminar.addActionListener(e -> enviar("TERMINAR_TURNO"));
        btnHistorial.addActionListener(e -> {
            // Abre o cierra el registro de la partida.
            scrollRegistro.setVisible(!scrollRegistro.isVisible());
            btnHistorial.setText(scrollRegistro.isVisible() ? "Cerrar historial" : "Historial");
            revalidate();
            repaint();
        });
        btnTransacciones.addActionListener(e -> enviar("CONSULTAR_TRANSACCIONES"));
        btnExportar.addActionListener(e -> enviar("EXPORTAR_TRANSACCIONES"));

        actualizarBotones();
        setMinimumSize(new Dimension(900, 700));
        setSize(1020, 760);
        setLocationRelativeTo(null);
    }

    private void actualizarBotones() {
        boolean miTurno = !terminada && miId.equals(turnoActual);
        btnTirar.setEnabled(miTurno && !yaTiro && !hayOferta);
        btnComprar.setEnabled(miTurno && hayOferta);
        btnNoComprar.setEnabled(miTurno && hayOferta);
        btnTerminar.setEnabled(miTurno && yaTiro && !hayOferta);
        btnHistorial.setEnabled(true);
        btnTransacciones.setEnabled(true);
        btnExportar.setEnabled(true);
    }

    private void log(String texto) {
        registro.append(texto + "\n");
        registro.setCaretPosition(registro.getDocument().getLength());
    }

    private void enviar(String mensaje) {
        if (salida != null) {
            salida.println(mensaje);
        }
    }

    //  MENSAJES DEL SERVIDOR

    private void procesar(String linea) {
        String[] p = linea.split("\\|", -1);
        switch (p[0]) {
            case "CONECTADO":
                log("Conectado como " + p[2] + " (id " + p[1] + ")");
                break;
            case "PARTIDA_INICIADA":
                log("La partida comenzo.");
                break;
            case "TABLERO":
                leerTablero(p[1]);
                break;
            case "ESTADO_JUEGO":
                leerEstado(p);
                break;
            case "TURNO":
                turnoActual = p[1];
                yaTiro = false;
                hayOferta = false;
                log("Turno de " + nombreDe(p[1]));
                break;
            case "DADOS":
                dado1 = Integer.parseInt(p[2]);
                dado2 = Integer.parseInt(p[3]);
                if (p[1].equals(miId)) {
                    yaTiro = true;
                }
                log(nombreDe(p[1]) + " saco " + p[2] + " + " + p[3] + " = " + p[4]);
                break;
            case "MOVIMIENTO":
                log(nombreDe(p[1]) + " avanza a " + p[3]);
                break;
            case "EVENTO":
                log(p[1]);
                break;
            case "OFERTA_COMPRA":
                if (turnoActual.equals(miId)) {
                    hayOferta = true;
                }
                break;
            case "OFERTA_RECHAZADA":
                if (p.length > 1 && p[1].equals(miId)) {
                    hayOferta = false;
                }
                break;
            case "COMPRA":
                hayOferta = false;
                log(nombreDe(p[1]) + " compro " + p[3] + " por " + p[4]);
                break;
            case "ELIMINADO":
                log(nombreDe(p[1]) + " quedo eliminado.");
                break;
            case "FIN_PARTIDA":
                terminada = true;
                log("FIN DE LA PARTIDA. Ganador: " + nombreDe(p[1]));
                JOptionPane.showMessageDialog(this, "Ganador: " + nombreDe(p[1]), "Fin de la partida",
                        JOptionPane.INFORMATION_MESSAGE);
                break;
            case "TRANSACCIONES":
                mostrarHistorial(linea.substring("TRANSACCIONES|".length()));
                break;
            case "EXPORTADO":
                log("Reporte TXT generado en el servidor: " + (p.length > 1 ? p[1] : ""));
                break;
            case "ERROR":
                log("Error: " + (p.length > 1 ? p[1] : ""));
                break;
            default:
                break;
        }
        if (!p[0].equals("ESTADO_JUEGO")) {
            // El estado completo llega despues de cada accion; basta con repintar.
            panelTablero.repaint();
        }
        actualizarBotones();
    }

    private void leerTablero(String datos) {
        String[] casillas = datos.split(";");
        for (String c : casillas) {
            String[] f = c.split(":");
            int id = Integer.parseInt(f[0]);
            nombresCasilla[id] = f[1];
            tiposCasilla[id] = f[2];
            preciosCasilla[id] = Integer.parseInt(f[3]);
        }
    }

    private void leerEstado(String[] p) {
        turnoActual = p[1];
        numeroTurno = Integer.parseInt(p[2]);
        maxTurnos = Integer.parseInt(p[3]);

        String[] jugadores = p[4].isEmpty() ? new String[0] : p[4].split(";");
        idsJugador = new String[jugadores.length];
        nombresJugador = new String[jugadores.length];
        saldos = new int[jugadores.length];
        posiciones = new int[jugadores.length];
        activos = new boolean[jugadores.length];
        for (int i = 0; i < jugadores.length; i++) {
            String[] f = jugadores[i].split(":");
            idsJugador[i] = f[0];
            nombresJugador[i] = f[1];
            saldos[i] = Integer.parseInt(f[2]);
            posiciones[i] = Integer.parseInt(f[3]);
            activos[i] = f[4].equals("1");
        }

        duenoDeCasilla = new String[24];
        if (p.length > 5 && !p[5].isEmpty()) {
            for (String d : p[5].split(",")) {
                String[] f = d.split("=");
                duenoDeCasilla[Integer.parseInt(f[0])] = f[1];
            }
        }

        lblEstado.setText("<html>Turno " + numeroTurno + "/" + maxTurnos + "<br>Juega: "
                + nombreDe(turnoActual) + "</html>");
        panelTablero.repaint();
        panelJugadores.repaint();
        actualizarBotones();
    }

    private String nombreDe(String id) {
        for (int i = 0; i < idsJugador.length; i++) {
            if (idsJugador[i].equals(id)) {
                return nombresJugador[i];
            }
        }
        return id;
    }

    private int indiceDe(String id) {
        for (int i = 0; i < idsJugador.length; i++) {
            if (idsJugador[i].equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private void mostrarHistorial(String texto) {
        JTextArea area = new JTextArea(texto.replace(" ; ", "\n"), 18, 60);
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Historial de transacciones",
                JOptionPane.PLAIN_MESSAGE);
    }

    //  TABLERO

    private class PanelTablero extends JPanel {

        PanelTablero() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(680, 680));
        }

        /** Columna y fila (en la cuadricula 7x7) de la casilla i. */
        private int[] celda(int i) {
            if (i <= 6) {
                return new int[] { 6 - i, 6 };
            }
            if (i <= 12) {
                return new int[] { 0, 6 - (i - 6) };
            }
            if (i <= 18) {
                return new int[] { i - 12, 0 };
            }
            return new int[] { 6, i - 18 };
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int lado = Math.min(getWidth(), getHeight()) - 20;
            int c = lado / LADO;
            lado = c * LADO;
            int x0 = (getWidth() - lado) / 2;
            int y0 = (getHeight() - lado) / 2;

            // Fondo del tablero y centro
            g.setColor(new Color(0xF3F1EA));
            g.fillRect(x0, y0, lado, lado);
            dibujarCentro(g, x0 + c, y0 + c, c * (LADO - 2));

            for (int i = 0; i < 24; i++) {
                if (nombresCasilla[i] == null) {
                    continue;
                }
                int[] rc = celda(i);
                dibujarCasilla(g, i, x0 + rc[0] * c, y0 + rc[1] * c, c);
            }
            for (int j = 0; j < idsJugador.length; j++) {
                dibujarFicha(g, j, x0, y0, c);
            }
        }

        private void dibujarCasilla(Graphics2D g, int i, int x, int y, int c) {
            boolean propiedad = "PROPIEDAD".equals(tiposCasilla[i]);
            boolean evento = "EVENTO".equals(tiposCasilla[i]);

            g.setColor(Color.WHITE);
            g.fillRect(x, y, c, c);

            if (propiedad) {
                // Franja de color de la ciudad (por grupos de la ruta)
                g.setColor(colorGrupo(i));
                g.fillRect(x, y, c, c / 5);
            } else if (evento) {
                g.setColor(new Color(0xFFE082));
                g.fillRect(x, y, c, c);
            } else {
                g.setColor(new Color(0xE0E0E0));
                g.fillRect(x, y, c, c);
            }

            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.2f));
            g.drawRect(x, y, c, c);

            // Dueno: borde grueso con el color del jugador
            if (duenoDeCasilla[i] != null) {
                int k = indiceDe(duenoDeCasilla[i]);
                if (k >= 0) {
                    g.setColor(COLORES_JUGADOR[k % 4]);
                    g.setStroke(new BasicStroke(4f));
                    g.drawRect(x + 2, y + 2, c - 4, c - 4);
                    g.setStroke(new BasicStroke(1f));
                }
            }

            g.setColor(Color.BLACK);
            g.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, c / 8)));
            dibujarTextoCentrado(g, nombresCasilla[i], x, y + (propiedad ? c / 5 + 4 : 4), c, c / 2);

            if (propiedad) {
                g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(9, c / 8)));
                dibujarTextoCentrado(g, "$" + preciosCasilla[i], x, y + c - c / 4, c, c / 4);
            } else if (evento) {
                g.setFont(new Font("SansSerif", Font.BOLD, c / 3));
                dibujarTextoCentrado(g, "?", x, y + c / 2, c, c / 2);
            } else if (nombresCasilla[i].equals("Inicio")) {
                g.setFont(new Font("SansSerif", Font.BOLD, c / 4));
                dibujarTextoCentrado(g, "SALIDA", x, y + c / 2, c, c / 3);
            }
        }

        private Color colorGrupo(int i) {
            Color[] grupos = {
                new Color(0x8D6E63), new Color(0x4FC3F7), new Color(0xEC407A), new Color(0xFB8C00),
                new Color(0xE53935), new Color(0xFDD835), new Color(0x43A047), new Color(0x3949AB)
            };
            return grupos[(i / 3) % grupos.length];
        }

        /** Escribe texto centrado, partiendo en lineas por espacios si no cabe. */
        private void dibujarTextoCentrado(Graphics2D g, String texto, int x, int y, int ancho, int alto) {
            FontMetrics fm = g.getFontMetrics();
            String[] palabras = texto.split(" ");
            java.util.List<String> lineas = new java.util.ArrayList<>();
            String actual = "";
            for (String w : palabras) {
                String prueba = actual.isEmpty() ? w : actual + " " + w;
                if (fm.stringWidth(prueba) <= ancho - 6 || actual.isEmpty()) {
                    actual = prueba;
                } else {
                    lineas.add(actual);
                    actual = w;
                }
            }
            lineas.add(actual);
            int total = lineas.size() * fm.getHeight();
            int yy = y + (alto - total) / 2 + fm.getAscent();
            for (String l : lineas) {
                g.drawString(l, x + (ancho - fm.stringWidth(l)) / 2, yy);
                yy += fm.getHeight();
            }
        }

        private void dibujarFicha(Graphics2D g, int j, int x0, int y0, int c) {
            if (!activos[j]) {
                return;
            }
            int[] rc = celda(posiciones[j]);
            // Cuatro espacios en fila, entre el nombre y el precio de la casilla
            int d = c / 4 - 2;
            int x = x0 + rc[0] * c + j * (c / 4) + 1;
            int y = y0 + rc[1] * c + c / 2;
            g.setColor(COLORES_JUGADOR[j % 4]);
            g.fillOval(x, y, d, d);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(1.5f));
            g.drawOval(x, y, d, d);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, d / 2 + 2));
            String ini = nombresJugador[j].substring(0, 1).toUpperCase();
            FontMetrics fm = g.getFontMetrics();
            g.drawString(ini, x + (d - fm.stringWidth(ini)) / 2, y + (d + fm.getAscent()) / 2 - 2);
        }

        private void dibujarCentro(Graphics2D g, int x, int y, int lado) {
            g.setColor(new Color(0xF3F1EA));
            g.fillRect(x, y, lado, lado);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, lado, lado);

            g.setFont(new Font("Serif", Font.BOLD, lado / 9));
            g.setColor(new Color(0xB71C1C));
            FontMetrics fm = g.getFontMetrics();
            String titulo = "MONOPOLY";
            g.drawString(titulo, x + (lado - fm.stringWidth(titulo)) / 2, y + lado / 5);
            g.setFont(new Font("Serif", Font.ITALIC, lado / 18));
            g.setColor(Color.DARK_GRAY);
            fm = g.getFontMetrics();
            String sub = "Costa Rica";
            g.drawString(sub, x + (lado - fm.stringWidth(sub)) / 2, y + lado / 5 + lado / 40 + (lado / 4) * 3 / 5 + lado / 12);

            // Bandera de Costa Rica entre el titulo y el subtitulo
            int bw = lado / 4;
            int bh = bw * 3 / 5;
            dibujarBandera(g, x + (lado - bw) / 2, y + lado / 5 + lado / 40, bw, bh);

            // Dados
            int d = lado / 6;
            dibujarDado(g, x + lado / 2 - d - 10, y + lado * 11 / 20, d, dado1);
            dibujarDado(g, x + lado / 2 + 10, y + lado * 11 / 20, d, dado2);
        }

        /** Bandera de Costa Rica: franjas azul, blanca, roja, blanca y azul (1:1:2:1:1). */
        private void dibujarBandera(Graphics2D g, int x, int y, int w, int h) {
            int u = h / 6;
            Color azul = new Color(0x002B7F);
            Color rojo = new Color(0xCE1126);
            g.setColor(azul);
            g.fillRect(x, y, w, h);
            g.setColor(Color.WHITE);
            g.fillRect(x, y + u, w, h - 2 * u);
            g.setColor(rojo);
            g.fillRect(x, y + 2 * u, w, h - 4 * u);
            g.setColor(Color.DARK_GRAY);
            g.setStroke(new BasicStroke(1f));
            g.drawRect(x, y, w, h);
        }

        private void dibujarDado(Graphics2D g, int x, int y, int d, int valor) {
            g.setColor(Color.WHITE);
            g.fillRoundRect(x, y, d, d, d / 5, d / 5);
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2f));
            g.drawRoundRect(x, y, d, d, d / 5, d / 5);
            int r = Math.max(4, d / 6);
            int a = d / 4;
            int m = d / 2;
            int b = 3 * d / 4;
            int[][] pos = {
                {}, {m, m}, {a, a, b, b}, {a, a, m, m, b, b}, {a, a, a, b, b, a, b, b},
                {a, a, a, b, m, m, b, a, b, b}, {a, a, a, m, a, b, b, a, b, m, b, b}
            };
            if (valor < 1 || valor > 6) {
                return;
            }
            int[] pts = pos[valor];
            for (int k = 0; k + 1 < pts.length; k += 2) {
                g.fillOval(x + pts[k] - r / 2, y + pts[k + 1] - r / 2, r, r);
            }
        }
    }

    //  LISTA DE JUGADORES (panel derecho)

    private class PanelJugadores extends JPanel {

        PanelJugadores() {
            setOpaque(false);
            setPreferredSize(new Dimension(260, 4 * 40 + 10));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            for (int j = 0; j < idsJugador.length; j++) {
                int y = 4 + j * 40;
                boolean turno = idsJugador[j].equals(turnoActual);

                g.setColor(turno ? new Color(0xFFF8E1) : new Color(0xF5F5F5));
                g.fillRoundRect(0, y, getWidth() - 2, 36, 10, 10);
                g.setColor(turno ? COLORES_JUGADOR[j % 4] : new Color(0xDDDDDD));
                g.drawRoundRect(0, y, getWidth() - 2, 36, 10, 10);

                g.setColor(COLORES_JUGADOR[j % 4]);
                g.fillOval(8, y + 9, 18, 18);

                g.setColor(activos[j] ? Color.BLACK : Color.GRAY);
                g.setFont(new Font("SansSerif", turno ? Font.BOLD : Font.PLAIN, 14));
                g.drawString(nombresJugador[j] + (activos[j] ? "" : " (eliminado)"), 34, y + 23);

                String saldo = "$" + saldos[j];
                FontMetrics fm = g.getFontMetrics();
                g.drawString(saldo, getWidth() - 10 - fm.stringWidth(saldo), y + 23);
            }
        }
    }

    //  ARRANQUE

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JTextField ip = new JTextField("localhost");
            JTextField puerto = new JTextField(String.valueOf(Protocolo.PUERTO));
            JTextField id = new JTextField("1");
            JTextField nombre = new JTextField("Jugador");
            javax.swing.JCheckBox organizador = new javax.swing.JCheckBox("Soy el organizador (aloja el banco)");
            JTextField maxTurnos = new JTextField("100");
            JPanel form = new JPanel(new GridLayout(6, 2, 4, 4));
            form.add(new JLabel("IP del servidor:"));
            form.add(ip);
            form.add(new JLabel("Puerto:"));
            form.add(puerto);
            form.add(new JLabel("Id (1-4):"));
            form.add(id);
            form.add(new JLabel("Nombre:"));
            form.add(nombre);
            form.add(organizador);
            form.add(new JLabel(""));
            form.add(new JLabel("Max. turnos (organizador):"));
            form.add(maxTurnos);

            if (JOptionPane.showConfirmDialog(null, form, "Conectar al Monopoly", JOptionPane.OK_CANCEL_OPTION)
                    != JOptionPane.OK_OPTION) {
                return;
            }
            try {
                String direccion = ip.getText().trim();
                int numeroPuerto = Integer.parseInt(puerto.getText().trim());

                if (organizador.isSelected()) {
                    // El organizador aloja el banco: se levanta el servidor en
                    // este mismo programa y se conecta a si mismo.
                    final Servidor.Servidor servidor =
                            new Servidor.Servidor(numeroPuerto, Integer.parseInt(maxTurnos.getText().trim()));
                    Thread hiloServidor = new Thread(servidor::iniciar, "servidor-banco");
                    hiloServidor.setDaemon(true);
                    hiloServidor.start();
                    Thread.sleep(500);
                    direccion = "localhost";
                }

                new VentanaJuego(direccion, numeroPuerto,
                        id.getText().trim(), nombre.getText().trim()).setVisible(true);
            } catch (IOException | NumberFormatException | InterruptedException e) {
                JOptionPane.showMessageDialog(null, "No se pudo conectar: " + e.getMessage());
            }
        });
    }
}
