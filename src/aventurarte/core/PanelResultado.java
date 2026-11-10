package aventurarte.core;

import aventurarte.modelo.Estrellas;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;

/** Vnetana de victoria, muestra las 3 estrellas Solo se dibuja, la lógica (clics, teclas, siguiente pantalla) está NivelPantalla
 */
public final class PanelResultado {
    /** Variables para dibujar la ventana. NivelPantalla lo llena */
    public static final class Datos {
        public boolean[] ganadas = new boolean[3];     // estrellas que se ganaron
        public int mostradas;                           // cuantas ya se "encendieron"
        public float t, anim;                           // segundos desde que empezo la victoria / reloj general
        public String[] valores = {"", "", ""};         // texto de la derecha de cada placa
        public int tiempo, mejorTiempo;
        public boolean hayRecord;                       // mostrar nuevo record
        public boolean botonesActivos;                  // los botones se pueden usar
        public int mx, my;
        public boolean mouseAbajo;
    }

    // (coordenadas logicas 480x270)
    public static final int PX = 106, PY = 114, PW = 268, PH = 108;
    // dos botones (mismo estilo del botón JUGAR) debajo de la tabla
    private final Boton btnRepetir = new Boton("mapa/ui/botones/boton_repetir", 114, 228);
    private final Boton btnSeguir = new Boton("hud/botones/boton_continuar", 234, 228);

    private static final Color OSCURO = new Color(0x2A1240), BORDE = new Color(0x5B3A94), BORDE_CLARO = new Color(0xC9AEF2);
    private static final Color CREMA = new Color(0xFFF8E8), VERDE = new Color(0x4CC46A), GRIS = new Color(0xB9A6D6), ORO = new Color(0xFFD23F);

    public boolean sobreRepetir(int x, int y) { return btnRepetir.contiene(x, y); }
    public boolean sobreSeguir(int x, int y) { return btnSeguir.contiene(x, y); }

    private final PixelFont fuente;

    public PanelResultado(PixelFont fuente) { this.fuente = fuente; }

    // ------------------------------------------------------------------ DIBUJO
    public void dibujar(Graphics2D g, Datos d) {
        if (d.t < 0.6f) return;
        float alfa = Math.min(1f, (d.t - 0.6f) / 0.3f);
        Composite previo = g.getComposite();
        g.setComposite(AlphaComposite.SrcOver.derive(alfa));

        brillo(g, d);
        tablita(g);
        titulo(g);
        filas(g, d);
        estrellasGrandes(g, d);
        if (d.hayRecord) recordNuevo(g, d);
        botones(g, d);

        g.setComposite(previo);
    }

    // sombra de luz detrás de la tabla
    private void brillo(Graphics2D g, Datos d) {
        float pulso = 0.75f + 0.25f * (float) Math.sin(d.anim * 2.5);
        Paint antes = g.getPaint();
        g.setPaint(new RadialGradientPaint(PX + PW / 2f, PY + PH / 2f - 10, 170,
                new float[]{0f, 0.55f, 1f},
                new Color[]{new Color(1f, 0.85f, 1f, 0.7f * pulso), new Color(0.75f, 0.5f, 1f, 0.32f * pulso), new Color(0.6f, 0.3f, 1f, 0f)}));
        g.fillRect(0, 0, 480, 270);
        g.setPaint(antes);
    }

    // diseño del contorno: oscuro, borde violeta, relleno lila con degradado
    private void tablita(Graphics2D g) {
        int x = PX, y = PY, w = PW, h = PH;
        g.setColor(new Color(0, 0, 0, 80));     // sombra
        g.fillRect(x + 3, y + h, w - 4, 3);
        capa(g, OSCURO, x, y, w, h, 3);
        capa(g, BORDE, x + 2, y + 2, w - 4, h - 4, 2);
        // relleno con degradado vertical (arriba mas claro)
        for (int i = 0; i < h - 8; i++) {
            float f = i / (float) (h - 9);
            g.setColor(mezcla(new Color(0xA987E2), new Color(0x7E58C0), f));
            int inset = i < 2 || i > h - 11 ? 1 : 0;
            g.fillRect(x + 4 + inset, y + 4 + i, w - 8 - 2 * inset, 1);
        }
        // luz arriba/izquierda y sombra abajo/derecha
        g.setColor(BORDE_CLARO);
        g.fillRect(x + 5, y + 4, w - 10, 1);
        g.fillRect(x + 4, y + 5, 1, h - 12);
        g.setColor(new Color(0x5B3A94));
        g.fillRect(x + 5, y + h - 5, w - 10, 1);
        g.fillRect(x + w - 5, y + 6, 1, h - 12);
        // deseño de manchas de piedra
        int sem = 7;
        for (int i = 0; i < 46; i++) {
            sem = (sem * 1103515245 + 12345) & 0x7fffffff;
            int px = x + 7 + (sem >> 4) % (w - 14);
            sem = (sem * 1103515245 + 12345) & 0x7fffffff;
            int py = y + 7 + (sem >> 4) % (h - 14);
            g.setColor((i & 1) == 0 ? new Color(255, 255, 255, 38) : new Color(60, 20, 110, 40));
            g.fillRect(px, py, 2, 1);
        }
        // grietas en las esquinas
        g.setColor(new Color(0x6A45A6));
        int[][] gr = {{x + 6, y + 14, x + 10, y + 18, x + 9, y + 24, x + 13, y + 28},
                {x + w - 6, y + h - 14, x + w - 11, y + h - 19, x + w - 10, y + h - 25, x + w - 15, y + h - 29}};
        for (int[] c : gr) for (int i = 0; i + 3 < c.length; i += 2) g.drawLine(c[i], c[i + 1], c[i + 2], c[i + 3]);
    }

    private void titulo(Graphics2D g) {
        String t = "¡VICTORIA!";
        fuente.drawOutlined(g, t, PX + (PW - PixelFont.width(t, 3)) / 2, PY + 20, 3, CREMA, new Color(0x3A1E66));
    }

    // una placa por cada estrella
    private void filas(Graphics2D g, Datos d) {
        int x0 = PX + 18, w = PW - 36;
        for (int i = 0; i < 3; i++) {
            int y = PY + 46 + i * 14;
            boolean ok = d.ganadas[i];
            plaqueta(g, x0, y, w, 12, ok ? new Color(0x5E3C9C) : new Color(0x6A4BA3));
            // circulo con una paloma (ganada) o rayita (no ganada)
            int cx = x0 + 8, cy = y + 6;
            g.setColor(OSCURO);
            g.fill(new Ellipse2D.Float(cx - 5, cy - 5, 10, 10));
            g.setColor(ok ? VERDE : new Color(0x8873AA));
            g.fill(new Ellipse2D.Float(cx - 4, cy - 4, 8, 8));
            g.setColor(Color.WHITE);
            if (ok) {
                g.drawLine(cx - 2, cy, cx - 1, cy + 1);
                g.drawLine(cx - 1, cy + 1, cx + 2, cy - 2);
            } else {
                g.fillRect(cx - 2, cy, 5, 1);
            }
            Color texto = ok ? CREMA : GRIS;
            fuente.draw(g, Estrellas.TEXTOS[i], x0 + 18, y + 3, 1, texto);
            if (!d.valores[i].isEmpty())
                fuente.draw(g, d.valores[i], x0 + w - 22 - PixelFont.width(d.valores[i], 1), y + 3, 1, ok ? new Color(0xFFE680) : GRIS);
            estrellita(g, x0 + w - 12, cy, ok && d.mostradas > i);
        }
        // tiempo y mejor tiempo
        int y = PY + 90;
        plaqueta(g, x0, y, w, 12, new Color(0x5E3C9C));
        g.drawImage(Assets.img("hud/iconos/reloj.png"), x0 + 4, y - 2, null);
        fuente.draw(g, "TIEMPO " + Util.mmss(d.tiempo), x0 + 24, y + 3, 1, CREMA);
        if (d.mejorTiempo > 0) {
            String m = "MEJOR " + Util.mmss(d.mejorTiempo);
            fuente.draw(g, m, x0 + w - 8 - PixelFont.width(m, 1), y + 3, 1, new Color(0xFFE680));
        }
    }

    private void plaqueta(Graphics2D g, int x, int y, int w, int h, Color c) {
        capa(g, new Color(0x3A1E66), x, y, w, h + 1, 1);
        capa(g, c, x + 1, y, w - 2, h - 1, 1);
        g.setColor(new Color(255, 255, 255, 40));
        g.fillRect(x + 3, y, w - 6, 1);
    }

    // estrella de 9 px: dorada si se ganó, apagada si no
    private void estrellita(Graphics2D g, int cx, int cy, boolean on) {
        int[] xs = new int[10], ys = new int[10];
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5, r = i % 2 == 0 ? 5.0 : 2.2;
            xs[i] = (int) Math.round(cx + r * Math.cos(a));
            ys[i] = (int) Math.round(cy + r * Math.sin(a) + 0.5);
        }
        g.setColor(on ? OSCURO : new Color(0x3A1E66));
        g.fillPolygon(xs, ys, 10);
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5, r = (i % 2 == 0 ? 5.0 : 2.2) - 1.2;
            xs[i] = (int) Math.round(cx + r * Math.cos(a));
            ys[i] = (int) Math.round(cy + r * Math.sin(a) + 0.5);
        }
        g.setColor(on ? ORO : new Color(0x7A66A0));
        g.fillPolygon(xs, ys, 10);
    }

    // las 3 estrellas grandes que muestran arriba: la del centro es más grande
    private void estrellasGrandes(Graphics2D g, Datos d) {
        int[][] pos = {{PX + PW / 2 - 82, PY - 30, 2}, {PX + PW / 2 - 36, PY - 54, 3}, {PX + PW / 2 + 34, PY - 30, 2}};
        for (int i = 0; i < 3; i++) {
            boolean on = d.ganadas[i] && d.mostradas > i;
            float desde = 0.9f + 0.55f * i;
            float u = d.t - desde;
            int salto = on && u < 0.3f ? (int) (10 * Math.sin(u / 0.3f * Math.PI)) : 0;
            int tam = 24 * pos[i][2];
            int x = pos[i][0], y = pos[i][1] - salto;
            if (on) {                  // sombra dorada de la estrella encendida
                Paint antes = g.getPaint();
                g.setPaint(new RadialGradientPaint(x + tam / 2f, y + tam / 2f, tam * 0.9f, new float[]{0f, 1f},
                        new Color[]{new Color(1f, 0.9f, 0.4f, 0.55f), new Color(1f, 0.8f, 0.2f, 0f)}));
                g.fillRect(x - tam / 2, y - tam / 2, tam * 2, tam * 2);
                g.setPaint(antes);
            }
            BufferedImage img = Assets.img("inventario/iconos/estrella_" + (on ? "on" : "off") + ".png");
            g.drawImage(img, x, y, tam, tam, null);
            if (on && u >= 0 && u < 0.6f) destellos(g, x + tam / 2, y + tam / 2, tam, u);
        }
    }

    // brillos que aparecen cuando se enciende una estrella
    private void destellos(Graphics2D g, int cx, int cy, int tam, float u) {
        float r = tam * 0.45f + u * 40f;
        int a = Math.max(0, (int) (255 * (1f - u / 0.6f)));
        g.setColor(new Color(255, 255, 255, a));
        int[][] dir = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        for (int[] q : dir) {
            int x = Math.round(cx + q[0] * r * (q[0] != 0 && q[1] != 0 ? 0.75f : 1f));
            int y = Math.round(cy + q[1] * r * (q[0] != 0 && q[1] != 0 ? 0.75f : 1f));
            g.fillRect(x - 1, y, 3, 1);
            g.fillRect(x, y - 1, 1, 3);
        }
    }

    // cinta dorada que parpadea cuando se rompe un record
    private void recordNuevo(Graphics2D g, Datos d) {
        if (d.mostradas < 3 || (int) (d.anim * 4) % 2 != 0) return;
        String t = "¡NUEVO RECORD!";
        int w = PixelFont.width(t, 1) + 16, x = PX + (PW - w) / 2, y = PY + PH - 7;
        capa(g, OSCURO, x, y, w, 13, 2);
        capa(g, ORO, x + 1, y + 1, w - 2, 11, 2);
        fuente.draw(g, t, x + 8, y + 3, 1, new Color(0x7A2A00));
    }

    // ------------------------------------------------------------------ BOTONES
    private void botones(Graphics2D g, Datos d) {
        Composite previo = g.getComposite();
        if (!d.botonesActivos) g.setComposite(AlphaComposite.SrcOver.derive(0.45f * ((AlphaComposite) previo).getAlpha()));
        boolean sobreR = d.botonesActivos && btnRepetir.contiene(d.mx, d.my), sobreS = d.botonesActivos && btnSeguir.contiene(d.mx, d.my);
        btnRepetir.dibujar(g, sobreR, sobreR && d.mouseAbajo);
        btnSeguir.dibujar(g, sobreS, sobreS && d.mouseAbajo);
        g.setComposite(previo);
        // atajos de teclado, debajo de cada boton
        if (d.botonesActivos) {
            String r = "TECLA R", s = "ENTER";
            fuente.draw(g, r, 114 + (btnRepetir.ancho() - PixelFont.width(r, 1)) / 2, 261, 1, new Color(0xF0E6FF));
            fuente.draw(g, s, 234 + (btnSeguir.ancho() - PixelFont.width(s, 1)) / 2, 261, 1, new Color(0xF0E6FF));
        }
    }

    // ------------------------------------------------------------------ FORMATO
    private static void capa(Graphics2D g, Color c, int x, int y, int w, int h, int esquina) {
        g.setColor(c);
        g.fillRect(x + esquina, y, w - 2 * esquina, h);
        g.fillRect(x, y + esquina, w, h - 2 * esquina);
        if (esquina > 1) {
            g.fillRect(x + 1, y + 1, w - 2, h - 2);
        }
    }

    private static Color mezcla(Color a, Color b, float f) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * f),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * f),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * f));
    }
}
