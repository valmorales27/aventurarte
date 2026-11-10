package aventurarte.pantallas;

import aventurarte.core.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;

/** Menu de pausa: REANUDAR / REINICIAR / OPCIONES / SALIR (con dialogo de confirmacion). */
public class PausaPantalla extends Pantalla {
    private final NivelPantalla nivel;
    private final Boton.Lista lista;
    private final Boton.Lista dialogo;
    private int confirmar = 0;            // 0 nada, 1 reiniciar, 2 salir

    public PausaPantalla(Juego j, NivelPantalla nivel) {
        super(j);
        this.nivel = nivel;
        String[] n = {"reanudar", "reiniciar", "opciones", "salir"};
        int[] ys = {93, 127, 161, 195};
        Boton[] b = new Boton[4];
        for (int i = 0; i < 4; i++) b[i] = new Boton("pausa/botones/boton_" + n[i], 160, ys[i]);
        lista = new Boton.Lista(b);
        dialogo = new Boton.Lista(new Boton("pausa/botones/boton_si", 155, 137), new Boton("pausa/botones/boton_no", 249, 137));
        dialogo.vertical = false;
        dialogo.sel = 1;
    }

    @Override public void alEntrar() { j.audio.sfx("pausa"); }

    @Override public void actualizar(float dt) {
        if (confirmar != 0) { actualizarDialogo(); return; }
        int a = lista.mouse(j.input);
        int k;
        while ((k = j.input.sigPulsada()) != -1) {
            if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_P) { a = 0; break; }
            int r = lista.tecla(k);
            if (r != -1) a = r;
        }
        if (a == -1) return;
        j.audio.sfx("clic");
        switch (a) {
            case 0: j.cerrar(); break;                         // el nivel se reanuda en alRecuperarFoco()
            case 1: confirmar = 1; dialogo.sel = 1; j.input.limpiarEventos(); break;
            case 2: j.apilar(new OpcionesPantalla(j)); break;
            default: confirmar = 2; dialogo.sel = 1; j.input.limpiarEventos();
        }
    }

    private void actualizarDialogo() {
        int a = dialogo.mouse(j.input);
        int k;
        while ((k = j.input.sigPulsada()) != -1) {
            if (k == KeyEvent.VK_ESCAPE) { a = 1; break; }
            int r = dialogo.tecla(k);
            if (r != -1) a = r;
        }
        if (a == -1) return;
        j.audio.sfx("clic");
        if (a == 1) { confirmar = 0; return; }
        int que = confirmar;
        confirmar = 0;
        if (que == 1) { j.cerrar(); nivel.reiniciar(); }         // cierra la pausa y reinicia el nivel
        else nivel.salirAlMapa();
    }

    @Override public void dibujar(Graphics2D g) {
        imagen(g, Assets.img("pausa/oscurecer_480x270.png"), 0, 0);
        BufferedImage panel = Assets.img("pausa/panel_pausa.png");
        imagen(g, panel, 146, 26);
        String sub = "NIVEL " + (nivel.numero() + 1) + " " + MapaPantalla.NOMBRES[nivel.numero()];
        j.fuente.draw(g, sub, 146 + panel.getWidth() / 2 - PixelFont.width(sub, 1) / 2, 78, 1, new Color(0x2A3A6A));
        lista.dibujar(g);
        if (confirmar != 0) {
            g.setColor(new Color(0, 0, 0, 90));
            g.fillRect(0, 0, W, H);
            imagen(g, Assets.img(confirmar == 1 ? "pausa/dialogo_reiniciar.png" : "pausa/dialogo_salir.png"), 138, 88);
            dialogo.dibujar(g);
        }
    }
}
