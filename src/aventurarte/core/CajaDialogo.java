package aventurarte.core;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/** Cajita de dialogo:
 * Uso (desde una pantalla):
 *     CajaDialogo c = new CajaDialogo(fuente, retrato, "JUGADOR", "DATO CURIOSO", texto);
 *     c.actualizar(dt);            // cada cuadro
 *     c.avanzar();                 // cuando el jugador pulsa ENTER / ESPACIO / clic
 *     c.dibujar(g);                // al final del dibujo de la pantalla
 *     if (c.terminada()) ...       // se cierra
 */
public class CajaDialogo {
    private static final Color OSCURO = new Color(0x5A3A28), NARANJA = new Color(0xE9A24B), NARANJA_OSC = new Color(0xB8683F);
    private static final Color CREMA = new Color(0xFFF3D9), CREMA_OSC = new Color(0xEAD3A6), TEXTO = new Color(0x5A3A28);
    private static final Color VERDE = new Color(0x3F9A6B), VERDE_OSC = new Color(0x1F5F3F);

    // posición de la caja (coordenadas lógicas 480x270)
    private static final int X = 58, Y = 216, W = 416, H = 50;
    private static final int TEXTO_X = 92, TEXTO_Y = 228, INTERLINEA = 11, TEXTO_ANCHO = 352;
    private static final float LETRAS_POR_SEG = 38f;
    private static final float ENTRADA = 0.35f, SALIDA = 0.22f;

    private final PixelFont fuente;
    private final BufferedImage retrato, contorno;
    private final String nombre, etiqueta;
    private final List<String> lineas = new ArrayList<>();
    private final int totalLetras;

    private float t;                    // segundos desde que apareció
    private float escribiendo;          // letras ya escritas (puede tener decimales)
    private float cerrandoT = -1;       // >= 0 mientras se esta cerrando

    public CajaDialogo(PixelFont fuente, BufferedImage retrato, String nombre, String etiqueta, String texto) {
        this.fuente = fuente;
        this.retrato = retrato;
        this.contorno = siluetaBlanca(retrato);
        this.nombre = nombre.toUpperCase();
        this.etiqueta = etiqueta;
        acomodar(texto.toUpperCase());
        int n = 0;
        for (String l : lineas) n += l.length();
        totalLetras = n;
    }

    // ------------------------------------------------------------------ LÓGICA
    public void actualizar(float dt) {
        t += dt;
        if (cerrandoT >= 0) cerrandoT += dt;
        else if (t > ENTRADA * 0.6f && escribiendo < totalLetras) escribiendo = Math.min(totalLetras, escribiendo + dt * LETRAS_POR_SEG);
    }

    /** ENTER / ESPACIO / clic: si todavía se está escribiendo muestra el texto, y cierra la cajita */
    public void avanzar() {
        if (cerrandoT >= 0 || t < 0.25f) return;
        if (escribiendo < totalLetras) escribiendo = totalLetras;
        else cerrandoT = 0;
    }

    public boolean completa() { return escribiendo >= totalLetras; }

    public boolean terminada() { return cerrandoT >= SALIDA; }

    public int numeroDeLineas() { return lineas.size(); }

    // ------------------------------------------------------------------ DIBUJO
    public void dibujar(Graphics2D g) {
        // la cajita sube desde abajo al aparecer y baja al cerrarse
        float p = cerrandoT >= 0 ? 1f - Math.min(1f, cerrandoT / SALIDA) : Math.min(1f, t / ENTRADA);
        float suave = p * p * (3 - 2 * p);
        int dy = Math.round((1f - suave) * 80);
        g.translate(0, dy);

        // retrato con contorno (aparece por la izquierda, sube y baja mientras se escribe)
        boolean hablando = !completa() && cerrandoT < 0;
        int rebote = hablando && (int) (t / 0.14f) % 2 == 1 ? -1 : 0;
        int rx = 8, ry = 188 + rebote;
        for (int ox = -1; ox <= 1; ox++)
            for (int oy = -1; oy <= 1; oy++)
                if (ox != 0 || oy != 0) g.drawImage(contorno, rx + ox * 2, ry + oy * 2, 76, 76, null);

        caja(g, X, Y, W, H);
        g.drawImage(retrato, rx, ry, 76, 76, null);                     // el retrato va delante de la caja

        // etiquetas: nombre (naranja) a la izquierda y "DATO CURIOSO" (verde) a la derecha
        etiqueta(g, TEXTO_X, 208, nombre, NARANJA, NARANJA_OSC);
        int wEt = PixelFont.width(etiqueta, 1) + 16;
        etiqueta(g, X + W - wEt - 14, 208, etiqueta, VERDE, VERDE_OSC);

        // texto letra por letra
        int quedan = (int) escribiendo;
        for (int i = 0; i < lineas.size() && quedan > 0; i++) {
            String l = lineas.get(i);
            String parte = l.substring(0, Math.min(l.length(), quedan));
            fuente.draw(g, parte, TEXTO_X, TEXTO_Y + i * INTERLINEA, 1, TEXTO);
            if (quedan < l.length() || (i == lineas.size() - 1 && !completa())) {
                // cursor "_" parpadeante mientras escribe, como en la referencia
                if ((int) (t / 0.25f) % 2 == 0) fuente.draw(g, "_", TEXTO_X + PixelFont.width(parte, 1) + 1, TEXTO_Y + i * INTERLINEA, 1, TEXTO);
            }
            quedan -= l.length();
        }

        // cuando termina de escribir: una flecha para continuar (pulsa ENTER, ESPACIO o da clic)
        if (completa() && cerrandoT < 0) {
            int bob = (int) (t / 0.3f) % 2;
            g.setColor(NARANJA_OSC);
            g.fillRect(X + W - 18, Y + H - 14 + bob, 7, 1);
            g.fillRect(X + W - 17, Y + H - 13 + bob, 5, 1);
            g.fillRect(X + W - 16, Y + H - 12 + bob, 3, 1);
            g.fillRect(X + W - 15, Y + H - 11 + bob, 1, 1);
        }
        g.translate(0, -dy);
    }

    /** Caja: borde oscuro, borde naranja, relleno crema, linea interior y rombos  */
    private static void caja(Graphics2D g, int x, int y, int w, int h) {
        g.setColor(new Color(0, 0, 0, 70));                                   // sombrita
        g.fillRect(x + 2, y + h, w - 2, 2);
        capa(g, OSCURO, x, y, w, h);
        capa(g, NARANJA, x + 1, y + 1, w - 2, h - 2);
        capa(g, CREMA, x + 2, y + 2, w - 4, h - 4);
        // linea fina interior (detalle del marco)
        g.setColor(CREMA_OSC);
        g.fillRect(x + 5, y + 4, w - 10, 1);
        g.fillRect(x + 5, y + h - 5, w - 10, 1);
        g.fillRect(x + 4, y + 5, 1, h - 10);
        g.fillRect(x + w - 5, y + 5, 1, h - 10);
        // rombitos en el centro de los lados
        rombo(g, x + w, y + h / 2);
        rombo(g, x, y + h / 2);
    }

    // rectángulo con las 4 esquinas recortadas 1 pixel
    private static void capa(Graphics2D g, Color c, int x, int y, int w, int h) {
        g.setColor(c);
        g.fillRect(x + 1, y, w - 2, h);
        g.fillRect(x, y + 1, w, h - 2);
    }

    private static void rombo(Graphics2D g, int cx, int cy) {
        for (int i = 0; i <= 4; i++) {   // contorno oscuro
            g.setColor(OSCURO);
            g.fillRect(cx - (4 - i), cy - i, 2 * (4 - i) + 1, 1);
            g.fillRect(cx - (4 - i), cy + i, 2 * (4 - i) + 1, 1);
        }
        for (int i = 0; i <= 3; i++) {    // relleno naranja
            g.setColor(NARANJA);
            g.fillRect(cx - (3 - i), cy - i, 2 * (3 - i) + 1, 1);
            g.fillRect(cx - (3 - i), cy + i, 2 * (3 - i) + 1, 1);
        }
        g.setColor(CREMA);
        g.fillRect(cx - 1, cy - 1, 2, 2);  // brillo al centro
    }

    private void etiqueta(Graphics2D g, int x, int y, String texto, Color fondo, Color borde) {
        int w = PixelFont.width(texto, 1) + 16, h = 15;
        capa(g, OSCURO, x, y, w, h);
        capa(g, borde, x + 1, y + 1, w - 2, h - 2);
        capa(g, fondo, x + 1, y + 1, w - 2, h - 4);
        fuente.draw(g, texto, x + 8, y + 4, 1, new Color(0x3A2418));
        fuente.draw(g, texto, x + 8, y + 3, 1, CREMA);
    }

    // ------------------------------------------------------------------ FORMATO
    // parte el texto en renglones que entre en TEXTO_ANCHO
    private void acomodar(String texto) {
        StringBuilder linea = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            String prueba = linea.length() == 0 ? palabra : linea + " " + palabra;
            if (PixelFont.width(prueba, 1) > TEXTO_ANCHO && linea.length() > 0) {
                lineas.add(linea.toString());
                linea = new StringBuilder(palabra);
            } else {
                linea = new StringBuilder(prueba);
            }
        }
        if (linea.length() > 0) lineas.add(linea.toString());
    }

    // copia del retrato toda de color crema (para dibujar el contorno tipo sticker)
    private static BufferedImage siluetaBlanca(BufferedImage src) {
        BufferedImage r = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < r.getHeight(); y++)
            for (int x = 0; x < r.getWidth(); x++)
                if ((src.getRGB(x, y) >>> 24) > 0) r.setRGB(x, y, 0xFFFFF8E8);
        return r;
    }
}
