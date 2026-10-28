package io.github.pauverblom.cronicas.data

enum class Tema(val etiqueta: String, val descripcion: String) {
    PERGAMINO_OSCURO("Pergamino oscuro", "Marrón oscuro y oro"),
    PERGAMINO_CLARO("Pergamino claro", "Papel crema, tinta oscura y rojo"),
    MATERIAL_YOU("Material You", "Colores del fondo de pantalla (Android 12+)"),
    RETRATO("Desde retrato", "Cada ficha toma los colores del retrato de su personaje")
}

enum class Modo(val etiqueta: String) { SISTEMA("Sistema"), CLARO("Claro"), OSCURO("Oscuro") }

data class AjustesApp(val tema: Tema = Tema.PERGAMINO_OSCURO, val modo: Modo = Modo.SISTEMA)
