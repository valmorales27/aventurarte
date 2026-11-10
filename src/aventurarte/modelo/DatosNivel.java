package aventurarte.modelo;

import java.util.ArrayList;
import java.util.List;

/**
 * Todo lo que DEFINE un nivel (no cambia mientras se juega): su largo, obstaculos, plataformas, monedas, llave, puerta y enemigos.
 * Las partidas usan una copia viva de esto (Mundo). Los niveles concretos estan en Niveles.java.
 */
public final class DatosNivel {
    /** Un enemigo colocado: tipo, donde empieza (x) y el tramo por donde se mueve [izq, der]. */
    public static final class EnemigoDef {
        public final TipoEnemigo tipo;
        public final int x, izq, der;
        public EnemigoDef(TipoEnemigo tipo, int x, int izq, int der) { this.tipo = tipo; this.x = x; this.izq = izq; this.der = der; }
    }

    public final int numero;                 // 0..4
    public final String nombre;
    public final String musica;
    public final boolean noche;
    public final int ancho;                  // largo del nivel en pixeles
    public final String[] imgObstaculos;     // nombres de archivo en res/img/niveles/N/obstaculos/
    public final List<int[]> obstaculos = new ArrayList<>();   // {x, indice de imgObstaculos}
    public final List<int[]> plataformas = new ArrayList<>();  // {x, yDeArriba, bloques}
    public final List<int[]> monedas = new ArrayList<>();      // {x, y} esquina de la moneda de 14x14
    public final List<EnemigoDef> enemigos = new ArrayList<>();
    public int llaveX, llaveY;
    public int puertaX;
    public int medioDesplazamiento;          // para que el "lugar representativo" quede junto a la puerta

    public DatosNivel(int numero, String nombre, String musica, boolean noche, int ancho, String... imgObstaculos) {
        this.numero = numero; this.nombre = nombre; this.musica = musica; this.noche = noche; this.ancho = ancho;
        this.imgObstaculos = imgObstaculos;
        this.puertaX = ancho - 130;
    }

    // ---------------------------------------------------------------- ayudas para construir niveles
    public DatosNivel obstaculo(int x, int tipo) { obstaculos.add(new int[]{x, tipo}); return this; }

    public DatosNivel plataforma(int x, int yArriba, int bloques) { plataformas.add(new int[]{x, yArriba, bloques}); return this; }

    public DatosNivel moneda(int x, int y) { monedas.add(new int[]{x, y}); return this; }

    /** n monedas en fila horizontal. */
    public DatosNivel fila(int x, int y, int n, int paso) { for (int i = 0; i < n; i++) moneda(x + i * paso, y); return this; }

    /** n monedas formando un arco (parabola) centrado en xCentro: en los extremos estan a yBase y en el centro a yBase-alto. */
    public DatosNivel arco(int xCentro, int yBase, int n, int paso, int alto) {
        double mid = (n - 1) / 2.0;
        for (int i = 0; i < n; i++) {
            double d = mid == 0 ? 0 : (i - mid) / mid;
            moneda((int) Math.round(xCentro + (i - mid) * paso - 7), (int) Math.round(yBase - alto * (1 - d * d)));
        }
        return this;
    }

    public DatosNivel llave(int x, int y) { llaveX = x; llaveY = y; return this; }

    public DatosNivel enemigo(TipoEnemigo t, int x, int izq, int der) { enemigos.add(new EnemigoDef(t, x, izq, der)); return this; }

    /** Revisa que el nivel cumpla el PDF (monedas y enemigos por nivel). Lanza error si no. */
    public void validar() {
        if (monedas.size() != Inventario.MONEDAS_TOTAL[numero])
            throw new IllegalStateException("Nivel " + (numero + 1) + ": hay " + monedas.size() + " monedas y deben ser " + Inventario.MONEDAS_TOTAL[numero]);
        if (enemigos.size() != Inventario.ENEMIGOS[numero])
            throw new IllegalStateException("Nivel " + (numero + 1) + ": hay " + enemigos.size() + " enemigos y deben ser " + Inventario.ENEMIGOS[numero]);
        if (obstaculos.size() < 3) throw new IllegalStateException("Nivel " + (numero + 1) + ": faltan obstaculos");
    }
}
