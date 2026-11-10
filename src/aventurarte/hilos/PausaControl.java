package aventurarte.hilos;

/**
 * Objeto COMPARTIDO por los tres hilos (temporizador, enemigos y eventos aleatorios) para pausar el juego.
 * Es igual que los objetos compartidos de clase (CuentaBanco, Minisuper...): sus metodos son synchronized,
 * asi que solo un hilo a la vez puede cambiar o leer las banderas.
 *
 *  - pausar()    : el juego se detiene (menu de pausa, historial abierto, cartel de inicio...)
 *  - reanudar()  : despierta a todos los hilos que estaban esperando (notifyAll)
 *  - detener()   : fin definitivo (salir del nivel o reiniciarlo): los hilos terminan
 *
 * Cada hilo llama a esperar() en cada vuelta de su ciclo; si esta en pausa se duerme con wait() y NO gasta CPU.
 */
public class PausaControl {

    // banderas (solo se tocan dentro de metodos synchronized)
    private boolean pausado;
    private boolean detenido;

    // constructor: el nivel arranca en pausa hasta que desaparece el cartel de inicio
    public PausaControl(boolean iniciaPausado) {
        this.pausado = iniciaPausado;
        this.detenido = false;
    }

    public synchronized void pausar() {
        pausado = true;
    }

    public synchronized void reanudar() {
        pausado = false;
        notifyAll();                  // despertamos a los hilos que estaban en wait()
    }

    public synchronized void detener() {
        detenido = true;
        notifyAll();                  // tambien despertamos a los que estaban en pausa para que terminen
    }

    public synchronized boolean estaPausado() {
        return pausado;
    }

    public synchronized boolean estaDetenido() {
        return detenido;
    }

    /**
     * Si el juego esta en pausa, el hilo que llama se queda dormido con wait() hasta que lo reanuden.
     * Devuelve false si hay que terminar el hilo (detener() o interrupt()).
     */
    public synchronized boolean esperar() {
        while (pausado && !detenido) {
            try {
                wait();
            } catch (InterruptedException e) {
                return false;
            }
        }
        return !detenido;
    }

    /**
     * Duerme "ms" milisegundos de juego ACTIVO: si en medio se pausa, el contador se congela.
     * Devuelve los ms realmente dormidos, o -1 si hay que terminar el hilo.
     * (No es synchronized porque Thread.sleep no debe tener bloqueado el objeto: pausar() tiene que poder entrar.)
     */
    public long dormir(long ms) {
        long dormido = 0;

        while (dormido < ms) {
            // si estamos en pausa, aqui se queda esperando
            if (!esperar()) {
                return -1;
            }

            // dormimos en pedacitos de 20 ms para poder reaccionar rapido a una pausa
            long trozo = Math.min(20, ms - dormido);
            long inicio = System.nanoTime();
            try {
                Thread.sleep(trozo);
            } catch (InterruptedException e) {
                return -1;
            }
            dormido = dormido + Math.max(1, (System.nanoTime() - inicio) / 1_000_000L);
        }

        return dormido;
    }
}
