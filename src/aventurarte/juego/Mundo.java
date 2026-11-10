package aventurarte.juego;

import aventurarte.core.Assets;
import aventurarte.core.Audio;
import aventurarte.modelo.DatosNivel;
import aventurarte.modelo.PerfilJugador;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * El nivel "vivo": jugador, enemigos, proyectiles, monedas que faltan, etc.
 * Lo comparten el hilo del juego y los hilos de enemigos / eventos, por eso:
 *   - las listas que otros hilos modifican (enemigos, proyectiles) son CopyOnWriteArrayList
 *   - los efectos de los eventos son volatile
 */
public final class Mundo {
    public enum Efecto { NINGUNO, VIENTO, LLUVIA, IMPULSO }

    public static final class Obstaculo {
        public final int x, y, w, h;
        public final BufferedImage img;
        Obstaculo(int x, BufferedImage img) { this.x = x; this.img = img; this.w = img.getWidth(); this.h = img.getHeight(); this.y = 218 - h; }
        public float izq() { return x + 2; }
        public float der() { return x + w - 2; }
        public float arriba() { return y; }
        public float abajo() { return y + h; }
        public float centro() { return x + w / 2f; }
    }

    public static final class Plataforma {
        public final int x, y, w, bloques;
        Plataforma(int x, int y, int bloques) { this.x = x; this.y = y; this.bloques = bloques; this.w = 16 * bloques + 2; }
    }

    public static final class Moneda {
        public final int x, y;
        public boolean tomada;
        Moneda(int x, int y) { this.x = x; this.y = y; }
    }

    public final DatosNivel d;
    public final Audio audio;
    public final Jugador jugador;
    public final List<Obstaculo> obstaculos = new ArrayList<>();
    public final List<Plataforma> plataformas = new ArrayList<>();
    public final List<Moneda> monedas = new ArrayList<>();
    public final List<Enemigo> enemigos = new CopyOnWriteArrayList<>();
    public final List<Proyectil> proyectiles = new CopyOnWriteArrayList<>();
    public boolean llaveTomada, puertaAbierta;

    // efectos de los eventos aleatorios (los escribe el hilo de eventos)
    private volatile Efecto efecto = Efecto.NINGUNO;
    private volatile float efectoRestante;
    private volatile int efectoDir;
    private volatile String textoEvento = "";
    private volatile float textoEventoT;

    public Mundo(DatosNivel d, PerfilJugador perfil, Audio audio) {
        this.d = d;
        this.audio = audio;
        this.jugador = new Jugador(perfil);
        String base = "niveles/" + (d.numero + 1) + "/obstaculos/";
        for (int[] o : d.obstaculos) obstaculos.add(new Obstaculo(o[0], Assets.img(base + d.imgObstaculos[o[1]])));
        for (int[] p : d.plataformas) plataformas.add(new Plataforma(p[0], p[1], p[2]));
        for (int[] c : d.monedas) monedas.add(new Moneda(c[0], c[1]));
        for (DatosNivel.EnemigoDef e : d.enemigos) enemigos.add(new Enemigo(e.tipo, e.x, e.izq, e.der));
    }

    // ---------------------------------------------------------------- eventos aleatorios
    public synchronized void activarEfecto(Efecto e, float segundos, int dir) { efecto = e; efectoRestante = segundos; efectoDir = dir; }

    public void avisoEvento(String texto) { textoEvento = texto; textoEventoT = 3f; }

    /** Lo llama el hilo del juego cada cuadro (asi la pausa tambien congela los efectos). */
    public synchronized void actualizarEfectos(float dt) {
        if (efectoRestante > 0) { efectoRestante -= dt; if (efectoRestante <= 0) efecto = Efecto.NINGUNO; }
        if (textoEventoT > 0) textoEventoT -= dt;
    }

    public Efecto getEfecto() { return efecto; }
    public float multiplicadorVelocidad() { Efecto e = efecto; return e == Efecto.LLUVIA ? 0.6f : e == Efecto.IMPULSO ? 1.4f : 1f; }
    public float viento() { return efecto == Efecto.VIENTO ? efectoDir * 70f : 0f; }
    public int direccionViento() { return efectoDir; }
    public String getTextoEvento() { return textoEvento; }
    public float getTextoEventoT() { return textoEventoT; }

    public boolean hayEnemigosRapidos() {
        for (Enemigo e : enemigos) if (e.tipo.rapido) return true;
        return false;
    }

    public static boolean solapa(float ax, float ay, float aw, float ah, float bx, float by, float bw, float bh) {
        return ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by;
    }
}
