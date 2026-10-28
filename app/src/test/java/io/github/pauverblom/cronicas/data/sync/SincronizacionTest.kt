package io.github.pauverblom.cronicas.data.sync

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.pauverblom.cronicas.data.AppDatabase
import io.github.pauverblom.cronicas.data.Backup
import io.github.pauverblom.cronicas.data.Backup.ConjuroExportado
import io.github.pauverblom.cronicas.data.Backup.PersonajeExportado
import io.github.pauverblom.cronicas.data.Caracteristicas
import io.github.pauverblom.cronicas.data.Compania
import io.github.pauverblom.cronicas.data.Conjuro
import io.github.pauverblom.cronicas.data.Estado
import io.github.pauverblom.cronicas.data.Habilidad
import io.github.pauverblom.cronicas.data.Nota
import io.github.pauverblom.cronicas.data.Objeto
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.data.Repository
import io.github.pauverblom.cronicas.data.Tesoro
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

class CanonicaTest {

    /** Una ficha con referencias entre filas: contenido antes que su contenedor, carga en montura y un pergamino. */
    private fun ficha(base: Long, pg: Int = 7) = PersonajeExportado(
        personaje = Personaje(id = base, nombre = "Ghitano", fotoPath = "/a/$base.jpg", creado = base),
        caracteristicas = Caracteristicas(base, fue = 9),
        estado = Estado(base, pgActuales = pg, pgMaximos = 8),
        tesoro = Tesoro(base, oro = 12),
        objetos = listOf(
            Objeto(id = base + 1, personajeId = base, nombre = "Daga", contenedorId = base + 2),
            Objeto(id = base + 2, personajeId = base, nombre = "Mochila", capacidad = 10, companiaId = base + 50),
            Objeto(id = base + 3, personajeId = base, nombre = "Pergamino", conjuroId = base + 70)
        ),
        habilidades = listOf(Habilidad(id = base + 4, personajeId = base, nombre = "Sigilo")),
        condiciones = emptyList(),
        companias = listOf(Compania(id = base + 50, personajeId = base, nombre = "Rucio")),
        conjuros = listOf(
            ConjuroExportado(Conjuro(id = base + 71, nombre = "Luz", descripcion = "texto $base"), 1, 1, 0),
            ConjuroExportado(Conjuro(id = base + 72, nombre = "Dormir"), 1, 2, 1)
        ),
        espacios = emptyList(),
        notas = listOf(Nota(id = base + 5, personajeId = base, titulo = "Pista", timestamp = 1000)),
        conjurosPergaminos = listOf(Conjuro(id = base + 70, nombre = "Escudo"))
    )

    @Test
    fun losIdsLocalesNoCuentan() {
        val a = ficha(100)
        val b = ficha(900).let { it.copy(conjuros = it.conjuros.reversed()) }
        assertEquals(Canonica.de(a), Canonica.de(b))
    }

    @Test
    fun unCambioDeContenidoSiCuenta() {
        assertNotEquals(Canonica.de(ficha(100, pg = 7)), Canonica.de(ficha(100, pg = 6)))
    }

    @Test
    fun cambiarDeContenedorCuenta() {
        val a = ficha(100)
        val b = a.copy(objetos = a.objetos.map { if (it.nombre == "Daga") it.copy(contenedorId = null) else it })
        assertNotEquals(Canonica.de(a), Canonica.de(b))
    }

    @Test
    fun codigos() {
        assertEquals("K7QM2XPA9RTE", Sincronizador.normalizar(" k7qm-2xpa 9rte "))
        assertEquals("K7QM-2XPA-9RTE", Sincronizador.formatear("K7QM2XPA9RTE"))
    }
}

/** Aplicar una ficha descargada sobre la local debe dejarla igual que la remota, sin cambios espurios que reenviar. */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, sdk = [34])
class AplicarFichaTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: Repository
    private lateinit var dir: File

    @Before
    fun crear() {
        val ctx = ApplicationProvider.getApplicationContext<Application>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
        repo = Repository(db)
        dir = File(ctx.filesDir, "retratos")
    }

    @After
    fun cerrar() = db.close()

    /** Crea un personaje con contenido insertado antes que su contenedor, montura, pergamino, notas y conjuros. */
    private suspend fun personajeCompleto(nombre: String): Long {
        val pid = repo.crearPersonaje(Personaje(nombre = nombre), Caracteristicas(0, fue = 14), 9, Tesoro(0, oro = 30))
        val montura = repo.insertCompania(Compania(personajeId = pid, nombre = "Rucio", capacidadVe = 20))
        val escudo = repo.insertConjuro(Conjuro(nombre = "Escudo prueba", esPropio = true))
        val daga = repo.insertObjeto(Objeto(personajeId = pid, nombre = "Daga"))
        val mochila = repo.insertObjeto(Objeto(personajeId = pid, nombre = "Mochila", capacidad = 10, companiaId = montura))
        repo.updateObjeto(Objeto(id = daga, personajeId = pid, nombre = "Daga", contenedorId = mochila))
        repo.insertObjeto(Objeto(personajeId = pid, nombre = "Pergamino", conjuroId = escudo))
        repo.insertNota(Nota(personajeId = pid, titulo = "Uno", timestamp = 5))
        repo.insertNota(Nota(personajeId = pid, titulo = "Dos", timestamp = 5))
        repo.insertNota(Nota(personajeId = pid, titulo = "Tres", timestamp = 9))
        val luz = repo.insertConjuro(Conjuro(nombre = "Luz prueba", esPropio = true))
        repo.aprenderConjuro(pid, luz, 1)
        repo.upsertEstado(repo.getEstado(pid)!!.copy(pgActuales = 4))
        return pid
    }

    @Test
    fun aplicarSobreOtroPersonajeDejaLaMismaFicha() = runBlocking {
        val origen = personajeCompleto("Origen")
        val destino = personajeCompleto("Destino")
        repo.upsertEstado(repo.getEstado(destino)!!.copy(pgActuales = 1))

        val remota = Backup.instantanea(repo, origen, conRetrato = false)
        Backup.escribir(db, remota, sobre = destino, dirRetratos = dir)

        val local = Backup.instantanea(repo, destino, conRetrato = false)
        assertEquals(Canonica.de(remota), Canonica.de(local))
        assertEquals(destino, local.personaje.id)
        assertEquals(4, local.estado!!.pgActuales)
    }

    @Test
    fun vincularComoPersonajeNuevoDejaLaMismaFicha() = runBlocking {
        val origen = personajeCompleto("Origen")
        val remota = Backup.instantanea(repo, origen, conRetrato = false)
        val nuevo = Backup.escribir(db, remota, sobre = null, dirRetratos = dir)
        assertNotEquals(origen, nuevo)
        assertEquals(Canonica.de(remota), Canonica.de(Backup.instantanea(repo, nuevo, conRetrato = false)))
    }

    @Test
    fun aplicarDosVecesEsEstable() = runBlocking {
        val origen = personajeCompleto("Origen")
        val remota = Backup.instantanea(repo, origen, conRetrato = false)
        Backup.escribir(db, remota, sobre = origen, dirRetratos = dir)
        val primera = Backup.instantanea(repo, origen, conRetrato = false)
        Backup.escribir(db, primera, sobre = origen, dirRetratos = dir)
        val segunda = Backup.instantanea(repo, origen, conRetrato = false)
        assertEquals(Canonica.de(remota), Canonica.de(segunda))
        assertEquals(remota.personaje.creado, segunda.personaje.creado)
    }
}
