package io.github.pauverblom.cronicas

import android.app.Application
import io.github.pauverblom.cronicas.data.AjustesRepository
import io.github.pauverblom.cronicas.data.AppDatabase
import io.github.pauverblom.cronicas.data.Repository
import io.github.pauverblom.cronicas.data.SeedData
import io.github.pauverblom.cronicas.ui.common.Ilustraciones
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CronicasApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: Repository
        private set

    lateinit var ajustes: AjustesRepository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = Repository(database)
        ajustes = AjustesRepository(this)
        Ilustraciones.cargar(assets)
        appScope.launch {
            SeedData.seedIfEmpty(repository, assets)
        }
    }
}
