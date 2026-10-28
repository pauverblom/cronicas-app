package io.github.pauverblom.cronicas.data

import io.github.pauverblom.cronicas.ui.plataforma.ahora

/** En la web no hay java.lang.System: las entidades compartidas usan este para su fecha por defecto. */
internal object System {
    fun currentTimeMillis(): Long = ahora()
}
