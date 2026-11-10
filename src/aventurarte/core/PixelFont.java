package aventurarte.core;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

/** Clase para la fuente del juego: pixel */
public class PixelFont {
    public static final String ORDER = " ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789:.!?/+-%";
    private final BufferedImage atlas;
    private final Map<Integer, BufferedImage> cache = new HashMap<>();

    public PixelFont(BufferedImage atlasBlanca) { this.atlas = atlasBlanca; }

    private BufferedImage tinted(Color c) {
        return cache.computeIfAbsent(c.getRGB() & 0xFFFFFF, k -> {
            BufferedImage t = new BufferedImage(atlas.getWidth(), atlas.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < t.getHeight(); y++)
                for (int x = 0; x < t.getWidth(); x++) {
                    int a = atlas.getRGB(x, y) >>> 24;
                    if (a > 0) t.setRGB(x, y, (a << 24) | k);
                }
            return t;
        });
    }

    public static int width(String s, int scale) { return 6 * scale * s.length() - scale; }

    //  carácteres extra
    private static final String ACENTOS_DE = "\u00C1\u00C9\u00CD\u00D3\u00DA\u00DC\u00D1", ACENTOS_BASE = "AEIOUUN";
    private static final Map<Character, String[]> EXTRA = new HashMap<>();
    static {
        EXTRA.put(',', new String[]{".....", ".....", ".....", ".....", "..#..", "..#..", ".#..."});
        EXTRA.put('"', new String[]{".#.#.", ".#.#.", ".#.#.", ".....", ".....", ".....", "....."});
        EXTRA.put('\'', new String[]{"..#..", "..#..", ".#...", ".....", ".....", ".....", "....."});
        EXTRA.put('(', new String[]{"...#.", "..#..", ".#...", ".#...", ".#...", "..#..", "...#."});
        EXTRA.put(')', new String[]{".#...", "..#..", "...#.", "...#.", "...#.", "..#..", ".#..."});
        EXTRA.put('_', new String[]{".....", ".....", ".....", ".....", ".....", ".....", "#####"});
        EXTRA.put(';', new String[]{".....", "..#..", ".....", ".....", "..#..", "..#..", ".#..."});
    }

    /** Texto plano. scale = 1, 2, 3... (en coordenadas 480x270) acepta acentos, signos, " ( ) _ */
    public void draw(Graphics2D g, String s, int x, int y, int scale, Color c) {
        BufferedImage t = tinted(c);
        s = s.toUpperCase();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            int px = x + i * 6 * scale;
            int idx = ORDER.indexOf(ch);
            if (idx > 0) { g.drawImage(t.getSubimage(idx * 6, 0, 5, 7), px, y, 5 * scale, 7 * scale, null); continue; }
            if (idx == 0) continue;
            int a = ACENTOS_DE.indexOf(ch);
            if (a >= 0) {                                   // letra normal + marca arriba
                g.drawImage(t.getSubimage(ORDER.indexOf(ACENTOS_BASE.charAt(a)) * 6, 0, 5, 7), px, y, 5 * scale, 7 * scale, null);
                g.setColor(c);
                if (ch == '\u00DC') { g.fillRect(px + scale, y - 2 * scale, scale, scale); g.fillRect(px + 3 * scale, y - 2 * scale, scale, scale); }
                else if (ch == '\u00D1') { g.fillRect(px + scale, y - scale, scale, scale); g.fillRect(px + 2 * scale, y - 2 * scale, scale, scale); g.fillRect(px + 3 * scale, y - scale, scale, scale); }
                else { g.fillRect(px + 3 * scale, y - 2 * scale, scale, scale); g.fillRect(px + 2 * scale, y - scale, scale, scale); }
                continue;
            }
            if (ch == '\u00A1' || ch == '\u00BF') {            // los signos de apertura son "!" y "?" volteados 180 grados
                int k = ORDER.indexOf(ch == '\u00A1' ? '!' : '?');
                g.drawImage(t.getSubimage(k * 6, 0, 5, 7), px + 5 * scale, y + 7 * scale, px, y, 0, 0, 5, 7, null);
                continue;
            }
            String[] f = EXTRA.get(ch);
            if (f != null) {
                g.setColor(c);
                for (int r = 0; r < 7; r++)
                    for (int q = 0; q < 5; q++)
                        if (f[r].charAt(q) == '#') g.fillRect(px + q * scale, y + r * scale, scale, scale);
            }
        }
    }

    /** Texto con contorno oscuro de 1 px */
    public void drawOutlined(Graphics2D g, String s, int x, int y, int scale, Color c, Color outline) {
        int[][] o = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {1, -1}, {-1, 1}, {1, 1}, {0, 2}};
        for (int[] d : o) draw(g, s, x + d[0], y + d[1], scale, outline);
        draw(g, s, x, y, scale, c);
    }
}
