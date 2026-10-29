import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
}

// La versión web (Kotlin/Wasm + Compose Multiplatform) compila tal cual estos archivos de la app Android:
// reglas, modelo de datos, tema, componentes y pantallas. Lo que depende de Android (base de datos, archivos,
// Firebase, tipografías...) lo sustituyen los archivos de src/wasmJsMain con las mismas declaraciones.
val appKotlin = rootProject.layout.projectDirectory.dir("app/src/main/java/io/github/pauverblom/cronicas")
val compartidos = listOf(
    "rules/**",
    "data/Entities.kt",
    "data/FichaExportada.kt",
    "data/ConjuroConDetalle.kt",
    "data/AjustesApp.kt",
    "data/Campanas.kt",
    "data/Modelo.kt",
    "data/sync/Canonica.kt",
    "data/sync/Codigos.kt",
    "ui/theme/Theme.kt",
    "ui/theme/Tipos.kt",
    "ui/hud/**",
    "ui/common/Carrusel.kt",
    "ui/common/Componentes.kt",
    "ui/common/SelectorColor.kt",
    "ui/common/Fuego.kt",
    "ui/hoja/**",
    "ui/home/**",
    "ui/wizard/**",
    "ui/ajustes/**",
    "ui/foto/RetratoAvatar.kt"
)

val copiarCodigo by tasks.registering(Sync::class) {
    from(appKotlin) { include(compartidos) }
    into(layout.buildDirectory.dir("compartido/io/github/pauverblom/cronicas"))
}

// Tipografías y catálogo de conjuros de la app, como recursos de Compose (se leen con Res.readBytes)
val copiarRecursos by tasks.registering(Sync::class) {
    from(rootProject.file("app/src/main/res/font")) { into("files/fuentes") }
    from(rootProject.file("app/src/main/assets")) { include("conjuros.json"); into("files") }
    into(layout.buildDirectory.dir("recursos"))
}

// Ilustraciones del reglamento: archivos estáticos junto a la página, y su lista como código
val ilustraciones = rootProject.file("app/src/main/assets/ilustraciones")
val copiarEstaticos by tasks.registering(Sync::class) {
    from(ilustraciones) { into("ilustraciones") }
    into(layout.buildDirectory.dir("estaticos"))
}
val listarIlustraciones by tasks.registering {
    val salida = layout.buildDirectory.dir("generado/io/github/pauverblom/cronicas/ui/common")
    inputs.dir(ilustraciones)
    outputs.dir(salida)
    doLast {
        val nombres = ilustraciones.listFiles().orEmpty().map { it.name }.filter { it.endsWith(".webp") }.sorted()
        val f = salida.get().file("IlustracionesDisponibles.kt").asFile
        f.parentFile.mkdirs()
        f.writeText(
            "package io.github.pauverblom.cronicas.ui.common\n\n" +
                "/** Generado por web/build.gradle.kts a partir de app/src/main/assets/ilustraciones. */\n" +
                "internal val ILUSTRACIONES_DISPONIBLES: Set<String> = setOf(\n" +
                nombres.joinToString(",\n") { "    \"$it\"" } + "\n)\n"
        )
    }
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "cronicas"
        browser {
            commonWebpackConfig { outputFileName = "cronicas.js" }
        }
        binaries.executable()
    }

    sourceSets {
        val wasmJsMain by getting {
            kotlin.srcDir(copiarCodigo.map { layout.buildDirectory.dir("compartido").get() })
            kotlin.srcDir(listarIlustraciones.map { layout.buildDirectory.dir("generado").get() })
            resources.srcDir(copiarEstaticos.map { layout.buildDirectory.dir("estaticos").get() })
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)
                implementation(libs.kmp.navigation.compose)
                implementation(libs.kmp.lifecycle.viewmodel.compose)
                implementation(libs.kmp.lifecycle.runtime.compose)
                implementation(libs.kotlinx.coroutines.core.web)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.coil.compose)
                implementation(libs.coil.network.ktor3)
                implementation(libs.ktor.client.js)
                implementation(libs.material.kolor)
            }
        }
    }
}

compose.resources {
    packageOfResClass = "io.github.pauverblom.cronicas.web.recursos"
    generateResClass = always
    customDirectory(
        sourceSetName = "wasmJsMain",
        directoryProvider = copiarRecursos.map { layout.buildDirectory.dir("recursos").get() }
    )
}
