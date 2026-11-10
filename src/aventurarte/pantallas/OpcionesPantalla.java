package aventurarte.pantallas;

import aventurarte.core.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;

/** El cuaderno de OPCIONES: pestañas SONIDO / CONTROLES / AYUDA. Se abre encima de lo que estes viendo. */
public class OpcionesPantalla extends Pantalla {
    private static final String[] TABS = {"sonido", "controles", "ayuda"};
    private static final int[] TAB_X = {100, 180, 260};
    private static final int[] PISTA_Y = {80, 114};
    private static final int PISTA_X = 212, PISTA_W = 150;

    private final Boton volver = new Boton("opciones/botones/boton_volver", 96, 208);
    private int tab = 0;
    private int fila = 0;                 // 0 musica, 1 efectos, 2 silenciar, 3 volver
    private int arrastre = -1;
    private boolean prevAbajo;
    private float t;

    public OpcionesPantalla(Juego j) { super(j); }

    @Override public void actualizar(float dt) {
        t += dt;
        Input in = j.input;
        boolean abajo = in.mouseAbajo;
        int mx = in.mx, my = in.my;

        // --- arrastrar sliders
        if (tab == 0) {
            if (abajo && !prevAbajo)
                for (int i = 0; i < 2; i++)
                    if (dentro(mx, my, PISTA_X - 8, PISTA_Y[i] - 6, PISTA_W + 16, 20)) { arrastre = i; fila = i; }
            if (!abajo && arrastre >= 0) { arrastre = -1; j.audio.sfx("clic"); }
            if (arrastre >= 0) poner(arrastre, Math.max(0, Math.min(1, (mx - PISTA_X) / (float) PISTA_W)));
        }
        prevAbajo = abajo;

        // --- clics
        List<int[]> clics = in.clics();
        for (int[] c : clics) {
            for (int i = 0; i < 3; i++)
                if (dentro(c[0], c[1], TAB_X[i], 0, 74, 24)) { tab = i; j.audio.sfx("clic"); }
            if (tab == 0 && dentro(c[0], c[1], 96, 146, 194, 16)) alternarSilencio();
            if (volver.contiene(c[0], c[1])) { cerrar(); return; }
        }

        // --- teclado
        int k;
        while ((k = in.sigPulsada()) != -1) {
            if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_BACK_SPACE) { cerrar(); return; }
            else if (k == KeyEvent.VK_TAB || k == KeyEvent.VK_E) { tab = (tab + 1) % 3; j.audio.sfx("clic"); }
            else if (k == KeyEvent.VK_Q) { tab = (tab + 2) % 3; j.audio.sfx("clic"); }
            else if (k == KeyEvent.VK_UP) fila = Math.max(0, fila - 1);
            else if (k == KeyEvent.VK_DOWN) fila = Math.min(3, fila + 1);
            else if (k == KeyEvent.VK_LEFT && tab == 0 && fila < 2) { poner(fila, vol(fila) - 0.1f); j.audio.sfx("clic"); }
            else if (k == KeyEvent.VK_RIGHT && tab == 0 && fila < 2) { poner(fila, vol(fila) + 0.1f); j.audio.sfx("clic"); }
            else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) {
                if (tab != 0 || fila == 3) { cerrar(); return; }
                if (fila == 2) alternarSilencio();
            }
        }
    }

    private void cerrar() { j.audio.sfx("clic"); j.guardarTodo(); j.cerrar(); }

    private float vol(int i) { return i == 0 ? j.audio.getVolMusica() : j.audio.getVolEfectos(); }

    private void poner(int i, float v) {
        v = Math.max(0f, Math.min(1f, v));
        if (i == 0) j.audio.setVolMusica(v); else j.audio.setVolEfectos(v);
    }

    private void alternarSilencio() { j.audio.setSilencio(!j.audio.isSilencio()); j.audio.sfx("clic"); }

    @Override public void dibujar(Graphics2D g) {
        imagen(g, Assets.img("opciones/oscurecer_480x270.png"), 0, 0);
        imagen(g, Assets.img("opciones/cuaderno_480x270.png"), 0, 0);
        for (int i = 0; i < 3; i++) {
            boolean act = i == tab;
            imagen(g, Assets.img("opciones/tabs/tab_" + TABS[i] + (act ? "_activa" : "_inactiva") + ".png"), TAB_X[i], act ? 0 : 1);
        }
        imagen(g, Assets.img("opciones/pagina_" + TABS[tab] + "_480x270.png"), 0, 0);
        if (tab == 0) paginaSonido(g);
        boolean sobre = volver.contiene(j.input.mx, j.input.my);
        volver.dibujar(g, sobre || (tab != 0 || fila == 3) && (int) (t * 3) % 2 == 0, sobre && j.input.mouseAbajo);
    }

    private void paginaSonido(Graphics2D g) {
        Color tinta = new Color(0x2A3A6A);
        for (int i = 0; i < 2; i++) {
            int y = PISTA_Y[i];
            float v = vol(i);
            int fw = (int) (PISTA_W * v);
            BufferedImage relleno = Assets.img("opciones/sliders/slider_relleno_" + (i == 0 ? "musica" : "efectos") + ".png");
            if (fw > 0) g.drawImage(relleno, PISTA_X, y, PISTA_X + fw, y + 10, 0, 0, fw, 10, null);
            boolean hover = arrastre == i || dentro(j.input.mx, j.input.my, PISTA_X + fw - 9, y - 5, 18, 20);
            imagen(g, Assets.img("opciones/sliders/perilla_" + (hover ? "hover" : "normal") + ".png"), PISTA_X + fw - 7, y - 4);
            j.fuente.draw(g, Math.round(v * 100) + "%", 372, y - 2, 2, tinta);
            if (fila == i) marcador(g, 198, y + 1);
        }
        if (j.audio.isSilencio()) imagen(g, Assets.img("opciones/sliders/casilla_on.png"), 96, 146);
        if (fila == 2) marcador(g, 84, 148);
    }

    /** Triangulito dorado que indica la fila seleccionada con el teclado. */
    private void marcador(Graphics2D g, int x, int y) {
        int[] xs = {x, x + 6, x}, ys = {y, y + 4, y + 8};
        g.setColor(new Color(0x2A1240));
        g.fillPolygon(new int[]{x - 1, x + 8, x - 1}, new int[]{y - 1, y + 4, y + 9}, 3);
        g.setColor(new Color(0xFFD23F));
        g.fillPolygon(xs, ys, 3);
    }
}
