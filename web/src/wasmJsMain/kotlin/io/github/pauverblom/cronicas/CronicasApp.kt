package io.github.pauverblom.cronicas

import android.app.Application
import io.github.pauverblom.cronicas.data.AjustesRepository
import io.github.pauverblom.cronicas.data.CampanasRepository
import io.github.pauverblom.cronicas.data.Repository
import io.github.pauverblom.cronicas.data.sync.Sincronizador

/** Los servicios de la app en la web (en Android los crea CronicasApp.onCreate). */
class CronicasApp(
    val repository: Repository,
    val ajustes: AjustesRepository,
    val sincronizador: Sincronizador,
    val campanas: CampanasRepository
) : Application()
