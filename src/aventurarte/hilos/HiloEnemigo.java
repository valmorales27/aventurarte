package aventurarte.hilos;

import aventurarte.juego.Enemigo;
import aventurarte.juego.Mundo;

/** Clase del HILO 2 - ENEMIGOS
    Hay un hilo por cada enemigo y cada tipo se comporta diferente
    el comportamiento esta en Enemigo.actualizar():
        lanzadores: patrullan, se detienen, lanzan un proyectil y recargan 2 s
        rápidos: caminan lento, ven al jugador y corren a 2.5x su velocidad normal
    El hilo solo trabaja a ~60 veces por segundo (sleep de 16 ms) y lo detenos con la pausa
 */
public class HiloEnemigo extends Thread {

    private Enemigo enemigo;
    private Mundo mundo;
    private PausaControl pausa;

    // constructor: recibe el enemigo que va a mover este hilo
    public HiloEnemigo(Enemigo enemigo, Mundo mundo, PausaControl pausa) {
        super("hilo-enemigo-" + enemigo.tipo.id);   // nombre del hilo
        setDaemon(true);
        this.enemigo = enemigo;
        this.mundo = mundo;
        this.pausa = pausa;
    }

    @Override
    public void run() {
        long previo = System.nanoTime();

        // el hilo vive mientras el enemigo no haya sido derrotado
        while (!enemigo.estaMuerto()) {

            // si el juego esta en pausa, el hilo espera, si el nivel acabo, salimos
            if (!pausa.esperar()) {
                break;
            }

            // calculamos cuántos segundos pasaron desde la vuelta anterior (máximo 50 ms)
            long ahora = System.nanoTime();
            float dt = Math.min(0.05f, (ahora - previo) / 1e9f);
            previo = ahora;

            // el enemigo decide que hacer (patrullar, alertarse, lanzar...)
            enemigo.actualizar(dt, mundo);

            // pausa corta (~60 veces por segundo)
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
