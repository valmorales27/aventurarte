package aventurarte.juego;

import aventurarte.core.Pantalla;
import aventurarte.core.PixelFont;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * DETALLITOS DE AMBIENTE (puro adorno, no afectan el juego ni usan hilos nuevos; los mueve el bucle del juego):
 *   - mariposas que revolotean y se apartan cuando te acercas
 *   - un perrito que cruza la calle de vez en cuando (sin ladrar)
 *   - confeti al ganar (el papel picado colgado esta quitado: tapaba el escenario)
 *   - confeti cuando ganas el nivel
 */
public final class Ambiente {
    private static final Color[] PAPEL = {new Color(0xE8548A), new Color(0xFF9A2E), new Color(0xFFD23F), new Color(0x2FB8D8), new Color(0x48B838), new Color(0x8A3AB0)};
    private static final Color[] ALAS = {new Color(0xFF9A2E), new Color(0xFFD23F), new Color(0x6FC7FF), new Color(0xFF7AB0), new Color(0xF2F2FF)};
    private static final Color OSCURO = new Color(0x2A1240);

    // ------------------------------------------------------------------ mariposas
    private static final class Mariposa {
        float x, y, ax, ay, fase, vel;
        Color ala;
        boolean monarca;
    }

    private final List<Mariposa> mariposas = new ArrayList<>();
    private final int nivel, ancho;
    private final Random azar;

    // ------------------------------------------------------------------ perrito
    private boolean perroActivo, perroDerecha;
    private float perroX, perroT, perroLadra;
    private float proximoPerro;
    private boolean perroLadro;

    // ------------------------------------------------------------------ papel picado
    private final BufferedImage[] banderas = new BufferedImage[18];
    private float viento;                 // suavizado: 0 = brisa, ~1 = evento VIENTO
    private float vientoDir = 1;

    // ------------------------------------------------------------------ confeti
    private static final class Papelito {
        float x, y, vx, vy, fase, giro;
        Color c;
        int w, h;
    }

    private final List<Papelito> confeti = new ArrayList<>();
    private float confetiT = -1;

    public Ambiente(int nivel, int anchoMundo) {
        this.nivel = nivel;
        this.ancho = anchoMundo;
        this.azar = new Random(1234L + nivel * 77L);
        int cuantas = Math.max(6, anchoMundo / 380);
        for (int i = 0; i < cuantas; i++) {
            Mariposa m = new Mariposa();
            m.ax = 240 + (anchoMundo - 520) * (i + azar.nextFloat() * 0.6f) / cuantas;
            m.ay = 100 + azar.nextInt(75);
            m.x = m.ax; m.y = m.ay;
            m.fase = azar.nextFloat() * 6.28f;
            m.vel = 0.7f + azar.nextFloat() * 0.6f;
            m.monarca = nivel == 4;                                           // Metepec: monarcas (Dia de Muertos)
            m.ala = m.monarca ? ALAS[0] : ALAS[azar.nextInt(ALAS.length)];
            mariposas.add(m);
        }
        proximoPerro = 14 + azar.nextFloat() * 10;
        for (int i = 0; i < banderas.length; i++) banderas[i] = crearBandera(PAPEL[i % PAPEL.length], new Random(900 + i * 31L));
    }

    // ------------------------------------------------------------------ logica
    /** dt en segundos; viento = Mundo.viento() (0 o +-70); activo = el nivel esta en juego (el perrito solo sale ahi). */
    public void actualizar(float dt, float camX, float vientoMundo, float jugadorX, boolean activo) {
        // viento suavizado
        float objetivo = Math.min(1f, Math.abs(vientoMundo) / 70f);
        viento += (objetivo - viento) * Math.min(1f, 3f * dt);
        if (vientoMundo != 0) vientoDir = Math.signum(vientoMundo);

        for (Mariposa m : mariposas) {
            m.fase += dt * m.vel;
            m.x = m.ax + 34 * (float) Math.sin(m.fase * 0.9f) + vientoDir * viento * 12;
            m.y = m.ay + 12 * (float) Math.sin(m.fase * 2.3f) + 6 * (float) Math.sin(m.fase * 0.5f);
            // si te acercas mucho, se va volando hacia adelante y hacia arriba
            if (Math.abs(m.x - jugadorX) < 40 && m.y > 90 && jugadorX > 0) {
                m.ax += 150 + azar.nextInt(80);
                m.ay = 90 + azar.nextInt(75);
                if (m.ax > ancho - 120) m.ax = 200 + azar.nextInt(400);
            }
        }

        if (!activo) return;
        if (!perroActivo) {
            proximoPerro -= dt;
            if (proximoPerro <= 0) {
                perroActivo = true;
                perroDerecha = azar.nextBoolean();
                perroX = perroDerecha ? camX - 30 : camX + Pantalla.W + 30;
                perroT = 0; perroLadra = 0; perroLadro = false;
            }
        } else {
            perroT += dt;
            perroX += (perroDerecha ? 1 : -1) * 46f * dt;
            if (perroX < camX - 60 || perroX > camX + Pantalla.W + 60) {
                perroActivo = false;
                proximoPerro = 28 + azar.nextFloat() * 22;
            }
        }
    }

    public void iniciarConfeti() {
        confeti.clear();
        confetiT = 0;
        Random r = new Random();
        for (int i = 0; i < 150; i++) {                      // lluvia desde arriba
            Papelito p = nuevo(r);
            p.x = r.nextFloat() * Pantalla.W; p.y = -r.nextFloat() * 220;
            p.vx = (r.nextFloat() - 0.5f) * 20; p.vy = 40 + r.nextFloat() * 50;
            confeti.add(p);
        }
        for (int lado = 0; lado < 2; lado++)                  // dos "canones" desde las esquinas de abajo
            for (int i = 0; i < 45; i++) {
                Papelito p = nuevo(r);
                p.x = lado == 0 ? 6 : Pantalla.W - 6; p.y = 215;
                p.vx = (lado == 0 ? 1 : -1) * (30 + r.nextFloat() * 110);
                p.vy = -(110 + r.nextFloat() * 130);
                confeti.add(p);
            }
    }

    private Papelito nuevo(Random r) {
        Papelito p = new Papelito();
        p.c = PAPEL[r.nextInt(PAPEL.length)];
        p.w = 2 + r.nextInt(3); p.h = 2 + r.nextInt(3);
        p.fase = r.nextFloat() * 6.28f; p.giro = 4 + r.nextFloat() * 6;
        return p;
    }

    public void actualizarConfeti(float dt) {
        if (confetiT < 0) return;
        confetiT += dt;
        for (Papelito p : confeti) {
            p.fase += p.giro * dt;
            p.vy = Math.min(75, p.vy + 190 * dt);                    // gravedad con "paracaidas"
            if (p.vy > 75 && p.vx != 0) p.vx *= (1f - 1.5f * dt);
            p.x += (p.vx + 14 * (float) Math.sin(p.fase * 0.5f)) * dt;
            p.y += p.vy * dt;
            if (p.y > 275 && confetiT < 4f) { p.y = -8; p.x = (float) Math.random() * Pantalla.W; p.vx = 0; }   // sigue lloviendo un rato
        }
    }

    // ------------------------------------------------------------------ dibujo
    /** Detras del personaje: el perrito en la calle. */
    public void dibujarAtras(Graphics2D g, float camX, float t, PixelFont fuente) {
        if (!perroActivo) return;
        int sx = Math.round(perroX - camX), sy = 213;
        dibujarPerro(g, sx, sy, perroDerecha, perroT, nivel == 4);
    }

    /** Delante del personaje: mariposas y papel picado. */
    public void dibujarFrente(Graphics2D g, float camX, float t) {
        for (Mariposa m : mariposas) {
            int sx = Math.round(m.x - camX), sy = Math.round(m.y);
            if (sx < -10 || sx > Pantalla.W + 10) continue;
            dibujarMariposa(g, sx, sy, m.ala, (int) ((t * 9 + m.fase) % 2f) == 0, m.monarca);
        }
        // (el papel picado colgado se quito para no tapar el escenario; la pieza cuerda() queda por si quieres volver a ponerlo)
    }

    public void dibujarConfeti(Graphics2D g) {
        if (confetiT < 0) return;
        for (Papelito p : confeti) {
            if (p.y < -10 || p.y > 272) continue;
            boolean ancho = Math.sin(p.fase) > 0;
            g.setColor(p.c);
            g.fillRect(Math.round(p.x), Math.round(p.y), ancho ? p.w : p.h, ancho ? p.h : p.w);
        }
    }

    // ------------------------------------------------------------------ piezas
    private void cuerda(Graphics2D g, float desplaza, float t, int y0, int sag, int semilla) {
        final int tramo = 160, paso = 13;
        int primero = (int) Math.floor(desplaza / tramo);
        for (int k = primero; k <= primero + Pantalla.W / tramo + 1; k++) {
            float xIni = k * tramo - desplaza;
            // la cuerda cuelga en forma de curva entre dos puntos
            int anterior = -1;
            for (int i = 0; i <= tramo; i += 2) {
                float u = i / (float) tramo;
                int y = y0 + Math.round(sag * (1 - (2 * u - 1) * (2 * u - 1)));
                int x = Math.round(xIni + i);
                g.setColor(new Color(0x6A4A38));
                g.fillRect(x, y, 2, 1);
                anterior = y;
            }
            for (int i = 6; i < tramo - 4; i += paso) {
                float u = i / (float) tramo;
                int y = y0 + Math.round(sag * (1 - (2 * u - 1) * (2 * u - 1))) + 1;
                int x = Math.round(xIni + i);
                int idx = Math.floorMod(k * 7 + i / paso + semilla, banderas.length);
                // balanceo: brisa suave siempre + rafagas fuertes con el evento de viento
                float brisa = 1.2f * (float) Math.sin(t * 1.6f + idx * 0.9f + k);
                float rafaga = viento * vientoDir * (4f + 2.5f * (float) Math.sin(t * 7f + idx));
                dibujarBandera(g, banderas[idx], x - 6, y, brisa + rafaga, viento > 0.2f ? (int) (2 * Math.sin(t * 9 + idx)) : 0);
            }
        }
    }

    // cada fila de la banderita se corre un poco mas que la anterior (como si colgara de la cuerda y se meciera)
    private void dibujarBandera(Graphics2D g, BufferedImage img, int x, int y, float balanceo, int aleteo) {
        int h = img.getHeight(), w = img.getWidth();
        for (int fila = 0; fila < h; fila++) {
            float f = fila / (float) (h - 1);
            int dx = Math.round(balanceo * f * f * 2.2f);
            int dy = Math.round(Math.abs(balanceo) * f * 0.35f) - (aleteo != 0 && fila == h - 1 ? 1 : 0);
            g.drawImage(img, x + dx, y + fila - dy, x + dx + w, y + fila - dy + 1, 0, fila, w, fila + 1, null);
        }
    }

    private static BufferedImage crearBandera(Color c, Random r) {
        int w = 12, h = 14;
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Color oscuro = c.darker();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                boolean borde = y == 0;
                // orilla de abajo en "dientes" (el corte tipico del papel picado)
                if (y >= h - 2 && x % 3 == 1) continue;
                im.setRGB(x, y, (borde ? oscuro : c).getRGB());
            }
        // recortes: rombito central, circulitos y picos
        int cx = 5 + r.nextInt(2), cy = 6;
        int[][] rombo = {{0, -2}, {-1, -1}, {0, -1}, {1, -1}, {-2, 0}, {-1, 0}, {0, 0}, {1, 0}, {2, 0}, {-1, 1}, {0, 1}, {1, 1}, {0, 2}};
        for (int[] q : rombo) if (cx + q[0] >= 0 && cx + q[0] < w) im.setRGB(cx + q[0], cy + q[1], 0);
        for (int i = 0; i < 4; i++) {
            int x = 1 + (i % 2) * 9, y = 3 + (i / 2) * 7;
            if (x < w && y < h) im.setRGB(x, y, 0);
        }
        return im;
    }

    private static void dibujarMariposa(Graphics2D g, int x, int y, Color ala, boolean abiertas, boolean monarca) {
        Color borde = monarca ? new Color(0x2A1240) : ala.darker().darker();
        g.setColor(borde);
        g.fillRect(x, y - 2, 1, 5);                                                   // cuerpo
        if (abiertas) {
            g.setColor(borde);
            g.fillRect(x - 5, y - 3, 5, 5); g.fillRect(x + 1, y - 3, 5, 5);           // contorno alas de arriba
            g.fillRect(x - 4, y + 2, 3, 2); g.fillRect(x + 2, y + 2, 3, 2);
            g.setColor(ala);
            g.fillRect(x - 4, y - 2, 4, 3); g.fillRect(x + 1, y - 2, 4, 3);
            g.fillRect(x - 3, y + 2, 2, 1); g.fillRect(x + 2, y + 2, 2, 1);
            g.setColor(Color.WHITE);
            g.fillRect(x - 3, y - 1, 1, 1); g.fillRect(x + 3, y - 1, 1, 1);
        } else {
            g.setColor(borde);
            g.fillRect(x - 2, y - 3, 2, 5); g.fillRect(x + 1, y - 3, 2, 5);
            g.setColor(ala);
            g.fillRect(x - 1, y - 2, 1, 3); g.fillRect(x + 1, y - 2, 1, 3);
        }
    }

    private static void dibujarPerro(Graphics2D g, int x, int pie, boolean derecha, float t, boolean xolo) {
        Color cuerpo = xolo ? new Color(0x5A5F7A) : new Color(0xC27A3E), oscuro = xolo ? new Color(0x3A3E55) : new Color(0x7A4A22);
        Color blanco = xolo ? new Color(0x8D93B5) : new Color(0xFFF1D6), tinta = new Color(0x2A1608);
        AffineTransformFlip f = new AffineTransformFlip(g, x + 10, derecha);
        int paso = (int) (t * 8) % 2;
        int y = pie - 12;
        // contorno
        g.setColor(tinta);
        g.fillRect(x + 2, y + 3, 13, 7);                       // cuerpo
        g.fillRect(x + 12, y, 7, 8);                           // cabeza
        g.fillRect(x + 18, y + 3, 3, 4);                       // hocico
        g.fillRect(x + 11, y - 1, 3, 5);                       // oreja
        g.fillRect(x, y + 1 + (paso == 0 ? 0 : 1), 3, 3);      // cola
        // cuerpo
        g.setColor(cuerpo);
        g.fillRect(x + 3, y + 4, 11, 5);
        g.fillRect(x + 13, y + 1, 5, 6);
        g.setColor(blanco);
        g.fillRect(x + 18, y + 4, 2, 2);                       // hocico claro
        g.fillRect(x + 4, y + 7, 5, 2);                        // pancita
        g.setColor(oscuro);
        g.fillRect(x + 12, y, 2, 4);                           // oreja
        g.fillRect(x + 6, y + 4, 3, 2);                        // mancha
        g.setColor(tinta);
        g.fillRect(x + 19, y + 3, 1, 1);                       // nariz
        g.fillRect(x + 16, y + 2, 1, 1);                       // ojo
        g.setColor(new Color(0xE8607A));
        g.fillRect(x + 18, y + 6, 1, 1);                       // lengueta
        // patas (se alternan al caminar)
        g.setColor(tinta);
        int a = paso == 0 ? 0 : 1, b = paso == 0 ? 1 : 0;
        g.fillRect(x + 3 + a, y + 9, 3, 3); g.fillRect(x + 7 - a, y + 9, 3, 3);
        g.fillRect(x + 11 + b, y + 9, 3, 3); g.fillRect(x + 14 - b, y + 9, 3, 3);
        g.setColor(cuerpo);
        g.fillRect(x + 4 + a, y + 9, 1, 2); g.fillRect(x + 8 - a, y + 9, 1, 2);
        g.fillRect(x + 12 + b, y + 9, 1, 2); g.fillRect(x + 15 - b, y + 9, 1, 2);
        f.restaurar();
    }

    /** Voltea el dibujo horizontalmente (para que el perrito mire hacia donde camina). */
    private static final class AffineTransformFlip {
        private final Graphics2D g;
        private final java.awt.geom.AffineTransform antes;
        AffineTransformFlip(Graphics2D g, int centro, boolean derecha) {
            this.g = g;
            this.antes = g.getTransform();
            if (!derecha) { g.translate(2 * centro, 0); g.scale(-1, 1); }
        }
        void restaurar() { g.setTransform(antes); }
    }
}
