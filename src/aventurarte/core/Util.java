package aventurarte.core;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Clase con herraminetas para dar formato, etc */
public final class Util {
    private Util() {}

    /** 125 -> "2:05" */
    public static String mmss(int segundos) { return String.format("%d:%02d", segundos / 60, segundos % 60); }

    public static float clamp(float v, float a, float b) { return Math.max(a, Math.min(b, v)); }

    public static int clamp(int v, int a, int b) { return Math.max(a, Math.min(b, v)); }

    private static final Map<String, BufferedImage> TINTES = new ConcurrentHashMap<>();

    /** Copia de la imagen pintada de un solo color (conserva la transparencia). Sirve para la llave "no encontrada" en gris. */
    public static BufferedImage tinte(BufferedImage src, int rgb) {
        return TINTES.computeIfAbsent(System.identityHashCode(src) + ":" + rgb, k -> {
            BufferedImage r = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < r.getHeight(); y++)
                for (int x = 0; x < r.getWidth(); x++) {
                    int a = src.getRGB(x, y) >>> 24;
                    if (a > 0) r.setRGB(x, y, (a << 24) | (rgb & 0xFFFFFF));
                }
            return r;
        });
    }
}
