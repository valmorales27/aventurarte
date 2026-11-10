package aventurarte.juego;

import aventurarte.core.Assets;

import java.awt.image.BufferedImage;

/** Un proyectil en vuelo (barro/tamal/suela/gancho del jugador, o el bloque/ovillo/chile/hueso de un enemigo). */
public final class Proyectil {
    public volatile float x, y;              // centro
    public final float vx;
    public final boolean delJugador;
    private final String carpeta;            // ruta (en res/img) donde estan vuelo_N.png e impacto_N.png
    private final BufferedImage[] vuelo = new BufferedImage[4], impacto = new BufferedImage[4];
    private float t, impT, recorrido;
    private final float alcance;
    public volatile boolean enImpacto, muerto;

    public Proyectil(float x, float y, float vx, boolean delJugador, String carpeta, float alcance) {
        this.x = x; this.y = y; this.vx = vx; this.delJugador = delJugador; this.carpeta = carpeta; this.alcance = alcance;
        for (int i = 0; i < 4; i++) {
            vuelo[i] = Assets.img(carpeta + "/vuelo_" + i + ".png");
            impacto[i] = Assets.img(carpeta + "/impacto_" + i + ".png");
        }
    }

    public void actualizar(float dt) {
        if (enImpacto) {
            impT += dt;
            if (impT >= 4 * 0.08f) muerto = true;
            return;
        }
        t += dt;
        float dx = vx * dt;
        x += dx;
        recorrido += Math.abs(dx);
        if (recorrido > alcance) muerto = true;
    }

    /** Llama cuando choca: pasa a la animacion de impacto y deja de hacer daño. */
    public void estrellar() { if (!enImpacto) { enImpacto = true; impT = 0; } }

    public BufferedImage frame() {
        return enImpacto ? impacto[Math.min(3, (int) (impT / 0.08f))] : vuelo[(int) (t / 0.08f) % 4];
    }

    // caja de choque (mas chica que la imagen)
    public float cx() { return x; }
    public float cy() { return y; }
    public float radio() { return 6; }
}
