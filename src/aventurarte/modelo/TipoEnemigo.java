package aventurarte.modelo;

/** Los 6 enemigos del juego. "rapido" = corre hacia ti (sin proyectil); los demas LANZAN algo. */
public enum TipoEnemigo {
    OSITO("osito", false),       // nivel 1  - lanza bloques de juguete
    TENIS("tenis", true),        // nivel 2  - corredor
    BORREGO("borrego", false),   // nivel 3  - lanza ovillos de lana
    TAMAL("tamal", false),       // nivel 4  - lanza chiles
    ALEBRIJE("alebrije", true),  // nivel 5  - corredor
    CALACA("calaca", false);     // nivel 5  - lanza huesos

    public final String id;
    public final boolean rapido;

    TipoEnemigo(String id, boolean rapido) { this.id = id; this.rapido = rapido; }
}
