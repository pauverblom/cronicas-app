package io.github.pauverblom.cronicas.rules.data

import io.github.pauverblom.cronicas.rules.Atributo
import io.github.pauverblom.cronicas.rules.Atributo.CAR
import io.github.pauverblom.cronicas.rules.Atributo.CON
import io.github.pauverblom.cronicas.rules.Atributo.DES
import io.github.pauverblom.cronicas.rules.Atributo.FUE
import io.github.pauverblom.cronicas.rules.Atributo.INT

/** Especie (reglamento, cap. 3). [pequeno] = tamaño P (6 m de movimiento, 1d2 sin armas). */
data class EspecieDef(
    val id: String,
    val nombre: String,
    val mods: Map<Atributo, Int>,
    val descripcion: String = "",
    val pequeno: Boolean,
    val idiomas: String,
    val rasgos: List<HabilidadDef>,
    /** Número de características primarias además de la de la clase. */
    val primariasExtra: Int = 1
) {
    val movimiento: Int get() = if (pequeno) 6 else 10
}

private val RASGOS_ELFO_HABILIDADES = HabilidadDef(
    "Habilidades élficas",
    "Explorador: +2 a moverse en silencio y trampas. Ladrón/asesino: +2 a escuchar, moverse en silencio y trampas."
)

object Especies {

    val elfo = EspecieDef(
        id = "elfo", nombre = "Elfo", mods = mapOf(DES to 1, CON to -1), pequeno = false, descripcion = "Criaturas ligadas a la naturaleza, de inteligencia inquisitiva e innegable belleza. Su aparente fragilidad engaña: son duros como robles y muy ágiles.",
        idiomas = "Élfico, común, enano, orco, goblin, mediano, dracónico",
        rasgos = listOf(
            HabilidadDef("Visión en la penumbra", "Ve con poca luz como un humano a plena luz."),
            HabilidadDef("Detectar puertas secretas", "Encuentra puertas y pasadizos ocultos.", atributo = Atributo.SAB),
            HabilidadDef("Entrenamiento élfico", "+1 al ataque con un tipo de arco o con espada larga o corta."),
            HabilidadDef("Resistencia élfica", "+10 a las TS contra hechizos de encantamiento y sueño."),
            HabilidadDef("Oído fino", "+2 a escuchar."),
            RASGOS_ELFO_HABILIDADES
        )
    )

    val elfoOscuro = EspecieDef(
        id = "elfo_oscuro", nombre = "Elfo oscuro", mods = mapOf(DES to 1, CON to -1), pequeno = false, descripcion = "Elfos de la sombra, taciturnos, atléticos y agraciados, de cabellos blanco níveo o gris perla. Pueden vivir más de mil años.",
        idiomas = "Élfico, común, enano, orco, goblin, mediano, dracónico",
        rasgos = listOf(
            HabilidadDef("Infravisión", "Ve en la oscuridad hasta 30 m."),
            HabilidadDef("Resistencia mágica", "+3 a las TS contra magia y RC 3."),
            HabilidadDef("Inmunidad al veneno", "Inmune a los venenos naturales."),
            RASGOS_ELFO_HABILIDADES
        )
    )

    val enano = EspecieDef(
        id = "enano", nombre = "Enano", mods = mapOf(DES to -1, CON to 1), pequeno = true, descripcion = "Vigorosos, fuertes y de cuerpo achaparrado, de carácter vehemente y gran sentido del honor. Moran en galerías bajo las montañas.",
        idiomas = "Enano, común, gnomo, orco, goblin, mediano, ogro, gigante, trol",
        rasgos = listOf(
            HabilidadDef("Infravisión", "Ve en la oscuridad hasta 35 m."),
            HabilidadDef("Conocimiento de la piedra", "Detecta trampas, pendientes y obras en la piedra."),
            HabilidadDef("Determinación", "+2 a las TS contra miedo y contra veneno; +3 contra conjuros arcanos."),
            HabilidadDef("Enemigos ancestrales", "+4 a la CA contra gigantes y ogros; +1 al ataque contra goblins y orcos."),
            HabilidadDef("Trampero subterráneo", "Ladrón: +2 a trampas en interiores.")
        )
    )

    val gnomo = EspecieDef(
        id = "gnomo", nombre = "Gnomo", mods = mapOf(INT to 1, FUE to -1), pequeno = true, descripcion = "Parientes lejanos de los enanos, más bajos que ellos. Curiosos e ingeniosos, con un don natural para la magia.",
        idiomas = "Gnomo, común, enano, élfico, mediano, kobold, goblin",
        rasgos = listOf(
            HabilidadDef("Infravisión", "Ve en la oscuridad hasta 18 m."),
            HabilidadDef("Magia innata", "Una vez al día cada uno: prestidigitación, luces danzantes y sonido fantasma."),
            HabilidadDef("Enemigos ancestrales", "+1 al ataque contra goblins y kobolds."),
            HabilidadDef("Oído fino", "Ladrón: +3 a escuchar.")
        )
    )

    val mediano = EspecieDef(
        id = "mediano", nombre = "Mediano", mods = mapOf(DES to 1, FUE to -1), pequeno = true, descripcion = "También llamados halflings: robustos, de escaso tamaño, apacibles y hogareños, pero observadores y sorprendentemente sigilosos.",
        idiomas = "Mediano, común, enano, gnomo, élfico, goblin",
        rasgos = listOf(
            HabilidadDef("Valentía", "+2 a las TS contra miedo."),
            HabilidadDef("Robustez", "+1 a las TS de CON."),
            HabilidadDef("Sigilo", "Explorador: +2 a camuflaje y moverse en silencio. Ladrón: +2 a moverse en silencio y esconderse.")
        )
    )

    val humano = EspecieDef(
        id = "humano", nombre = "Humano", mods = emptyMap(), pequeno = false, descripcion = "La especie más versátil y extendida, presente en todos los entornos, con culturas y folclores propios muy variados.",
        idiomas = "Común y una lengua regional",
        rasgos = listOf(HabilidadDef("Versatilidad", "Elige dos características primarias además de la de su clase.")),
        primariasExtra = 2
    )

    val semielfo = EspecieDef(
        id = "semielfo", nombre = "Semielfo", mods = mapOf(DES to 1, CON to -1), pequeno = false, descripcion = "Hijos de humanos y elfos. Heredan la gracia élfica y la versatilidad humana, y se mueven con soltura en ambos mundos.",
        idiomas = "Común, élfico, mediano, enano y una lengua regional",
        rasgos = listOf(
            HabilidadDef("Sangre élfica", "+4 a las TS contra encantamiento y sueño."),
            HabilidadDef("Carisma", "+2 a las tiradas sociales de CAR.", atributo = CAR),
            HabilidadDef("Oído fino", "+2 a escuchar."),
            RASGOS_ELFO_HABILIDADES
        )
    )

    val semiorco = EspecieDef(
        id = "semiorco", nombre = "Semiorco", mods = mapOf(FUE to 1, CON to 1, CAR to -2), pequeno = false, descripcion = "Fuertes y resistentes, de olfato muy aguzado. Viven a caballo entre humanos y orcos, a menudo mirados con recelo.",
        idiomas = "Común, orco, goblin y una lengua regional",
        rasgos = listOf(
            HabilidadDef("Infravisión", "Ve en la oscuridad hasta 18 m."),
            HabilidadDef("Piel dura", "+1 a la CA sin armadura."),
            HabilidadDef("Resistencia", "+2 a las TS contra enfermedades."),
            HabilidadDef("Olfato agudo", "Rastrea y detecta por el olor. Explorador: +2 a rastrear.")
        )
    )

    val todas: List<EspecieDef> = listOf(elfo, elfoOscuro, enano, gnomo, mediano, humano, semielfo, semiorco)

    fun porId(id: String?): EspecieDef? = todas.firstOrNull { it.id == id }
}

object Alineamientos {
    val todos = listOf(
        "Legal bueno", "Legal neutral", "Legal malvado",
        "Neutral bueno", "Neutral", "Neutral malvado",
        "Caótico bueno", "Caótico neutral", "Caótico malvado"
    )

    val deidades = listOf(
        "Valion y Aneirin", "Penumbra", "Silas el Indiferente", "Velex", "Taranis", "Orión",
        "Nébula", "Legis", "Isthar", "Soltar", "Zamordax", "Marvol Moru"
    )

    /** Aviso si el alineamiento no encaja con la clase, o null. */
    fun aviso(clase: ClaseDef?, alineamiento: String): String? {
        if (clase == null || alineamiento.isBlank()) return null
        return when (clase.id) {
            "asesino" -> "El asesino no puede ser bueno".takeIf { alineamiento.endsWith("bueno") }
            "druida" -> "El druida debe ser neutral".takeIf { "neutral" !in alineamiento.lowercase() }
            "paladin" -> "El paladín debe ser legal bueno".takeIf { alineamiento != "Legal bueno" }
            else -> null
        }
    }
}
