package aventurarte.juego;

import aventurarte.core.Assets;
import aventurarte.core.Audio;
import aventurarte.modelo.PerfilJugador;

import java.awt.image.BufferedImage;

/**
 * El personaje del jugador.  x = centro de los pies, y = linea de los pies (214 sobre el suelo).
 * Solo el hilo del juego lo modifica; los hilos de enemigos solo leen x, y (volatile).
 */
public final class Jugador {
    public static final float VEL = 130f, SALTO = 400f, GRAVEDAD = 900f;
    public static final int MEDIO_ANCHO = 9, ALTO = 60;         // caja de choque: 18 x 60 sobre los pies
    public static final int PISO = 214;

    public volatile float x = 70, y = PISO;
    public float vx, vy;
    public boolean enSuelo = true, derecha = true;
    public float invulnerable, golpeT, lanzarT = -1, enfriamiento, animT;
    public boolean victoria, derrotado;
    private boolean lanzoProyectil;
    private final BufferedImage hoja;
    private final String idPersonaje;
    private final int manoX, manoY;

    public Jugador(PerfilJugador perfil) {
        hoja = Assets.img(perfil.rutaHojaPerfil());
        idPersonaje = perfil.id();
        manoX = perfil.getPersonaje() == 0 ? 38 : 37;
        manoY = 39;
    }

    /** Dibuja/colisiona: la caja de choque. */
    public float izq() { return x - MEDIO_ANCHO; }
    public float der() { return x + MEDIO_ANCHO; }
    public float arriba() { return y - ALTO; }

    public boolean puedeMoverse() { return !victoria && !derrotado; }

    /** Un cuadro de fisica + animacion. mult = multiplicador de velocidad de los eventos; viento = empuje en px/s. */
    public void actualizar(float dt, boolean izqP, boolean derP, boolean saltarP, boolean lanzarP, Mundo m, float mult, float viento) {
        animT += dt;
        if (invulnerable > 0) invulnerable -= dt;
        if (golpeT > 0) golpeT -= dt;
        if (enfriamiento > 0) enfriamiento -= dt;
        if (lanzarT >= 0) { lanzarT += dt; if (lanzarT > 4 * 0.09f) lanzarT = -1; }

        float prevY = y;
        // ---------- horizontal
        int dir = 0;
        if (puedeMoverse()) { if (izqP) dir--; if (derP) dir++; }
        vx = dir * VEL * mult + (puedeMoverse() ? viento : 0);
        if (dir != 0) derecha = dir > 0;
        x += vx * dt;
        x = Math.max(12, Math.min(m.d.ancho - 12, x));
        for (Mundo.Obstaculo o : m.obstaculos) {
            if (solapaX(o.izq(), o.der()) && y > o.arriba() + 6 && arriba() < o.abajo()) {
                if (vx > 0 || (vx == 0 && x < o.centro())) x = o.izq() - MEDIO_ANCHO;
                else x = o.der() + MEDIO_ANCHO;
            }
        }
        // ---------- salto
        if (saltarP && enSuelo && puedeMoverse()) {
            vy = -SALTO; enSuelo = false;
            m.audio.sfx("salto");
        }
        // ---------- vertical
        vy += GRAVEDAD * dt;
        y += vy * dt;
        enSuelo = false;
        if (y >= PISO) { y = PISO; vy = 0; enSuelo = true; }
        if (vy >= 0) {
            for (Mundo.Obstaculo o : m.obstaculos) aterrizar(o.izq(), o.der(), o.arriba(), prevY);
            for (Mundo.Plataforma p : m.plataformas) aterrizar(p.x, p.x + p.w, p.y + 1, prevY);
        }
        // ---------- lanzar
        if (lanzarP && enfriamiento <= 0 && lanzarT < 0 && puedeMoverse()) {
            lanzarT = 0; enfriamiento = 0.55f; lanzoProyectil = false;
            m.audio.sfx("lanzar");
        }
        if (lanzarT >= 0.09f * 2 && !lanzoProyectil) {          // en el cuadro 2 "suelta" la mano
            lanzoProyectil = true;
            float sx = x - 20, sy = y - 72;
            float px = derecha ? sx + manoX : sx + (39 - manoX);
            m.proyectiles.add(new Proyectil(px, sy + manoY, derecha ? 260 : -260, true, "personajes/" + idPersonaje + "/proyectil", 300));
        }
    }

    private boolean solapaX(float a, float b) { return der() > a && izq() < b; }

    private void aterrizar(float a, float b, float top, float prevY) {
        if (solapaX(a, b) && prevY <= top + 2 && y >= top) { y = top; vy = 0; enSuelo = true; }
    }

    /** Daño: parpadea 1.5 s sin poder ser golpeado de nuevo. */
    public void golpeado() { invulnerable = 1.5f; golpeT = 0.3f; lanzarT = -1; }

    public void rebotar() { vy = -280; enSuelo = false; }

    // ---------------------------------------------------------------- sprite
    private static final int[] REPOSO = {0, 1, 0, 2};
    private static final float[] REPOSO_T = {0.6f, 0.6f, 0.6f, 0.15f};

    public BufferedImage frame() {
        int fila, col;
        if (derrotado) { fila = 6; col = Math.min(1, (int) (animT / 0.4f)); }
        else if (victoria) { fila = 5; col = (int) (animT / 0.2f) % 3; }
        else if (golpeT > 0) { fila = 4; col = (int) ((0.3f - golpeT) / 0.15f) % 2; }
        else if (lanzarT >= 0) { fila = 3; col = Math.min(3, (int) (lanzarT / 0.09f)); }
        else if (!enSuelo) { fila = 2; col = vy < -120 ? 0 : vy > 120 ? 2 : 1; }
        else if (Math.abs(vx) > 5) { fila = 1; col = (int) (animT / 0.1f) % 6; }
        else {
            fila = 0;
            float ciclo = 0; for (float d : REPOSO_T) ciclo += d;
            float tt = animT % ciclo; int i = 0;
            while (tt >= REPOSO_T[i]) { tt -= REPOSO_T[i]; i++; }
            col = REPOSO[i];
        }
        return hoja.getSubimage(col * 40, fila * 73, 40, 73);
    }
}
