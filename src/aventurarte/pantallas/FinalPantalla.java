package aventurarte.pantallas;

import aventurarte.core.*;
import aventurarte.modelo.Inventario;

import java.awt.*;
import java.awt.image.BufferedImage;

/** Fin del juego: muestra TODOS los objetos recolectados (llaves, monedas, tiempo, intentos). */
public class FinalPantalla extends Pantalla {
    private final boolean gano;
    private final Boton menu = new Boton("inventario/final/botones/boton_menu", 74, 240);
    private final Boton otraVez = new Boton("inventario/final/botones/boton_otra_vez", 270, 240);
    private final Boton.Lista lista;
    private float t;

    public FinalPantalla(Juego j, boolean gano) {
        super(j);
        this.gano = gano;
        lista = new Boton.Lista(menu, otraVez);
        lista.vertical = false;
        lista.sel = 1;
    }

    @Override public void alEntrar() {
        j.audio.pararMusica();
        j.audio.jingle(gano ? "fanfarria" : "sfx_perder");
    }

    @Override public void actualizar(float dt) {
        t += dt;
        if (t > 3f) j.audio.musica("musica_menu");
        int a = lista.mouse(j.input);
        int k;
        while ((k = j.input.sigPulsada()) != -1) {
            int r = lista.tecla(k);
            if (r != -1) a = r;
            if (k == java.awt.event.KeyEvent.VK_ESCAPE) a = 0;
        }
        if (a == 0) { j.audio.sfx("clic"); j.ir(new MenuPantalla(j)); }   // el historial de esta partida se puede seguir viendo
        else if (a == 1) {
            j.audio.sfx("clic");
            j.inv = new Inventario();                       // partida nueva
            j.ir(new MapaPantalla(j, -1));
        }
    }

    @Override public void dibujar(Graphics2D g) {
        Inventario.Snapshot s = j.inv.snapshot();
        imagen(g, Assets.img("inventario/final/fondo_final_480x270.png"), 0, 0);
        imagen(g, Assets.img("inventario/libro/fondo_libro_total_480x270.png"), 0, 0);
        HistorialPantalla.paginaTotal(j, g, s);
        BufferedImage cinta = Assets.img(gano ? "inventario/final/cinta_felicidades.png" : "hud/avisos/aviso_fin_del_juego.png");
        float a = Math.min(1f, t * 2);
        conAlpha(g, a, () -> imagen(g, cinta, (W - cinta.getWidth()) / 2, 0));
        lista.dibujar(g);
        if (gano) imagen(g, Assets.img("inventario/final/confeti_frente_480x270.png"), 0, 0);
    }
}
