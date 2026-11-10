package aventurarte.pantallas;

import aventurarte.core.*;
import aventurarte.modelo.PerfilJugador;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

/** Paso 1 del inicio: "COMO TE LLAMAS?" (el nombre del jugador). */
public class NombrePantalla extends Pantalla {
    private final BufferedImage fondo = Assets.img("usuario/nombre/fondo_nombre_480x270.png");
    private final BufferedImage gafete = Assets.img("usuario/nombre/gafete_300x136.png");
    private final BufferedImage cursor = Assets.img("usuario/nombre/cursor.png");
    private final BufferedImage aviso = Assets.img("usuario/nombre/aviso_escribe_tu_nombre.png");
    private final Boton.Lista lista = new Boton.Lista(new Boton("usuario/nombre/botones/boton_siguiente", 167, 212));
    private float t, temblor = -1, avisoT;
    private static final int[] SACUDIDA = {3, -3, 3, -3, 0};

    public NombrePantalla(Juego j) { super(j); }

    @Override public void alEntrar() { j.audio.musica("musica_menu"); }

    @Override public void actualizar(float dt) {
        t += dt;
        if (temblor >= 0) temblor += dt;
        if (avisoT > 0) avisoT -= dt;
        PerfilJugador p = j.perfil;
        char c;
        while ((c = j.input.sigEscrita()) != 0) p.setNombre(p.getNombre() + c);
        int accion = lista.mouse(j.input);
        int k;
        while ((k = j.input.sigPulsada()) != -1) {
            if (k == KeyEvent.VK_BACK_SPACE && !p.getNombre().isEmpty())
                p.setNombre(p.getNombre().substring(0, p.getNombre().length() - 1));
            else if (k == KeyEvent.VK_ENTER) accion = 0;
            else if (k == KeyEvent.VK_ESCAPE) { j.ir(new MenuPantalla(j)); return; }
        }
        if (accion == 0) {
            if (!p.nombreValido()) { temblor = 0; avisoT = 2f; j.audio.sfx("golpe"); }
            else { j.audio.sfx("clic"); j.ir(new AvatarPantalla(j)); }
        }
    }

    @Override public void dibujar(Graphics2D g) {
        imagen(g, fondo, 0, 0);
        int dx = 0;
        if (temblor >= 0) {
            int i = (int) (temblor / 0.04f);
            if (i < SACUDIDA.length) dx = SACUDIDA[i]; else temblor = -1;
        }
        imagen(g, gafete, 90 + dx, 58);
        String n = j.perfil.getNombre();
        Color sombra = new Color(0xC8C0D8), tinta = new Color(0x2A3A6A);
        int x = 116 + dx;
        if (n.isEmpty()) {
            j.fuente.draw(g, "ESCRIBE AQUI", x, 120, 3, sombra);
        } else {
            j.fuente.draw(g, n, x + 2, 122, 3, sombra);
            j.fuente.draw(g, n, x, 120, 3, tinta);
        }
        if ((int) (t * 2) % 2 == 0) {
            int cx = n.isEmpty() ? 111 + dx : x + PixelFont.width(n, 3) + 4;
            imagen(g, cursor, cx, 119);
        }
        if (avisoT > 0) conAlpha(g, Math.min(1f, avisoT * 2), () -> imagen(g, aviso, 160, 178));
        lista.dibujar(g);
    }
}
