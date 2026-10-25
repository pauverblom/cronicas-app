package com.example.cronicasmarca.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "personaje")
data class Personaje(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val clase: String = "",
    val especie: String = "",
    val nivel: Int = 1,
    val experiencia: Int = 0,
    val xpSiguiente: Int = 0,
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
    val fotoUri: String? = null
)

@Serializable
@Entity(
    tableName = "caracteristicas",
    foreignKeys = [ForeignKey(
        entity = Personaje::class,
        parentColumns = ["id"],
        childColumns = ["personajeId"],
        onDelete = ForeignKey.CASCADE
    )]
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
)

@Serializable
@Entity(
    tableName = "estado_combate",
    foreignKeys = [ForeignKey(
        entity = Personaje::class,
        parentColumns = ["id"],
        childColumns = ["personajeId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class EstadoCombate(
    @PrimaryKey val personajeId: Long,
    val pgActuales: Int = 0,
    val pgMaximos: Int = 0,
    val danoAtenuado: Int = 0,
    val dadoGolpe: String = "d4",
    val ba: Int = 0,
    val caBase: Int = 10,
    val caArmadura: Int = 0,
    val caEquipo: Int = 0,
    val caOtros: Int = 0,
    val caOverride: Int? = null,
    val tipoArmadura: String = "",
    val movMarchar: Int = 12,
    val movCorrer: Int = 24
)

@Serializable
@Entity(
    tableName = "arma",
    foreignKeys = [ForeignKey(
        entity = Personaje::class,
        parentColumns = ["id"],
        childColumns = ["personajeId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Arma(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personajeId: Long,
    val nombre: String,
    val baExtra: Int = 0,
    val formulaDano: String = "1d6",
    val tipo: String = "cuerpo",
    val notas: String = "",
    val ve: Int = 0
)

@Serializable
@Entity(tableName = "conjuro")
data class Conjuro(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val nivel: Int = 0,
    val tl: Int = 1,
    val componentes: String = "",
    val alcance: String = "",
    val duracion: String = "",
    val ts: String = "",
    val rc: String = "",
    val descripcion: String = "",
    val notaTactica: String = "",
    val esPropio: Boolean = false,
    val verificar: Boolean = false
)

@Serializable
@Entity(
    tableName = "conjuro_personaje",
    primaryKeys = ["personajeId", "conjuroId"],
    foreignKeys = [
        ForeignKey(
            entity = Personaje::class,
            parentColumns = ["id"],
            childColumns = ["personajeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Conjuro::class,
            parentColumns = ["id"],
            childColumns = ["conjuroId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ConjuroPersonaje(
    val personajeId: Long,
    val conjuroId: Long,
    val conocido: Boolean = true,
    val preparadoVeces: Int = 0,
    val gastadoVeces: Int = 0
)

@Serializable
@Entity(
    tableName = "espacios_conjuro",
    primaryKeys = ["personajeId", "nivel"],
    foreignKeys = [ForeignKey(
        entity = Personaje::class,
        parentColumns = ["id"],
        childColumns = ["personajeId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class EspaciosConjuro(
    val personajeId: Long,
    val nivel: Int,
    val total: Int,
    val override_: Int? = null
)

@Serializable
@Entity(
    tableName = "objeto",
    foreignKeys = [ForeignKey(
        entity = Personaje::class,
        parentColumns = ["id"],
        childColumns = ["personajeId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Objeto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personajeId: Long,
    val nombre: String,
    val cantidad: Int = 1,
    val ve: Float = 0f,
    val esMagico: Boolean = false,
    val descripcion: String = ""
)

@Serializable
@Entity(
    tableName = "tesoro",
    foreignKeys = [ForeignKey(
        entity = Personaje::class,
        parentColumns = ["id"],
        childColumns = ["personajeId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Tesoro(
    @PrimaryKey val personajeId: Long,
    val oro: Int = 0,
    val plata: Int = 0,
    val platino: Int = 0,
    val cobre: Int = 0,
    val gemas: String = ""
)

@Serializable
@Entity(
    tableName = "efecto_activo",
    foreignKeys = [ForeignKey(
        entity = Personaje::class,
        parentColumns = ["id"],
        childColumns = ["personajeId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class EfectoActivo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personajeId: Long,
    val nombre: String,
    val unidad: String = "asalto",
    val restante: Int = 0,
    val conjuroId: Long? = null
)

@Serializable
@Entity(
    tableName = "nota",
    foreignKeys = [ForeignKey(
        entity = Personaje::class,
        parentColumns = ["id"],
        childColumns = ["personajeId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Nota(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personajeId: Long,
    val titulo: String = "",
    val contenido: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "accion_undo",
    foreignKeys = [ForeignKey(
        entity = Personaje::class,
        parentColumns = ["id"],
        childColumns = ["personajeId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class AccionUndo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personajeId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val tipo: String,
    val payloadJson: String
)
