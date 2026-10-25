package io.github.pauverblom.cronicas.rules.data

import io.github.pauverblom.cronicas.rules.Atributo
import io.github.pauverblom.cronicas.rules.Atributo.CAR
import io.github.pauverblom.cronicas.rules.Atributo.CON
import io.github.pauverblom.cronicas.rules.Atributo.DES
import io.github.pauverblom.cronicas.rules.Atributo.FUE
import io.github.pauverblom.cronicas.rules.Atributo.INT
import io.github.pauverblom.cronicas.rules.Atributo.SAB

/** Habilidad de clase o rasgo de especie. [atributo] es la característica con la que se tira, si la hay. */
data class HabilidadDef(
    val nombre: String,
    val descripcion: String,
    val nivel: Int = 1,
    val atributo: Atributo? = null
)

enum class TablaConjuros { DIVINA, ARCANA }

/**
 * Clase de aventurero (reglamento, cap. 2).
 * [xp] es el mínimo de PX para cada nivel 1..12; a partir del 13 se suman [xpTras12] por nivel.
 * [ba] es el bonificador de ataque para los niveles 1..12 (no sube después).
 * [armas]/[armaduras] son ids de catálogo permitidos; null significa "cualquiera".
 */
data class ClaseDef(
    val id: String,
    val nombre: String,
    val principal: Atributo,
    val descripcion: String = "",
    val dado: Int,
    val pgFijos: Int,
    val alineamiento: String,
    val xp: List<Int>,
    val xpTras12: Int,
    val ba: List<Int>,
    val armas: Set<String>? = null,
    val armasProhibidas: Set<String> = emptySet(),
    val armaduras: Set<String>? = null,
    val escudos: Set<String>? = null,
    val oroInicial: String,
    val habilidades: List<HabilidadDef> = emptyList(),
    val tablaConjuros: TablaConjuros? = null,
    /** Lista de conjuros del reglamento que usa: "clerigo", "druida", "ilusionista" o "mago". */
    val listaConjuros: String? = null,
    /** Característica que da conjuros adicionales. */
    val atributoConjuros: Atributo? = null
) {
    fun baEnNivel(nivel: Int): Int = ba[nivel.coerceIn(1, 12) - 1]

    /** PX mínimos para alcanzar [nivel]. */
    fun xpParaNivel(nivel: Int): Int = when {
        nivel <= 1 -> 0
        nivel <= 12 -> xp[nivel - 1]
        else -> xp[11] + (nivel - 12) * xpTras12
    }
}

private val CUEROS = setOf("cuero_acolchado", "cuero_endurecido", "cuero_tachonado")
private val ESCUDOS_MADERA = setOf("escudo_pequeno_madera", "escudo_medio_madera", "escudo_pesado_madera")
private val NINGUNO = emptySet<String>()

private val BA_COMBATIENTE = (0..11).toList()
private val BA_PICARO = listOf(0, 1, 1, 1, 2, 2, 2, 3, 3, 3, 4, 4)
private val BA_SACERDOTE = listOf(0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6)
private val BA_ARCANO = listOf(0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3)

private fun xp(vararg n: Int) = listOf(0) + n.toList()

object Clases {

    val asesino = ClaseDef(
        id = "asesino", nombre = "Asesino", descripcion = "Maestros del disfraz y la infiltración, especializados en asesinatos por encargo. Suelen moverse por grandes ciudades, envueltos en intrigas políticas y comerciales.", principal = DES, dado = 6, pgFijos = 2,
        alineamiento = "Cualquiera no bueno",
        xp = xp(1751, 3501, 7001, 14001, 25001, 50001, 90001, 150001, 200001, 350001, 500001), xpTras12 = 150000,
        ba = BA_PICARO, armaduras = CUEROS, oroInicial = "3d4 × 10 mo",
        habilidades = listOf(
            HabilidadDef("Ataque furtivo", "Contra un objetivo que no espera el ataque: +2 al ataque y +4 al daño. A distancia, solo a menos de 10 m."),
            HabilidadDef("Ataque letal", "Tras estudiar a la víctima 3 asaltos, un ataque furtivo con éxito la mata si falla una TS de CON."),
            HabilidadDef("Detectar punto débil", "Espiando 1d3 × 10 min averigua DG, alineamiento, armas ocultas y objetos inusuales del objetivo.", atributo = SAB),
            HabilidadDef("Disfraz", "Hacerse pasar por otra persona. Penalizadores según sexo, especie, edad y cercanía del observador.", atributo = CAR),
            HabilidadDef("Envenenar", "Identificar, fabricar y usar venenos. +1 a las TS contra venenos desde el nivel 3.", atributo = INT),
            HabilidadDef("Escalar", "Trepar paredes y techos a mitad de movimiento. Fallar por 5 o más: caída.", atributo = DES),
            HabilidadDef("Esconderse", "Pasar inadvertido; puede moverse a mitad de movimiento sin ser visto.", atributo = DES),
            HabilidadDef("Escuchar", "Oír a través de puertas y detectar ruidos lejanos.", atributo = SAB),
            HabilidadDef("Montar y desmontar trampas", "Encontrar, desactivar y preparar trampas.", atributo = INT),
            HabilidadDef("Moverse en silencio", "Desplazarse sin hacer ruido a movimiento normal.", atributo = DES)
        )
    )

    val barbaro = ClaseDef(
        id = "barbaro", nombre = "Bárbaro", descripcion = "Guerreros poderosos de voluntad indómita, tenaces y arrojados en combate. Vienen de tierras poco civilizadas y tribus gobernadas por caudillos.", principal = CON, dado = 12, pgFijos = 5, alineamiento = "Cualquiera",
        xp = xp(2101, 4701, 9401, 20001, 40001, 80001, 170001, 340001, 600001, 800001, 1000001), xpTras12 = 200000,
        ba = BA_COMBATIENTE, oroInicial = "2d4 × 10 mo",
        habilidades = listOf(
            HabilidadDef("Alerta", "Difícil de sorprender."),
            HabilidadDef("Instintos primitivos", "+4 a las tiradas para percibir peligros y emboscadas en entornos salvajes."),
            HabilidadDef("Intimidar", "Amedrentar a otros con su presencia.", atributo = CON),
            HabilidadDef("Rastreador nato", "Seguir rastros en terreno salvaje."),
            HabilidadDef("Furia desatada", "Entra en furia en combate: más daño y aguante. Afecta a 3 objetivos desde el nivel 6 y a 4 desde el 10.", nivel = 4),
            HabilidadDef("Aguante", "Resiste el cansancio y el dolor más allá de lo normal.", nivel = 6),
            HabilidadDef("Llamada ancestral", "Invoca la ayuda de los espíritus de sus ancestros.", nivel = 10)
        )
    )

    val bardo = ClaseDef(
        id = "bardo", nombre = "Bardo", descripcion = "Se abren camino con encanto, talento e ingenio. Gente de mundo que aprende habilidades muy variadas y anima a sus compañeros con su arte.", principal = CAR, dado = 10, pgFijos = 4, alineamiento = "Cualquiera",
        xp = xp(1501, 3251, 7501, 15001, 30001, 60001, 120001, 240001, 450001, 625001, 800001), xpTras12 = 175000,
        ba = BA_COMBATIENTE,
        armas = setOf(
            "arco_corto", "arco_corto_compuesto", "arco_largo", "arco_largo_compuesto", "baston", "cachiporra",
            "cerbatana", "cimitarra", "daga", "daga_plata", "dardo", "espada_corta", "espada_larga", "hacha_mano",
            "jabalina", "lanza", "martillo_guerra", "martillo_ligero"
        ),
        armaduras = CUEROS + setOf("anillas", "camisote_mallas", "cota_escamas", "laminada", "peto_coraza"),
        oroInicial = "3d4 × 10 mo",
        habilidades = listOf(
            HabilidadDef("Conocimiento", "Saber sobre leyendas, historia y objetos notables.", atributo = CAR),
            HabilidadDef("Exaltar", "Con su música otorga +2 a los aliados (+3 al nivel 6, +4 al 12, +5 al 18).", atributo = CAR),
            HabilidadDef("Leer lenguajes", "Descifrar escritos en lenguas desconocidas.", atributo = INT),
            HabilidadDef("Fascinar", "Cautiva a quienes escuchan su actuación.", nivel = 4),
            HabilidadDef("Infundir valor", "Inspira a los aliados contra el miedo.", nivel = 9)
        )
    )

    val caballero = ClaseDef(
        id = "caballero", nombre = "Caballero", descripcion = "Famosos por su pericia marcial, su honor y su disciplina. Soldados cultivados que se deben a su señor y a un estricto código.", principal = CAR, dado = 10, pgFijos = 4, alineamiento = "Cualquiera",
        xp = xp(2251, 4501, 9001, 18001, 36001, 72001, 150001, 300001, 600001, 725001, 900001), xpTras12 = 175000,
        ba = BA_COMBATIENTE, oroInicial = "6d4 × 10 mo (y un caballo de monta)",
        habilidades = listOf(
            HabilidadDef("Inspirar coraje", "Sus aliados ganan valor ante el peligro."),
            HabilidadDef("Montura", "Empieza con un caballo de monta (2d8+2 PG)."),
            HabilidadDef("Jinete experto", "Maniobras y combate a caballo.", atributo = DES),
            HabilidadDef("Arrojo", "Se lanza al combate sin vacilar.", nivel = 3),
            HabilidadDef("Desmoralizar", "Merma la moral de los enemigos.", nivel = 5),
            HabilidadDef("Dominio del campo de batalla", "Coordina a sus aliados en combate.", nivel = 8),
            HabilidadDef("Llamada a las armas", "Reúne seguidores bajo su estandarte.", nivel = 10)
        )
    )

    val clerigo = ClaseDef(
        id = "clerigo", nombre = "Clérigo", descripcion = "Han consagrado su vida a servir a una deidad y defender su doctrina, con los conjuros que les otorga su dios y con las armas.", principal = SAB, dado = 8, pgFijos = 3, alineamiento = "Cualquiera",
        xp = xp(2251, 5001, 9001, 18001, 35001, 70001, 140001, 300001, 425001, 650001, 900001), xpTras12 = 250000,
        ba = BA_SACERDOTE,
        armas = setOf("baston", "cachiporra", "garrote", "honda", "lucero_alba", "martillo_guerra", "martillo_ligero", "maza"),
        oroInicial = "2d10 × 10 mo",
        habilidades = listOf(
            HabilidadDef(
                "Expulsar muertos vivientes",
                "TS de SAB contra ND 12 + DG del muerto viviente. Alcance 20 m; afecta a 1d12 + mod. CAR criaturas. " +
                    "Nivel ≥ DG+5: los destruye; nivel ≥ DG+10: destruye el máximo. Los clérigos malvados los controlan.",
                atributo = SAB
            ),
            HabilidadDef("Conjuros divinos", "Lanza conjuros de clérigo; los prepara tras descansar y rezar.")
        ),
        tablaConjuros = TablaConjuros.DIVINA, listaConjuros = "clerigo", atributoConjuros = SAB
    )

    val druida = ClaseDef(
        id = "druida", nombre = "Druida", descripcion = "Devotos de la naturaleza, sabios y ponderados, que luchan por el equilibrio de todas las cosas y protegen la tierra y el mar.", principal = SAB, dado = 8, pgFijos = 3, alineamiento = "Cualquiera neutral",
        xp = xp(2001, 4251, 8501, 17001, 35001, 70001, 180001, 275001, 400001, 525001, 650001), xpTras12 = 175000,
        ba = BA_SACERDOTE,
        armas = setOf(
            "arco_corto", "arco_corto_compuesto", "arco_largo", "arco_largo_compuesto", "baston", "cimitarra", "daga",
            "daga_plata", "espada_corta", "espada_larga", "garrote", "guadana", "hacha_mano", "honda", "hoz",
            "martillo_guerra", "martillo_ligero", "maza", "lanza"
        ),
        armaduras = CUEROS, escudos = ESCUDOS_MADERA, oroInicial = "2d10 × 10 mo",
        habilidades = listOf(
            HabilidadDef("Conocimiento de la naturaleza", "Identificar plantas, animales y agua potable.", atributo = SAB),
            HabilidadDef("Lenguaje druídico", "Lengua secreta de los druidas."),
            HabilidadDef("Conjuros divinos", "Lanza conjuros de druida."),
            HabilidadDef("Resistencia a los elementos", "+2 a las TS contra fuego, frío y electricidad.", nivel = 2),
            HabilidadDef("Zancada de la floresta", "Atraviesa la maleza sin dejar rastro ni perder velocidad.", nivel = 3),
            HabilidadDef("Forma salvaje", "Adopta la forma de un animal.", nivel = 6)
        ),
        tablaConjuros = TablaConjuros.DIVINA, listaConjuros = "druida", atributoConjuros = SAB
    )

    val explorador = ClaseDef(
        id = "explorador", nombre = "Explorador", descripcion = "Rastreadores y supervivientes de las tierras salvajes. Se mimetizan con el entorno y conocen como nadie los caminos y sus peligros.", principal = FUE, dado = 10, pgFijos = 4, alineamiento = "Cualquiera",
        xp = xp(2251, 4501, 9001, 18001, 40001, 75001, 150001, 250001, 500001, 725001, 950001), xpTras12 = 225000,
        ba = BA_COMBATIENTE,
        armaduras = CUEROS + setOf("anillas", "armadura_varillas", "cota_escamas", "cota_mallas", "peto_coraza"),
        oroInicial = "3d8 × 10 mo",
        habilidades = listOf(
            HabilidadDef("Camuflaje", "Ocultarse en entornos naturales.", atributo = DES),
            HabilidadDef("Escalar", "Trepar superficies naturales.", atributo = DES),
            HabilidadDef("Maestría en combate", "+1 al daño por nivel contra humanoides y gigantes."),
            HabilidadDef("Moverse en silencio", "Desplazarse sin hacer ruido.", atributo = DES),
            HabilidadDef("Neutralizar venenos", "Tratar envenenamientos con remedios naturales.", atributo = SAB),
            HabilidadDef("Rastrear", "Seguir huellas y rastros.", atributo = SAB),
            HabilidadDef("Supervivencia", "Encontrar comida, agua y refugio.", atributo = SAB),
            HabilidadDef("Trampero", "Preparar y detectar trampas en la naturaleza.", atributo = SAB),
            HabilidadDef("Enemigo usual", "Elige un tipo de enemigo: +2 al ataque, a la CA y a rastrearlo.", nivel = 6)
        )
    )

    val guerrero = ClaseDef(
        id = "guerrero", nombre = "Guerrero", descripcion = "Luchadores experimentados que consagran su vida al arte de la guerra. Dominan armas y armaduras de todo tipo.", principal = FUE, dado = 10, pgFijos = 4, alineamiento = "Cualquiera",
        xp = xp(2001, 4001, 8501, 17001, 34001, 68001, 136001, 272001, 500001, 750001, 1000001), xpTras12 = 250000,
        ba = (1..12).toList(), oroInicial = "3d8 × 10 mo",
        habilidades = listOf(
            HabilidadDef("Especialización en armas", "Con el arma elegida: +1 al ataque y al daño (+2 desde el nivel 7)."),
            HabilidadDef("Maestría en combate", "Un ataque extra contra enemigos de 1 DG o menos; otro más cada 4 niveles.", nivel = 4),
            HabilidadDef("Ataque extra", "Un ataque adicional por asalto.", nivel = 10)
        )
    )

    val ilusionista = ClaseDef(
        id = "ilusionista", nombre = "Ilusionista", descripcion = "Usan la magia para alterar lo que los demás perciben: imágenes y sonidos falsos, engaños de la mente e incluso cambios en la realidad.", principal = INT, dado = 4, pgFijos = 1, alineamiento = "Cualquiera",
        xp = xp(2601, 5201, 10401, 20801, 42501, 85001, 170001, 340001, 500001, 750001, 900001), xpTras12 = 150000,
        ba = BA_ARCANO, armas = setOf("baston", "daga", "daga_plata", "dardo", "garrote"), armaduras = NINGUNO, escudos = NINGUNO,
        oroInicial = "1d10 × 10 mo",
        habilidades = listOf(
            HabilidadDef("Sentidos agudizados", "+1 a las TS contra ilusiones, aumentando con el nivel hasta +6."),
            HabilidadDef("Disfraz", "Hacerse pasar por otra persona.", atributo = CAR),
            HabilidadDef("Conjuros arcanos", "Lanza conjuros de ilusionista desde su libro de conjuros.")
        ),
        tablaConjuros = TablaConjuros.ARCANA, listaConjuros = "ilusionista", atributoConjuros = INT
    )

    val ladron = ClaseDef(
        id = "ladron", nombre = "Ladrón", descripcion = "Buscavidas hábiles y astutos de las duras calles de la ciudad: trapichean, roban, engañan y reúnen información para sobrevivir.", principal = DES, dado = 6, pgFijos = 2, alineamiento = "Cualquiera",
        xp = xp(1251, 2501, 6001, 12001, 24001, 48001, 80001, 120001, 175001, 325001, 450001), xpTras12 = 125000,
        ba = BA_PICARO, armaduras = CUEROS, oroInicial = "3d4 × 10 mo",
        habilidades = listOf(
            HabilidadDef("Abrir cerraduras", "Forzar cerraduras con herramientas de ladrón.", atributo = DES),
            HabilidadDef("Ataque por la espalda", "+4 al ataque y daño ×2 (×3 al nivel 5, ×4 al nivel 9)."),
            HabilidadDef("Descifrar escritura", "Entender textos en clave o lenguas extrañas.", atributo = INT),
            HabilidadDef("Escalar muros", "Trepar paredes a mitad de movimiento.", atributo = DES),
            HabilidadDef("Esconderse", "Pasar inadvertido entre las sombras.", atributo = DES),
            HabilidadDef("Escuchar", "Oír a través de puertas y detectar ruidos.", atributo = SAB),
            HabilidadDef("Germanía", "Jerga secreta de los ladrones."),
            HabilidadDef("Hurtar", "Robar bolsas y objetos sin ser visto.", atributo = DES),
            HabilidadDef("Montar y desmontar trampas", "Encontrar, desactivar y preparar trampas.", atributo = INT),
            HabilidadDef("Moverse en silencio", "Desplazarse sin hacer ruido.", atributo = DES),
            HabilidadDef("Ataque furtivo", "Contra un objetivo desprevenido: +2 al ataque y +4 al daño.", nivel = 4)
        )
    )

    val mago = ClaseDef(
        id = "mago", nombre = "Mago", descripcion = "Estudiosos de lo arcano que aprenden sus conjuros de los libros. Frágiles al principio, terriblemente poderosos con el tiempo.", principal = INT, dado = 4, pgFijos = 1, alineamiento = "Cualquiera",
        xp = xp(2601, 5201, 10401, 20801, 42501, 85001, 170001, 340001, 500001, 750001, 1000001), xpTras12 = 250000,
        ba = BA_ARCANO, armas = setOf("baston", "daga", "daga_plata", "dardo", "garrote"), armaduras = NINGUNO, escudos = NINGUNO,
        oroInicial = "1d10 × 10 mo",
        habilidades = listOf(
            HabilidadDef("Conjuros arcanos", "Lanza conjuros de mago desde su libro de conjuros.")
        ),
        tablaConjuros = TablaConjuros.ARCANA, listaConjuros = "mago", atributoConjuros = INT
    )

    val mistico = ClaseDef(
        id = "mistico", nombre = "Místico", descripcion = "Disciplinados y frugales, buscan la perfección del cuerpo y del espíritu y convierten su cuerpo en un arma letal con las artes marciales.", principal = CON, dado = 12, pgFijos = 5, alineamiento = "Cualquiera",
        xp = xp(1751, 4001, 8501, 20001, 40001, 80001, 160001, 325001, 550001, 750001, 1250001), xpTras12 = 250000,
        ba = BA_COMBATIENTE,
        armasProhibidas = setOf(
            "arco_corto", "arco_corto_compuesto", "arco_largo", "arco_largo_compuesto", "ballesta_ligera", "ballesta_mano",
            "ballesta_pesada", "ballesta_repeticion", "espada_bastarda", "espada_dos_manos", "hacha_batalla",
            "lanza_caballeria", "martillo_guerra", "tridente", "pica"
        ),
        armaduras = NINGUNO, escudos = NINGUNO, oroInicial = "2d4 × 10 mo",
        habilidades = listOf(
            HabilidadDef("Ataque aturdidor", "Un golpe sin armas puede aturdir al objetivo."),
            HabilidadDef("Caída ralentizada", "Reduce el daño por caída si hay una pared cerca."),
            HabilidadDef("Curación rápida", "Recupera puntos de golpe más deprisa."),
            HabilidadDef("Enlentecimiento vital", "Puede ralentizar sus funciones vitales y parecer muerto."),
            HabilidadDef("Esquivar proyectiles", "Desvía proyectiles dirigidos contra él.", atributo = DES),
            HabilidadDef("Impacto letal", "Golpe capaz de matar en el acto."),
            HabilidadDef("Mente preclara", "Resistencia a efectos que afectan a la mente."),
            HabilidadDef("Puño de hierro", "Sus golpes cuentan como arma mágica +1 desde el nivel 3, hasta +5 al nivel 17."),
            HabilidadDef("Resistencia superior", "Bonificadores a las TS.")
        )
    )

    val paladin = ClaseDef(
        id = "paladin", nombre = "Paladín", descripcion = "Guerreros con una fe capaz de mover montañas. Defienden los principios de su dios bajo un estricto código moral.", principal = CAR, dado = 10, pgFijos = 4, alineamiento = "Legal bueno",
        xp = xp(2701, 5501, 12001, 24001, 48001, 95001, 180001, 360001, 700001, 1000001, 1300001), xpTras12 = 300000,
        ba = BA_COMBATIENTE, oroInicial = "6d4 × 10 mo",
        habilidades = listOf(
            HabilidadDef("Aura divina", "Irradia una presencia sagrada."),
            HabilidadDef("Curar enfermedad", "Cura enfermedades por imposición de manos."),
            HabilidadDef("Detectar el mal", "Percibe la presencia del mal."),
            HabilidadDef("Resistencia divina", "Bonificadores a las TS."),
            HabilidadDef("Sanar con las manos", "Cura 2 PG por nivel una vez al día."),
            HabilidadDef("Expulsar muertos vivientes", "Como un clérigo de dos niveles menos.", nivel = 3, atributo = SAB),
            HabilidadDef("Montura", "Obtiene una montura especial.", nivel = 4),
            HabilidadDef("Aura de coraje", "Los aliados cercanos son inmunes al miedo.", nivel = 6),
            HabilidadDef("Castigar el mal", "Golpe sagrado contra criaturas malvadas.", nivel = 9),
            HabilidadDef("Curación divina", "Poder de curación mayor.", nivel = 12)
        )
    )

    val todas: List<ClaseDef> = listOf(
        asesino, barbaro, bardo, caballero, clerigo, druida, explorador, guerrero, ilusionista, ladron, mago, mistico, paladin
    )

    fun porId(id: String?): ClaseDef? = todas.firstOrNull { it.id == id }

    /** Espacios de conjuro por nivel de lanzador (1..20) y nivel de conjuro (0..9). */
    val espaciosDivinos: List<List<Int>> = listOf(
        listOf(3, 1), listOf(4, 2), listOf(4, 2, 1), listOf(4, 3, 2), listOf(4, 3, 2, 1),
        listOf(5, 3, 3, 2), listOf(5, 4, 3, 2, 1), listOf(5, 4, 3, 3, 2), listOf(5, 4, 4, 3, 2, 1), listOf(5, 4, 4, 3, 3, 2),
        listOf(6, 5, 4, 4, 3, 2, 1), listOf(6, 5, 4, 4, 3, 3, 2), listOf(6, 5, 5, 4, 4, 3, 2, 1), listOf(6, 5, 5, 4, 4, 3, 3, 2),
        listOf(6, 5, 5, 5, 4, 4, 3, 2, 1), listOf(6, 6, 5, 5, 4, 4, 3, 3, 2), listOf(7, 6, 5, 5, 5, 4, 4, 3, 2, 1),
        listOf(7, 6, 6, 5, 5, 4, 4, 3, 3, 2), listOf(7, 6, 6, 5, 5, 5, 4, 4, 3, 2), listOf(7, 6, 6, 6, 5, 5, 4, 4, 3, 3)
    )

    val espaciosArcanos: List<List<Int>> = listOf(
        listOf(4, 2), listOf(4, 3), listOf(4, 3, 1), listOf(4, 3, 2), listOf(5, 4, 2, 1),
        listOf(5, 4, 3, 2), listOf(5, 4, 3, 2, 1), listOf(5, 4, 3, 3, 2), listOf(5, 5, 4, 3, 2, 1), listOf(6, 5, 4, 3, 3, 2),
        listOf(6, 5, 4, 4, 3, 2, 1), listOf(6, 5, 4, 4, 3, 3, 2), listOf(6, 5, 5, 4, 4, 3, 2, 1), listOf(6, 6, 5, 4, 4, 3, 3, 2),
        listOf(6, 6, 5, 5, 4, 4, 3, 2, 1), listOf(7, 6, 5, 5, 4, 4, 3, 3, 2), listOf(7, 6, 5, 5, 5, 4, 4, 3, 2, 1),
        listOf(7, 6, 6, 5, 5, 4, 4, 3, 3, 2), listOf(7, 6, 6, 5, 5, 5, 4, 4, 3, 2), listOf(7, 7, 6, 6, 5, 5, 4, 4, 3, 3)
    )

    /** Tabla del místico (p. 32): CA base, daño sin armas principal/secundario y movimiento por nivel. */
    data class FilaMistico(val ca: Int, val principal: String, val secundario: String?, val movimiento: Int)

    private val tablaMistico = listOf(
        FilaMistico(11, "1d4", null, 12), FilaMistico(12, "1d6", null, 12), FilaMistico(12, "1d6", null, 14),
        FilaMistico(13, "1d8", null, 14), FilaMistico(13, "1d8", null, 14), FilaMistico(13, "1d8", "1d4", 16),
        FilaMistico(14, "1d10", "1d4", 16), FilaMistico(14, "1d10", "1d4", 16), FilaMistico(14, "1d10", "1d6", 16),
        FilaMistico(14, "1d10", "1d6", 18), FilaMistico(15, "1d10", "1d6", 18), FilaMistico(15, "1d10", "1d6", 18)
    )

    fun mistico(nivel: Int): FilaMistico = tablaMistico[nivel.coerceIn(1, 12) - 1]

    /**
     * Dado de golpe para multiclase (p. 35), indexado por el dado de cada clase.
     * Devuelve el dado y los PG fijos por nivel tras el 10.
     */
    fun dadoMulticlase(a: Int, b: Int): Pair<Int, Int> {
        val orden = listOf(4, 6, 8, 10, 12)
        val tabla = listOf(
            listOf(4 to 1, 4 to 1, 6 to 2, 6 to 2, 8 to 3),
            listOf(4 to 1, 6 to 2, 6 to 2, 8 to 3, 8 to 3),
            listOf(6 to 2, 6 to 2, 8 to 3, 8 to 3, 10 to 4),
            listOf(6 to 2, 8 to 3, 8 to 3, 10 to 4, 10 to 4),
            listOf(8 to 3, 8 to 3, 10 to 4, 10 to 4, 12 to 3)
        )
        val i = orden.indexOf(a).coerceAtLeast(0)
        val j = orden.indexOf(b).coerceAtLeast(0)
        return tabla[i][j]
    }
}
