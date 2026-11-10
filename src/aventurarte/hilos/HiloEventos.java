package aventurarte.hilos;

import aventurarte.core.Audio;
import aventurarte.juego.Mundo;
import aventurarte.modelo.Inventario;

import java.util.Random;

/** Clase del HILO 3 - EVENTOS ALEATORIOS
   Cada 8-16 s de juego ocurre un evento:
   VIENTO (empuja), LLUVIA (lento), IMPULSO (rápido), +10 SEG o -10 SEG en el reloj
   Cada evento se anota en el Inventario, que es synchronized, para verlo despues en el historial
 */
public class HiloEventos extends Thread {

    // los eventos posibles, cada uno con el texto que sale en pantalla
    public enum Evento {
        VIENTO("EVENTO: VIENTO"),
        LLUVIA("EVENTO: LLUVIA"),
        IMPULSO("EVENTO: IMPULSO"),
        MAS_TIEMPO("EVENTO: +10 SEG"),
        MENOS_TIEMPO("EVENTO: -10 SEG");

        public final String texto;

        Evento(String texto) {
            this.texto = texto;
        }
    }

    private Mundo mundo;
    private PausaControl pausa;
    private HiloTemporizador timer;
    private Inventario inv;
    private Audio audio;
    private int nivel;
    private Random azar;

    // constructor
    public HiloEventos(Mundo mundo, PausaControl pausa, HiloTemporizador timer, Inventario inv, Audio audio, int nivel) {
        super("hilo-eventos");
        setDaemon(true);
        this.mundo = mundo;
        this.pausa = pausa;
        this.timer = timer;
        this.inv = inv;
        this.audio = audio;
        this.nivel = nivel;
        this.azar = new Random();
    }

    @Override
    public void run() {
        // el primer evento llega entre 9 y 13 segundos despues de empezar
        long espera = 9000 + azar.nextInt(4000);

        // dormir() devuelve -1 cuando el nivel termina, entonces el hilo acaba
        while (pausa.dormir(espera) >= 0) {

            // elegimos un evento al azar y lo lanzamos
            Evento[] todos = Evento.values();
            lanzar(todos[azar.nextInt(todos.length)]);

            // el siguiente llega entre 8 y 16 segundos despues
            espera = 8000 + azar.nextInt(8000);
        }
    }

    // aplica el efecto del evento, lo anota en el inventario y avisa al jugador
    private void lanzar(Evento e) {
        switch (e) {
            case VIENTO:
                int direccion = azar.nextBoolean() ? 1 : -1;
                mundo.activarEfecto(Mundo.Efecto.VIENTO, 4f, direccion);
                break;
            case LLUVIA:
                mundo.activarEfecto(Mundo.Efecto.LLUVIA, 5f, 0);
                break;
            case IMPULSO:
                mundo.activarEfecto(Mundo.Efecto.IMPULSO, 5f, 0);
                break;
            case MAS_TIEMPO:
                timer.sumarSegundos(10);
                break;
            case MENOS_TIEMPO:
                timer.sumarSegundos(-10);
                break;
        }

        inv.registrar(nivel, timer.getTranscurridoSeg(), Inventario.Tipo.EVENTO, e.texto);
        mundo.avisoEvento(e.texto);
        audio.sfx("evento");
    }
}
