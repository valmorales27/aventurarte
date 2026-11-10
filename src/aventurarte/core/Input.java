package aventurarte.core;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/** Clase para las acciones del usuario (teclado y mouse).
    Los listeners de Swing (hilo EDT) se pasan a esta clase, y el hilo del juego los lee.
 */
public final class Input {
    private final Set<Integer> teclas = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<Integer> pulsadas = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Character> escritas = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<int[]> clics = new ConcurrentLinkedQueue<>();
    private final AtomicInteger rueda = new AtomicInteger();

    public volatile int mx = -1, my = -1;
    public volatile boolean mouseAbajo;

    // ---------------------------------------------------------------- ESCRITURA (la llama el EDT)
    void teclaAbajo(int vk) { if (teclas.add(vk)) pulsadas.add(vk); }
    void teclaArriba(int vk) { teclas.remove(vk); }
    void caracter(char c) { escritas.add(c); }
    void mouseSoltado(int x, int y) { clics.add(new int[]{x, y}); }
    void rueda(int n) { rueda.addAndGet(n); }
    void perdioFoco() { teclas.clear(); mouseAbajo = false; }

    // ---------------------------------------------------------------- LECTURA (la llama el juego)
    /** true mientras la tecla este sostenida */
    public boolean tecla(int vk) { return teclas.contains(vk); }

    /** Siguiente tecla que se acaba de pulsar (una vez por pulsación) o -1 */
    public int sigPulsada() { Integer i = pulsadas.poll(); return i == null ? -1 : i; }

    /** Siguiente caracter escrito (para el nombre) o 0 */
    public char sigEscrita() { Character c = escritas.poll(); return c == null ? 0 : c; }

    /** Siguiente clic (al soltyar el boton) o null */
    public int[] sigClic() { return clics.poll(); }

    /** Saca los clics pendientes de una vez (para repartirlos entre varios elementos de la pantalla) */
    public java.util.List<int[]> clics() {
        java.util.List<int[]> r = new java.util.ArrayList<>();
        int[] c;
        while ((c = clics.poll()) != null) r.add(c);
        return r;
    }

    /** Giro acumulado de la rueda -> cambio de "ventana", desde la ultima lectura (negativo = hacia arriba) */
    public int rueda() { return rueda.getAndSet(0); }

    /** Se llama al cambiar de pantalla para que una pulsación no se pase a la siguiente */
    public void limpiarEventos() {
        pulsadas.clear(); escritas.clear(); clics.clear(); rueda.set(0);
    }
}
