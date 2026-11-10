package aventurarte.core;

import java.awt.*;

/** Clase para la pantalla del juego (splash, menu, mapa, nivel, pausa...). Todas se dibujan en un frame de 480x270
   Juego las apila: la de arriba recibe actualizar() todas se dibujan de abajo hacia arriba
 */
public abstract class Pantalla {
    public static final int W = 480, H = 270;

    protected final Juego j;

    protected Pantalla(Juego j) { this.j = j; }

    /** Se llama cuando la pantalla entra a la pila */
    public void alEntrar() { }

    /** Se llama cuando sale de la pila */
    public void alSalir() { }

    /** Se llama cuando la pantalla que estaba encima se cerró y esta vuelve a ser la de arriba */
    public void alRecuperarFoco() { }

    /** Lógica de la pantalla. dt = segundos desde el cuadro anterior */
    public abstract void actualizar(float dt);

    /** Dibujo en coordenadas (480x270) */
    public abstract void dibujar(Graphics2D g);

    // ---------------------------------------------------------------- FORMATO DE DIBUJO
    protected static void imagen(Graphics2D g, java.awt.image.BufferedImage im, int x, int y) { g.drawImage(im, x, y, null); }

    protected static void conAlpha(Graphics2D g, float a, Runnable r) {
        Composite c = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, a))));
        r.run();
        g.setComposite(c);
    }

    protected static boolean dentro(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
