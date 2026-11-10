package aventurarte.core;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;

/** El cartelito que se desliza en la esquina de abajo a la derecha cuando se desbloaque una tarejta o logro
    si llegan varios a la vez, salen uno despues del otro */
final class AvisoLogro {
    private static final float ENTRA = 0.35f, QUEDA = 2.2f, SALE = 0.35f;
    private static final int W = 160, H = 28;

    private final Deque<String> fila = new ArrayDeque<>();
    private String actual;
    private float t;

    synchronized void encolar(String texto) { fila.add(texto); }

    synchronized void actualizar(float dt) {
        if (actual == null) {
            actual = fila.poll();
            t = 0;
            return;
        }
        t += dt;
        if (t >= ENTRA + QUEDA + SALE) actual = null;
    }

    synchronized void dibujar(Graphics2D g, PixelFont f) {
        if (actual == null) return;
        boolean logro = actual.startsWith("LOGRO");
        String titulo = logro ? "¡LOGRO DESBLOQUEADO!" : "¡TARJETA NUEVA!";
        String nombre = actual.substring(actual.indexOf(':') + 2);

        float k;                                              // 0 = escondido a la derecha, 1 = a la vista
        if (t < ENTRA) k = t / ENTRA;
        else if (t > ENTRA + QUEDA) k = 1 - (t - ENTRA - QUEDA) / SALE;
        else k = 1;
        k = k * k * (3 - 2 * k);
        int x = 480 - Math.round((W + 8) * k), y = 270 - H - 10;

        Color borde = logro ? new Color(0xF2C23C) : new Color(0xB07AF0);
        g.setColor(new Color(0x2A1240));
        g.fillRect(x, y, W, H);
        g.setColor(borde);
        g.fillRect(x, y, W, 1); g.fillRect(x, y + H - 1, W, 1); g.fillRect(x, y, 1, H); g.fillRect(x + W - 1, y, 1, H);
        g.setColor(new Color(0x4A2470));
        g.fillRect(x + 2, y + 2, W - 4, 1);
        // insignia (estrella) o tarjetita
        int ix = x + 7, iy = y + 7;
        if (logro) {
            g.setColor(new Color(0xF2C23C));
            int[] px = {ix + 7, ix + 9, ix + 14, ix + 10, ix + 12, ix + 7, ix + 2, ix + 4, ix, ix + 5};
            int[] py = {iy, iy + 5, iy + 5, iy + 8, iy + 14, iy + 10, iy + 14, iy + 8, iy + 5, iy + 5};
            g.fillPolygon(px, py, 10);
        } else {
            g.setColor(new Color(0xB07AF0));
            g.fillRect(ix + 1, iy, 11, 14);
            g.setColor(new Color(0xF0E0FF));
            g.fillRect(ix + 3, iy + 2, 7, 6);
            g.setColor(new Color(0x7A4AB0));
            g.fillRect(ix + 3, iy + 10, 7, 2);
        }
        f.draw(g, titulo, x + 28, y + 6, 1, borde);
        f.draw(g, nombre, x + 28, y + 16, 1, Color.WHITE);
    }
}
