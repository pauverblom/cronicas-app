package com.example.cronicasmarca

import android.app.Application
import com.example.cronicasmarca.data.AppDatabase
import com.example.cronicasmarca.data.Repository
import com.example.cronicasmarca.data.SeedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CronicasApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var repository: Repository
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        repository = Repository(database)
        appScope.launch {
            SeedData.seedIfEmpty(repository)
        }
    }
}
