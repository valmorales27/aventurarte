package aventurarte;

import aventurarte.core.GamePanel;
import aventurarte.core.Juego;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** Punto de entrada.  Ejecuta esta clase (clic derecho > Run 'Main.main()') para jugar. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Juego juego = new Juego();
            GamePanel panel = new GamePanel(juego);

            JFrame ventana = new JFrame("AVENTURARTE - Ruta de los cinco pueblos");
            Image icono = Juego.icono();
            if (icono != null) ventana.setIconImage(icono);
            ventana.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            ventana.addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) {
                    panel.detener();
                    juego.guardarTodo();
                    System.exit(0);
                }
            });
            ventana.setContentPane(panel);
            ventana.pack();
            ventana.setMinimumSize(new Dimension(480 + 16, 270 + 39));
            ventana.setLocationRelativeTo(null);
            ventana.setVisible(true);
            panel.requestFocusInWindow();

            juego.iniciar();
            panel.iniciar();
        });
    }
}
