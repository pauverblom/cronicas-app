package com.example.cronicasmarca.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Personaje::class,
        Caracteristicas::class,
        EstadoCombate::class,
        Arma::class,
        Conjuro::class,
        ConjuroPersonaje::class,
        EspaciosConjuro::class,
        Objeto::class,
        Tesoro::class,
        EfectoActivo::class,
        Nota::class,
        AccionUndo::class
    ],
    version = 3
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personajeDao(): PersonajeDao
    abstract fun caracteristicasDao(): CaracteristicasDao
    abstract fun estadoCombateDao(): EstadoCombateDao
    abstract fun armaDao(): ArmaDao
    abstract fun conjuroDao(): ConjuroDao
    abstract fun conjuroPersonajeDao(): ConjuroPersonajeDao
    abstract fun espaciosConjuroDao(): EspaciosConjuroDao
    abstract fun objetoDao(): ObjetoDao
    abstract fun tesoroDao(): TesoroDao
    abstract fun efectoActivoDao(): EfectoActivoDao
    abstract fun notaDao(): NotaDao
    abstract fun accionUndoDao(): AccionUndoDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
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
