package aventurarte.core;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

/** Panel de Swing donde se ve el juego. Se hace toda la configiración, dibuja la pantalla de 480x270,
    se escala y traduce teclado y mouse a la clase Input
    HILOS:  el hilo "bucle-juego" (clase interna) actualiza y dibuja a ~60 cuadros por segundo,
    el hilo de Swing (EDT) solo muestra la imagen terminada y recibe los eventos
 */
public final class GamePanel extends JPanel {
    private final Juego juego;
    private BufferedImage frente = nuevoLienzo(), atras = nuevoLienzo();
    private final Object candado = new Object();
    private Thread hilo;
    private volatile boolean corriendo;
    private double escala = 2;
    private int ox, oy;

    public GamePanel(Juego juego) {
        this.juego = juego;
        setPreferredSize(new Dimension(Pantalla.W * 2, Pantalla.H * 2));
        setBackground(Color.BLACK);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);  // para que TAB llegue al juego
        instalarEventos();
    }

    private static BufferedImage nuevoLienzo() { return new BufferedImage(Pantalla.W, Pantalla.H, BufferedImage.TYPE_INT_RGB); }

    // ------------------------------------------------------------------ BUCLE DEL JUEGO
    public void iniciar() {
        corriendo = true;
        hilo = new Thread(this::bucle, "bucle-juego");
        hilo.setDaemon(true);
        hilo.start();
    }

    public void detener() {
        corriendo = false;
        try { if (hilo != null) hilo.join(500); } catch (InterruptedException ignorar) { }
    }

    private void bucle() {
        final long paso = 1_000_000_000L / 60;
        long previo = System.nanoTime();
        while (corriendo) {
            long ahora = System.nanoTime();
            float dt = Math.min(0.05f, (ahora - previo) / 1e9f);    // no más de 50 ms
            previo = ahora;
            try {
                juego.actualizar(dt);
                Graphics2D g = atras.createGraphics();
                try {
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, Pantalla.W, Pantalla.H);
                    juego.dibujar(g);
                } finally { g.dispose(); }
                synchronized (candado) { BufferedImage t = frente; frente = atras; atras = t; }
                repaint();
            } catch (Throwable t) {
                t.printStackTrace(); // un error en una pantalla no debe cerrar el bucle
            }
            if (juego.salir) { corriendo = false; SwingUtilities.invokeLater(() -> {
                Window w = SwingUtilities.getWindowAncestor(this);
                if (w != null) w.dispatchEvent(new WindowEvent(w, WindowEvent.WINDOW_CLOSING));
            }); }
            long resto = paso - (System.nanoTime() - ahora);
            if (resto > 0) {
                try { Thread.sleep(resto / 1_000_000L, (int) (resto % 1_000_000L)); } catch (InterruptedException e) { return; }
            }
        }
    }

    // ------------------------------------------------------------------ MOSTRAMOS (EDT)
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        double e = Math.min(getWidth() / (double) Pantalla.W, getHeight() / (double) Pantalla.H);
        escala = e >= 1 ? Math.floor(e) : e;        // escala entera = pixeles adecuados
        int w = (int) Math.round(Pantalla.W * escala), h = (int) Math.round(Pantalla.H * escala);
        ox = (getWidth() - w) / 2;
        oy = (getHeight() - h) / 2;
        synchronized (candado) { g.drawImage(frente, ox, oy, w, h, null); }
        Toolkit.getDefaultToolkit().sync();
    }

    // ------------------------------------------------------------------ EVENTOS -> INPUT
    private int lx(MouseEvent e) { return (int) Math.floor((e.getX() - ox) / escala); }
    private int ly(MouseEvent e) { return (int) Math.floor((e.getY() - oy) / escala); }

    private void instalarEventos() {
        final Input in = juego.input;
        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_M) { juego.audio.setSilencio(!juego.audio.isSilencio()); return; }
                in.teclaAbajo(e.getKeyCode());
            }
            @Override public void keyReleased(KeyEvent e) { in.teclaArriba(e.getKeyCode()); }
            @Override public void keyTyped(KeyEvent e) { char c = e.getKeyChar(); if (c >= 32 && c != 127) in.caracter(c); }
        });
        MouseAdapter m = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { in.mx = lx(e); in.my = ly(e); }
            @Override public void mouseDragged(MouseEvent e) { in.mx = lx(e); in.my = ly(e); }
            @Override public void mousePressed(MouseEvent e) { requestFocusInWindow(); in.mx = lx(e); in.my = ly(e); in.mouseAbajo = true; }
            @Override public void mouseReleased(MouseEvent e) { in.mx = lx(e); in.my = ly(e); in.mouseAbajo = false; in.mouseSoltado(in.mx, in.my); }
            @Override public void mouseWheelMoved(MouseWheelEvent e) { in.rueda(e.getWheelRotation()); }
        };
        addMouseListener(m);
        addMouseMotionListener(m);
        addMouseWheelListener(m);
        addFocusListener(new FocusAdapter() { @Override public void focusLost(FocusEvent e) { in.perdioFoco(); } });
    }
}
