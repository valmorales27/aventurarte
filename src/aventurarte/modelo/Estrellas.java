package aventurarte.modelo;

/**
 * Las 3 estrellas de cada nivel. Se ganan al COMPLETAR el nivel:
 *   estrella 1: completar el nivel (siempre)
 *   estrella 2: juntar TODAS las monedas del nivel
 *   estrella 3: terminar sin perder ningun corazon en ese nivel
 * El tiempo no da estrella: se guarda como "mejor tiempo" en Records.
 */
public class Estrellas {

    public static final String[] TEXTOS = {"NIVEL COMPLETADO", "TODAS LAS MONEDAS", "SIN PERDER CORAZONES"};

    /** Devuelve un arreglo de 3 booleanos: cual estrella se gano. */
    public static boolean[] calcular(int nivel, int monedas, int corazonesPerdidos) {
        boolean[] ganadas = new boolean[3];
        ganadas[0] = true;
        ganadas[1] = monedas >= Inventario.MONEDAS_TOTAL[nivel];
        ganadas[2] = corazonesPerdidos == 0;
        return ganadas;
    }

    public static int contar(boolean[] ganadas) {
        int total = 0;
        for (boolean g : ganadas) {
            if (g) total++;
        }
        return total;
    }

    /**
     * true si el resultado "nuevo" es mejor que el "viejo": primero mas estrellas,
     * si empatan mas monedas, y si vuelven a empatar menos tiempo.
     */
    public static boolean esMejor(int estNuevo, int monNuevo, int tNuevo, int estViejo, int monViejo, int tViejo) {
        if (estNuevo != estViejo) return estNuevo > estViejo;
        if (monNuevo != monViejo) return monNuevo > monViejo;
        return tNuevo < tViejo;
    }
}
