package aventurarte.pantallas;

import aventurarte.core.*;
import aventurarte.hilos.*;
import aventurarte.juego.*;
import aventurarte.modelo.Coleccion;
import aventurarte.modelo.DatosCuriosos;
import aventurarte.modelo.DatosNivel;
import aventurarte.modelo.Estrellas;
import aventurarte.modelo.Inventario;
import aventurarte.modelo.Niveles;
import aventurarte.modelo.Records;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * El NIVEL jugable.  Aqui se juntan las dos mitades del juego:
 *
 *   HILO DEL JUEGO (bucle de 60 FPS):  mueve al jugador y a los proyectiles, resuelve TODAS las colisiones y dibuja.
 *   HILOS DEL NIVEL:  HiloTemporizador (el reloj de 3 min), un HiloEnemigo por enemigo, y HiloEventos (eventos al azar).
 *
 * Todos comparten un solo Inventario (synchronized) y una PausaControl que los congela a la vez.
 */
public class NivelPantalla extends Pantalla {
    private enum Estado { LISTO, DATO, JUGANDO, VICTORIA, REINICIA, FIN }

    private final int n;
    private final DatosNivel d;
    private final Escena escena;
    private Mundo m;
    private PausaControl pausa;
    private HiloTemporizador timer;
    private final List<Thread> hilos = new ArrayList<>();

    private Estado estado = Estado.LISTO;
    private float estadoT, tAnim, camX;
    private float vinetaDano, corRomperT = -1;
    private int corRomperIdx;
    private boolean pocoTiempoAvisado;
    private BufferedImage avisoImg, avisoPendiente;
    private float avisoT, avisoDur, avisoPendienteEn = -1, brilloT = -1, puertaCooldown;

    // ---- dato curioso del pueblo (cajita de dialogo antes de empezar; solo la primera vez que juegas el nivel)
    private CajaDialogo dialogo;
    private boolean datoMostrado;

    // ---- repetir un nivel que ya estaba completado (para ganar mas estrellas)
    private final boolean repeticion;
    private Inventario.Respaldo respaldo;           // lo que tenia el nivel antes de repetirlo

    // ---- resultado del nivel (se llena al completarlo)
    private boolean[] estrellasGanadas = new boolean[3];
    private int resMonedas, resTiempo, resGolpes, estrellasMostradas;
    private Records.Resultado resultado;
    private boolean recordAvisado;
    private boolean lugarVisto;
    private final java.util.Set<Integer> vistos = new java.util.HashSet<>();   // enemigos cuya tarjeta ya se reviso en este nivel
    private Ambiente ambiente;                      // mariposas, perrito, papel picado y confeti
    private PanelResultado panel;
    private final PanelResultado.Datos datosPanel = new PanelResultado.Datos();
    private static final float ESTRELLAS_LISTAS = 3.4f;     // desde aqui ya se pueden usar los botones del panel

    private final Boton btnPausa = new Boton("hud/botones/boton_pausa", 444, 0, 5, 0, 0);
    private final Boton btnBolsa = new Boton("inventario/libro/botones/boton_inventario_hud", 356, 0, 5, 0, 0);

    public NivelPantalla(Juego j, int n) {
        super(j);
        this.n = n;
        this.d = Niveles.crear(n);
        this.escena = new Escena(d);
        this.repeticion = j.inv.snapshot().completado[n];
        this.datoMostrado = repeticion || Boolean.getBoolean("aventurarte.sindato");   // al repetir no se vuelve a mostrar
        if (repeticion) {
            respaldo = j.inv.respaldarNivel(n);      // guardamos lo que ya tenias para devolverlo si te va peor
            j.inv.empezarRepeticion(n);              // y empiezas con 3 corazones y 0 monedas
        } else {
            j.inv.reiniciarNivel(n);                 // empieza limpio (los intentos gastados se conservan)
        }
        j.inv.iniciarNivel(n);
        panel = new PanelResultado(j.fuente);
        ambiente = new Ambiente(n, d.ancho);
        iniciarMundo();
    }

    public int numero() { return n; }

    public boolean esRepeticion() { return repeticion; }

    /** Solo para pruebas automaticas. */
    public Mundo mundo() { return m; }

    // ------------------------------------------------------------------ vida de los hilos
    private void iniciarMundo() {
        detenerHilos();
        m = new Mundo(d, j.perfil, j.audio);
        pausa = new PausaControl(true);                    // arrancan en pausa hasta que desaparece el cartel
        timer = new HiloTemporizador(pausa);
        hilos.add(timer);
        for (Enemigo e : m.enemigos) hilos.add(new HiloEnemigo(e, m, pausa));
        hilos.add(new HiloEventos(m, pausa, timer, j.inv, j.audio, n));
        for (Thread t : hilos) t.start();
        estado = Estado.LISTO;
        estadoT = 0; camX = 0; corRomperT = -1; vinetaDano = 0; pocoTiempoAvisado = false;
        avisoImg = null; avisoPendiente = null; avisoPendienteEn = -1; brilloT = -1; puertaCooldown = 0;
    }

    // abre la cajita con el dato curioso del pueblo (el personaje que elegiste "habla")
    private void abrirDato() {
        datoMostrado = true;
        estado = Estado.DATO; estadoT = 0;
        String nombre = j.perfil.getNombre().isEmpty() ? "JUGADOR" : j.perfil.getNombre();
        dialogo = new CajaDialogo(j.fuente, Assets.img(j.perfil.rutaRetrato()), nombre, "DATO CURIOSO", DatosCuriosos.de(n));
    }

    private void detenerHilos() {
        if (pausa != null) pausa.detener();
        for (Thread t : hilos) t.interrupt();
        for (Thread t : hilos) { try { t.join(150); } catch (InterruptedException ignorar) { } }
        hilos.clear();
    }

    @Override public void alEntrar() { j.audio.musica(d.musica); }

    @Override public void alSalir() { detenerHilos(); }

    @Override public void alRecuperarFoco() {
        j.audio.reanudarMusica();
        if (estado == Estado.JUGANDO) pausa.reanudar();
    }

    /** Lo llama el menu de pausa (REINICIAR). */
    public void reiniciar() {
        if (repeticion) j.inv.reiniciarRepeticion(n);
        else j.inv.reiniciarNivel(n);
        iniciarMundo();
    }

    /** Lo llama el menu de pausa (SALIR). */
    public void salirAlMapa() {
        if (repeticion) j.inv.restaurarNivel(n, respaldo);   // abandonar una repeticion no te quita lo que ya tenias
        else j.inv.reiniciarNivel(n);                        // el nivel abandonado no cuenta (los intentos gastados si)
        j.ir(new MapaPantalla(j, -1, n));
    }

    /** Termina una repeticion sin ganar: devuelve el nivel como estaba y vuelve al mapa. */
    private void terminarRepeticionSinGanar() {
        j.inv.restaurarNivel(n, respaldo);
        j.ir(new MapaPantalla(j, -1, n));
    }

    private int seg() { return timer.getTranscurridoSeg(); }

    // ------------------------------------------------------------------ logica (hilo del juego)
    @Override public void actualizar(float dt) {
        tAnim += dt; estadoT += dt;
        if (vinetaDano > 0) vinetaDano -= dt;
        if (corRomperT >= 0) corRomperT += dt;
        if (avisoT > 0) avisoT -= dt;
        if (puertaCooldown > 0) puertaCooldown -= dt;
        if (brilloT >= 0) { brilloT += dt; if (brilloT > 1f) brilloT = -1; }
        if (avisoPendienteEn >= 0) { avisoPendienteEn -= dt; if (avisoPendienteEn < 0) { mostrar(avisoPendiente, 2f); avisoPendiente = null; } }
        m.actualizarEfectos(dt);
        ambiente.actualizar(dt, camX, m.viento(), m.jugador.x, estado == Estado.JUGANDO);
        ambiente.actualizarConfeti(dt);

        Input in = j.input;
        boolean abrirPausa = false, abrirHistorial = false, confirmar = false, teclaSeguir = false, teclaRepetir = false, clicSeguir = false, clicRepetir = false;
        int k;
        while ((k = in.sigPulsada()) != -1) {
            if (k == KeyEvent.VK_ESCAPE || k == KeyEvent.VK_P) abrirPausa = true;
            else if (k == KeyEvent.VK_I) abrirHistorial = true;
            else if (k == KeyEvent.VK_ENTER || k == KeyEvent.VK_SPACE) { confirmar = true; teclaSeguir = true; }
            else if (k == KeyEvent.VK_R) teclaRepetir = true;
        }
        for (int[] c : in.clics()) {
            confirmar = true;
            if (panel.sobreRepetir(c[0], c[1])) clicRepetir = true;
            else if (panel.sobreSeguir(c[0], c[1])) clicSeguir = true;
            if (btnPausa.contiene(c[0], c[1])) abrirPausa = true;
            if (btnBolsa.contiene(c[0], c[1])) abrirHistorial = true;
        }
        boolean activo = estado == Estado.JUGANDO || estado == Estado.LISTO || estado == Estado.DATO;
        if (activo && (abrirPausa || abrirHistorial)) {
            pausa.pausar();                                   // congela timer, enemigos y eventos
            j.audio.pausarMusica();
            j.apilar(abrirPausa ? new PausaPantalla(j, this) : new HistorialPantalla(j, this));
            return;
        }

        switch (estado) {
            case LISTO:
                m.jugador.actualizar(dt, false, false, false, false, m, 1, 0);
                if (estadoT >= 2.5f) {
                    if (datoMostrado) { estado = Estado.JUGANDO; estadoT = 0; pausa.reanudar(); }
                    else abrirDato();                                // primero el dato curioso, el reloj sigue detenido
                }
                break;
            case DATO:
                m.jugador.actualizar(dt, false, false, false, false, m, 1, 0);
                dialogo.actualizar(dt);
                if (confirmar) { dialogo.avanzar(); }
                if (dialogo.terminada()) { dialogo = null; estado = Estado.JUGANDO; estadoT = 0; pausa.reanudar(); }
                break;
            case JUGANDO:
                jugar(dt, in);
                break;
            case VICTORIA:
                m.jugador.actualizar(dt, false, false, false, false, m, 1, 0);
                animarEstrellas();
                // cuando ya salieron las estrellas: CONTINUAR (ENTER / ESPACIO / boton) o REPETIR (R / boton)
                if (estadoT >= ESTRELLAS_LISTAS) {
                    if (teclaRepetir || clicRepetir) { j.audio.sfx("clic"); j.ir(new NivelPantalla(j, n)); return; }
                    if (teclaSeguir || clicSeguir) {
                        j.audio.sfx("clic");
                        if (repeticion) j.ir(new MapaPantalla(j, -1, n));         // repetir no mueve al personaje por el mapa
                        else if (j.inv.snapshot().juegoCompleto()) j.ir(new FinalPantalla(j, true));
                        else j.ir(new MapaPantalla(j, n));
                        return;
                    }
                }
                break;
            case REINICIA:
                m.jugador.actualizar(dt, false, false, false, false, m, 1, 0);
                if (estadoT >= 2.8f) reiniciar();
                break;
            case FIN:
                m.jugador.actualizar(dt, false, false, false, false, m, 1, 0);
                if (estadoT >= 3.2f) {
                    if (repeticion) terminarRepeticionSinGanar();             // perder una repeticion NO acaba la partida
                    else j.ir(new FinalPantalla(j, false));
                    return;
                }
                break;
        }

        // camara
        float objetivo = m.jugador.x - 190;
        objetivo = Util.clamp(objetivo, 0, d.ancho - W);
        camX += (objetivo - camX) * Math.min(1f, 6f * dt);
        camX = Util.clamp(camX, 0, d.ancho - W);
        if (estado == Estado.LISTO || estado == Estado.DATO || estado == Estado.JUGANDO) descubrirCosas();
    }

    private void jugar(float dt, Input in) {
        Jugador p = m.jugador;
        boolean izq = in.tecla(KeyEvent.VK_LEFT) || in.tecla(KeyEvent.VK_A);
        boolean der = in.tecla(KeyEvent.VK_RIGHT) || in.tecla(KeyEvent.VK_D);
        boolean saltar = in.tecla(KeyEvent.VK_SPACE) || in.tecla(KeyEvent.VK_UP) || in.tecla(KeyEvent.VK_W);
        boolean lanzar = in.tecla(KeyEvent.VK_Z) || in.tecla(KeyEvent.VK_X) || in.tecla(KeyEvent.VK_J);
        p.actualizar(dt, izq, der, saltar, lanzar, m, m.multiplicadorVelocidad(), m.viento());

        // ---- proyectiles
        for (Proyectil pr : m.proyectiles) {
            pr.actualizar(dt);
            if (!pr.enImpacto) resolverProyectil(pr);
            if (pr.muerto) m.proyectiles.remove(pr);
        }
        // ---- enemigos: tocar / pisar
        for (Enemigo e : m.enemigos) {
            if (e.estaMuerto()) { m.enemigos.remove(e); continue; }
            if (!e.peligroso()) continue;
            if (Mundo.solapa(p.izq(), p.arriba(), 2 * Jugador.MEDIO_ANCHO, Jugador.ALTO, e.izqCaja(), e.arribaCaja(), 28, 50)) {
                if (p.vy > 0 && p.y <= e.arribaCaja() + 24) {            // cae encima: lo aplasta
                    if (e.golpear(2, m)) enemigoVencido();
                    p.rebotar();
                } else golpeJugador("GOLPE DE ENEMIGO");
            }
        }
        // ---- monedas
        int s = seg();
        for (Mundo.Moneda c : m.monedas) {
            if (c.tomada) continue;
            if (Mundo.solapa(p.izq(), p.arriba(), 2 * Jugador.MEDIO_ANCHO, Jugador.ALTO, c.x, c.y, 14, 14)) {
                c.tomada = true;
                if (j.inv.agregarMoneda(n, s)) j.audio.sfx("moneda");
            }
        }
        // ---- llave
        if (!m.llaveTomada && Mundo.solapa(p.izq(), p.arriba(), 2 * Jugador.MEDIO_ANCHO, Jugador.ALTO, d.llaveX, d.llaveY, escena.llaveAncho(), escena.llaveAlto())) {
            m.llaveTomada = true;
            j.inv.tomarLlave(n, s);
            j.avisarLogros(j.coleccion.descubrir(j.nombreJugador(), Coleccion.idLlave(n)));
            j.audio.sfx("llave");
            mostrar(Assets.img("hud/avisos/aviso_llave_obtenida.png"), 2f);
            brilloT = 0;
            m.puertaAbierta = true;
            j.inv.registrar(n, s, Inventario.Tipo.PUERTA, "PUERTA ABIERTA");
            avisoPendiente = Assets.img("hud/avisos/aviso_puerta_abierta.png");
            avisoPendienteEn = 2.1f;
        }
        // ---- puerta
        int pw = escena.puertaAncho();
        if (Mundo.solapa(p.izq(), p.arriba(), 2 * Jugador.MEDIO_ANCHO, Jugador.ALTO, d.puertaX + 10, 128, pw - 20, 90)) {
            if (m.puertaAbierta) { completar(); return; }
            if (puertaCooldown <= 0) { mostrar(Assets.img("hud/avisos/aviso_consigue_la_llave.png"), 2f); puertaCooldown = 3.5f; j.audio.sfx("golpe"); }
        }
        // ---- tiempo
        int restantes = timer.getRestantes();
        if (restantes <= 30 && !pocoTiempoAvisado && !timer.isAgotado()) {
            pocoTiempoAvisado = true;
            mostrar(Assets.img("hud/avisos/aviso_poco_tiempo.png"), 2f);
            j.inv.registrar(n, s, Inventario.Tipo.TIEMPO, "QUEDAN 30 SEG");
            j.audio.sfx("poco_tiempo");
        } else if (restantes > 35) pocoTiempoAvisado = false;
        if (timer.isAgotado()) tiempoAgotado();
    }

    private void resolverProyectil(Proyectil pr) {
        for (Mundo.Obstaculo o : m.obstaculos)
            if (pr.x > o.x && pr.x < o.x + o.w && pr.y > o.y) { pr.estrellar(); return; }
        Jugador p = m.jugador;
        if (pr.delJugador) {
            for (Enemigo e : m.enemigos) {
                if (!e.peligroso()) continue;
                if (Math.abs(pr.x - e.x) < 20 && pr.y > e.arribaCaja() && pr.y < Enemigo.PISO) {
                    pr.estrellar();
                    if (e.golpear(1, m)) enemigoVencido();
                    return;
                }
            }
        } else if (Mundo.solapa(pr.x - 6, pr.y - 6, 12, 12, p.izq(), p.arriba(), 2 * Jugador.MEDIO_ANCHO, Jugador.ALTO)) {
            pr.estrellar();
            golpeJugador("GOLPE: PROYECTIL");
        }
    }

    private void enemigoVencido() {
        j.inv.registrar(n, seg(), Inventario.Tipo.EVENTO, "ENEMIGO VENCIDO");
        j.avisarLogros(j.coleccion.enemigoVencido(j.nombreJugador()));
    }

    /** Descubre las tarjetas de lo que aparece en pantalla: el pueblo al empezar y cada enemigo la primera vez que lo ves. */
    private void descubrirCosas() {
        String nombre = j.nombreJugador();
        java.util.List<String> nuevos = new ArrayList<>();
        if (!lugarVisto) { lugarVisto = true; nuevos.addAll(j.coleccion.descubrir(nombre, Coleccion.idLugar(n))); }
        for (Enemigo e : m.enemigos) {
            if (e.x + 24 < camX || e.x - 24 > camX + W) continue;                 // todavia no sale en pantalla
            int id = Coleccion.idEnemigo(e.tipo);
            if (vistos.add(id)) nuevos.addAll(j.coleccion.descubrir(nombre, id));
        }
        if (!nuevos.isEmpty()) j.avisarLogros(nuevos);
    }

    /** El jugador pierde un intento (corazon). Si ya no le quedan, termina el juego. */
    private void golpeJugador(String texto) {
        Jugador p = m.jugador;
        if (estado != Estado.JUGANDO || p.invulnerable > 0) return;
        int s = seg();
        j.inv.registrar(n, s, Inventario.Tipo.GOLPE, texto);
        int quedan = j.inv.perderIntento(n, s);
        corRomperIdx = Math.max(0, quedan); corRomperT = 0; vinetaDano = 0.3f;
        if (quedan <= 0) { finDelJuego(); return; }
        p.golpeado();
        j.audio.sfx("golpe");
        mostrar(Assets.img("hud/avisos/aviso_intento_perdido.png"), 1.8f);
    }

    private void tiempoAgotado() {
        int s = HiloTemporizador.LIMITE;
        j.inv.registrar(n, s, Inventario.Tipo.TIEMPO, "TIEMPO AGOTADO");
        int quedan = j.inv.perderIntento(n, s);
        corRomperIdx = Math.max(0, quedan); corRomperT = 0;
        pausa.pausar();
        if (quedan <= 0) { finDelJuego(); return; }
        estado = Estado.REINICIA; estadoT = 0;
        j.audio.sfx("perder");
        mostrar(Assets.img("hud/avisos/aviso_tiempo_agotado.png"), 2.6f);
    }

    private void finDelJuego() {
        estado = Estado.FIN; estadoT = 0;
        pausa.pausar();
        m.jugador.derrotado = true; m.jugador.animT = 0;
        j.inv.registrar(n, seg(), Inventario.Tipo.FIN, "SIN INTENTOS");
        j.audio.pararMusica();
        j.audio.sfx("perder");
        mostrar(Assets.img(repeticion ? "hud/avisos/aviso_intento_perdido.png" : "hud/avisos/aviso_fin_del_juego.png"), 3f);
    }

    private void completar() {
        estado = Estado.VICTORIA; estadoT = 0;
        pausa.pausar();                                          // el reloj, los enemigos y los eventos se detienen
        calcularResultado();
        m.jugador.victoria = true; m.jugador.animT = 0;
        j.audio.pararMusica();
        j.audio.jingle("sfx_ganar");
        ambiente.iniciarConfeti();
    }

    /**
     * Al llegar a la puerta: saca las estrellas, anota el nivel en el inventario y guarda las mejores marcas.
     * Si estabas repitiendo y te fue peor que antes, el inventario conserva tu resultado anterior.
     */
    private void calcularResultado() {
        int s = seg();
        Inventario.Snapshot antes = j.inv.snapshot();                 // todavia sin el nivel anotado
        resMonedas = antes.monedas[n];
        resGolpes = antes.intentosUsados[n];
        resTiempo = s;
        estrellasGanadas = Estrellas.calcular(n, resMonedas, resGolpes);
        int estrellas = Estrellas.contar(estrellasGanadas);

        j.inv.completarNivel(n, s, estrellas);
        resultado = j.records.registrar(j.nombreJugador(), n, estrellas, resMonedas, s);

        if (repeticion && !Estrellas.esMejor(estrellas, resMonedas, s,
                respaldo.getEstrellas(), respaldo.getMonedas(), respaldo.getTiempo())) {
            j.inv.restaurarNivel(n, respaldo);                        // fue peor: el inventario se queda con lo anterior
        }
        estrellasMostradas = 0; recordAvisado = false;

        // logros de este resultado (la tarjeta del lugar/enemigos ya se descubrio antes)
        boolean record = resultado != null && resultado.hayRecord();
        boolean todas = estrellasGanadas[1];
        j.avisarLogros(j.coleccion.nivelCompletado(j.nombreJugador(), resGolpes, todas, estrellas, s, record,
                j.inv.snapshot().juegoCompleto()));
    }

    /** Enciende las estrellas una por una (con sonido) y avisa si hay record. */
    private void animarEstrellas() {
        float inicio = 0.9f, cada = 0.55f;
        while (estrellasMostradas < 3 && estadoT >= inicio + cada * estrellasMostradas) {
            if (estrellasGanadas[estrellasMostradas]) j.audio.sfx("moneda");
            estrellasMostradas++;
        }
        if (!recordAvisado && estrellasMostradas >= 3 && estadoT >= inicio + cada * 3) {
            recordAvisado = true;
            if (resultado != null && resultado.hayRecord()) j.audio.sfx("evento");
        }
    }

    private void mostrar(BufferedImage img, float dur) { avisoImg = img; avisoDur = dur; avisoT = dur; }

    // ------------------------------------------------------------------ dibujo
    @Override public void dibujar(Graphics2D g) {
        float p = estado == Estado.LISTO ? 0 : (float) (timer.getTranscurrido() / HiloTemporizador.LIMITE);
        escena.dibujarFondo(g, camX, p, tAnim);
        escena.dibujarObjetos(g, m, camX, tAnim);
        ambiente.dibujarAtras(g, camX, tAnim, j.fuente);                       // el perrito va detras del personaje
        int cx = Math.round(camX);

        for (Enemigo e : m.enemigos) {
            if (e.estaMuerto()) continue;
            BufferedImage f = e.frame();
            int sx = Math.round(e.x) - 24 - cx, sy = Enemigo.PISO - 62;
            if (sx > W + 50 || sx < -100) continue;
            if (e.derecha) g.drawImage(f, sx, sy, null); else g.drawImage(f, sx + 48, sy, -48, 64, null);
        }
        for (Proyectil pr : m.proyectiles) {
            BufferedImage f = pr.frame();
            g.drawImage(f, Math.round(pr.x) - f.getWidth() / 2 - cx, Math.round(pr.y) - f.getHeight() / 2, null);
        }
        Jugador pl = m.jugador;
        if (!(pl.invulnerable > 0 && ((int) (pl.invulnerable * 12)) % 2 == 0)) {
            BufferedImage f = pl.frame();
            int sx = Math.round(pl.x) - 20 - cx, sy = Math.round(pl.y) - 72;
            if (pl.derecha) g.drawImage(f, sx, sy, null); else g.drawImage(f, sx + 40, sy, -40, 73, null);
        }
        if (brilloT >= 0)
            g.drawImage(Assets.img("hud/iconos/brillo_" + Math.min(3, (int) (brilloT / 0.2f) % 4) + ".png"), Math.round(pl.x) - 12 - cx, Math.round(pl.y) - 90, null);
        escena.dibujarFrente(g, camX);
        ambiente.dibujarFrente(g, camX, tAnim);                                // mariposas y papel picado

        clima(g);
        if (vinetaDano > 0) conAlpha(g, vinetaDano / 0.3f, () -> imagen(g, Assets.img("hud/efectos/vineta_dano_480x270.png"), 0, 0));
        if (d.noche) conAlpha(g, 0.7f, () -> imagen(g, Assets.img("hud/efectos/vineta_noche_480x270.png"), 0, 0));
        if (estado == Estado.JUGANDO && timer.getRestantes() <= 30)
            conAlpha(g, 0.35f + 0.35f * (float) Math.sin(tAnim * 6), () -> imagen(g, Assets.img("hud/efectos/vineta_poco_tiempo_480x270.png"), 0, 0));

        hud(g);
        avisos(g);
        if (estado == Estado.VICTORIA) { ambiente.dibujarConfeti(g); resultados(g); }
        if (estado == Estado.DATO && dialogo != null) dialogo.dibujar(g);
    }

    private void clima(Graphics2D g) {
        Mundo.Efecto e = m.getEfecto();
        if (e == Mundo.Efecto.LLUVIA) {
            g.setColor(new Color(170, 200, 255, 150));
            for (int i = 0; i < 70; i++) {
                int x = (int) ((i * 53 + tAnim * 120) % (W + 40)) - 20;
                int y = (int) ((i * 97 + tAnim * 380) % H);
                g.drawLine(x, y, x - 3, y + 9);
            }
        } else if (e == Mundo.Efecto.VIENTO) {
            int dir = m.direccionViento();
            g.setColor(new Color(255, 255, 255, 110));
            for (int i = 0; i < 18; i++) {
                int y = (i * 37) % 200 + 20;
                int x = (int) (((i * 71 + dir * tAnim * 420) % (W + 80) + W + 80) % (W + 80)) - 40;
                g.drawLine(x, y, x + 24, y);
            }
        }
    }

    private void hud(Graphics2D g) {
        Inventario.Snapshot s = j.inv.snapshot();
        // vidas
        imagen(g, Assets.img("hud/paneles/panel_vidas.png"), 6, 5);
        int quedan = Math.max(0, 3 - s.intentosUsados[n]);
        for (int i = 0; i < 3; i++) {
            int hx = 6 + 8 + i * 17, hy = 5 + 8;
            if (i < quedan) imagen(g, Assets.img("hud/iconos/corazon_lleno.png"), hx, hy);
            else if (i == corRomperIdx && corRomperT >= 0 && corRomperT < 0.48f)
                imagen(g, Assets.img("hud/iconos/corazon_romper_" + (int) (corRomperT / 0.12f) + ".png"), hx - 3, hy - 2);
            else imagen(g, Assets.img("hud/iconos/corazon_vacio.png"), hx, hy);
        }
        if (repeticion) {
            String t = "REPETICION";
            int w = PixelFont.width(t, 1) + 12;
            g.setColor(new Color(0x2A1240)); g.fillRoundRect(6, 34, w, 13, 6, 6);
            g.setColor(new Color(0xFFD23F)); g.drawRoundRect(6, 34, w, 13, 6, 6);
            j.fuente.draw(g, t, 12, 37, 1, new Color(0xFFF1D6));
        }
        // monedas
        imagen(g, Assets.img("hud/paneles/panel_monedas.png"), 74, 5);
        imagen(g, escena.monedaFrame(tAnim), 74 + 6, 5 + 6);
        j.fuente.draw(g, String.format("%02d/%02d", s.monedas[n], Inventario.MONEDAS_TOTAL[n]), 74 + 29, 5 + 6, 2, new Color(0xFFE680));
        // reloj (cuenta regresiva)
        int rest = estado == Estado.LISTO ? HiloTemporizador.LIMITE : timer.getRestantes();
        boolean urgente = rest <= 30 && estado != Estado.LISTO;
        imagen(g, Assets.img("hud/paneles/panel_timer_" + (urgente ? "urgente" : "normal") + ".png"), 194, 5);
        if (!urgente || (int) (tAnim * 3) % 2 == 0)
            j.fuente.draw(g, Util.mmss(rest), 194 + 29, 5 + 6, 2, urgente ? new Color(0xFF8A8A) : new Color(0xFFF1D6));
        // barra de tiempo con sol / luna
        imagen(g, Assets.img("hud/paneles/barra_tiempo.png"), 176, 34);
        float prog = estado == Estado.LISTO ? 0 : (float) (timer.getTranscurrido() / HiloTemporizador.LIMITE);
        int mx = 176 + 3 + Math.round(prog * (128 - 6)) - 6;
        imagen(g, Assets.img("hud/iconos/marcador_" + (d.noche ? "luna" : "sol") + ".png"), mx, 33);
        // llave
        imagen(g, Assets.img("hud/paneles/slot_llave_" + (n + 1) + (s.llave[n] ? "_lleno" : "_vacio") + ".png"), 392, 5);
        // botones
        int ratonX = j.input.mx, ratonY = j.input.my;
        btnBolsa.dibujar(g, btnBolsa.contiene(ratonX, ratonY), j.input.mouseAbajo && btnBolsa.contiene(ratonX, ratonY));
        btnPausa.dibujar(g, btnPausa.contiene(ratonX, ratonY), j.input.mouseAbajo && btnPausa.contiene(ratonX, ratonY));
    }

    private void avisos(Graphics2D g) {
        if (estado == Estado.LISTO) {
            BufferedImage c = Assets.img("hud/carteles/cartel_nivel_" + (n + 1) + ".png");
            float a = Math.min(1f, Math.min(estadoT / 0.4f, (2.5f - estadoT) / 0.4f));
            conAlpha(g, a, () -> imagen(g, c, 115, 70));
        }
        if (avisoImg != null && avisoT > 0) {
            float t = avisoDur - avisoT;
            float a = Math.min(1f, Math.min(t / 0.25f, avisoT / 0.4f));
            conAlpha(g, a, () -> imagen(g, avisoImg, (W - avisoImg.getWidth()) / 2, 70));
        }
        if (m.getTextoEventoT() > 0 && estado == Estado.JUGANDO) {
            String t = m.getTextoEvento();
            int w = PixelFont.width(t, 1) + 16;
            float a = Math.min(1f, m.getTextoEventoT());
            conAlpha(g, a, () -> {
                g.setColor(new Color(0x2A1240));
                g.fillRoundRect((W - w) / 2, 52, w, 14, 6, 6);
                g.setColor(new Color(0xFFD23F));
                g.drawRoundRect((W - w) / 2, 52, w, 14, 6, 6);
                j.fuente.draw(g, t, (W - PixelFont.width(t, 1)) / 2, 56, 1, new Color(0xFFF1D6));
            });
        }
    }

    /** Panel de estrellas que sale despues de "NIVEL COMPLETADO". */
    private void resultados(Graphics2D g) {
        PanelResultado.Datos d = datosPanel;
        d.ganadas = estrellasGanadas;
        d.mostradas = estrellasMostradas;
        d.t = estadoT;
        d.anim = tAnim;
        d.valores = new String[]{"", resMonedas + "/" + Inventario.MONEDAS_TOTAL[n], resGolpes == 1 ? "1 GOLPE" : resGolpes + " GOLPES"};
        d.tiempo = resTiempo;
        d.mejorTiempo = resultado != null ? resultado.mejorTiempo : 0;
        d.hayRecord = recordAvisado && resultado != null && resultado.hayRecord();
        d.botonesActivos = estadoT >= ESTRELLAS_LISTAS;
        d.mx = j.input.mx; d.my = j.input.my; d.mouseAbajo = j.input.mouseAbajo;
        panel.dibujar(g, d);
    }
}
