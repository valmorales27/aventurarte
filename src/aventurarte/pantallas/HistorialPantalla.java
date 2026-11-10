package aventurarte.pantallas;

import aventurarte.core.*;
import aventurarte.modelo.Coleccion;
import aventurarte.modelo.Inventario;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * El "diario del viajero": una pagina por nivel (llave, monedas, tiempo, intentos y REGISTRO de todo lo que paso)
 * y una pagina TODO con los objetos de toda la partida.  Ademas tiene la COLECCION (tarjetas de lugares, llaves
 * y enemigos que has descubierto) y los LOGROS (insignias).  Se abre con la tecla I o con la bolsa del HUD,
 * y tambien desde el menu principal.
 */
public class HistorialPantalla extends Pantalla {
    private static final Color[] COLOR_NIVEL = {new Color(0xE8548A), new Color(0x3E6FB0), new Color(0x2FB8D8), new Color(0x48B838), new Color(0x8A3AB0)};
    private static final Color MARRON = new Color(0x5A3A28);
    private static final String[] LLAVES = Inventario.LLAVE;

    private final NivelPantalla desdeNivel;
    private final Boton cerrar = new Boton("inventario/libro/botones/boton_cerrar", 414, 0, 0, 0, 0);
    // pestanas: 0..4 niveles, 5 = TODO, 6 = TARJETAS, 7 = LOGROS
    private static final int TODO = 5, TARJETAS = 6, LOGROS = 7, NUM_TABS = 8;
    private static final int[] TAB_X = {40, 76, 112, 148, 184, 220, 272, 340};
    private static final int[] TAB_W = {32, 32, 32, 32, 32, 48, 64, 54};
    private static final String[] TAB_TXT = {null, null, null, null, null, "TODO", "TARJETAS", "LOGROS"};
    private static final Color VIOLETA = new Color(0x7A4AB0), DORADO = new Color(0xF2C23C), GRIS = new Color(0xAA9C82);
    private static final Map<Integer, BufferedImage> FRENTE_ENEMIGO = new HashMap<>();

    private int tab;
    private int sel;                          // tarjeta elegida en la pagina TARJETAS
    private int scroll;
    private boolean scrollInicial = true;
    private boolean arrastrandoBarra;

    public HistorialPantalla(Juego j, NivelPantalla desdeNivel) {
        super(j);
        this.desdeNivel = desdeNivel;
        Inventario.Snapshot s = j.inv.snapshot();
        tab = desdeNivel != null ? desdeNivel.numero() : (s.juegoCompleto() ? TODO : Math.max(0, Math.min(4, j.nivelActual)));
        if (tab < 5 && s.estado(tab).equals("bloqueado")) tab = 0;
        String yo = j.nombreJugador();
        for (Coleccion.Tarjeta t : Coleccion.TARJETAS) if (j.coleccion.tieneTarjeta(yo, t.id)) { sel = t.id; break; }
    }

    @Override public void alEntrar() { j.audio.sfx("pausa"); }

    @Override public void actualizar(float dt) {
        Inventario.Snapshot s = j.inv.snapshot();
        int total = tab < TODO ? s.log.get(tab).size() : 0;
        int maxScroll = Math.max(0, total - 9);
        if (scrollInicial) { scroll = maxScroll; scrollInicial = false; }

        for (int[] c : j.input.clics()) {
            if (cerrar.contiene(c[0], c[1])) { cerrar(); return; }
            for (int i = 0; i < NUM_TABS; i++)
                if (dentro(c[0], c[1], TAB_X[i], 1, TAB_W[i], 23)) irATab(i, s);
            if (tab == TARJETAS) {
                for (Coleccion.Tarjeta t : Coleccion.TARJETAS)
                    if (dentro(c[0], c[1], celdaX(t.id), celdaY(t.id), CELDA, CELDA)) { sel = t.id; j.audio.sfx("clic"); }
            }
            if (tab < TODO) {
                if (dentro(c[0], c[1], 418, 62, 14, 14)) scroll--;
                if (dentro(c[0], c[1], 418, 192, 14, 14)) scroll++;
            }
        }
        // arrastrar la barra de scroll
        Input in = j.input;
        if (tab < TODO && maxScroll > 0) {
            if (in.mouseAbajo && dentro(in.mx, in.my, 417, 76, 12, 114)) arrastrandoBarra = true;
            if (!in.mouseAbajo) arrastrandoBarra = false;
            if (arrastrandoBarra) scroll = Math.round(Util.clamp((in.my - 76) / 114f, 0, 1) * maxScroll);
        }
        int rueda = in.rueda();
        if (rueda != 0) scroll += rueda;
        int k;
        while ((k = in.sigPulsada()) != -1) {
            if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_I) { cerrar(); return; }
            else if (k == KeyEvent.VK_LEFT) irATab(tab - 1, s);
            else if (k == KeyEvent.VK_RIGHT) irATab(tab + 1, s);
            else if (k == KeyEvent.VK_UP) { if (tab == TARJETAS) moverSel(-1); else scroll--; }
            else if (k == KeyEvent.VK_DOWN) { if (tab == TARJETAS) moverSel(1); else scroll++; }
        }
        scroll = Util.clamp(scroll, 0, maxScroll);
    }

    private void irATab(int i, Inventario.Snapshot s) {
        if (i < 0 || i >= NUM_TABS || i == tab) return;
        if (i < 5 && s.estado(i).equals("bloqueado")) { j.audio.sfx("golpe"); return; }
        tab = i; scrollInicial = true; j.audio.sfx("clic");
    }

    private void moverSel(int d) {
        int n = Coleccion.TARJETAS.size();
        sel = (sel + d + n) % n;
        j.audio.sfx("clic");
    }

    private void cerrar() { j.audio.sfx("pausa"); j.cerrar(); }

    // ------------------------------------------------------------------ dibujo
    @Override public void dibujar(Graphics2D g) {
        Inventario.Snapshot s = j.inv.snapshot();
        imagen(g, Assets.img("pausa/oscurecer_480x270.png"), 0, 0);
        if (tab < TODO) {
            imagen(g, Assets.img("inventario/libro/fondo_libro_nivel_480x270.png"), 0, 0);
            paginaNivel(g, s, tab);
        } else if (tab == TODO) {
            imagen(g, Assets.img("inventario/libro/fondo_libro_total_480x270.png"), 0, 0);
            paginaTotal(j, g, s);
        } else {
            imagen(g, Assets.img("inventario/libro/fondo_libro_coleccion_480x270.png"), 0, 0);
            if (tab == TARJETAS) paginaTarjetas(g); else paginaLogros(g);
        }
        for (int i = 0; i < NUM_TABS; i++) {
            boolean act = i == tab;
            String nom;
            if (i == TODO) nom = "todo_" + (act ? "activa" : "inactiva");
            else if (i == TARJETAS) nom = "tarjetas_" + (act ? "activa" : "inactiva");
            else if (i == LOGROS) nom = "logros_" + (act ? "activa" : "inactiva");
            else nom = "nivel_" + (i + 1) + "_" + (s.estado(i).equals("bloqueado") ? "bloqueada" : act ? "activa" : "inactiva");
            int ty = act ? 1 : 2;
            imagen(g, Assets.img("inventario/libro/tabs/tab_" + nom + ".png"), TAB_X[i], ty);
            if (TAB_TXT[i] != null) {
                String t = TAB_TXT[i];
                int tx = TAB_X[i] + (TAB_W[i] - PixelFont.width(t, 1)) / 2, tyy = ty + 8;
                if (i == TODO) j.fuente.draw(g, t, tx, tyy, 1, MARRON);
                else {
                    j.fuente.draw(g, t, tx + 1, tyy + 1, 1, new Color(0x2A1240));
                    j.fuente.draw(g, t, tx, tyy, 1, Color.WHITE);
                }
            }
        }
        cerrar.dibujar(g, cerrar.contiene(j.input.mx, j.input.my), j.input.mouseAbajo && cerrar.contiene(j.input.mx, j.input.my));
    }

    private void paginaNivel(Graphics2D g, Inventario.Snapshot s, int n) {
        j.fuente.draw(g, "NIVEL " + (n + 1), 54, 44, 1, COLOR_NIVEL[n]);
        j.fuente.draw(g, MapaPantalla.NOMBRES[n], 104, 44, 1, MARRON);
        // llave
        BufferedImage llave = Assets.img("niveles/" + (n + 1) + "/llave.png");
        imagen(g, s.llave[n] ? llave : Util.tinte(llave, 0xAA9C82), 64, 77);
        j.fuente.draw(g, LLAVES[n], 124, 80, 2, MARRON);
        if (s.llave[n]) {
            j.fuente.draw(g, "ENCONTRADA", 124, 98, 1, new Color(0x2F6B2A));
            int cx = 124 + PixelFont.width("ENCONTRADA", 1) + 5;
            g.setColor(new Color(0x2F6B2A));
            g.setStroke(new BasicStroke(1.5f));
            g.drawPolyline(new int[]{cx, cx + 2, cx + 7}, new int[]{102, 105, 97}, 3);
            g.setStroke(new BasicStroke(1f));
        } else j.fuente.draw(g, "NO ENCONTRADA", 124, 98, 1, new Color(0xC8503A));
        // monedas
        int tot = Inventario.MONEDAS_TOTAL[n];
        String cont = String.format("%02d/%02d", s.monedas[n], tot);
        j.fuente.draw(g, cont, 228 - PixelFont.width(cont, 2), 118, 2, s.monedas[n] >= tot ? new Color(0x2F6B2A) : new Color(0x8A5A00));
        BufferedImage llena = Assets.img("inventario/iconos/moneda_llena.png"), vacia = Assets.img("inventario/iconos/moneda_vacia.png");
        for (int i = 0; i < tot; i++)
            imagen(g, i < s.monedas[n] ? llena : vacia, 54 + (i % 10) * 17, 138 + (i / 10) * 18);
        // estadisticas
        int x = 52;
        imagen(g, Assets.img("hud/iconos/reloj.png"), x + 4, 213);
        j.fuente.draw(g, s.completado[n] ? Util.mmss(s.tiempo[n]) : "--:--", x + 24, 215, 1, MARRON);
        x = 112;
        int quedan = 3 - Math.min(3, s.intentosUsados[n]);
        for (int k = 0; k < 3; k++)
            imagen(g, Assets.img(k < quedan ? "inventario/iconos/corazon_intento_11x9.png" : "inventario/iconos/corazon_intento_usado_11x9.png"), x + 4 + 13 * k, 214);
        x = 172;
        imagen(g, Assets.img("inventario/iconos/icono_enemigo.png"), x + 4, 212);
        j.fuente.draw(g, "X" + Inventario.ENEMIGOS[n], x + 24, 215, 1, MARRON);
        // registro
        List<Inventario.Registro> log = s.log.get(n);
        for (int r = 0; r < 9; r++) {
            int idx = scroll + r;
            if (idx >= log.size()) break;
            Inventario.Registro e = log.get(idx);
            int y = 62 + r * 16;
            imagen(g, Assets.img("inventario/iconos/" + e.icono() + ".png"), 256, y + 1);
            j.fuente.draw(g, Util.mmss(e.seg), 276, y + 5, 1, new Color(0x8A7560));
            j.fuente.draw(g, e.texto, 304, y + 5, 1, MARRON);
        }
        j.fuente.draw(g, "EVENTOS: " + log.size(), 256, 218, 1, MARRON);
        // scroll
        boolean a1 = dentro(j.input.mx, j.input.my, 418, 62, 14, 14), a2 = dentro(j.input.mx, j.input.my, 418, 192, 14, 14);
        imagen(g, Assets.img("inventario/libro/botones/scroll_arriba" + (a1 ? "_hover" : "") + ".png"), 418, 62);
        imagen(g, Assets.img("inventario/libro/botones/scroll_abajo" + (a2 ? "_hover" : "") + ".png"), 418, 192);
        int total = log.size();
        if (total > 9) {
            int alto = Math.max(10, 114 * 9 / total);
            int max = total - 9;
            int by = 76 + Math.round((114 - alto) * (scroll / (float) max));
            g.setColor(new Color(0xB8683F));
            g.fillRect(419, by, 9, alto);
        }
    }

    // ------------------------------------------------------------------ COLECCION
    private static final int CELDA = 40;
    private static int celdaX(int id) { return 50 + (id % 4) * 44; }
    private static int celdaY(int id) { return 63 + (id / 4) * 43; }

    /** Imagen de una tarjeta: el lugar del pueblo, la llave o el enemigo en reposo. */
    private static BufferedImage imgTarjeta(Coleccion.Tarjeta t) {
        switch (t.tipo) {
            case LUGAR: return Assets.img("niveles/" + (t.nivel + 1) + "/lugar_representativo.png");
            case LLAVE: return Assets.img("niveles/" + (t.nivel + 1) + "/llave.png");
            default:
                return FRENTE_ENEMIGO.computeIfAbsent(t.id, k -> recortar(
                        Assets.img("enemigos/" + t.enemigo.id + "/hoja.png").getSubimage(0, 0, 48, 64)));
        }
    }

    /** Quita el espacio transparente de alrededor (asi el enemigo se ve mas grande en su tarjeta). */
    private static BufferedImage recortar(BufferedImage im) {
        int x0 = im.getWidth(), y0 = im.getHeight(), x1 = -1, y1 = -1;
        for (int y = 0; y < im.getHeight(); y++)
            for (int x = 0; x < im.getWidth(); x++)
                if ((im.getRGB(x, y) >>> 24) > 0) { x0 = Math.min(x0, x); y0 = Math.min(y0, y); x1 = Math.max(x1, x); y1 = Math.max(y1, y); }
        if (x1 < 0) return im;
        BufferedImage r = new BufferedImage(x1 - x0 + 1, y1 - y0 + 1, BufferedImage.TYPE_INT_ARGB);
        r.getGraphics().drawImage(im, -x0, -y0, null);
        return r;
    }

    /** Dibuja la imagen dentro de una caja, sin deformarla. Si hay que achicarla usa suavizado; si se agranda, pixeles nitidos. */
    private static void ajustar(Graphics2D g, BufferedImage im, int x, int y, int w, int h) {
        float k = Math.min(w / (float) im.getWidth(), h / (float) im.getHeight());
        if (k >= 1) k = (float) Math.floor(k);
        int dw = Math.round(im.getWidth() * k), dh = Math.round(im.getHeight() * k);
        Object antes = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, k < 1 ? RenderingHints.VALUE_INTERPOLATION_BILINEAR : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        g.drawImage(im, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh, null);
        if (antes != null) g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, antes);
    }

    private void paginaTarjetas(Graphics2D g) {
        String yo = j.nombreJugador();
        int tiene = j.coleccion.totalTarjetas(yo), total = Coleccion.TARJETAS.size();
        Color hdr = new Color(0xB8683F);
        j.fuente.draw(g, "TARJETAS", 56, 45, 1, hdr);
        String cont = tiene + "/" + total;
        j.fuente.draw(g, cont, 220 - PixelFont.width(cont, 1), 45, 1, tiene == total ? new Color(0x2F6B2A) : MARRON);

        for (Coleccion.Tarjeta t : Coleccion.TARJETAS) {
            boolean ok = j.coleccion.tieneTarjeta(yo, t.id);
            int x = celdaX(t.id), y = celdaY(t.id);
            boolean elegida = t.id == sel;
            boolean hover = dentro(j.input.mx, j.input.my, x, y, CELDA, CELDA);
            g.setColor(elegida ? new Color(0xE8548A) : new Color(0xC8B07A));
            g.fillRect(x - 1, y - 1, CELDA + 2, CELDA + 2);
            if (elegida) { g.setColor(new Color(0xFFF6DA)); g.fillRect(x, y, CELDA, CELDA); }
            g.setColor(ok ? (hover ? new Color(0xFFF6DA) : new Color(0xFBF1D8)) : new Color(0xE8DAB0));
            g.fillRect(x + (elegida ? 1 : 0), y + (elegida ? 1 : 0), CELDA - (elegida ? 2 : 0), CELDA - (elegida ? 2 : 0));
            BufferedImage im = imgTarjeta(t);
            if (ok) ajustar(g, im, x + 3, y + 3, CELDA - 6, CELDA - 6);
            else {
                ajustar(g, Util.tinte(im, 0xCDBE98), x + 3, y + 3, CELDA - 6, CELDA - 6);
                j.fuente.draw(g, "?", x + CELDA / 2 - 5, y + CELDA / 2 - 7, 2, new Color(0x8A7560));
            }
        }

        // ---- detalle de la tarjeta elegida (pagina derecha)
        Coleccion.Tarjeta t = Coleccion.TARJETAS.get(sel);
        boolean ok = j.coleccion.tieneTarjeta(yo, t.id);
        Color hdr2 = new Color(0xB8683F);
        j.fuente.draw(g, ok ? t.nombre : "???", 260, 45, 1, hdr2);
        String tipo = t.tipo == Coleccion.Tipo.LUGAR ? "LUGAR" : t.tipo == Coleccion.Tipo.LLAVE ? "LLAVE" : "ENEMIGO";
        j.fuente.draw(g, tipo, 428 - PixelFont.width(tipo, 1), 45, 1, VIOLETA);

        g.setColor(new Color(0xC8B07A));
        g.fillRect(266, 65, 156, 112);
        g.setColor(ok ? new Color(0xFFF6DA) : new Color(0xE8DAB0));
        g.fillRect(267, 66, 154, 110);
        BufferedImage im = imgTarjeta(t);
        if (ok) ajustar(g, im, 271, 70, 146, 102);
        else {
            ajustar(g, Util.tinte(im, 0xCDBE98), 271, 70, 146, 102);
            j.fuente.draw(g, "?", 344 - 9, 106, 4, new Color(0x8A7560));
        }
        if (ok) {
            centrado(g, t.nombre, 344, 186, 2, MARRON);
            centrado(g, t.linea1, 344, 206, 1, new Color(0x8A7560));
            centrado(g, t.linea2, 344, 217, 1, new Color(0x8A7560));
        } else {
            centrado(g, "AUN NO LA DESCUBRES", 344, 190, 1, new Color(0xC8503A));
            String pista = t.tipo == Coleccion.Tipo.LUGAR ? "ENTRA AL NIVEL " + (t.nivel + 1)
                    : t.tipo == Coleccion.Tipo.LLAVE ? "ENCUENTRALA EN EL NIVEL " + (t.nivel + 1)
                    : "BUSCALO EN EL NIVEL " + (t.nivel + 1);
            centrado(g, pista, 344, 206, 1, new Color(0x8A7560));
        }
    }

    private void centrado(Graphics2D g, String s, int cx, int y, int escala, Color c) {
        j.fuente.draw(g, s, cx - PixelFont.width(s, escala) / 2, y, escala, c);
    }

    // ------------------------------------------------------------------ LOGROS
    private static final Color[] MEDALLA = {
            new Color(0xE8548A), new Color(0x2FB8D8), new Color(0xF2A03C), new Color(0xF2C23C), new Color(0x48B838),
            new Color(0xD8503A), new Color(0x8A5AD8), new Color(0x3E6FB0), new Color(0xB04A8A), new Color(0x2F9B6A)};

    private void paginaLogros(Graphics2D g) {
        String yo = j.nombreJugador();
        int tiene = j.coleccion.totalLogros(yo), total = Coleccion.LOGROS.length;
        Color hdr = new Color(0xB8683F);
        j.fuente.draw(g, "LOGROS", 56, 45, 1, hdr);
        j.fuente.draw(g, "INSIGNIAS", 260, 45, 1, hdr);
        String cont = tiene + "/" + total;
        j.fuente.draw(g, cont, 428 - PixelFont.width(cont, 1), 45, 1, tiene == total ? new Color(0x2F6B2A) : MARRON);

        for (Coleccion.Logro l : Coleccion.LOGROS) {
            int col = l.id / 5, fila = l.id % 5;
            int x = col == 0 ? 50 : 258, y = 64 + fila * 34;
            boolean ok = j.coleccion.tieneLogro(yo, l.id);
            g.setColor(new Color(0xC8B07A));
            g.fillRect(x, y, 176, 31);
            g.setColor(ok ? new Color(0xFFF6DA) : new Color(0xE8DAB0));
            g.fillRect(x + 1, y + 1, 174, 29);
            medalla(g, x + 5, y + 4, l.id, ok);
            j.fuente.draw(g, l.nombre, x + 36, y + 6, 1, ok ? MARRON : new Color(0x8A7560));
            j.fuente.draw(g, l.descripcion, x + 36, y + 18, 1, ok ? new Color(0x2F6B2A) : GRIS.darker());
            String prog = progreso(yo, l.id);
            if (prog != null && !ok) j.fuente.draw(g, prog, x + 172 - PixelFont.width(prog, 1), y + 6, 1, VIOLETA);
        }
    }

    /** Lo que llevas para los logros que se juntan poco a poco (por ejemplo 7/10 enemigos). */
    private String progreso(String yo, int id) {
        if (id == Coleccion.CAZADOR) return Math.min(Coleccion.CAZADOR_META, j.coleccion.enemigosVencidos(yo)) + "/" + Coleccion.CAZADOR_META;
        if (id == Coleccion.EXPLORADOR) return j.coleccion.totalTarjetas(yo) + "/" + Coleccion.TARJETAS.size();
        if (id == Coleccion.LLAVERO) {
            int c = 0;
            for (int i = 0; i < 5; i++) if (j.coleccion.tieneTarjeta(yo, Coleccion.idLlave(i))) c++;
            return c + "/5";
        }
        return null;
    }

    /** La insignia: medalla redonda de color con una estrella; en gris con candado si aun no la ganas. */
    private void medalla(Graphics2D g, int x, int y, int id, boolean ok) {
        Color base = ok ? MEDALLA[id] : new Color(0xBDB19A);
        g.setColor(new Color(0x2A1240));
        g.fillOval(x - 1, y - 1, 26, 26);
        g.setColor(base.darker());
        g.fillOval(x, y, 24, 24);
        g.setColor(base);
        g.fillOval(x + 1, y + 1, 22, 20);
        g.setColor(new Color(255, 255, 255, ok ? 110 : 60));
        g.fillOval(x + 4, y + 3, 9, 4);
        if (ok) {
            g.setColor(new Color(0x2A1240));
            estrella(g, x + 12, y + 12, 8.5f);
            g.setColor(Color.WHITE);
            estrella(g, x + 12, y + 12, 7f);
        } else {
            g.setColor(new Color(0x6A5E4A));
            g.fillRect(x + 8, y + 11, 8, 7);
            g.drawRect(x + 9, y + 6, 5, 5);
            g.setColor(new Color(0xBDB19A));
            g.fillRect(x + 11, y + 13, 2, 3);
        }
    }

    private static void estrella(Graphics2D g, float cx, float cy, float r) {
        int[] px = new int[10], py = new int[10];
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5, rr = i % 2 == 0 ? r : r * 0.42;
            px[i] = Math.round((float) (cx + Math.cos(a) * rr));
            py[i] = Math.round((float) (cy + Math.sin(a) * rr));
        }
        g.fillPolygon(px, py, 10);
    }

    /** Pagina TODO: tu personaje, las 5 llaves y los totales. La usa tambien la pantalla final. */
    static void paginaTotal(Juego j, Graphics2D g, Inventario.Snapshot s) {
        BufferedImage hoja = Assets.img(j.perfil.rutaHojaFrente());
        g.drawImage(hoja.getSubimage(0, 0, 40, 73), 98, 44, 80, 146, null);
        String nom = j.perfil.getNombre().isEmpty() ? "JUGADOR" : j.perfil.getNombre();
        j.fuente.draw(g, nom, 138 - PixelFont.width(nom, 2) / 2, 200, 2, MARRON);

        int[][] pos = {{256, 64}, {318, 64}, {380, 64}, {256, 108}, {318, 108}};
        for (int i = 0; i < 5; i++) {
            String est = s.llave[i] ? "tiene" : s.estado(i).equals("bloqueado") ? "bloqueado" : "falta";
            g.drawImage(Assets.img("inventario/slots_llave/slot_llave_" + (i + 1) + "_" + est + ".png"), pos[i][0], pos[i][1], null);
        }
        boolean completo = s.totalLlaves() == 5;
        g.drawImage(Assets.img("inventario/iconos/estrella_" + (completo ? "on" : "off") + ".png"), 396, 110, null);
        String t = completo ? "COMPLETO!" : "LLAVES " + s.totalLlaves() + "/5";
        j.fuente.draw(g, t, 380 + (56 - PixelFont.width(t, 1)) / 2, 138, 1, completo ? new Color(0x2F6B2A) : new Color(0x8A7560));

        int totMon = 0;
        for (int m : Inventario.MONEDAS_TOTAL) totMon += m;
        // estrellas ganadas en esta partida (arriba a la derecha de "MIS OBJETOS")
        String est = s.totalEstrellas() + "/15";
        g.drawImage(Assets.img("inventario/iconos/estrella_on.png"), 372, 42, 14, 14, null);
        j.fuente.draw(g, est, 390, 46, 1, MARRON);
        String[] v = {s.totalMonedas() + "/" + totMon, Util.mmss(s.totalTiempo()), String.valueOf(s.totalIntentos())};
        int[] ys = {157, 179, 201};
        for (int i = 0; i < 3; i++) j.fuente.draw(g, v[i], 428 - PixelFont.width(v[i], 2), ys[i], 2, MARRON);
    }
}
