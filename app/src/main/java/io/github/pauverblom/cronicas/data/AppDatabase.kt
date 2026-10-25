package io.github.pauverblom.cronicas.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Personaje::class,
        Caracteristicas::class,
        Estado::class,
        Objeto::class,
        Tesoro::class,
        Habilidad::class,
        Condicion::class,
        Compania::class,
        Conjuro::class,
        ConjuroPersonaje::class,
        EspacioOverride::class,
        Nota::class
    ],
    version = 4
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personajeDao(): PersonajeDao
    abstract fun caracteristicasDao(): CaracteristicasDao
    abstract fun estadoDao(): EstadoDao
    abstract fun objetoDao(): ObjetoDao
    abstract fun tesoroDao(): TesoroDao
    abstract fun habilidadDao(): HabilidadDao
    abstract fun condicionDao(): CondicionDao
    abstract fun companiaDao(): CompaniaDao
    abstract fun conjuroDao(): ConjuroDao
    abstract fun notaDao(): NotaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // La versión 4 rehace el esquema para la ficha de personaje: los datos de la 3 se descartan.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cronicas_marca.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
