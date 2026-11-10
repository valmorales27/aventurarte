package aventurarte.hilos;

/** Clase para HILO 1 - TEMPORIZADOR del nivel
    Cuenta hasta 180 s (3 minutos). Respeta la pausa
    Otro hilo (el de eventos aleatorios) puede darle o quitarle tiempo con sumarSegundos(),
    por eso los métodos que tocan el tiempo son synchronized
 */
public class HiloTemporizador extends Thread {

    public static final int LIMITE = 180;  // segundos que dura el nivel

    private PausaControl pausa;
    private double transcurrido;      // segundos que ya pasaron
    private boolean agotado;      // true cuando se acaba el tiempo

    // constructor
    public HiloTemporizador(PausaControl pausa) {
        super("hilo-temporizador");      // nombre del hilo
        setDaemon(true);      // si se cierra el juego, este hilo no lo detiene
        this.pausa = pausa;
        this.transcurrido = 0;
        this.agotado = false;
    }

    @Override
    public void run() {
        while (!isAgotado()) {
            // dormimos 50 ms de juego activo (si hay pausa, se detiene)
            long ms = pausa.dormir(50);

            // -1 significa que el nivel terminó y hay se termina el hilo
            if (ms < 0) {
                break;
            }

            avanzar(ms / 1000.0);
        }
    }

    // suma el tiempo que pasó, synchronized porque tambien lo modifica sumarSegundos()
    private synchronized void avanzar(double segundos) {
        transcurrido = transcurrido + segundos;

        if (transcurrido >= LIMITE) {
            transcurrido = LIMITE;
            agotado = true;
        }
    }

    // Positivo = bonus (regresa el reloj), negativo = castigo.
    public synchronized void sumarSegundos(double segundos) {
        transcurrido = Math.max(0, Math.min(LIMITE, transcurrido - segundos));

        if (transcurrido >= LIMITE) {
            agotado = true;
        }
    }

    public synchronized double getTranscurrido() {
        return transcurrido;
    }

    public int getTranscurridoSeg() {
        return (int) getTranscurrido();
    }

    public int getRestantes() {
        return (int) Math.ceil(LIMITE - getTranscurrido());
    }

    public synchronized boolean isAgotado() {
        return agotado;
    }
}
