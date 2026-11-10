package aventurarte.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Inventario compartido por los 3 hilos (temporizador, enemigos, eventos aleatorios) y por el hilo que dibuja.
 * TODOS los metodos que escriben son synchronized sobre el mismo objeto, asi que no se pierden monedas ni eventos.
 * El hilo de dibujo NUNCA lee los campos directo: pide snapshot(), una copia inmutable, y dibuja con ella sin bloquear a nadie.
 */
public class Inventario {

    public enum Tipo { MONEDA, LLAVE, GOLPE, EVENTO, PUERTA, VIDA, TIEMPO, FIN }

    /** Una linea del REGISTRO.  seg = segundos transcurridos en el nivel. Los nombres de icono coinciden con iconos/registro_<tipo>.png */
    public static final class Registro {
        public final int nivel, seg;
        public final Tipo tipo;
        public final String texto;           // maximo 19 caracteres, solo A-Z 0-9 : + - ! ?
        Registro(int nivel, int seg, Tipo tipo, String texto) { this.nivel = nivel; this.seg = seg; this.tipo = tipo; this.texto = texto; }
        public String icono() { return "registro_" + tipo.name().toLowerCase(); }
    }

    public static final int NIVELES = 5;
    public static final int[] MONEDAS_TOTAL = {10, 15, 20, 30, 30};
    public static final int[] ENEMIGOS = {1, 2, 3, 4, 5};
    public static final String[] LLAVE = {"PELUCHE", "ZAPATO", "SUETER", "TAMAL", "JARRITO"};

    private final int[] monedas = new int[NIVELES];
    private final boolean[] llave = new boolean[NIVELES];
    private final boolean[] completado = new boolean[NIVELES];
    private final int[] tiempo = new int[NIVELES];            // segundos usados al completar (0 = aun no)
    private final int[] intentosUsados = new int[NIVELES];
    private final int[] estrellas = new int[NIVELES];          // 0..3 estrellas ganadas en cada nivel (esta partida)
    private final List<List<Registro>> log = new ArrayList<>();
    private int nivelActual = 0;

    public Inventario() { for (int i = 0; i < NIVELES; i++) log.add(new ArrayList<>()); }

    // ------------------------------------------------------------ escritura (cualquier hilo)
    public synchronized void iniciarNivel(int nivel) { nivelActual = nivel; }

    public synchronized boolean agregarMoneda(int nivel, int seg) {
        if (monedas[nivel] >= MONEDAS_TOTAL[nivel]) return false;
        monedas[nivel]++;
        log.get(nivel).add(new Registro(nivel, seg, Tipo.MONEDA, "MONEDA +1"));
        return true;
    }

    public synchronized void tomarLlave(int nivel, int seg) {
        if (llave[nivel]) return;
        llave[nivel] = true;
        log.get(nivel).add(new Registro(nivel, seg, Tipo.LLAVE, "LLAVE: " + LLAVE[nivel]));
    }

    public synchronized void registrar(int nivel, int seg, Tipo tipo, String texto) {   // GOLPE, EVENTO, PUERTA, TIEMPO...
        log.get(nivel).add(new Registro(nivel, seg, tipo, texto));
    }

    public synchronized int perderIntento(int nivel, int seg) {
        intentosUsados[nivel]++;
        log.get(nivel).add(new Registro(nivel, seg, Tipo.VIDA, "INTENTO PERDIDO"));
        return 3 - intentosUsados[nivel];                    // intentos que quedan
    }

    public synchronized void completarNivel(int nivel, int seg) { completarNivel(nivel, seg, 1); }

    /** Igual, pero guarda tambien cuantas estrellas (1..3) se ganaron en este nivel. */
    public synchronized void completarNivel(int nivel, int seg, int estrellasGanadas) {
        completado[nivel] = true; tiempo[nivel] = seg; estrellas[nivel] = estrellasGanadas;
        log.get(nivel).add(new Registro(nivel, seg, Tipo.FIN, "NIVEL COMPLETADO"));
    }

    /** Al reiniciar un nivel (menu de pausa) se borra lo del nivel actual. */
    public synchronized void reiniciarNivel(int nivel) {
        monedas[nivel] = 0; llave[nivel] = false; completado[nivel] = false; tiempo[nivel] = 0; estrellas[nivel] = 0;
        log.get(nivel).clear();                               // los intentos usados NO se borran
    }

    // ------------------------------------------------------------ REPETIR un nivel que ya estaba completado
    /**
     * Copia de TODO lo de un nivel. Se guarda antes de repetirlo para poder devolverlo tal cual
     * si la repeticion sale peor (o si el jugador abandona o pierde).
     */
    public static final class Respaldo {
        final int monedas, tiempo, intentos, estrellas;
        final boolean llave, completado;
        final List<Registro> log;
        Respaldo(int monedas, int tiempo, int intentos, int estrellas, boolean llave, boolean completado, List<Registro> log) {
            this.monedas = monedas; this.tiempo = tiempo; this.intentos = intentos; this.estrellas = estrellas;
            this.llave = llave; this.completado = completado; this.log = log;
        }
        public int getMonedas() { return monedas; }
        public int getTiempo() { return tiempo; }
        public int getIntentos() { return intentos; }
        public int getEstrellas() { return estrellas; }
    }

    public synchronized Respaldo respaldarNivel(int nivel) {
        return new Respaldo(monedas[nivel], tiempo[nivel], intentosUsados[nivel], estrellas[nivel],
                llave[nivel], completado[nivel], new ArrayList<>(log.get(nivel)));
    }

    public synchronized void restaurarNivel(int nivel, Respaldo r) {
        monedas[nivel] = r.monedas; tiempo[nivel] = r.tiempo; intentosUsados[nivel] = r.intentos; estrellas[nivel] = r.estrellas;
        llave[nivel] = r.llave; completado[nivel] = r.completado;
        log.get(nivel).clear(); log.get(nivel).addAll(r.log);
    }

    /** Empieza una repeticion: monedas, llave, intentos y registro en cero. El nivel sigue "completado" (conserva tiempo y estrellas). */
    public synchronized void empezarRepeticion(int nivel) {
        monedas[nivel] = 0; llave[nivel] = false; intentosUsados[nivel] = 0;
        log.get(nivel).clear();
    }

    /** Reinicio (menu de pausa o tiempo agotado) DENTRO de una repeticion: igual que reiniciarNivel pero sigue completado. */
    public synchronized void reiniciarRepeticion(int nivel) {
        monedas[nivel] = 0; llave[nivel] = false;
        log.get(nivel).clear();                               // los intentos usados NO se borran
    }

    // ------------------------------------------------------------ lectura para el dibujo
    public static final class Snapshot {
        public final int[] monedas, tiempo, intentosUsados, estrellas;
        public final boolean[] llave, completado;
        public final List<List<Registro>> log;
        public final int nivelActual;
        Snapshot(Inventario i) {
            monedas = i.monedas.clone(); tiempo = i.tiempo.clone(); intentosUsados = i.intentosUsados.clone(); estrellas = i.estrellas.clone();
            llave = i.llave.clone(); completado = i.completado.clone(); nivelActual = i.nivelActual;
            List<List<Registro>> l = new ArrayList<>();
            for (List<Registro> r : i.log) l.add(Collections.unmodifiableList(new ArrayList<>(r)));
            log = Collections.unmodifiableList(l);
        }
        /** "completado" | "en_curso" | "bloqueado"  (el nivel N+1 se desbloquea al completar el N) */
        public String estado(int nivel) {
            if (nivel < 0 || nivel >= completado.length) return "en_curso";     // pestana TODO (5): nunca bloqueada
            if (completado[nivel]) return "completado";
            if (nivel == 0 || completado[nivel - 1]) return "en_curso";
            return "bloqueado";
        }
        public int totalEstrellas() { int s = 0; for (int e : estrellas) s += e; return s; }
        public int totalMonedas() { int s = 0; for (int m : monedas) s += m; return s; }
        public int totalLlaves() { int s = 0; for (boolean b : llave) if (b) s++; return s; }
        public int totalTiempo() { int s = 0; for (int t : tiempo) s += t; return s; }
        public int totalIntentos() { int s = 0; for (int u : intentosUsados) s += u; return s; }
        public boolean juegoCompleto() { for (boolean b : completado) if (!b) return false; return true; }
    }

    public synchronized Snapshot snapshot() { return new Snapshot(this); }
}
