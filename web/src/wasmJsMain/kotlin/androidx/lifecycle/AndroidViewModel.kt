package androidx.lifecycle

import android.app.Application

/** Equivalente web de AndroidViewModel: un ViewModel que conoce la aplicación. */
abstract class AndroidViewModel(private val application: Application) : ViewModel() {
    @Suppress("UNCHECKED_CAST")
    fun <T : Application> getApplication(): T = application as T
}
