package aventurarte.core;

import javax.sound.sampled.*;
import java.io.*;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/** Musica de fondo y efectos de sonido
 * todos los metodos son synchronized porque los hilos de enemigos / eventos tambien reproducen sonidos
 * los .wav se encuntran en res/audio */

public final class Audio {
    private final Map<String, byte[]> datos = new ConcurrentHashMap<>();
    private Clip musica;
    private String musicaActual = "";
    private boolean musicaPausada;
    private float volMusica = 0.6f, volEfectos = 0.8f;
    private boolean silencio;
    private boolean disponible = true;      // false solo si la computadora NO tiene salida de audio
    private int fallosDispositivo;           // intentos seguidos que fallaron por el dispositivo sin salida de audio
    private final java.util.Set<String> avisados = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private int activos;

    // ------------------------------------------------------------------ MÚSICA
    public synchronized void musica(String nombre) {
        if (nombre.equals(musicaActual) && musica != null) { if (!musicaPausada && !musica.isRunning()) musica.start(); return; }
        pararMusica();
        if (!disponible) return;
        try {
            musica = crearClip(nombre);
            musicaActual = nombre;
            musicaPausada = false;
            aplicarGanancia(musica, volMusica);
            musica.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Throwable e) {
            musica = null;
            musicaActual = "";
            fallo(nombre, e);
        }
    }

    public synchronized void pararMusica() {
        if (musica != null) { musica.stop(); musica.close(); musica = null; }
        musicaActual = "";
    }

    public synchronized void pausarMusica() {
        if (musica != null && musica.isRunning()) { musica.stop(); }
        musicaPausada = true;
    }

    public synchronized void reanudarMusica() {
        musicaPausada = false;
        if (musica != null && !musica.isRunning()) musica.loop(Clip.LOOP_CONTINUOUSLY);
    }

    // ------------------------------------------------------------------ EFECTOS
    public synchronized void sfx(String nombre) {
        if (!disponible || silencio || activos > 24) return;
        try {
            final Clip c = crearClip("sfx_" + nombre);
            aplicarGanancia(c, volEfectos);
            activos++;
            c.addLineListener(ev -> {
                if (ev.getType() == LineEvent.Type.STOP) { c.close(); synchronized (Audio.this) { activos--; } }
            });
            c.start();
        } catch (Throwable e) {
            fallo("sfx_" + nombre, e);      // si falla un efecto de sonido, el juego sigue
        }
    }

    /** Reproduce un wav por nombre completo (sin "sfx_"), ejemplo "fanfarria" */
    public synchronized void jingle(String nombre) {
        if (!disponible || silencio) return;
        try {
            final Clip c = crearClip(nombre);
            aplicarGanancia(c, volEfectos);
            activos++;
            c.addLineListener(ev -> {
                if (ev.getType() == LineEvent.Type.STOP) { c.close(); synchronized (Audio.this) { activos--; } }
            });
            c.start();
        } catch (Throwable e) { fallo(nombre, e); }
    }

    // ------------------------------------------------------------------ VOLUMEN
    public synchronized float getVolMusica() { return volMusica; }
    public synchronized float getVolEfectos() { return volEfectos; }
    public synchronized boolean isSilencio() { return silencio; }

    public synchronized void setVolMusica(float v) { volMusica = clamp(v); if (musica != null) aplicarGanancia(musica, volMusica); }
    public synchronized void setVolEfectos(float v) { volEfectos = clamp(v); }
    public synchronized void setSilencio(boolean s) {
        silencio = s;
        if (musica != null) aplicarGanancia(musica, volMusica);
    }

    private float clamp(float v) { return Math.max(0f, Math.min(1f, v)); }

    private void aplicarGanancia(Clip c, float vol) {
        try {
            FloatControl g = (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN);
            float v = silencio ? 0f : vol;
            float dB = (float) (20.0 * Math.log10(Math.max(v, 0.0001)));
            g.setValue(Math.max(dB, g.getMinimum()));
        } catch (Throwable ignorar) { }
    }

    private Clip crearClip(String nombre) throws Exception {
        byte[] b = datos.computeIfAbsent(nombre, n -> {
            try (InputStream in = Audio.class.getResourceAsStream("/audio/" + n + ".wav")) {
                if (in == null) throw new IllegalStateException("Falta el archivo /audio/" + n + ".wav");
                return in.readAllBytes();
            } catch (IOException e) { throw new IllegalStateException(e); }
        });
        AudioInputStream ais = normalizar(AudioSystem.getAudioInputStream(new ByteArrayInputStream(b)));
        Clip c = AudioSystem.getClip();
        c.open(ais);
        fallosDispositivo = 0;
        return c;
    }

    /** Deja el audio en PCM de 16 bits con signo
     si el .wav esta en 8 bits, 24 bits, 32 bits o con decimales (float), lo convierte solito */
    static AudioInputStream normalizar(AudioInputStream in) {
        AudioFormat f = in.getFormat();
        boolean yaBien = f.getEncoding() == AudioFormat.Encoding.PCM_SIGNED && f.getSampleSizeInBits() == 16 && !f.isBigEndian();
        if (yaBien) return in;
        AudioFormat destino = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, f.getSampleRate(), 16, f.getChannels(), f.getChannels() * 2, f.getSampleRate(), false);
        if (AudioSystem.isConversionSupported(destino, f)) return AudioSystem.getAudioInputStream(destino, in);
        return in;   // si no se puede convertir, se intenta tal cual y se muestra el error en consola
    }

    /** Un archivo malo solo desactiva tal sonido (y se muestra un mensaje, el audio se apaga por completo solo si no hay bocinas */
    private void fallo(String nombre, Throwable e) {
        Throwable causa = e;
        while (causa.getCause() != null) causa = causa.getCause();
        boolean deDispositivo = causa instanceof LineUnavailableException || causa instanceof SecurityException
                || (causa instanceof IllegalArgumentException && String.valueOf(causa.getMessage()).contains("No line matching"));
        if (deDispositivo) {
            if (++fallosDispositivo >= 3) {
                disponible = false;
                System.err.println("[Audio] no se pudo usar la salida de audio de la computadora; el juego continua sin sonido.");
            }
            return;
        }
        if (avisados.add(nombre))
            System.err.println("[Audio] no se pudo reproducir '" + nombre + "': " + causa.getMessage() + "  (revisa que sea un WAV)");
    }

    // ------------------------------------------------------------------ GUARDAR Y CARGAR AJUSTES
    public synchronized void guardar(File f) {
        try (OutputStream o = new FileOutputStream(f)) {
            Properties p = new Properties();
            p.setProperty("musica", String.valueOf(volMusica));
            p.setProperty("efectos", String.valueOf(volEfectos));
            p.setProperty("silencio", String.valueOf(silencio));
            p.store(o, "AVENTURARTE - audio");
        } catch (IOException ignorar) { }
    }

    public synchronized void cargar(File f) {
        try (InputStream in = new FileInputStream(f)) {
            Properties p = new Properties();
            p.load(in);
            volMusica = clamp(Float.parseFloat(p.getProperty("musica", "0.6")));
            volEfectos = clamp(Float.parseFloat(p.getProperty("efectos", "0.8")));
            silencio = Boolean.parseBoolean(p.getProperty("silencio", "false"));
        } catch (Exception ignorar) { }
    }
}
