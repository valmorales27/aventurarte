package aventurarte.core;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

/** Botón hecho con 3-4 imágenes (normal, hover, pressed, disabled)
    rutaBase = ruta sin el estado, ejemplo "menu/menu/botones/boton_jugar"  (busca ..._normal.png, ..._hover.png, etc.) */
public final class Boton {
    public final int x, y;
    public boolean habilitado = true;
    private final BufferedImage normal, hover, pressed, disabled;
    private final int hx, hy, hw, hh;      // zona de click (relativa a x,y)
    private final int hoverDx, pressedDx;

    /** margen = pixeles que se recortan por cada lado para la zona de clic */
    public Boton(String rutaBase, int x, int y, int margen, int hoverDx, int pressedDx) {
        this.x = x; this.y = y;
        normal = Assets.img(rutaBase + "_normal.png");
        hover = Assets.existe(rutaBase + "_hover.png") ? Assets.img(rutaBase + "_hover.png") : normal;
        pressed = Assets.existe(rutaBase + "_pressed.png") ? Assets.img(rutaBase + "_pressed.png") : hover;
        disabled = Assets.existe(rutaBase + "_disabled.png") ? Assets.img(rutaBase + "_disabled.png") : null;
        hx = margen; hy = margen; hw = normal.getWidth() - 2 * margen; hh = normal.getHeight() - 2 * margen;
        this.hoverDx = hoverDx; this.pressedDx = pressedDx;
    }

    public Boton(String rutaBase, int x, int y) { this(rutaBase, x, y, 3, 0, 0); }

    public boolean contiene(int mx, int my) { return mx >= x + hx && mx < x + hx + hw && my >= y + hy && my < y + hy + hh; }

    public int ancho() { return normal.getWidth(); }

    public void dibujar(Graphics2D g, boolean seleccionado, boolean presionado) {
        if (!habilitado) {
            if (disabled != null) g.drawImage(disabled, x, y, null);
            else {
                Composite c = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.45f));
                g.drawImage(normal, x, y, null);
                g.setComposite(c);
            }
            return;
        }
        if (seleccionado && presionado) g.drawImage(pressed, x + pressedDx, y, null);
        else if (seleccionado) g.drawImage(hover, x + hoverDx, y, null);
        else g.drawImage(normal, x, y, null);
    }

    /** Grupo de botones con selección por mouse y por teclado (flechas + ENTER)
     * Uso en una pantalla:
     *    int a = lista.mouse(j.input);                       // clic
     *    while ((k = j.input.sigPulsada()) != -1) a = lista.tecla(k);   // teclado
     */
    public static final class Lista {
        public final Boton[] botones;
        public int sel = 0;
        public boolean presionado;
        private int ux = -99, uy = -99;
        public boolean vertical = true;

        public Lista(Boton... botones) { this.botones = botones; sel = primeroHabilitado(0, 1); }

        private int primeroHabilitado(int desde, int paso) {
            int n = botones.length;
            for (int k = 0; k < n; k++) {
                int i = Math.floorMod(desde + k * paso, n);
                if (botones[i].habilitado) return i;
            }
            return 0;
        }

        /** Procesa mouse leyendo los clics pendientes. Devuelve el indice clicado o -1 */
        public int mouse(Input in) { return mouse(in, in.clics()); }

        /** Igual, pero con una lista de clics ya sacada de Input (para que la pantalla use los mismos clics en otros elementos) */
        public int mouse(Input in, java.util.List<int[]> clics) {
            if (in.mx != ux || in.my != uy) {
                ux = in.mx; uy = in.my;
                for (int i = 0; i < botones.length; i++)
                    if (botones[i].habilitado && botones[i].contiene(in.mx, in.my)) sel = i;
            }
            presionado = in.mouseAbajo && botones[sel].habilitado && botones[sel].contiene(in.mx, in.my);
            int res = -1;
            for (int[] c : clics)
                for (int i = 0; i < botones.length; i++)
                    if (botones[i].habilitado && botones[i].contiene(c[0], c[1])) { sel = i; res = i; }
            return res;
        }

        /** Procesa una tecla. Devuelve el indice activado con ENTER/ESPACIO o -1 */
        public int tecla(int vk) {
            int ant = vertical ? KeyEvent.VK_UP : KeyEvent.VK_LEFT, sig = vertical ? KeyEvent.VK_DOWN : KeyEvent.VK_RIGHT;
            if (vk == ant || (vertical && vk == KeyEvent.VK_W) || (!vertical && vk == KeyEvent.VK_A)) sel = primeroHabilitado(sel - 1, -1);
            else if (vk == sig || (vertical && vk == KeyEvent.VK_S) || (!vertical && vk == KeyEvent.VK_D)) sel = primeroHabilitado(sel + 1, 1);
            else if (vk == KeyEvent.VK_ENTER || vk == KeyEvent.VK_SPACE) return botones[sel].habilitado ? sel : -1;
            return -1;
        }

        public void dibujar(Graphics2D g) {
            for (int i = 0; i < botones.length; i++) botones[i].dibujar(g, i == sel, presionado && i == sel);
        }
    }
}
