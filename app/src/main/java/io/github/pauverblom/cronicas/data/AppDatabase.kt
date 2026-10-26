package io.github.pauverblom.cronicas.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

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
    version = 6
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

        /** Conjuro guardado en un pergamino. */
        private val MIGRACION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE objeto ADD COLUMN conjuroId INTEGER")
            }
        }

        /** Daño atenuado y otros ataques, de la ficha impresa. */
        private val MIGRACION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE estado ADD COLUMN danoAtenuado INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE estado ADD COLUMN otrosAtaques TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // La versión 4 rehace el esquema para la ficha de personaje: los datos de la 3 se descartan.
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cronicas_marca.db"
                )
                    .addMigrations(MIGRACION_4_5, MIGRACION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
