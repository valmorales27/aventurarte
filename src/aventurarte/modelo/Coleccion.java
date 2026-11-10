package aventurarte.modelo;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * COLECCION y LOGROS de cada jugador (se guardan en ~/.aventurarte/coleccion.properties, igual que las marcas).
 *
 *  - TARJETAS: una por cada lugar, llave y enemigo que descubres jugando (16 en total).
 *  - LOGROS:   insignias por hazanas ("pasar un nivel sin golpes", "vencer 10 enemigos"...).
 *
 * Todo es synchronized porque lo escribe el hilo del juego y lo lee la pantalla del historial.
 * Los metodos que desbloquean algo devuelven los NOMBRES nuevos para mostrar el aviso "LOGRO DESBLOQUEADO".
 */
public class Coleccion {

    // ------------------------------------------------------------------ tarjetas
    public enum Tipo { LUGAR, LLAVE, ENEMIGO }

    /** Una tarjeta de la coleccion. */
    public static final class Tarjeta {
        public final int id;
        public final Tipo tipo;
        public final int nivel;              // para LUGAR y LLAVE: 0..4;  para ENEMIGO: el nivel donde aparece
        public final String nombre, linea1, linea2;
        public final TipoEnemigo enemigo;    // solo ENEMIGO
        Tarjeta(int id, Tipo tipo, int nivel, String nombre, String linea1, String linea2, TipoEnemigo enemigo) {
            this.id = id; this.tipo = tipo; this.nivel = nivel; this.nombre = nombre;
            this.linea1 = linea1; this.linea2 = linea2; this.enemigo = enemigo;
        }
    }

    public static final List<Tarjeta> TARJETAS = new ArrayList<>();
    static {
        String[] pueblos = {"XONACATL\u00C1N", "SAN MATEO ATENCO", "GUALUPITA", "OCOYOACAC", "METEPEC"};
        String[][] frase = {
                {"CUNA DE XONITA,", "LA MASCOTA DEL PUEBLO"},
                {"LA TIERRA DEL CALZADO", "Y DE LAS DOS PLAZAS"},
                {"TIERRA DEL SARAPE", "Y DE SU CINE"},
                {"EN LA NARIZ DE LOS", "OCOTES Y DEL TAMAL"},
                {"PUEBLO M\u00C1GICO Y", "\u00C1RBOL DE LA VIDA"}};
        for (int i = 0; i < 5; i++) TARJETAS.add(new Tarjeta(i, Tipo.LUGAR, i, pueblos[i], frase[i][0], frase[i][1], null));
        for (int i = 0; i < 5; i++)
            TARJETAS.add(new Tarjeta(5 + i, Tipo.LLAVE, i, Inventario.LLAVE[i], "LLAVE DE " + pueblos[i], "ABRE LA PUERTA", null));
        Object[][] en = {
                {TipoEnemigo.OSITO, 0, "OSITO", "LANZA BLOQUES", "DE JUGUETE"},
                {TipoEnemigo.TENIS, 1, "TENIS", "UN CORREDOR VELOZ,", "\u00A1P\u00CDSALO!"},
                {TipoEnemigo.BORREGO, 2, "BORREGO", "LANZA OVILLOS", "DE LANA"},
                {TipoEnemigo.TAMAL, 3, "TAMAL", "LANZA CHILES", "PICOSITOS"},
                {TipoEnemigo.ALEBRIJE, 4, "ALEBRIJE", "UN CORREDOR", "MUY COLORIDO"},
                {TipoEnemigo.CALACA, 4, "CALACA", "LANZA HUESOS", "DESDE LEJOS"}};
        for (int i = 0; i < en.length; i++)
            TARJETAS.add(new Tarjeta(10 + i, Tipo.ENEMIGO, (Integer) en[i][1], (String) en[i][2], (String) en[i][3], (String) en[i][4], (TipoEnemigo) en[i][0]));
    }

    public static int idLugar(int nivel) { return nivel; }
    public static int idLlave(int nivel) { return 5 + nivel; }
    public static int idEnemigo(TipoEnemigo t) { return 10 + t.ordinal(); }

    // ------------------------------------------------------------------ logros
    public static final class Logro {
        public final int id;
        public final String nombre, descripcion;
        Logro(int id, String nombre, String descripcion) { this.id = id; this.nombre = nombre; this.descripcion = descripcion; }
    }

    public static final int PRIMER_PASO = 0, SIN_GOLPES = 1, MONEDERO = 2, TRES_ESTRELLAS = 3, VELOCISTA = 4,
            CAZADOR = 5, LLAVERO = 6, EXPLORADOR = 7, RECORDISTA = 8, RUTA_COMPLETA = 9;

    public static final int CAZADOR_META = 10;          // enemigos para el logro CAZADOR
    public static final int VELOCISTA_SEG = 60;         // segundos para el logro VELOCISTA

    public static final Logro[] LOGROS = {
            new Logro(PRIMER_PASO, "PRIMER PASO", "PASA TU PRIMER NIVEL"),
            new Logro(SIN_GOLPES, "SIN UN RASGU\u00D1O", "NIVEL SIN NING\u00DAN GOLPE"),
            new Logro(MONEDERO, "MONEDERO LLENO", "JUNTA TODAS LAS MONEDAS"),
            new Logro(TRES_ESTRELLAS, "ESTRELLA TRIPLE", "GANA 3 ESTRELLAS"),
            new Logro(VELOCISTA, "VELOCISTA", "PASA UN NIVEL EN 1:00"),
            new Logro(CAZADOR, "CAZADOR", "VENCE A 10 ENEMIGOS"),
            new Logro(LLAVERO, "LLAVERO", "CONSIGUE LAS 5 LLAVES"),
            new Logro(EXPLORADOR, "EXPLORADOR", "DESCUBRE 16 TARJETAS"),
            new Logro(RECORDISTA, "RECORDISTA", "ROMPE UN RECORD"),
            new Logro(RUTA_COMPLETA, "RUTA COMPLETA", "PASA LOS 5 PUEBLOS")};

    // ------------------------------------------------------------------ guardado
    private final File archivo;
    private final Properties datos = new Properties();

    public Coleccion(File archivo) {
        this.archivo = archivo;
        try (InputStream in = new FileInputStream(archivo)) {
            datos.load(in);
        } catch (Exception e) {
            // primera vez o archivo danado: empezamos en blanco
        }
    }

    private static String nom(String nombre) { return nombre.isEmpty() ? "JUGADOR" : nombre; }

    private boolean tiene(String nombre, String campo, int id) {
        return "1".equals(datos.getProperty(nom(nombre) + "." + campo + "." + id));
    }

    private void poner(String nombre, String campo, int id) { datos.setProperty(nom(nombre) + "." + campo + "." + id, "1"); }

    public synchronized boolean tieneTarjeta(String nombre, int id) { return tiene(nombre, "tarjeta", id); }

    public synchronized boolean tieneLogro(String nombre, int id) { return tiene(nombre, "logro", id); }

    public synchronized int totalTarjetas(String nombre) {
        int c = 0;
        for (Tarjeta t : TARJETAS) if (tiene(nombre, "tarjeta", t.id)) c++;
        return c;
    }

    public synchronized int totalLogros(String nombre) {
        int c = 0;
        for (Logro l : LOGROS) if (tiene(nombre, "logro", l.id)) c++;
        return c;
    }

    public synchronized int enemigosVencidos(String nombre) {
        try { return Integer.parseInt(datos.getProperty(nom(nombre) + ".vencidos", "0")); }
        catch (NumberFormatException e) { return 0; }
    }

    // ------------------------------------------------------------------ desbloqueos
    /**
     * Desbloquea una tarjeta. Devuelve los avisos nuevos: el nombre de la tarjeta ("TARJETA: OSITO") y,
     * si con ella se logra EXPLORADOR o LLAVERO, tambien ese logro.
     */
    public synchronized List<String> descubrir(String nombre, int id) {
        List<String> nuevos = new ArrayList<>();
        if (id < 0 || id >= TARJETAS.size() || tiene(nombre, "tarjeta", id)) return nuevos;
        poner(nombre, "tarjeta", id);
        nuevos.add("TARJETA: " + TARJETAS.get(id).nombre);
        boolean llaves = true;
        for (int i = 0; i < 5; i++) llaves &= tiene(nombre, "tarjeta", idLlave(i));
        if (llaves) ganar(nombre, LLAVERO, nuevos);
        if (totalTarjetas(nombre) == TARJETAS.size()) ganar(nombre, EXPLORADOR, nuevos);
        guardar();
        return nuevos;
    }

    /** Suma un enemigo vencido. A los 10 gana CAZADOR. */
    public synchronized List<String> enemigoVencido(String nombre) {
        List<String> nuevos = new ArrayList<>();
        datos.setProperty(nom(nombre) + ".vencidos", String.valueOf(enemigosVencidos(nombre) + 1));
        if (enemigosVencidos(nombre) >= CAZADOR_META) ganar(nombre, CAZADOR, nuevos);
        guardar();
        return nuevos;
    }

    /** Se llama al completar un nivel: revisa los logros de ese resultado. */
    public synchronized List<String> nivelCompletado(String nombre, int golpes, boolean todasMonedas, int estrellas,
                                                     int seg, boolean record, boolean juegoCompleto) {
        List<String> nuevos = new ArrayList<>();
        ganar(nombre, PRIMER_PASO, nuevos);
        if (golpes == 0) ganar(nombre, SIN_GOLPES, nuevos);
        if (todasMonedas) ganar(nombre, MONEDERO, nuevos);
        if (estrellas >= 3) ganar(nombre, TRES_ESTRELLAS, nuevos);
        if (seg <= VELOCISTA_SEG) ganar(nombre, VELOCISTA, nuevos);
        if (record) ganar(nombre, RECORDISTA, nuevos);
        if (juegoCompleto) ganar(nombre, RUTA_COMPLETA, nuevos);
        guardar();
        return nuevos;
    }

    private void ganar(String nombre, int id, List<String> nuevos) {
        if (tiene(nombre, "logro", id)) return;
        poner(nombre, "logro", id);
        nuevos.add("LOGRO: " + LOGROS[id].nombre);
    }

    private void guardar() {
        try (OutputStream out = new FileOutputStream(archivo)) {
            datos.store(out, "AVENTURARTE - coleccion y logros");
        } catch (IOException e) {
            // si no se puede guardar, el juego sigue normal
        }
    }
}
