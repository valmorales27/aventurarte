package aventurarte.juego;

import aventurarte.core.Assets;
import aventurarte.modelo.TipoEnemigo;

import java.awt.image.BufferedImage;

/**
 * Un enemigo y su "cerebro".  Lo mueve SU PROPIO hilo (HiloEnemigo) llamando a actualizar() ~60 veces por segundo.
 * x = centro de los pies. Los campos que lee el hilo de dibujo/colisiones son volatile.
 *
 *   LANZADOR : PATRULLA -> (ve al jugador de frente a < 220 px) LANZA -> RECARGA (2 s) -> PATRULLA
 *   RAPIDO   : PATRULLA lenta -> (ve al jugador a < 260 px) ALERTA 0.4 s -> CORRE a 2.5x del jugador -> (borde del tramo) PATRULLA
 *   daño     : GOLPE (2 cuadros) y DERROTA (4 cuadros); despues muere y su hilo termina.
 */
public final class Enemigo {
    public enum Estado { PATRULLA, ALERTA, CORRE, LANZA, RECARGA, GOLPE, DERROTA }

    public static final int PISO = 214;
    public final TipoEnemigo tipo;
    public final int izq, der;                 // tramo por donde se mueve
    public volatile float x;
    public volatile boolean derecha = false;
    public volatile Estado estado = Estado.PATRULLA;
    public volatile boolean muerto;

    private int vida = 2;
    private float animT, estadoT, enfriamiento;
    private boolean disparado;
    private final BufferedImage hoja;

    public Enemigo(TipoEnemigo tipo, int x, int izq, int der) {
        this.tipo = tipo; this.x = x; this.izq = izq; this.der = der;
        this.hoja = Assets.img("enemigos/" + tipo.id + "/hoja.png");
    }

    public boolean estaMuerto() { return muerto; }

    /** Puede lastimar al jugador al tocarlo? */
    public boolean peligroso() { return estado != Estado.GOLPE && estado != Estado.DERROTA; }

    // caja de choque
    public float izqCaja() { return x - 14; }
    public float derCaja() { return x + 14; }
    public float arribaCaja() { return PISO - 50; }

    // ---------------------------------------------------------------- cerebro (se ejecuta en el hilo del enemigo)
    public synchronized void actualizar(float dt, Mundo m) {
        animT += dt;
        estadoT += dt;
        if (enfriamiento > 0) enfriamiento -= dt;
        float jx = m.jugador.x, jy = m.jugador.y;
        float dist = Math.abs(jx - x);
        boolean alAlcance = Math.abs(jy - PISO) < 100 && !m.jugador.derrotado;

        switch (estado) {
            case PATRULLA: {
                float v = tipo.rapido ? 30 : 35;
                x += (derecha ? 1 : -1) * v * dt;
                if (x >= der) { x = der; derecha = false; }
                else if (x <= izq) { x = izq; derecha = true; }
                if (enfriamiento <= 0 && alAlcance) {
                    if (!tipo.rapido && dist < 220 && ((jx > x) == derecha)) cambiar(Estado.LANZA);
                    else if (tipo.rapido && dist < 260) { derecha = jx > x; cambiar(Estado.ALERTA); m.audio.sfx("alerta"); }
                }
                break;
            }
            case LANZA:
                if (!disparado && estadoT >= 0.2f) {                      // cuadro 2 = suelta
                    disparado = true;
                    float cx = derecha ? x - 24 + 43 : x - 24 + 4, cy = PISO - 62 + 37;
                    m.proyectiles.add(new Proyectil(cx, cy, derecha ? 150 : -150, false, "enemigos/" + tipo.id + "/proyectil", 420));
                    m.audio.sfx("lanzar");
                }
                if (estadoT >= 0.4f) cambiar(Estado.RECARGA);
                break;
            case RECARGA:
                if (estadoT >= 2f) cambiar(Estado.PATRULLA);              // recarga de 2 s
                break;
            case ALERTA:
                if (estadoT >= 0.4f) { derecha = jx > x; cambiar(Estado.CORRE); }
                break;
            case CORRE: {
                x += (derecha ? 1 : -1) * Jugador.VEL * 2.5f * dt;        // 2.5 veces la velocidad del jugador
                if (x >= der) { x = der; derecha = false; cambiar(Estado.PATRULLA); enfriamiento = 2f; }
                else if (x <= izq) { x = izq; derecha = true; cambiar(Estado.PATRULLA); enfriamiento = 2f; }
                break;
            }
            case GOLPE:
                if (estadoT >= 0.3f) { cambiar(Estado.PATRULLA); enfriamiento = 0.6f; }
                break;
            case DERROTA:
                if (estadoT >= 4 * 0.12f + 0.25f) muerto = true;
                break;
        }
    }

    private void cambiar(Estado e) { estado = e; estadoT = 0; disparado = false; }

    /** El jugador lo toca/le pega. Devuelve true si este golpe lo DERROTA. */
    public synchronized boolean golpear(int dano, Mundo m) {
        if (estado == Estado.DERROTA || muerto) return false;
        vida -= dano;
        if (vida <= 0) { cambiar(Estado.DERROTA); m.audio.sfx("derrota_enemigo"); return true; }
        cambiar(Estado.GOLPE);
        m.audio.sfx("impacto");
        return false;
    }

    // ---------------------------------------------------------------- sprite
    public synchronized BufferedImage frame() {
        int fila, col;
        switch (estado) {
            case PATRULLA: fila = 1; col = (int) (animT / 0.1f) % 6; break;
            case CORRE: fila = 1; col = (int) (animT / 0.09f) % 6; break;
            case ALERTA: case LANZA: fila = 2; col = Math.min(3, (int) (estadoT / 0.1f)); break;
            case GOLPE: fila = 3; col = (int) (estadoT / 0.15f) % 2; break;
            case DERROTA: fila = 4; col = Math.min(3, (int) (estadoT / 0.12f)); break;
            default: fila = 0; col = new int[]{0, 1, 0, 2}[(int) (animT / 0.6f) % 4];       // RECARGA = reposo
        }
        return hoja.getSubimage(col * 48, fila * 64, 48, 64);
    }
}
