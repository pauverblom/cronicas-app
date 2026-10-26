package io.github.pauverblom.cronicas.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.pauverblom.cronicas.rules.Atributo
import kotlinx.serialization.Serializable

/** Id de clase/especie para las entradas personalizadas (sin datos de reglamento). */
const val PERSONALIZADA = "custom"

@Serializable
@Entity(tableName = "personaje")
data class Personaje(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val jugador: String = "",
    /** Id de [io.github.pauverblom.cronicas.rules.data.Clases], o [PERSONALIZADA]. */
    val claseId: String = PERSONALIZADA,
    /** Nombre mostrado cuando la clase es personalizada. */
    val claseNombre: String = "",
    /** Clase secundaria para la regla opcional de multiclase. */
    val claseSecundariaId: String? = null,
    val especieId: String = PERSONALIZADA,
    val especieNombre: String = "",
    val nivel: Int = 1,
    val experiencia: Int = 0,
    val alineamiento: String = "",
    val deidad: String = "",
    val lugarOrigen: String = "",
    val idiomas: String = "",
    val descripcion: String = "",
    val personalidad: String = "",
    val trasfondo: String = "",
    val cicatrices: String = "",
    val altura: String = "",
    val peso: String = "",
    val edad: String = "",
    val genero: String = "",
    val ojos: String = "",
    val cabello: String = "",
    /** Ruta absoluta del retrato dentro de filesDir, o null. */
    val fotoPath: String? = null,
    /** Color semilla (ARGB) extraído del retrato para el tema "Desde retrato". */
    val colorSemilla: Int? = null,
    val creado: Long = System.currentTimeMillis()
)

/** Puntuaciones tiradas (sin modificadores de especie), primarias y modificadores forzados a mano. */
@Serializable
@Entity(
    tableName = "caracteristicas",
    foreignKeys = [ForeignKey(Personaje::class, ["id"], ["personajeId"], onDelete = ForeignKey.CASCADE)]
)
data class Caracteristicas(
    @PrimaryKey val personajeId: Long,
    val fue: Int = 10,
    val des: Int = 10,
    val con: Int = 10,
    val int_: Int = 10,
    val sab: Int = 10,
    val car: Int = 10,
    val fuePrimaria: Boolean = false,
    val desPrimaria: Boolean = false,
    val conPrimaria: Boolean = false,
    val intPrimaria: Boolean = false,
    val sabPrimaria: Boolean = false,
    val carPrimaria: Boolean = false,
    val fueOverrideMod: Int? = null,
    val desOverrideMod: Int? = null,
    val conOverrideMod: Int? = null,
    val intOverrideMod: Int? = null,
    val sabOverrideMod: Int? = null,
    val carOverrideMod: Int? = null
) {
    fun base(a: Atributo): Int = when (a) {
        Atributo.FUE -> fue
        Atributo.DES -> des
        Atributo.CON -> con
        Atributo.INT -> int_
        Atributo.SAB -> sab
        Atributo.CAR -> car
    }

    fun primaria(a: Atributo): Boolean = when (a) {
        Atributo.FUE -> fuePrimaria
        Atributo.DES -> desPrimaria
        Atributo.CON -> conPrimaria
        Atributo.INT -> intPrimaria
        Atributo.SAB -> sabPrimaria
        Atributo.CAR -> carPrimaria
    }

    fun overrideMod(a: Atributo): Int? = when (a) {
        Atributo.FUE -> fueOverrideMod
        Atributo.DES -> desOverrideMod
        Atributo.CON -> conOverrideMod
        Atributo.INT -> intOverrideMod
        Atributo.SAB -> sabOverrideMod
        Atributo.CAR -> carOverrideMod
    }

    fun conBase(a: Atributo, v: Int): Caracteristicas = when (a) {
        Atributo.FUE -> copy(fue = v)
        Atributo.DES -> copy(des = v)
        Atributo.CON -> copy(con = v)
        Atributo.INT -> copy(int_ = v)
        Atributo.SAB -> copy(sab = v)
        Atributo.CAR -> copy(car = v)
    }

    fun conPrimaria(a: Atributo, v: Boolean): Caracteristicas = when (a) {
        Atributo.FUE -> copy(fuePrimaria = v)
        Atributo.DES -> copy(desPrimaria = v)
        Atributo.CON -> copy(conPrimaria = v)
        Atributo.INT -> copy(intPrimaria = v)
        Atributo.SAB -> copy(sabPrimaria = v)
        Atributo.CAR -> copy(carPrimaria = v)
    }

    fun conOverrideMod(a: Atributo, v: Int?): Caracteristicas = when (a) {
        Atributo.FUE -> copy(fueOverrideMod = v)
        Atributo.DES -> copy(desOverrideMod = v)
        Atributo.CON -> copy(conOverrideMod = v)
        Atributo.INT -> copy(intOverrideMod = v)
        Atributo.SAB -> copy(sabOverrideMod = v)
        Atributo.CAR -> copy(carOverrideMod = v)
    }
}

/** PG y valores derivados que el jugador ha fijado a mano (null = calculado). */
@Serializable
@Entity(
    tableName = "estado",
    foreignKeys = [ForeignKey(Personaje::class, ["id"], ["personajeId"], onDelete = ForeignKey.CASCADE)]
)
data class Estado(
    @PrimaryKey val personajeId: Long,
    val pgActuales: Int = 0,
    val pgMaximos: Int = 0,
    val dgOverride: String? = null,
    val baOverride: Int? = null,
    /** Otros bonificadores a la CA que no salen del equipo. */
    val caOtros: Int = 0,
    val caOverride: Int? = null,
    val movOverride: Int? = null,
    /** Parte de los PG perdidos que es daño atenuado: no mata y se recupera a 1 PG por minuto. */
    @ColumnInfo(defaultValue = "0") val danoAtenuado: Int = 0,
    /** Ataques que no salen de un arma (garras, aliento...), como en la ficha. */
    @ColumnInfo(defaultValue = "") val otrosAtaques: String = ""
)

/**
 * Cualquier objeto del inventario: armas, armaduras, equipo, contenedores...
 * Un objeto puede estar dentro de un contenedor ([contenedorId]) o cargado en una montura ([companiaId]).
 */
@Serializable
@Entity(
    tableName = "objeto",
    foreignKeys = [ForeignKey(Personaje::class, ["id"], ["personajeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("personajeId")]
)
data class Objeto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personajeId: Long,
    /** Nombre de [io.github.pauverblom.cronicas.rules.data.Categoria]. */
    val categoria: String = "EQUIPO",
    val catalogId: String? = null,
    val nombre: String,
    val cantidad: Int = 1,
    /** VE de una unidad. */
    val ve: Float = 0f,
    val equipado: Boolean = false,
    val contenedorId: Long? = null,
    val companiaId: Long? = null,
    /** Capacidad (CAP) si es un contenedor. */
    val capacidad: Int? = null,
    /** Armas: daño a una mano y a dos manos (versátiles). */
    val formulaDano: String = "",
    val formulaDanoDosManos: String = "",
    /** Nombre de [io.github.pauverblom.cronicas.rules.data.TipoArma]. */
    val tipoArma: String = "CUERPO",
    val alcance: Int? = null,
    val dosManos: Boolean = false,
    /** CA base de armaduras, escudos y cascos; bonificador a la CA de otros objetos mágicos. */
    val ca: Int = 0,
    /** Bonificador mágico: al ataque y daño en armas, a la CA en armaduras y escudos. */
    val bonoMagico: Int = 0,
    val esMagico: Boolean = false,
    val notas: String = "",
    /** Pergaminos: el [Conjuro] que contienen. Sin clave foránea, para no perder el objeto si se borra el conjuro. */
    val conjuroId: Long? = null
)

@Serializable
@Entity(
    tableName = "tesoro",
    foreignKeys = [ForeignKey(Personaje::class, ["id"], ["personajeId"], onDelete = ForeignKey.CASCADE)]
)
data class Tesoro(
    @PrimaryKey val personajeId: Long,
    val cobre: Int = 0,
    val plata: Int = 0,
    val electro: Int = 0,
    val oro: Int = 0,
    val platino: Int = 0,
    val gemas: String = ""
)

/** Habilidad de clase, rasgo de especie o habilidad añadida a mano. */
@Serializable
@Entity(
    tableName = "habilidad",
    foreignKeys = [ForeignKey(Personaje::class, ["id"], ["personajeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("personajeId")]
)
data class Habilidad(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personajeId: Long,
    /** "clase", "especie" o "propia". */
    val origen: String = ORIGEN_PROPIA,
    val nombre: String,
    /** Abreviatura de la característica con la que se tira, o null. */
    val atributo: String? = null,
    val descripcion: String = "",
    /** Las habilidades de reglamento borradas se ocultan para no volver a añadirlas al subir de nivel. */
    val oculta: Boolean = false
) {
    companion object {
        const val ORIGEN_CLASE = "clase"
        const val ORIGEN_ESPECIE = "especie"
        const val ORIGEN_PROPIA = "propia"
    }
}

/** Condición activa (bendecido, envenenado...). Sin temporizador: se quita a mano. */
@Serializable
@Entity(
    tableName = "condicion",
    foreignKeys = [ForeignKey(Personaje::class, ["id"], ["personajeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("personajeId")]
)
data class Condicion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personajeId: Long,
    val nombre: String,
    val nota: String = ""
)

/** Montura, compañero animal o familiar (página 2 de la ficha). */
@Serializable
@Entity(
    tableName = "compania",
    foreignKeys = [ForeignKey(Personaje::class, ["id"], ["personajeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("personajeId")]
)
data class Compania(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personajeId: Long,
    /** "Montura", "Compañero animal" o "Familiar". */
    val tipo: String = "Montura",
    val nombre: String = "",
    val especie: String = "",
    val movimiento: String = "",
    val ataque: String = "",
    val pgActuales: Int = 0,
    val pgMaximos: Int = 0,
    val ca: Int = 10,
    val habilidades: String = "",
    /** VE que puede cargar; 0 si no lleva carga. */
    val capacidadVe: Int = 0
)

/**
 * Conjuro del catálogo o creado por el usuario ([esPropio]).
 * [niveles] guarda el nivel en cada lista de clase, p. ej. "clerigo=1;druida=2".
 */
@Serializable
@Entity(tableName = "conjuro", indices = [Index("nombre")])
data class Conjuro(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val niveles: String = "",
    val tl: String = "",
    val componentes: String = "",
    val alcance: String = "",
    val duracion: String = "",
    val ts: String = "",
    val rc: String = "",
    val descripcion: String = "",
    val esPropio: Boolean = false
) {
    val nivelesPorClase: Map<String, Int>
        get() = niveles.split(';').mapNotNull { part ->
            val (k, v) = part.split('=').takeIf { it.size == 2 } ?: return@mapNotNull null
            v.trim().toIntOrNull()?.let { k.trim() to it }
        }.toMap()

    companion object {
        fun formatNiveles(map: Map<String, Int>): String = map.entries.joinToString(";") { "${it.key}=${it.value}" }
    }
}

/** Conjuro en el grimorio (conocido) de un personaje, con cuántas veces está preparado y usado. */
@Serializable
@Entity(
    tableName = "conjuro_personaje",
    primaryKeys = ["personajeId", "conjuroId"],
    foreignKeys = [
        ForeignKey(Personaje::class, ["id"], ["personajeId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Conjuro::class, ["id"], ["conjuroId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("conjuroId")]
)
data class ConjuroPersonaje(
    val personajeId: Long,
    val conjuroId: Long,
    /** Nivel del conjuro para este personaje (según su clase). */
    val nivel: Int,
    val preparadoVeces: Int = 0,
    val usadoVeces: Int = 0
)

/** Espacios de conjuro fijados a mano para un nivel de conjuro. */
@Serializable
@Entity(
    tableName = "espacio_override",
    primaryKeys = ["personajeId", "nivel"],
    foreignKeys = [ForeignKey(Personaje::class, ["id"], ["personajeId"], onDelete = ForeignKey.CASCADE)]
)
data class EspacioOverride(
    val personajeId: Long,
    val nivel: Int,
    val total: Int
)

@Serializable
@Entity(
    tableName = "nota",
    foreignKeys = [ForeignKey(Personaje::class, ["id"], ["personajeId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("personajeId")]
)
data class Nota(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personajeId: Long,
    val titulo: String = "",
    val contenido: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
