package aventurarte.core;

import javax.sound.sampled.*;
import java.io.*;

/** Clase para revisar tus sonidos antes de jugar.
    No abre el juego: solo lee los .wav de res/audio y muestra archivo por archivo,
    si esta bien, si falta o si tiene algun problema (formato, etc.) se usa con Run 'RevisarAudio.main()'.
 */
public class RevisarAudio {

    // nombre, tipo (M = musica en bucle, E = efecto corto, J = jingle), segundos minimos y maximos recomendados
    private static final Object[][] ESPERADOS = {
            {"musica_menu", "M", 10, 240}, {"musica_nivel_1", "M", 10, 240}, {"musica_nivel_2", "M", 10, 240},
            {"musica_nivel_3", "M", 10, 240}, {"musica_nivel_4", "M", 10, 240}, {"musica_nivel_5", "M", 10, 240},
            {"fanfarria", "J", 1, 12},
            {"sfx_clic", "E", 0, 1}, {"sfx_pausa", "E", 0, 2}, {"sfx_salto", "E", 0, 1}, {"sfx_moneda", "E", 0, 2},
            {"sfx_llave", "E", 0, 3}, {"sfx_puerta", "E", 0, 3}, {"sfx_golpe", "E", 0, 2}, {"sfx_impacto", "E", 0, 2},
            {"sfx_lanzar", "E", 0, 2}, {"sfx_alerta", "E", 0, 2}, {"sfx_derrota_enemigo", "E", 0, 3},
            {"sfx_evento", "E", 0, 3}, {"sfx_poco_tiempo", "E", 0, 4}, {"sfx_ganar", "E", 0, 8}, {"sfx_perder", "E", 0, 8}};

    public static void main(String[] args) {
        // por si la ejecutamos desde otra carpeta: RevisarAudio <carpeta con los wav>
        File carpeta = args.length > 0 ? new File(args[0]) : new File("res/audio");
        carpetaExplicita = args.length > 0;
        int ok = 0, avisos = 0, malos = 0;
        System.out.println("=== REVISION DE AUDIO ===");
        for (Object[] e : ESPERADOS) {
            String nombre = (String) e[0];
            String tipo = (String) e[1];
            double min = ((Integer) e[2]).doubleValue(), max = ((Integer) e[3]).doubleValue();
            byte[] datos = leer(nombre, carpeta);
            if (datos == null) {
                System.out.printf("[FALTA ] %-22s no encuentro %s.wav%n", nombre, nombre);
                malos++;
                continue;
            }
            try {
                AudioInputStream original = AudioSystem.getAudioInputStream(new ByteArrayInputStream(datos));
                AudioFormat f = original.getFormat();
                double seg = original.getFrameLength() / (double) f.getFrameRate();
                String formato = String.format("%.0f Hz, %d bit, %s", f.getSampleRate(), f.getSampleSizeInBits(), f.getChannels() == 1 ? "mono" : "estereo");

                java.util.List<String> notas = new java.util.ArrayList<>();
                // si no es PCM de 16 bits el juego lo convierte solo, muestra un mensaje
                if (f.getEncoding() != AudioFormat.Encoding.PCM_SIGNED || f.getSampleSizeInBits() != 16)
                    notas.add("no es PCM de 16 bits (el juego lo convierte, pero mejor usa convertir_audio)");
                if (seg > max) notas.add(String.format("dura %.1f s y lo normal es menos de %.0f s", seg, max));
                if (seg < min) notas.add(String.format("dura solo %.1f s y lo normal es al menos %.0f s", seg, min));
                if (tipo.equals("M") && datos.length > 30_000_000) notas.add("pesa mas de 30 MB; usa 44100 Hz o recortalo");

                double pico = pico(Audio.normalizar(AudioSystem.getAudioInputStream(new ByteArrayInputStream(datos))));
                if (pico < 0.05) notas.add("casi no se oye (volumen muy bajo); normalizalo en Audacity");
                else if (pico >= 0.999) notas.add("esta al maximo y puede sonar 'chillon' (recortado); bajale el volumen");

                // se hace la prueba: abrimos un Clip como lo hace el juego (si no hay bocinas, esto se omite)
                String clip = "";
                try {
                    Clip c = AudioSystem.getClip();
                    c.open(Audio.normalizar(AudioSystem.getAudioInputStream(new ByteArrayInputStream(datos))));
                    c.close();
                } catch (LineUnavailableException | IllegalArgumentException sinBocinas) {
                    clip = " (sin dispositivo de audio aqui: no se pudo abrir el Clip)";
                }

                String estado = notas.isEmpty() ? "[ OK   ]" : "[AVISO ]";
                System.out.printf("%s %-22s %5.1f s  %s%s%n", estado, nombre, seg, formato, clip);
                for (String n : notas) System.out.println("           - " + n);
                if (notas.isEmpty()) ok++; else avisos++;
            } catch (UnsupportedAudioFileException ex) {
                System.out.printf("[ERROR ] %-22s no es un WAV valido (¿es un mp3 con la extension cambiada?). Convierte con convertir_audio%n", nombre);
                malos++;
            } catch (Exception ex) {
                System.out.printf("[ERROR ] %-22s %s%n", nombre, ex);
                malos++;
            }
        }
        System.out.printf("%nResumen: %d bien, %d con avisos, %d con problemas (de %d archivos).%n", ok, avisos, malos, ESPERADOS.length);
        System.out.println(malos == 0 ? "Los avisos no impiden jugar: son solo recomendaciones." : "Arregla los que dicen FALTA o ERROR para que suenen en el juego.");
    }

    // primero busca en el classpath (res como Resources Root), si no en la carpeta indicada
    private static boolean carpetaExplicita;

    private static byte[] leer(String nombre, File carpeta) {
        if (carpetaExplicita) {
            File f = new File(carpeta, nombre + ".wav");
            try { return f.isFile() ? java.nio.file.Files.readAllBytes(f.toPath()) : null; } catch (IOException e) { return null; }
        }
        try (InputStream in = RevisarAudio.class.getResourceAsStream("/audio/" + nombre + ".wav")) {
            if (in != null) return in.readAllBytes();
        } catch (IOException ignorar) { }
        File f = new File(carpeta, nombre + ".wav");
        try {
            return f.isFile() ? java.nio.file.Files.readAllBytes(f.toPath()) : null;
        } catch (IOException e) {
            return null;
        }
    }

    // volumen maximo (0..1) del archivo ya convertido a 16 bits
    private static double pico(AudioInputStream in) throws IOException {
        byte[] b = in.readAllBytes();
        int max = 0;
        for (int i = 0; i + 1 < b.length; i += 2) {
            int v = Math.abs((short) ((b[i + 1] << 8) | (b[i] & 0xFF)));
            if (v > max) max = v;
        }
        return max / 32768.0;
    }
}
