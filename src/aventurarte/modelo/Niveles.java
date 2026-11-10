package aventurarte.modelo;

import static aventurarte.modelo.TipoEnemigo.*;

/**
 * LOS 5 NIVELES.  Aqui cambias el diseño: donde van obstaculos, plataformas, monedas, llave y enemigos.
 * (Las coordenadas x son del MUNDO, 0 = inicio del nivel; y = 214 es el suelo donde pisan los pies.)
 *
 * Reglas del PDF que se validan al iniciar (DatosNivel.validar):
 *   monedas  : 10, 15, 20, 30, 30         enemigos : 1, 2, 3, 4, 5 (nivel 5: 2 rapidos + 3 lanzadores)
 */
public final class Niveles {
    private Niveles() {}

    public static final int ULTIMO = 4;

    public static DatosNivel crear(int n) {
        DatosNivel d;
        switch (n) {
            case 0: d = nivel1(); break;
            case 1: d = nivel2(); break;
            case 2: d = nivel3(); break;
            case 3: d = nivel4(); break;
            default: d = nivel5();
        }
        d.validar();
        return d;
    }

    // ------------------------------------------------------------------ NIVEL 1: Xonacatlan (llave PELUCHE)
    private static DatosNivel nivel1() {
        DatosNivel d = new DatosNivel(0, "XONACATLAN", "musica_nivel_1", false, 2600,
                "banca.png", "caja_peluche.png", "maceta_arbusto.png");
        d.medioDesplazamiento = 50;      // coloca el lugar representativo junto a la puerta
        d.obstaculo(520, 0).obstaculo(980, 1).obstaculo(1560, 2).obstaculo(2000, 0);
        d.arco(543, 172, 3, 26, 32);                 // sobre la banca
        d.fila(760, 186, 2, 26);                     // en el suelo
        d.plataforma(1180, 160, 3).fila(1196, 134, 2, 24);
        d.arco(1577, 172, 3, 26, 34);                // sobre la maceta
        d.plataforma(2150, 150, 3).llave(2166, 118);
        d.enemigo(OSITO, 1400, 1320, 1480);
        return d;
    }

    // ------------------------------------------------------------------ NIVEL 2: San Mateo Atenco (llave ZAPATO)
    private static DatosNivel nivel2() {
        DatosNivel d = new DatosNivel(1, "SAN MATEO ATENCO", "musica_nivel_2", false, 3000,
                "barrera_obra.png", "cajas_zapatos.png", "carrito_calzado.png");
        d.medioDesplazamiento = 672;      // coloca el lugar representativo junto a la puerta
        d.obstaculo(450, 0).obstaculo(900, 1).obstaculo(1350, 2).obstaculo(1750, 0).obstaculo(2150, 1).obstaculo(2500, 2);
        d.arco(473, 172, 3, 26, 32);
        d.arco(917, 164, 3, 26, 30);
        d.arco(1373, 164, 3, 26, 32);
        d.plataforma(1600, 158, 4).fila(1614, 132, 3, 24);
        d.arco(2167, 164, 3, 26, 30);
        d.plataforma(2650, 156, 3).llave(2666, 124);
        d.enemigo(TENIS, 1150, 1000, 1300);          // rapidos: tramo largo
        d.enemigo(TENIS, 2000, 1850, 2100);
        return d;
    }

    // ------------------------------------------------------------------ NIVEL 3: Gualupita (llave SUETER)
    private static DatosNivel nivel3() {
        DatosNivel d = new DatosNivel(2, "GUALUPITA", "musica_nivel_3", false, 3400,
                "borrego.png", "cerca_madera.png", "paca_lana.png");
        d.medioDesplazamiento = 533;      // coloca el lugar representativo junto a la puerta
        int[] xs = {400, 800, 1250, 1700, 2150, 2600};
        int[] ty = {2, 1, 0, 2, 1, 0};
        for (int i = 0; i < xs.length; i++) d.obstaculo(xs[i], ty[i]);
        int[] anchos = {42, 50, 38, 42, 50, 38};
        for (int i = 0; i < xs.length; i++) d.arco(xs[i] + anchos[i] / 2, 168, 3, 26, 32);
        d.plataforma(1450, 160, 2).fila(1462, 134, 2, 24);
        d.plataforma(2800, 150, 3).llave(2816, 118);
        d.enemigo(BORREGO, 1000, 940, 1060);
        d.enemigo(BORREGO, 1950, 1890, 2010);
        d.enemigo(BORREGO, 2400, 2340, 2460);
        return d;
    }

    // ------------------------------------------------------------------ NIVEL 4: Ocoyoacac (llave TAMAL)
    private static DatosNivel nivel4() {
        DatosNivel d = new DatosNivel(3, "OCOYOACAC", "musica_nivel_4", false, 3800,
                "canasta_hojas.png", "olla_tamalera.png", "troncos.png");
        d.medioDesplazamiento = 310;      // coloca el lugar representativo junto a la puerta
        int[] xs = {400, 850, 1300, 1750, 2200, 2650, 3050};
        int[] ty = {0, 2, 1, 0, 2, 1, 0};
        int[] ancho = {40, 36, 44, 40, 36, 44, 40};
        for (int i = 0; i < xs.length; i++) {
            d.obstaculo(xs[i], ty[i]);
            if (ty[i] == 1) d.arco(xs[i] + ancho[i] / 2, 126, 3, 26, 24);    // la olla es muy alta: arco mas arriba
            else d.arco(xs[i] + ancho[i] / 2, 168, 3, 26, 32);
        }
        d.plataforma(1500, 160, 4).fila(1514, 134, 4, 22);
        d.plataforma(1560, 112, 3).fila(1572, 86, 3, 22);
        d.fila(3250, 186, 2, 26);
        d.plataforma(3400, 150, 3).llave(3416, 118);
        d.enemigo(TAMAL, 700, 640, 760);
        d.enemigo(TAMAL, 1100, 1040, 1160);
        d.enemigo(TAMAL, 1950, 1890, 2010);
        d.enemigo(TAMAL, 2450, 2390, 2510);
        return d;
    }

    // ------------------------------------------------------------------ NIVEL 5: Metepec (llave JARRITO) - Dia de Muertos
    private static DatosNivel nivel5() {
        DatosNivel d = new DatosNivel(4, "METEPEC", "musica_nivel_5", true, 4200,
                "cantaros.png", "macizo_cempasuchil.png", "ofrenda_calaveras.png");
        d.medioDesplazamiento = 153;      // coloca el lugar representativo junto a la puerta
        int[] xs = {400, 800, 1200, 1650, 2100, 2550, 3000, 3400};
        int[] ty = {1, 0, 2, 1, 0, 2, 1, 0};
        int[] ancho = {42, 36, 40, 42, 36, 40, 42, 36};
        for (int i = 0; i < xs.length; i++) {
            d.obstaculo(xs[i], ty[i]);
            d.arco(xs[i] + ancho[i] / 2, 168, 3, 26, 32);
        }
        d.plataforma(1400, 160, 3).fila(1412, 134, 3, 22);
        d.plataforma(2300, 160, 3).fila(2312, 134, 3, 22);
        d.plataforma(3700, 150, 3).llave(3716, 118);
        d.enemigo(ALEBRIJE, 600, 450, 750);
        d.enemigo(CALACA, 1000, 940, 1060);
        d.enemigo(CALACA, 1900, 1840, 1960);
        d.enemigo(ALEBRIJE, 2800, 2650, 2950);
        d.enemigo(CALACA, 3200, 3140, 3260);
        return d;
    }
}
