package aventurarte.core;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carga de imagenes desde la carpeta res/img
 * BufferedImage fondo = Assets.img("menu/menu/fondo_480x270.png");
 */
public final class Assets {
    private static final Map<String, BufferedImage> CACHE = new ConcurrentHashMap<>();
    private static final Map<BufferedImage, BufferedImage> VOLTEADAS = new ConcurrentHashMap<>();

    private Assets() {}

    /** Ruta relativa a res/img, por ejemplo "hud/paneles/panel_vidas.png" */
    public static BufferedImage img(String ruta) {
        BufferedImage b = CACHE.get(ruta);
        if (b != null) return b;
        try (InputStream in = Assets.class.getResourceAsStream("/img/" + ruta)) {
            if (in == null)
                throw new IllegalStateException("No encuentro el recurso /img/" + ruta
                        + "  -> revisar que la carpeta 'res' este marcada como Resources Root");
            BufferedImage raw = ImageIO.read(in);
            BufferedImage c = new BufferedImage(raw.getWidth(), raw.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = c.createGraphics();
            g.drawImage(raw, 0, 0, null);
            g.dispose();
            CACHE.put(ruta, c);
            return c;
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer /img/" + ruta, e);
        }
    }

    public static boolean existe(String ruta) {
        return CACHE.containsKey(ruta) || Assets.class.getResource("/img/" + ruta) != null;
    }

    /** Una fila de una hoja de sprites: n celdas de w x h empezando en la columna 0 */
    public static BufferedImage[] fila(BufferedImage hoja, int fila, int n, int w, int h) {
        BufferedImage[] r = new BufferedImage[n];
        for (int i = 0; i < n; i++) r[i] = hoja.getSubimage(i * w, fila * h, w, h);
        return r;
    }

    /** Copia horizontalmente invertida (caché) */
    public static BufferedImage voltear(BufferedImage src) {
        return VOLTEADAS.computeIfAbsent(src, s -> {
            BufferedImage r = new BufferedImage(s.getWidth(), s.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = r.createGraphics();
            g.drawImage(s, s.getWidth(), 0, -s.getWidth(), s.getHeight(), null);
            g.dispose();
            return r;
        });
    }

    /** Lee un archivo de texto de res/data (por ejemplo camino_puntos.csv) */
    public static String texto(String rutaEnData) {
        try (InputStream in = Assets.class.getResourceAsStream("/data/" + rutaEnData)) {
            if (in == null) throw new IllegalStateException("No encuentro /data/" + rutaEnData);
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
