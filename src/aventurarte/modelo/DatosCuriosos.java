package aventurarte.modelo;

/**
 * Los "datos curiosos" que salen en una cajita de dialogo antes de empezar cada pueblo (nivel).
 * Para cambiar un texto, solo edita la frase de abajo: la cajita acomoda las lineas solita.
 * Consejo: no pases de ~170 letras (3 renglones). La fuente del juego esta en MAYUSCULAS, pero aqui escribe normal.
 */
public final class DatosCuriosos {
    private DatosCuriosos() { }

    // un texto por nivel (0 = Xonacatlan ... 4 = Metepec)
    private static final String[] TEXTOS = {
            "\u00BFSab\u00EDas que el 28 de abril de 2019 Xonacatl\u00E1n logr\u00F3 el R\u00E9cord Guinness con \"Xonita\", la osa de peluche m\u00E1s grande del mundo? \u00A1Mide m\u00E1s de 20 metros y pesa unas 4 toneladas!",
            "\u00BFSab\u00EDas que San Mateo Atenco es \"La tierra del calzado\"? \u00A1En sus Plazas Naranja y Azul se vende calzado directo de f\u00E1brica, y en la Azul hay comida tradicional!",
            "\u00BFSab\u00EDas que Gualupita es la \"Tierra del Sarape\"? \u00A1Sus artesanos tejen sarapes, su\u00E9teres, gorros y bufandas de lana en telares!",
            "\u00BFSab\u00EDas que Ocoyoacac significa \"en la nariz de los ocotes\"? \u00A1Es famoso por su Feria del Tamal de Ollita, un tamal con un huequito relleno de salsa y carne!",
            "\u00BFSab\u00EDas que Metepec es Pueblo M\u00E1gico desde 2012? \u00A1Su \u00C1rbol de la Vida de barro tiene siete ramas, como los siete d\u00EDas de la creaci\u00F3n!"
    };

    public static String de(int nivel) { return TEXTOS[Math.max(0, Math.min(TEXTOS.length - 1, nivel))]; }
}
