package aventurarte.modelo;

import java.io.*;
import java.text.Normalizer;
import java.util.Properties;

/** Datos del jugador: nombre + personaje + color + accesorio. Compartido entre pantallas (campos volatile). */
public class PerfilJugador {
    public static final String[] PERSONAJES = {"alfarera", "tamalero", "zapatero", "tejedora"};
    public static final String[][] VARIANTES = {
            {"terracota", "verde", "anil"}, {"maiz", "verde", "rojo"},
            {"azul", "salvia", "terracota"}, {"coral", "turquesa", "mostaza"}};
    public static final String[] DECO = {"sombrero", "gorro", "gorra", "gorro"};
    public static final int MAX_NOMBRE = 12;

    private volatile String nombre = "";
    private volatile int personaje = 0;      // 0..3
    private volatile int color = 0;          // 0..2  (orden de VARIANTES)
    private volatile boolean conAccesorio = true;

    /** La fuente del juego solo tiene A-Z y 0-9: quita acentos, la enie pasa a N, pone MAYUSCULAS y recorta a 12. */
    public static String limpiarNombre(String s) {
        String t = Normalizer.normalize(s.toUpperCase(), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        StringBuilder sb = new StringBuilder();
        for (char c : t.toCharArray())
            if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) sb.append(c);
        return sb.length() > MAX_NOMBRE ? sb.substring(0, MAX_NOMBRE) : sb.toString();
    }

    /** true si es valido para continuar (al menos 1 caracter). */
    public boolean nombreValido() { return !nombre.isEmpty(); }

    public String variante() { return VARIANTES[personaje][color]; }

    /** Nombre del personaje elegido: "alfarera", "tamalero", "zapatero" o "tejedora". */
    public String id() { return PERSONAJES[personaje]; }

    /** "con_sombrero", "sin_gorro", etc. */
    public String decoSufijo() { return (conAccesorio ? "con_" : "sin_") + DECO[personaje]; }

    /** Ruta (dentro de res/img) de la hoja de PERFIL 6x7 (celdas 40x73) que se usa DENTRO del juego. */
    public String rutaHojaPerfil() { return "personajes/" + id() + "/perfil_" + variante() + "_" + decoSufijo() + ".png"; }

    /** Ruta de la hoja de FRENTE 3x3 (fila 0 reposo, fila 1 victoria, fila 2 derrota) para los menus. */
    public String rutaHojaFrente() { return "personajes/" + id() + "/frente_" + variante() + "_" + decoSufijo() + ".png"; }

    public String rutaRetrato() { return "personajes/" + id() + "/retrato_" + variante() + "_" + decoSufijo() + ".png"; }

    public void guardar(File f) throws IOException {
        Properties p = new Properties();
        p.setProperty("nombre", nombre);
        p.setProperty("personaje", String.valueOf(personaje));
        p.setProperty("color", String.valueOf(color));
        p.setProperty("accesorio", String.valueOf(conAccesorio));
        try (OutputStream o = new FileOutputStream(f)) { p.store(o, "AVENTURARTE"); }
    }

    public static PerfilJugador cargar(File f) {
        PerfilJugador r = new PerfilJugador();
        try (InputStream in = new FileInputStream(f)) {
            Properties p = new Properties(); p.load(in);
            r.nombre = limpiarNombre(p.getProperty("nombre", ""));
            r.personaje = Math.floorMod(Integer.parseInt(p.getProperty("personaje", "0")), 4);
            r.color = Math.floorMod(Integer.parseInt(p.getProperty("color", "0")), 3);
            r.conAccesorio = Boolean.parseBoolean(p.getProperty("accesorio", "true"));
        } catch (Exception e) { /* primera vez o archivo danado: valores por defecto */ }
        return r;
    }

    public String getNombre() { return nombre; }
    public void setNombre(String n) { nombre = limpiarNombre(n); }
    public int getPersonaje() { return personaje; }
    public void setPersonaje(int p) { personaje = Math.floorMod(p, 4); color = 0; }   // al cambiar de personaje vuelve al color 1
    public int getColor() { return color; }
    public void setColor(int c) { color = Math.floorMod(c, 3); }
    public boolean isConAccesorio() { return conAccesorio; }
    public void setConAccesorio(boolean b) { conAccesorio = b; }
}
