package aventurarte.pantallas;

import aventurarte.core.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

/** Menu principal: JUGAR, HISTORIAL, OPCIONES, SALIR. */
public class MenuPantalla extends Pantalla {
    private final BufferedImage fondo = Assets.img("menu/menu/fondo_480x270.png");
    private final BufferedImage logo = Assets.img("menu/logo/logo_pequeno.png");
    private final BufferedImage poste = Assets.img("menu/menu/poste.png");
    private final Boton.Lista lista;

    public MenuPantalla(Juego j) {
        super(j);
        String[] n = {"jugar", "historial", "opciones", "salir"};
        int[][] pos = {{310, 126}, {313, 158}, {310, 190}, {313, 222}};
        Boton[] b = new Boton[4];
        for (int i = 0; i < 4; i++) b[i] = new Boton("menu/menu/botones/boton_" + n[i], pos[i][0], pos[i][1], 0, -4, 1);
        lista = new Boton.Lista(b);
    }

    @Override public void alEntrar() { j.audio.musica("musica_menu"); }
    @Override public void alRecuperarFoco() { j.audio.musica("musica_menu"); }

    @Override public void actualizar(float dt) {
        int accion = lista.mouse(j.input);
        int k;
        while ((k = j.input.sigPulsada()) != -1) {
            int a = lista.tecla(k);
            if (a != -1) accion = a;
        }
        if (accion != -1) elegir(accion);
    }

    private void elegir(int i) {
        j.audio.sfx("clic");
        switch (i) {
            case 0: j.nuevaPartidaSiHaceFalta(); j.ir(new NombrePantalla(j)); break;
            case 1: j.apilar(new HistorialPantalla(j, null)); break;
            case 2: j.apilar(new OpcionesPantalla(j)); break;
            default: j.salir = true;
        }
    }

    @Override public void dibujar(Graphics2D g) {
        imagen(g, fondo, 0, 0);
        imagen(g, logo, 10, 22);
        imagen(g, poste, 454, 120);
        lista.dibujar(g);
    }
}
