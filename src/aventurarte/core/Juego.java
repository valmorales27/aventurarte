package aventurarte.core;

import aventurarte.modelo.Coleccion;
import aventurarte.modelo.Inventario;
import aventurarte.modelo.PerfilJugador;
import aventurarte.modelo.Records;
import aventurarte.pantallas.SplashPantalla;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Clase para los elementos generales del juego: elementos que van en todas las pantallas (entrada, audio, fuente, perfil, inventario) */

public final class Juego {
    public final Input input = new Input();
    public final Audio audio = new Audio();
    public final PixelFont fuente;
    public final PerfilJugador perfil;
    /** Mejores marcas (estrellas, monedas, tiempo) de cada jugador; se guardan en un archivo */
    public final Records records;
    /** Tarjetas descubiertas y logros de cada jugador (igual se guardan en un archivo) */
    public final Coleccion coleccion;
    private final AvisoLogro aviso = new AvisoLogro();
    /** Inventario de la partida actual. Se reemplaza por uno nuevo al pulsar "otra vez" */
    public volatile Inventario inv = new Inventario();
    /** El nivel (0..4) que se esta jugando o se acaba de jugar */
    public volatile int nivelActual = 0;

    public volatile boolean salir;

    private final List<Pantalla> pila = new ArrayList<>();

    public Juego() {
        fuente = new PixelFont(Assets.img("hud/fuente/fuente_atlas_blanca.png"));
        perfil = PerfilJugador.cargar(archivo("perfil.properties"));
        audio.cargar(archivo("audio.properties"));
        records = new Records(archivo("records.properties"));
        coleccion = new Coleccion(archivo("coleccion.properties"));
    }

    /** Nombre con el que se guardan las marcas (si aún no hay nombre, "JUGADOR"). */
    public String nombreJugador() {
        String n = perfil.getNombre();
        return n.isEmpty() ? "JUGADOR" : n;
    }

    /** Carpeta donde se guardan perfil y ajustes ~/.aventurarte */
    public static File dirDatos() {
        File d = new File(System.getProperty("user.home"), ".aventurarte");
        d.mkdirs();
        return d;
    }

    public static File archivo(String nombre) { return new File(dirDatos(), nombre); }

    public void guardarTodo() {
        try { perfil.guardar(archivo("perfil.properties")); } catch (Exception ignorar) { }
        audio.guardar(archivo("audio.properties"));
    }

    /** true si la partida actual ya terminó (se han gandado los 5 niveles o se acabaron los intentos de un nivel) */
    public boolean partidaTerminada() {
        Inventario.Snapshot s = inv.snapshot();
        if (s.juegoCompleto()) return true;
        for (int u : s.intentosUsados) if (u >= 3) return true;
        return false;
    }

    /** Si la partida anterior ya terminó, empieza una nueva con inventario vacío */
    public void nuevaPartidaSiHaceFalta() { if (partidaTerminada()) inv = new Inventario(); }

    public static Image icono() {
        try { return ImageIO.read(Juego.class.getResourceAsStream("/img/menu/logo/icono_ventana_64.png")); }
        catch (Exception e) { return null; }
    }

    // ------------------------------------------------------------------ PILA DE PANTALLAS
    public void iniciar() { ir(new SplashPantalla(this)); }

    /** Reemplaza toda la pila por esta pantalla. */
    public void ir(Pantalla p) {
        List<Pantalla> viejas = new ArrayList<>(pila);
        pila.clear();
        for (int i = viejas.size() - 1; i >= 0; i--) viejas.get(i).alSalir();
        pila.add(p);
        input.limpiarEventos();
        p.alEntrar();
    }

    /** Pone una pantalla encima de la actual (pausa, opciones, historial...). La de abajo se sigue "dibujando/creando" */
    public void apilar(Pantalla p) {
        pila.add(p);
        input.limpiarEventos();
        p.alEntrar();
    }

    /** Quita la pantalla de arriba y se muestra de nuevo la de abajo */
    public void cerrar() {
        if (pila.size() <= 1) return;
        Pantalla top = pila.remove(pila.size() - 1);
        top.alSalir();
        input.limpiarEventos();
        pila.get(pila.size() - 1).alRecuperarFoco();
    }

    public Pantalla actual() { return pila.isEmpty() ? null : pila.get(pila.size() - 1); }

    public int profundidad() { return pila.size(); }

    // ------------------------------------------------------------------ TARJETAS
    /** Muestra los avisos "TARJETA: ..." / "LOGRO: ..." que salen en una esquina */
    public void avisarLogros(List<String> textos) {
        for (String t : textos) aviso.encolar(t);
        if (!textos.isEmpty()) audio.sfx("evento");
    }

    public void actualizar(float dt) {
        aviso.actualizar(dt);
        Pantalla p = actual();
        if (p != null) p.actualizar(dt);
    }

    public void dibujar(Graphics2D g) {
        List<Pantalla> copia = new ArrayList<>(pila);
        for (Pantalla p : copia) p.dibujar(g);
        aviso.dibujar(g, fuente);
    }
}
