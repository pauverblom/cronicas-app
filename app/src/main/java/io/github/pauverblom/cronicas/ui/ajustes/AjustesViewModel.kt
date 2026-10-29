package io.github.pauverblom.cronicas.ui.ajustes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.pauverblom.cronicas.CronicasApp
import io.github.pauverblom.cronicas.data.ColoresTema
import io.github.pauverblom.cronicas.data.Modo
import io.github.pauverblom.cronicas.data.Tema
import kotlinx.coroutines.launch

class AjustesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as CronicasApp).ajustes
    val ajustes = repo.ajustes
    fun setTema(t: Tema) = viewModelScope.launch { repo.setTema(t) }
    fun setModo(m: Modo) = viewModelScope.launch { repo.setModo(m) }
    fun setColores(c: ColoresTema) = viewModelScope.launch { repo.setColores(c) }
}
