package io.github.pauverblom.cronicas.ui.hud

/** En la web todo corre en un hilo: el `synchronized` de la caché de teselas no hace falta. */
internal inline fun <R> synchronized(@Suppress("UNUSED_PARAMETER") lock: Any, block: () -> R): R = block()
