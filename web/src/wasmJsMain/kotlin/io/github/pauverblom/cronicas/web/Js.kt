package io.github.pauverblom.cronicas.web

import kotlinx.coroutines.await
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.js.Promise

// Puente con las ayudas de JavaScript de resources/ayudas.js y resources/nube.js.

@JsFun("(accept) => window.cronicasElegirArchivo(accept)")
private external fun jsElegirArchivo(accept: String): Promise<JsString?>

@JsFun("(nombre, mime, base64) => window.cronicasDescargar(nombre, mime, base64)")
external fun jsDescargar(nombre: String, mime: String, base64: String)

@JsFun("(base64) => window.cronicasProcesarRetrato(base64)")
private external fun jsProcesarRetrato(base64: String): Promise<JsString?>

@JsFun("(ms) => window.cronicasFechaHora(ms)")
external fun jsFechaHora(ms: Double): String

/** Pantalla táctil (iPhone, iPad, Android): el puntero principal es un dedo. */
@JsFun("() => navigator.maxTouchPoints > 0 && window.matchMedia('(pointer: coarse)').matches")
external fun jsTactil(): Boolean

@JsFun("(argb) => window.cronicasPintarBarra(argb)")
external fun jsPintarBarra(argb: Int)

@JsFun("() => Date.now()")
external fun jsAhora(): Double

@JsFun("() => !window.matchMedia('(prefers-reduced-motion: reduce)').matches")
external fun jsAnimaciones(): Boolean

@JsFun("() => { const b = document.baseURI; return b.substring(0, b.lastIndexOf('/') + 1); }")
external fun jsBaseUrl(): String

@JsFun("() => window.cronicasListo()")
external fun jsListo()

@JsFun("(n) => { const a = new Uint32Array(1); crypto.getRandomValues(a); return a[0] % n; }")
external fun jsAzar(n: Int): Int

@JsFun("(k) => window.localStorage.getItem(k)")
external fun jsLeerLocal(clave: String): String?

@JsFun("(k, v) => { try { window.localStorage.setItem(k, v); return true; } catch (e) { console.warn('localStorage lleno', e); return false; } }")
external fun jsGuardarLocal(clave: String, valor: String): Boolean

@JsFun("(k) => window.localStorage.removeItem(k)")
external fun jsBorrarLocal(clave: String)

@OptIn(ExperimentalEncodingApi::class)
fun aBase64(bytes: ByteArray): String = Base64.encode(bytes)

@OptIn(ExperimentalEncodingApi::class)
fun deBase64(texto: String): ByteArray = Base64.decode(texto)

/**
 * Abre ya el selector de archivos del navegador (hay que llamarlo dentro del toque del usuario: Safari no lo abre
 * desde código asíncrono) y devuelve una función que espera el archivo elegido, o null si se cancela.
 * El archivo llega como JSON {"nombre", "mime", "base64"}.
 */
fun elegirArchivo(accept: String): suspend () -> String? {
    val promesa = jsElegirArchivo(accept)
    return { promesa.await<JsString?>()?.toString() }
}

/** JSON {"base64", "color"} del retrato recortado, o null si no es una imagen. */
suspend fun procesarRetrato(base64: String): String? = jsProcesarRetrato(base64).await<JsString?>()?.toString()

// ── Firebase (nube.js) ──

@JsFun("() => window.cronicasNubeLista")
private external fun jsNubeLista(): Promise<JsAny?>

@JsFun("() => window.cronicasNube.sesion()")
private external fun jsNubeSesion(): Promise<JsAny?>

@JsFun("(c) => window.cronicasNube.leer(c)")
private external fun jsNubeLeer(codigo: String): Promise<JsString?>

@JsFun("(c, f, d, h) => window.cronicasNube.escribir(c, f, d, h)")
private external fun jsNubeEscribir(codigo: String, formato: Int, datos: String, retratoHash: String)

@JsFun("(c, cb) => window.cronicasNube.escuchar(c, cb)")
private external fun jsNubeEscuchar(codigo: String, cb: (String) -> Unit): Int

@JsFun("(id) => window.cronicasNube.dejar(id)")
private external fun jsNubeDejar(id: Int)

@JsFun("(c) => window.cronicasNube.leerRetrato(c)")
private external fun jsNubeLeerRetrato(codigo: String): Promise<JsString?>

@JsFun("(c, h, i) => window.cronicasNube.escribirRetrato(c, h, i)")
private external fun jsNubeEscribirRetrato(codigo: String, hash: String, imagen: String?)

/** La API de Firestore de nube.js. Solo existe si hay configuración de Firebase y el SDK ha cargado. */
object Nube {
    /** Espera a nube.js (como mucho 15 s, por si el SDK no se puede descargar). */
    suspend fun disponible(): Boolean = withTimeoutOrNull(15_000) { jsNubeLista().await<JsAny?>() } != null

    suspend fun sesion() {
        jsNubeSesion().await<JsAny?>()
    }

    /** JSON {"formato", "datos", "retratoHash"} del servidor, o null si no existe. */
    suspend fun leer(codigo: String): String? = jsNubeLeer(codigo).await<JsString?>()?.toString()

    fun escribir(codigo: String, formato: Int, datos: String, retratoHash: String) = jsNubeEscribir(codigo, formato, datos, retratoHash)

    fun escuchar(codigo: String, cb: (String) -> Unit): Int = jsNubeEscuchar(codigo, cb)

    fun dejar(id: Int) = jsNubeDejar(id)

    /** JSON {"hash", "imagen"}, o null si no hay retrato. */
    suspend fun leerRetrato(codigo: String): String? = jsNubeLeerRetrato(codigo).await<JsString?>()?.toString()

    fun escribirRetrato(codigo: String, hash: String, imagen: String?) = jsNubeEscribirRetrato(codigo, hash, imagen)
}
