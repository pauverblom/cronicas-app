package io.github.pauverblom.cronicas.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.pauverblom.cronicas.CronicasApp
import io.github.pauverblom.cronicas.data.Backup
import io.github.pauverblom.cronicas.data.DatosCampanas
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.data.ResumenPersonaje
import io.github.pauverblom.cronicas.ui.foto.Retratos
import io.github.pauverblom.cronicas.ui.plataforma.Archivo
import io.github.pauverblom.cronicas.ui.plataforma.escribirArchivo
import io.github.pauverblom.cronicas.ui.plataforma.leerArchivo
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as CronicasApp).repository
    private val sync = (app as CronicasApp).sincronizador
    private val campanasRepo = (app as CronicasApp).campanas

    val syncDisponible = sync.disponible
    val vinculos = sync.vinculos

    val personajes = repo.resumenes().map<List<ResumenPersonaje>, List<ResumenPersonaje>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val campanas = campanasRepo.datos.map<DatosCampanas, DatosCampanas?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun eliminar(p: Personaje) = viewModelScope.launch {
        repo.deletePersonaje(p)
        campanasRepo.editar { it.mover(p.id, null) }
    }

    fun moverACampana(pid: Long, campanaId: Long?) = viewModelScope.launch { campanasRepo.editar { it.mover(pid, campanaId) } }
    fun crearCampana(pid: Long, nombre: String) = viewModelScope.launch { campanasRepo.editar { it.crearYMover(pid, nombre) } }
    fun renombrarCampana(id: Long, nombre: String) = viewModelScope.launch { campanasRepo.editar { it.renombrar(id, nombre) } }
    fun disolverCampana(id: Long) = viewModelScope.launch { campanasRepo.editar { it.disolver(id) } }
    fun plegarCampana(id: Long, plegada: Boolean) = viewModelScope.launch { campanasRepo.editar { it.plegar(id, plegada) } }

    fun exportar(pid: Long, archivo: Archivo, onFin: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = runCatching {
            val json = Backup.exportar(repo, pid)
            escribirArchivo(getApplication(), archivo, json.encodeToByteArray())
        }.isSuccess
        onFin(ok)
    }

    fun activarSync(pid: Long, onFin: (String?) -> Unit) = viewModelScope.launch {
        onFin(runCatching { sync.activar(pid) }.getOrNull())
    }

    /** Devuelve el id del personaje vinculado, o el mensaje de error. */
    fun vincular(codigo: String, onFin: (Result<Long>) -> Unit) = viewModelScope.launch {
        onFin(runCatching { sync.vincular(codigo) })
    }

    fun desvincular(pid: Long) = sync.desvincular(pid)

    fun importar(archivo: Archivo, onFin: (Long?) -> Unit) = viewModelScope.launch {
        val app = getApplication<Application>()
        val id = runCatching {
            val texto = leerArchivo(app, archivo).decodeToString()
            Backup.importar(repo, texto, Retratos.directorio(app))
        }.getOrNull()
        onFin(id)
    }
}
