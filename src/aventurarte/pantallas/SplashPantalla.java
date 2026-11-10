package aventurarte.pantallas;

import aventurarte.core.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Random;

/** Pantalla de inicio: aparece el logo con un fundido, brillitos, y parpadea "PRESIONA ENTER". */
public class SplashPantalla extends Pantalla {
    private final BufferedImage splash = Assets.img("menu/splash/splash_480x270.png");
    private final BufferedImage texto = Assets.img("menu/splash/texto_presiona_enter_480x270.png");
    private final BufferedImage[] destello = new BufferedImage[4];
    private float t;
    private final Random rnd = new Random(5);
    private final int[][] chispas = new int[5][3];    // x, y, fase inicial

    public SplashPantalla(Juego j) {
        super(j);
        for (int i = 0; i < 4; i++) destello[i] = Assets.img("menu/splash/destello_" + i + ".png");
        for (int[] c : chispas) { c[0] = 40 + rnd.nextInt(400); c[1] = 40 + rnd.nextInt(130); c[2] = rnd.nextInt(40); }
    }

    @Override public void alEntrar() { j.audio.musica("musica_menu"); }

    @Override public void actualizar(float dt) {
        t += dt;
        int k;
        while ((k = j.input.sigPulsada()) != -1)
            if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) siguiente();
        if (j.input.sigClic() != null) siguiente();
        if (t > 12) siguiente();
    }

    private void siguiente() { j.audio.sfx("clic"); j.ir(new MenuPantalla(j)); }

    @Override public void dibujar(Graphics2D g) {
        g.setColor(new Color(0x14102A));
        g.fillRect(0, 0, W, H);
        conAlpha(g, t / 1.0f, () -> imagen(g, splash, 0, 0));
        if (t > 1.2f) {
            // brillitos: cada uno cicla por sus 4 cuadros de 100 ms y se mueve de lugar cada ciclo
            for (int[] c : chispas) {
                int f = ((int) (t * 10) + c[2]) % 40;
                if (f < 4) {
                    int ciclo = (int) (t * 10 + c[2]) / 40;
                    int x = 30 + (c[0] * (ciclo + 3) * 7) % 420, y = 30 + (c[1] * (ciclo + 5) * 11) % 130;
                    imagen(g, destello[f], x, y);
                }
            }
            if ((t * 1000) % 1100 < 700) imagen(g, texto, 0, 0);
        }
    }
}
