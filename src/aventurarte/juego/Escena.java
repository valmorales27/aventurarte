package aventurarte.juego;

import aventurarte.core.Assets;
import aventurarte.core.Pantalla;
import aventurarte.modelo.DatosNivel;

import java.awt.*;
import java.awt.image.BufferedImage;

/** Dibuja el escenario de un nivel: cielo que cambia, nubes, montañas, suelo, props, plataformas, obstaculos, monedas, llave y puerta. */
public final class Escena {
    private final DatosNivel d;
    private final BufferedImage[] cielo = new BufferedImage[3], nubes = new BufferedImage[3];
    private final BufferedImage lejos, medio, suelo, cerca, frente, llave, puertaC, puertaA, pIzq, pMedio, pDer;
    private final BufferedImage[] moneda = new BufferedImage[4];

    public Escena(DatosNivel d) {
        this.d = d;
        String b = "niveles/" + (d.numero + 1) + "/";
        for (int i = 0; i < 3; i++) {
            cielo[i] = Assets.img(b + "cielo_" + i + ".png");
            nubes[i] = Assets.existe(b + "nubes_" + i + ".png") ? Assets.img(b + "nubes_" + i + ".png") : null;
        }
        lejos = Assets.img(b + "lejos.png");
        medio = Assets.img(b + "medio.png");
        suelo = Assets.img(b + "suelo.png");
        cerca = Assets.img(b + "cerca.png");
        frente = Assets.existe(b + "frente.png") ? Assets.img(b + "frente.png") : null;
        llave = Assets.img(b + "llave.png");
        puertaC = Assets.img(b + "puerta_cerrada.png");
        puertaA = Assets.img(b + "puerta_abierta.png");
        pIzq = Assets.img(b + "plataforma_izq.png");
        pMedio = Assets.img(b + "plataforma_medio.png");
        pDer = Assets.img(b + "plataforma_der.png");
        for (int i = 0; i < 4; i++) moneda[i] = Assets.img(b + "moneda_" + i + ".png");
    }

    public BufferedImage monedaFrame(float t) { return moneda[(int) (t / 0.12f) % 4]; }

    public boolean tieneFrente() { return frente != null; }

    private static void tile(Graphics2D g, BufferedImage im, double desplazamiento, int y) {
        int w = im.getWidth();
        int off = (int) (desplazamiento % w);
        if (off < 0) off += w;
        for (int x = -off; x < Pantalla.W; x += w) g.drawImage(im, x, y, null);
    }

    private static void fundido(Graphics2D g, BufferedImage a, BufferedImage b, float t, int y) {
        g.drawImage(a, 0, y, null);
        Composite c = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0, Math.min(1, t))));
        g.drawImage(b, 0, y, null);
        g.setComposite(c);
    }

    /** Capas del FONDO (detras del personaje).  p = tiempo transcurrido / 180 (0..1). */
    public void dibujarFondo(Graphics2D g, double camX, float p, float t) {
        p = Math.max(0, Math.min(1, p));
        BufferedImage ca = p < 0.5f ? cielo[0] : cielo[1], cb = p < 0.5f ? cielo[1] : cielo[2];
        float f = (p < 0.5f ? p : p - 0.5f) * 2f;
        fundido(g, ca, cb, f, 0);
        // nubes: si no existe nubes_1 se usa nubes_0 en la primera mitad
        BufferedImage n0 = nubes[0], n1 = nubes[1] != null ? nubes[1] : nubes[0], n2 = nubes[2] != null ? nubes[2] : n1;
        BufferedImage na = p < 0.5f ? n0 : n1, nb = p < 0.5f ? n1 : n2;
        int wn = na.getWidth();
        int off = (int) ((camX * 0.10 + t * 4) % wn);
        for (int x = -off; x < Pantalla.W; x += wn) {
            g.drawImage(na, x, 30, null);
            Composite c = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, f));
            g.drawImage(nb, x, 30, null);
            g.setComposite(c);
        }
        tile(g, lejos, camX * 0.25, 0);
        tile(g, medio, camX * 0.55 + d.medioDesplazamiento, 0);
        tile(g, suelo, camX, 206);
        tile(g, cerca, camX, 0);
    }

    /** Plataformas, obstaculos, monedas, llave y puerta. */
    public void dibujarObjetos(Graphics2D g, Mundo m, double camX, float t) {
        int cx = (int) camX;
        for (Mundo.Plataforma p : m.plataformas) {
            int sx = p.x - cx;
            if (sx > Pantalla.W + 20 || sx + p.w < -20) continue;
            for (int i = 0; i < p.bloques; i++) {
                BufferedImage im = i == 0 ? pIzq : i == p.bloques - 1 ? pDer : pMedio;
                g.drawImage(im, sx + 16 * i, p.y, null);
            }
        }
        for (Mundo.Obstaculo o : m.obstaculos) {
            int sx = o.x - cx;
            if (sx > Pantalla.W + 20 || sx + o.w < -20) continue;
            g.drawImage(o.img, sx, o.y, null);
        }
        // puerta (borde de abajo en y = 218)
        BufferedImage pu = m.puertaAbierta ? puertaA : puertaC;
        g.drawImage(pu, d.puertaX - cx, 218 - pu.getHeight(), null);
        BufferedImage mon = monedaFrame(t);
        for (Mundo.Moneda c : m.monedas) {
            if (c.tomada) continue;
            int sx = c.x - cx;
            if (sx > Pantalla.W + 20 || sx < -20) continue;
            g.drawImage(mon, sx, c.y, null);
        }
        if (!m.llaveTomada) {
            int bob = Math.round((float) Math.sin(t * 3.5f) * 3);
            g.drawImage(llave, d.llaveX - cx, d.llaveY + bob, null);
        }
    }

    /** Capa que pasa por DELANTE del personaje (banderitas, ramas, papel picado). */
    public void dibujarFrente(Graphics2D g, double camX) {
        if (frente != null) tile(g, frente, camX, 0);
    }

    public int llaveAncho() { return llave.getWidth(); }
    public int llaveAlto() { return llave.getHeight(); }
    public int puertaAncho() { return puertaC.getWidth(); }
    public int puertaAlto() { return puertaC.getHeight(); }
}
