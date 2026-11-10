package aventurarte.modelo;

import java.io.*;
import java.util.Properties;

/**
 * Las MEJORES MARCAS de cada jugador, guardadas en un archivo (~/.aventurarte/records.properties).
 * Por cada nombre de jugador y cada nivel guarda: mejores estrellas, mas monedas y menor tiempo.
 * Los metodos son synchronized (como en CuentaBanco) porque se leen desde el hilo del juego y se escriben al ganar.
 */
public class Records {

    /** Lo que paso al registrar un resultado: sirve para mostrar "NUEVO RECORD!". */
    public static class Resultado {
        public boolean recordEstrellas;
        public boolean recordMonedas;
        public boolean recordTiempo;
        public int mejorTiempo;            // el mejor tiempo despues de registrar (0 = ninguno)
        public boolean primeraVez;         // true si es la primera vez que completas ese nivel
        /** Hay algo para celebrar, pero la primera vez no cuenta como "record" (todo era nuevo). */
        public boolean hayRecord() { return !primeraVez && (recordEstrellas || recordMonedas || recordTiempo); }
    }

    private final File archivo;
    private final Properties datos = new Properties();

    public Records(File archivo) {
        this.archivo = archivo;
        try (InputStream in = new FileInputStream(archivo)) {
            datos.load(in);
        } catch (Exception e) {
            // primera vez o archivo danado: empezamos en blanco
        }
    }

    private static String clave(String nombre, String campo, int nivel) {
        String n = nombre.isEmpty() ? "JUGADOR" : nombre;
        return n + "." + campo + "." + nivel;
    }

    private int leer(String nombre, String campo, int nivel) {
        try {
            return Integer.parseInt(datos.getProperty(clave(nombre, campo, nivel), "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public synchronized int estrellas(String nombre, int nivel) { return leer(nombre, "estrellas", nivel); }

    public synchronized int monedas(String nombre, int nivel) { return leer(nombre, "monedas", nivel); }

    /** Mejor tiempo en segundos (0 = todavia no lo has completado). */
    public synchronized int tiempo(String nombre, int nivel) { return leer(nombre, "tiempo", nivel); }

    public synchronized int totalEstrellas(String nombre) {
        int total = 0;
        for (int i = 0; i < Inventario.NIVELES; i++) total += estrellas(nombre, i);
        return total;
    }

    /** Anota un nivel completado. Solo reemplaza lo que haya mejorado. */
    public synchronized Resultado registrar(String nombre, int nivel, int estrellas, int monedas, int seg) {
        Resultado r = new Resultado();
        int segs = Math.max(1, seg);

        if (estrellas > estrellas(nombre, nivel)) {
            datos.setProperty(clave(nombre, "estrellas", nivel), String.valueOf(estrellas));
            r.recordEstrellas = true;
        }
        if (monedas > monedas(nombre, nivel)) {
            datos.setProperty(clave(nombre, "monedas", nivel), String.valueOf(monedas));
            r.recordMonedas = true;
        }
        int anterior = tiempo(nombre, nivel);
        r.primeraVez = anterior == 0;
        if (anterior == 0 || segs < anterior) {
            datos.setProperty(clave(nombre, "tiempo", nivel), String.valueOf(segs));
            r.recordTiempo = anterior != 0;
        }
        r.mejorTiempo = tiempo(nombre, nivel);

        guardar();
        return r;
    }

    private void guardar() {
        try (OutputStream out = new FileOutputStream(archivo)) {
            datos.store(out, "AVENTURARTE - mejores marcas");
        } catch (IOException e) {
            // si no se puede guardar, el juego sigue normal
        }
    }
}
