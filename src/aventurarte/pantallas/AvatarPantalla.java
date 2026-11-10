package aventurarte.pantallas;

import aventurarte.core.*;
import aventurarte.modelo.PerfilJugador;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;

/** Paso 2 del inicio: el libro "ELIGE TU ARTESANO" (personaje, color y accesorio). */
public class AvatarPantalla extends Pantalla {
    private static final String[] TITULO = {"ALFARERA", "TAMALERO", "ZAPATERO", "TEJEDORA"};
    private static final String[] MUNICIPIO = {"METEPEC", "OCOYOACAC", "SAN MATEO", "GUALUPITA"};
    private static final int[] NIVEL = {5, 4, 2, 3};
    private static final String[] ACCESORIO = {"SOMBRERO", "GORRO", "GORRA", "GORRO"};
    private static final String[] PRENDA = {"MANDIL", "COLOR", "CAMISA", "SUETER"};
    private static final String[] PROYECTIL = {"BARRO", "TAMAL", "SUELA", "GANCHO"};
    private static final int[] SECUENCIA = {0, 1, 0, 2};
    private static final float[] DURACION = {0.6f, 0.6f, 0.6f, 0.15f};

    private final BufferedImage fondo = Assets.img("usuario/libro/fondo_libro_480x270.png");
    private final BufferedImage corazon = Assets.img("usuario/libro/corazon_retrato_9x8.png");
    private final Boton.Lista botones = new Boton.Lista(
            new Boton("usuario/libro/botones/boton_volver", 60, 244, 1, 0, 0),
            new Boton("usuario/libro/botones/boton_listo", 320, 244, 1, 0, 0));
    private float t, anim, aviso;
    private int seq;

    public AvatarPantalla(Juego j) { super(j); botones.vertical = false; botones.sel = 1; }

    @Override public void alEntrar() { j.audio.musica("musica_menu"); }

    @Override public void actualizar(float dt) {
        t += dt; anim += dt; if (aviso > 0) aviso -= dt;
        if (anim >= DURACION[seq]) { anim -= DURACION[seq]; seq = (seq + 1) % SECUENCIA.length; }
        PerfilJugador p = j.perfil;
        java.util.List<int[]> clics = j.input.clics();
        int accion = botones.mouse(j.input, clics);
        for (int[] c : clics) clicEnElementos(c[0], c[1]);     // tabs, flechas, toggles y colores

        int k;
        while ((k = j.input.sigPulsada()) != -1) {
            if (k == KeyEvent.VK_LEFT) cambiarPersonaje(p.getPersonaje() - 1);
            else if (k == KeyEvent.VK_RIGHT) cambiarPersonaje(p.getPersonaje() + 1);
            else if (k == KeyEvent.VK_UP) { p.setColor(p.getColor() - 1); j.audio.sfx("clic"); }
            else if (k == KeyEvent.VK_DOWN) { p.setColor(p.getColor() + 1); j.audio.sfx("clic"); }
            else if (k == KeyEvent.VK_SPACE) { p.setConAccesorio(!p.isConAccesorio()); j.audio.sfx("clic"); }
            else if (k == KeyEvent.VK_ENTER) accion = 1;
            else if (k == KeyEvent.VK_ESCAPE) accion = 0;
        }
        if (accion == 0) { j.audio.sfx("clic"); j.ir(new NombrePantalla(j)); }
        else if (accion == 1) {
            if (!p.nombreValido()) { aviso = 2f; j.audio.sfx("golpe"); }
            else { j.audio.sfx("clic"); j.guardarTodo(); j.ir(new MapaPantalla(j, -1)); }
        }
    }

    private void cambiarPersonaje(int n) {
        PerfilJugador p = j.perfil;
        p.setPersonaje(n);            // al cambiar de personaje el color vuelve a 0 y se mantiene "con/sin accesorio"
        j.audio.sfx("clic");
    }

    private void clicEnElementos(int mx, int my) {
        PerfilJugador p = j.perfil;
        for (int i = 0; i < 4; i++)
            if (dentro(mx, my, 432, 34 + 30 * i, 22, 26)) { cambiarPersonaje(i); return; }
        if (dentro(mx, my, 58, 108, 14, 22)) { cambiarPersonaje(p.getPersonaje() - 1); return; }
        if (dentro(mx, my, 214, 108, 14, 22)) { cambiarPersonaje(p.getPersonaje() + 1); return; }
        if (dentro(mx, my, 250, 106, 44, 14)) { p.setConAccesorio(true); j.audio.sfx("clic"); return; }
        if (dentro(mx, my, 302, 106, 44, 14)) { p.setConAccesorio(false); j.audio.sfx("clic"); return; }
        for (int k = 0; k < 3; k++)
            if (dentro(mx, my, 247 + 32 * k, 137, 30, 30)) { p.setColor(k); j.audio.sfx("clic"); return; }
    }

    @Override public void dibujar(Graphics2D g) {
        PerfilJugador p = j.perfil;
        int pi = p.getPersonaje();
        String id = p.id();
        imagen(g, fondo, 0, 0);
        // a) cabecera
        BufferedImage deco = Assets.img("usuario/libro/iconos_deco/deco_" + id + ".png");
        imagen(g, deco, 58, 45 - deco.getHeight() / 2);
        // b) personaje de frente x2 (reposo)
        BufferedImage hoja = Assets.img(p.rutaHojaFrente());
        BufferedImage f = hoja.getSubimage(SECUENCIA[seq] * 40, 0, 40, 73);
        g.drawImage(f, 103, 58, 80, 146, null);
        // c) flechas
        flecha(g, "izq", 58);
        flecha(g, "der", 214);
        // d) pestañas de personaje
        for (int i = 0; i < 4; i++) {
            String n = PerfilJugador.PERSONAJES[i];
            imagen(g, Assets.img("usuario/libro/tabs/tab_" + n + (i == pi ? "_activa" : "_inactiva") + ".png"), 432, 34 + 30 * i);
        }
        // e) retrato
        imagen(g, Assets.img(p.rutaRetrato()), 257, 41);
        imagen(g, corazon, 255, 39);
        // f) textos
        Color marron = new Color(0x5A3A28), terracota = new Color(0xB8683F), azul = new Color(0x4A6E96);
        j.fuente.draw(g, TITULO[pi], 308, 40, 2, marron);
        j.fuente.draw(g, MUNICIPIO[pi], 308, 60, 1, terracota);
        j.fuente.draw(g, "NIVEL " + NIVEL[pi], 308, 72, 1, azul);
        j.fuente.draw(g, ACCESORIO[pi], 250, 96, 1, terracota);
        j.fuente.draw(g, PRENDA[pi], 250, 128, 1, terracota);
        // g) toggles
        boolean con = p.isConAccesorio();
        imagen(g, Assets.img("usuario/libro/toggles/toggle_con_" + (con ? "activo" : "inactivo") + ".png"), 250, 106);
        imagen(g, Assets.img("usuario/libro/toggles/toggle_sin_" + (con ? "inactivo" : "activo") + ".png"), 302, 106);
        // h) colores
        for (int k = 0; k < 3; k++) {
            String v = PerfilJugador.VARIANTES[pi][k];
            imagen(g, Assets.img("usuario/libro/colores/color_" + id + "_" + k + "_" + v + (k == p.getColor() ? "_sel" : "") + ".png"), 247 + 32 * k, 137);
        }
        // i) habilidad
        imagen(g, Assets.img("personajes/" + id + "/proyectil/vuelo_" + ((int) (t * 12.5f) % 4) + ".png"), 254, 190);
        j.fuente.draw(g, "LANZA", 280, 192, 1, marron);
        j.fuente.draw(g, PROYECTIL[pi], 280, 202, 1, terracota);
        // j) EN JUEGO (perfil, reposo)
        BufferedImage hp = Assets.img(p.rutaHojaPerfil());
        imagen(g, hp.getSubimage(SECUENCIA[seq] * 40, 0, 40, 73), 371, 112);
        // k) nombre
        String n = p.getNombre();
        if (n.isEmpty()) j.fuente.draw(g, "TU NOMBRE", 108, 214, 1, new Color(0xB9A48C));
        else j.fuente.draw(g, n, 108, 214, 1, marron);
        // l) botones
        botones.dibujar(g);
        if (aviso > 0) {
            conAlpha(g, Math.min(1f, aviso * 2), () -> {
                g.setColor(new Color(0x2A1240));
                g.fillRect(150, 226, 180, 14);
                j.fuente.draw(g, "ESCRIBE TU NOMBRE!", 240 - PixelFont.width("ESCRIBE TU NOMBRE!", 1) / 2, 230, 1, new Color(0xFFE680));
            });
        }
    }

    private void flecha(Graphics2D g, String lado, int x) {
        boolean sobre = dentro(j.input.mx, j.input.my, x, 108, 14, 22);
        imagen(g, Assets.img("usuario/libro/flechas/flecha_" + lado + (sobre ? "_hover" : "") + ".png"), x, 108);
    }
}
