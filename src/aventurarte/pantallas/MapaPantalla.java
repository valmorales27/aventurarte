package aventurarte.pantallas;

import aventurarte.core.*;
import aventurarte.modelo.Inventario;
import aventurarte.modelo.PerfilJugador;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.List;

/** "ELIGE TU PUEBLO": el mapa con los 5 niveles. Se desbloquea un nivel al completar el anterior. */
public class MapaPantalla extends Pantalla {
    public static final String[] NOMBRES = {"XONACATLAN", "SAN MATEO ATENCO", "GUALUPITA", "OCOYOACAC", "METEPEC"};
    /** Nombres cortos para el panel de abajo (el nombre completo ya sale en el letrero del mapa). */
    private static final String[] CORTOS = {"XONACATLAN", "SAN MATEO", "GUALUPITA", "OCOYOACAC", "METEPEC"};
    private static final int[][] NODO = {{64, 158}, {152, 112}, {240, 160}, {330, 114}, {418, 162}};
    private static final int[][] HITO = {{22, 79, 85, 65}, {112, 48, 81, 50}, {204, 90, 73, 56}, {288, 57, 84, 43}, {383, 87, 70, 61}};
    private static final int[][] INSIGNIA = {{49, 143}, {137, 97}, {225, 145}, {315, 99}, {403, 147}};
    private static final int[][] LLAVE = {{51, 190}, {139, 144}, {227, 192}, {317, 146}, {405, 194}};
    private static final int[][] FLECHA = {{57, 63}, {145, 32}, {233, 74}, {323, 41}, {411, 71}};
    private static final int[] DY = {0, 1, 2, 1};

    private final BufferedImage fondo = Assets.img("mapa/fondo_mapa_480x270.png");
    private final Boton jugar = new Boton("mapa/ui/botones/boton_jugar", 298, 228);
    private final Boton repetir = new Boton("mapa/ui/botones/boton_repetir", 297, 228);
    private final BufferedImage bloqueado = Assets.img("mapa/ui/botones/boton_bloqueado.png");
    private final Boton volver = new Boton("mapa/ui/botones/boton_volver", 24, 228, 0, 0, 0);
    private final Boton opciones = new Boton("mapa/ui/botones/boton_opciones", 424, 228, 0, 0, 0);
    private final float[][] camino;        // puntos (x,y) del camino
    private int sel;
    private float t;

    // animacion de caminar de un pueblo al siguiente
    private final int desde;               // nivel recien completado (-1 = sin animacion)
    private float pos;                     // indice (decimal) dentro de la lista de puntos
    private boolean caminando;

    /** Abre el mapa dejando seleccionado el nivel "seleccion" (por ejemplo, el que acabas de repetir). */
    public MapaPantalla(Juego j, int recienCompletado, int seleccion) {
        this(j, recienCompletado);
        if (!caminando && seleccion >= 0 && seleccion < 5) sel = seleccion;
    }

    public MapaPantalla(Juego j, int recienCompletado) {
        super(j);
        this.desde = recienCompletado;
        camino = cargarCamino();
        Inventario.Snapshot s = j.inv.snapshot();
        sel = 0;
        for (int i = 0; i < 5; i++) if (!s.completado[i]) { sel = i; break; }
        if (s.juegoCompleto()) sel = 4;
        if (desde >= 0 && desde < 4) { caminando = true; pos = 80 * desde + 80; sel = desde + 1; }
    }

    private static float[][] cargarCamino() {
        String[] lineas = Assets.texto("camino_puntos.csv").split("\\R");
        java.util.List<float[]> r = new java.util.ArrayList<>();
        for (String l : lineas) {
            if (l.isBlank() || l.startsWith("#")) continue;
            String[] p = l.split(",");
            r.add(new float[]{Float.parseFloat(p[0]), Float.parseFloat(p[1])});
        }
        return r.toArray(new float[0][]);
    }

    @Override public void alEntrar() { j.audio.musica("musica_menu"); }
    @Override public void alRecuperarFoco() { j.audio.musica("musica_menu"); }

    @Override public void actualizar(float dt) {
        t += dt;
        Inventario.Snapshot s = j.inv.snapshot();
        if (caminando) {
            pos += 55f * dt;
            if (pos >= 80 * desde + 160) { pos = 80 * desde + 160; caminando = false; j.audio.sfx("evento"); }
            j.input.limpiarEventos();
            return;
        }
        List<int[]> clics = j.input.clics();
        for (int[] c : clics) {
            for (int i = 0; i < 5; i++) {
                if (dentro(c[0], c[1], HITO[i][0], HITO[i][1], HITO[i][2], HITO[i][3])
                        || dentro(c[0], c[1], INSIGNIA[i][0], INSIGNIA[i][1], 31, 31)) seleccionar(i, s);
            }
            boolean repetirClic = s.estado(sel).equals("completado") && repetir.contiene(c[0], c[1]);
            if (jugable(s) && (repetirClic || (!s.estado(sel).equals("completado") && jugar.contiene(c[0], c[1])))) { entrar(); return; }
            if (volver.contiene(c[0], c[1])) { j.audio.sfx("clic"); j.ir(new MenuPantalla(j)); return; }
            if (opciones.contiene(c[0], c[1])) { j.audio.sfx("clic"); j.apilar(new OpcionesPantalla(j)); return; }
        }
        int k;
        while ((k = j.input.sigPulsada()) != -1) {
            if (k == KeyEvent.VK_LEFT) seleccionar(sel - 1, s);
            else if (k == KeyEvent.VK_RIGHT) seleccionar(sel + 1, s);
            else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) { if (jugable(s)) { entrar(); return; } else j.audio.sfx("golpe"); }
            else if (k == KeyEvent.VK_ESCAPE) { j.ir(new MenuPantalla(j)); return; }
            else if (k == KeyEvent.VK_I) { j.apilar(new HistorialPantalla(j, null)); return; }
        }
    }

    /** Se puede entrar al nivel "en curso" (el siguiente por jugar) y tambien REPETIR los ya completados para ganar mas estrellas. */
    private boolean jugable(Inventario.Snapshot s) { return !caminando && !s.estado(sel).equals("bloqueado"); }

    private void seleccionar(int i, Inventario.Snapshot s) {
        if (i < 0 || i > 4 || i == sel) return;
        if (s.estado(i).equals("bloqueado")) { j.audio.sfx("golpe"); return; }
        sel = i;
        j.audio.sfx("clic");
    }

    private void entrar() {
        j.audio.sfx("puerta");
        j.nivelActual = sel;
        j.ir(new NivelPantalla(j, sel));
    }

    @Override public void dibujar(Graphics2D g) {
        Inventario.Snapshot s = j.inv.snapshot();
        imagen(g, fondo, 0, 0);
        // caminos ya recorridos
        for (int i = 0; i < 4; i++)
            if (s.completado[i] && !(caminando && i == desde))
                imagen(g, Assets.img("mapa/caminos/camino_hecho_" + (i + 1) + "_a_" + (i + 2) + ".png"), 0, 0);
        // hitos, llaves
        for (int i = 0; i < 5; i++) {
            boolean bloq = s.estado(i).equals("bloqueado") || (caminando && i == desde + 1);
            imagen(g, Assets.img("mapa/hitos/hito_" + (i + 1) + (bloq ? "_bloqueado" : "_color") + ".png"), HITO[i][0], HITO[i][1]);
            imagen(g, Assets.img("mapa/llaves/llave_mini_" + (i + 1) + (s.llave[i] ? "_llena" : "_vacia") + ".png"), LLAVE[i][0], LLAVE[i][1]);
        }
        // pulso + insignias
        for (int i = 0; i < 5; i++) {
            String e = s.estado(i);
            if (caminando && i == desde + 1) e = "bloqueado";
            String nom = e.equals("completado") ? "completado" : e.equals("en_curso") ? "disponible" : "bloqueado";
            if (nom.equals("disponible"))
                imagen(g, Assets.img("mapa/insignias/pulso_" + ((int) (t / 0.15f) % 4) + ".png"), NODO[i][0] - 20, NODO[i][1] - 20);
            imagen(g, Assets.img("mapa/insignias/insignia_" + (i + 1) + "_" + nom + ".png"), INSIGNIA[i][0], INSIGNIA[i][1]);
        }
        // flecha de seleccion
        if (!caminando) {
            int dy = DY[(int) (t / 0.15f) % 4];
            imagen(g, Assets.img("mapa/ui/flecha_seleccion.png"), FLECHA[sel][0], FLECHA[sel][1] + dy);
        }
        dibujarAvatar(g, s);
        hud(g, s);
        panelInfo(g, s);
        // botones
        int mx = j.input.mx, my = j.input.my;
        boolean press = j.input.mouseAbajo;
        if (s.estado(sel).equals("completado") && !caminando) {
            // nivel ya completado: se puede REPETIR para ganar mas estrellas
            repetir.dibujar(g, repetir.contiene(mx, my), press && repetir.contiene(mx, my));
        } else if (!jugable(s)) {
            // la placa original traia el texto cortado: limpiamos el centro y escribimos BLOQUEADO completo
            imagen(g, bloqueado, 298, 228);
            g.setColor(new Color(0xA8A4B0));
            g.fillRect(298 + 5, 228 + 8, 96, 17);
            int cx = 298 + 53;
            int tx = cx - PixelFont.width("BLOQUEADO", 1) / 2 + 5;
            g.setColor(new Color(0x635472));
            g.fillRect(tx - 14, 228 + 15, 8, 6);            // cuerpo del candado
            g.drawRect(tx - 13, 228 + 11, 5, 4);            // arco del candado
            j.fuente.draw(g, "BLOQUEADO", tx, 244, 1, new Color(0x635472));
        }
        else jugar.dibujar(g, jugar.contiene(mx, my), press && jugar.contiene(mx, my));
        volver.dibujar(g, volver.contiene(mx, my), press && volver.contiene(mx, my));
        opciones.dibujar(g, opciones.contiene(mx, my), press && opciones.contiene(mx, my));
    }

    private void dibujarAvatar(Graphics2D g, Inventario.Snapshot s) {
        BufferedImage av = Assets.img("mapa/avatares/avatar_" + j.perfil.id() + ".png");
        int fx, fy;
        if (caminando) {
            int i = Math.min((int) pos, camino.length - 1);
            fx = Math.round(camino[i][0]); fy = Math.round(camino[i][1]) + 4;
            fy -= (int) Math.abs(Math.sin(t * 14) * 2);            // saltito al caminar
        } else {
            fx = NODO[sel][0] + 22; fy = NODO[sel][1] + 10;
            fy -= (int) Math.abs(Math.sin(t * 3) * 1.5);
        }
        imagen(g, av, fx - av.getWidth() / 2, fy - av.getHeight());
    }

    private void hud(Graphics2D g, Inventario.Snapshot s) {
        imagen(g, Assets.img("mapa/ui/panel_vidas.png"), 6, 5);
        int quedan = Math.max(0, 3 - s.intentosUsados[sel]);
        for (int i = 0; i < 3; i++)
            imagen(g, Assets.img(i < quedan ? "hud/iconos/corazon_lleno.png" : "hud/iconos/corazon_vacio.png"), 6 + 8 + i * 17, 5 + 8);
        imagen(g, Assets.img("mapa/ui/panel_llaves.png"), 382, 5);
        j.fuente.draw(g, s.totalLlaves() + "/5", 382 + 29, 5 + 6, 2, new Color(0xFFE680));
        // total de mejores estrellas del jugador
        String est = j.records.totalEstrellas(j.nombreJugador()) + "/15";
        int ew = 22 + PixelFont.width(est, 1) + 8;
        g.setColor(new Color(0x2A1240)); g.fillRoundRect(382, 35, ew, 15, 7, 7);
        g.setColor(new Color(0xFFD23F)); g.drawRoundRect(382, 35, ew, 15, 7, 7);
        g.drawImage(Assets.img("inventario/iconos/estrella_on.png"), 386, 37, 11, 11, null);
        j.fuente.draw(g, est, 402, 39, 1, new Color(0xFFE680));
        imagen(g, Assets.img("mapa/ui/titulo_elige_tu_pueblo.png"), 125, 3);
    }

    private void panelInfo(Graphics2D g, Inventario.Snapshot s) {
        imagen(g, Assets.img("mapa/ui/panel_info.png"), 72, 222);
        String e = s.estado(sel);
        String nom = e.equals("completado") ? "completado" : e.equals("en_curso") ? "disponible" : "bloqueado";
        imagen(g, Assets.img("mapa/insignias/insignia_" + (sel + 1) + "_" + nom + ".png"), 79, 229);
        j.fuente.drawOutlined(g, CORTOS[sel], 122, 228, 2, new Color(0xFFF1D6), new Color(0x2A1240));
        if (!e.equals("bloqueado")) {
            // mejores estrellas de este pueblo (se guardan por jugador, aunque cierres el juego)
            int est = j.records.estrellas(j.nombreJugador(), sel);
            int ex = 122 + PixelFont.width(CORTOS[sel], 2) + 10;
            for (int i = 0; i < 3; i++)
                g.drawImage(Assets.img("inventario/iconos/estrella_" + (i < est ? "on" : "off") + ".png"), ex + i * 15, 227, 14, 14, null);
        }
        String mon = String.format("%02d/%02d", s.monedas[sel], Inventario.MONEDAS_TOTAL[sel]);
        j.fuente.draw(g, mon, 72 + 68, 222 + 28, 1, new Color(0xFFE680));
        imagen(g, Assets.img("mapa/llaves/llave_mini_" + (sel + 1) + (s.llave[sel] ? "_llena" : "_vacia") + ".png"), 72 + 108, 222 + 25);
        j.fuente.draw(g, "X" + Inventario.ENEMIGOS[sel], 72 + 160, 222 + 28, 1, new Color(0xFFB0A0));
    }
}
