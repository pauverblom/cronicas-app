package io.github.pauverblom.cronicas.rules.data

/** Categorías de objeto del inventario. Se guardan por nombre en la base de datos. */
enum class Categoria(val etiqueta: String) {
    ARMA("Arma"),
    MUNICION("Munición"),
    ARMADURA("Armadura"),
    ESCUDO("Escudo"),
    CASCO("Casco"),
    CONTENEDOR("Contenedor"),
    EQUIPO("Equipo");

    companion object {
        fun of(name: String): Categoria = entries.firstOrNull { it.name == name } ?: EQUIPO
    }
}

/** Cómo se usa un arma: cuerpo a cuerpo, arrojadiza (cuerpo a cuerpo o lanzada) o solo a distancia. */
enum class TipoArma { CUERPO, ARROJADIZA, DISTANCIA;

    companion object {
        fun of(name: String?): TipoArma = entries.firstOrNull { it.name == name } ?: CUERPO
    }
}

/**
 * Artículo de catálogo (reglamento, cap. 5). Los precios van en monedas de cobre (1 mo = 100 mc).
 * [ve] es el VE de un lote de [unidades] (p. ej. 10 flechas = VE 1).
 */
data class ArticuloDef(
    val id: String,
    val nombre: String,
    val categoria: Categoria,
    val precioMc: Int,
    val ve: Float,
    val unidades: Int = 1,
    val dano: String? = null,
    val danoDosManos: String? = null,
    val tipo: TipoArma = TipoArma.CUERPO,
    val alcance: Int? = null,
    val dosManos: Boolean = false,
    val notas: String = "",
    val ca: Int = 0,
    val capacidad: Int? = null
)

private const val MO = 100
private const val MP = 10

private fun arma(
    id: String, nombre: String, precio: Int, dano: String, ve: Float, alcance: Int? = null,
    tipo: TipoArma = if (alcance != null) TipoArma.ARROJADIZA else TipoArma.CUERPO,
    dosManos: Boolean = false, versatil: String? = null, notas: String = ""
) = ArticuloDef(
    id, nombre, Categoria.ARMA, precio, ve, dano = dano, danoDosManos = versatil, tipo = tipo,
    alcance = alcance, dosManos = dosManos, notas = notas
)

private fun distancia(id: String, nombre: String, precio: Int, dano: String, ve: Float, alcance: Int, dosManos: Boolean = true, notas: String = "") =
    arma(id, nombre, precio, dano, ve, alcance, TipoArma.DISTANCIA, dosManos, notas = notas)

private fun equipo(id: String, nombre: String, precio: Int, ve: Float, unidades: Int = 1, capacidad: Int? = null, notas: String = "") =
    ArticuloDef(
        id, nombre, if (capacidad != null) Categoria.CONTENEDOR else Categoria.EQUIPO, precio, ve,
        unidades = unidades, capacidad = capacidad, notas = notas
    )

object Catalogo {

    private const val ASTADA = "Astada: daño doble si se afianza contra una carga."

    val armas: List<ArticuloDef> = listOf(
        arma("alabarda", "Alabarda", 8 * MO, "1d10", 5f, dosManos = true, notas = ASTADA),
        distancia("arco_corto", "Arco corto", 15 * MO, "1d6", 3f, 20),
        distancia("arco_corto_compuesto", "Arco corto compuesto", 25 * MO, "1d8", 3f, 25),
        distancia("arco_largo", "Arco largo", 25 * MO, "1d6", 4f, 30),
        distancia("arco_largo_compuesto", "Arco largo compuesto", 50 * MO, "1d8", 4f, 35),
        distancia("ballesta_ligera", "Ballesta ligera", 25 * MO, "1d6", 4f, 25),
        distancia("ballesta_mano", "Ballesta de mano", 100 * MO, "1d4", 2f, 10, dosManos = false),
        distancia("ballesta_pesada", "Ballesta pesada", 40 * MO, "1d10", 5f, 40),
        distancia("ballesta_repeticion", "Ballesta de repetición", 200 * MO, "1d8", 5f, 30, notas = "Cargador de 6 virotes."),
        arma("baston", "Bastón", 4 * MO, "1d8", 4f, dosManos = true),
        arma("cachiporra", "Cachiporra", 2 * MO, "1d6", 2f),
        distancia("cerbatana", "Cerbatana", 4 * MO, "1d4", 1f, 15, dosManos = false),
        arma("cimitarra", "Cimitarra", 15 * MO, "1d6", 3f),
        arma("daga", "Daga", 3 * MO, "1d4", 1f, alcance = 6),
        arma("daga_plata", "Daga de plata", 30 * MO, "1d4", 1f, alcance = 6),
        arma("dardo", "Dardo", 5 * MP, "1d3", 1f, alcance = 6),
        arma("espada_corta", "Espada corta", 5 * MO, "1d6", 2f),
        arma("espada_larga", "Espada larga", 10 * MO, "1d8", 3f, versatil = "2d4"),
        arma("espada_bastarda", "Espada bastarda", 20 * MO, "1d10", 4f, versatil = "1d12"),
        arma("espada_dos_manos", "Espada a dos manos", 15 * MO, "2d6", 5f, dosManos = true),
        arma("estoque", "Estoque", 15 * MO, "1d6", 3f),
        arma("garrote", "Garrote", 3 * MO, "1d4", 2f),
        arma("guadana", "Guadaña", 18 * MO, "2d4", 4f, dosManos = true),
        arma("gran_hacha", "Gran hacha", 20 * MO, "1d12", 4f, dosManos = true),
        arma("hacha_batalla", "Hacha de batalla", 7 * MO, "1d8", 3f),
        arma("hacha_mano", "Hacha de mano", 4 * MO, "1d6", 2f, alcance = 6),
        distancia("honda", "Honda", 2 * MO, "1d4", 1f, 15, dosManos = false),
        arma("hoz", "Hoz", 6 * MO, "1d4", 3f),
        arma("jabalina", "Jabalina", 1 * MO, "1d4", 2f, alcance = 9),
        arma("lanza", "Lanza", 4 * MO, "1d6", 2f, alcance = 9, notas = ASTADA),
        arma("lanza_caballeria", "Lanza de caballería", 10 * MO, "1d8", 7f, notas = "Daño doble cargando a caballo."),
        arma("latigo", "Látigo", 3 * MO, "1d3", 2f, notas = "Alcance 4 m."),
        arma("lucero_alba", "Lucero del alba", 5 * MO, "2d4", 3f),
        arma("martillo_guerra", "Martillo de guerra", 7 * MO, "1d8", 4f),
        arma("martillo_ligero", "Martillo ligero", 1 * MO, "1d4", 2f, alcance = 9),
        arma("maza", "Maza", 5 * MO, "1d6", 3f),
        arma("pica", "Pica", 8 * MO, "1d8", 6f, dosManos = true, notas = ASTADA),
        arma("tridente", "Tridente", 4 * MO, "1d8", 5f, alcance = 3, dosManos = true, notas = ASTADA)
    )

    val municion: List<ArticuloDef> = listOf(
        ArticuloDef("dardos_cerbatana", "Dardos de cerbatana", Categoria.MUNICION, 10 * MO, 1f, unidades = 20),
        ArticuloDef("flechas", "Flechas", Categoria.MUNICION, 3 * MO, 1f, unidades = 10),
        ArticuloDef("virotes", "Virotes", Categoria.MUNICION, 5 * MO, 2f, unidades = 20),
        ArticuloDef("virotes_ligeros", "Virotes ligeros", Categoria.MUNICION, 5 * MO, 1f, unidades = 20)
    )

    private fun armadura(id: String, nombre: String, precio: Int, ca: Int, ve: Float, notas: String = "") =
        ArticuloDef(id, nombre, Categoria.ARMADURA, precio, ve, ca = ca, notas = notas)

    val armaduras: List<ArticuloDef> = listOf(
        armadura("cuero_acolchado", "Cuero acolchado", 5 * MO, 1, 2f),
        armadura("cuero_endurecido", "Cuero endurecido", 20 * MO, 2, 2f),
        armadura("cuero_tachonado", "Cuero tachonado", 30 * MO, 3, 3f),
        armadura("cota_escamas", "Cota de escamas", 65 * MO, 4, 4f, "Incluye cofia de cuero."),
        armadura("anillas", "Armadura de anillas", 85 * MO, 4, 3f),
        armadura("camisote_mallas", "Camisote de mallas", 100 * MO, 4, 3f),
        armadura("armadura_varillas", "Armadura de varillas", 200 * MO, 4, 3f),
        armadura("laminada", "Armadura laminada", 100 * MO, 5, 4f),
        armadura("cota_mallas", "Cota de mallas", 150 * MO, 5, 4f, "Incluye cofia de mallas."),
        armadura("peto_coraza", "Peto de coraza", 300 * MO, 5, 3f),
        armadura("cota_bandas", "Cota de bandas", 250 * MO, 6, 4f),
        armadura("placas_mallas", "Placas y mallas", 600 * MO, 7, 4f, "Incluye yelmo."),
        armadura("armadura_completa", "Armadura completa", 1000 * MO, 8, 4f, "Incluye yelmo completo.")
    )

    private fun casco(id: String, nombre: String, precio: Int, ca: Int, ve: Float) =
        ArticuloDef(id, nombre, Categoria.CASCO, precio, ve, ca = ca, notas = "Solo contra ataques a la cabeza.")

    val cascos: List<ArticuloDef> = listOf(
        casco("cofia_cuero", "Cofia de cuero", 4 * MO, 1, 1f),
        casco("bacinete", "Bacinete", 15 * MO, 2, 1f),
        casco("celada", "Celada", 5 * MO, 3, 1f),
        casco("cofia_mallas", "Cofia de mallas", 15 * MO, 4, 1f),
        casco("yelmo", "Yelmo", 10 * MO, 5, 1f),
        casco("yelmo_completo", "Yelmo completo", 20 * MO, 7, 2f)
    )

    private fun escudo(id: String, nombre: String, precio: Int, ca: Int, cubre: String, ve: Float) =
        ArticuloDef(id, nombre, Categoria.ESCUDO, precio, ve, ca = ca, notas = "Cubre contra $cubre por asalto.")

    val escudos: List<ArticuloDef> = listOf(
        escudo("broquel", "Broquel / rodela", 2 * MO, 1, "1 atacante", 2f),
        escudo("escudo_pequeno_madera", "Escudo pequeño de madera", 3 * MO, 1, "1 atacante", 2f),
        escudo("escudo_pequeno_metal", "Escudo pequeño de metal", 10 * MO, 1, "1 atacante", 2f),
        escudo("escudo_medio_madera", "Escudo medio de madera", 5 * MO, 1, "2 atacantes", 3f),
        escudo("escudo_medio_metal", "Escudo medio de metal", 15 * MO, 1, "2 atacantes", 3f),
        escudo("escudo_pesado_madera", "Escudo pesado de madera", 7 * MO, 1, "3 atacantes", 4f),
        escudo("escudo_pesado_metal", "Escudo pesado de metal", 20 * MO, 1, "3 atacantes", 4f),
        escudo("paves", "Pavés", 55 * MO, 6, "todos los atacantes", 12f)
    )

    val equipo: List<ArticuloDef> = listOf(
        equipo("aceite", "Aceite (frasco)", 1 * MO, 1f),
        equipo("agua_bendita", "Agua bendita", 25 * MO, 1f),
        equipo("ajo", "Ajo (cabezas)", 5 * MO, 0f, unidades = 3),
        equipo("alforjas", "Alforjas", 1 * MP, 1f, capacidad = 3),
        equipo("antorchas", "Antorchas", 2 * MP, 1f, unidades = 4),
        equipo("arnica", "Árnica", 10 * MO, 0f),
        equipo("barra", "Barra", 2 * MO, 2f),
        equipo("botella", "Botella", 2 * MO, 1f, capacidad = 1),
        equipo("cadena", "Cadena (3 m)", 30 * MO, 1f),
        equipo("catalejo", "Catalejo", 1000 * MO, 1f),
        equipo("cerrojo", "Cerrojo", 20 * MO, 1f),
        equipo("cuerda_canamo", "Cuerda de cáñamo (15 m)", 1 * MO, 3f),
        equipo("cuerda_seda", "Cuerda de seda (15 m)", 10 * MO, 2f),
        equipo("escala", "Escala (3 m)", 1 * MO, 3f),
        equipo("escarpias", "Escarpias", 1 * MO, 2f, unidades = 12),
        equipo("espejo", "Espejo", 5 * MO, 1f),
        equipo("estacas", "Estacas", 5, 1f, unidades = 3),
        equipo("estuche", "Estuche", 1 * MO, 1f),
        equipo("farol", "Farol", 9 * MO, 2f),
        equipo("frasco", "Frasco", 3, 1f),
        equipo("grilletes", "Grilletes", 15 * MO, 1f),
        equipo("herramientas_ladron", "Herramientas de ladrón", 30 * MO, 1f),
        equipo("libro_conjuros", "Libro de conjuros", 15 * MO, 1f),
        equipo("manta", "Manta", 5 * MP, 2f),
        equipo("martillo", "Martillo", 5 * MP, 2f),
        equipo("mochila", "Mochila", 1 * MO, 2f, capacidad = 8),
        equipo("odre", "Odre", 1 * MO, 3f),
        equipo("pala", "Pala", 2 * MO, 3f),
        equipo("pergamino", "Pergamino", 4 * MP, 0f),
        equipo("pertiga", "Pértiga", 2 * MP, 5f),
        equipo("petate", "Petate", 1 * MP, 3f),
        equipo("pica_escalada", "Pica de escalada", 3 * MO, 3f),
        equipo("pluma", "Pluma", 1 * MP, 0f),
        equipo("polea", "Polea", 5 * MO, 2f),
        equipo("puas", "Púas", 1 * MO, 1f, unidades = 12),
        equipo("racion", "Ración (1 día)", 5 * MP, 1f),
        equipo("saco_grande", "Saco grande", 2 * MP, 2f, capacidad = 10),
        equipo("saco_pequeno", "Saco pequeño", 1 * MP, 1f, capacidad = 6),
        equipo("simbolo_madera", "Símbolo sagrado de madera", 1 * MO, 1f),
        equipo("simbolo_plata", "Símbolo sagrado de plata", 25 * MO, 1f),
        equipo("tinta", "Tinta", 8 * MO, 0f),
        equipo("tiza", "Tiza", 1, 1f),
        equipo("velas", "Velas", 1 * MP, 1f, unidades = 10),
        equipo("vial", "Vial", 1 * MP, 0f),
        equipo("yesca", "Yesca y pedernal", 1 * MO, 0f),
        equipo("muda_comun", "Muda común", 2 * MO, 3f),
        equipo("muda_buena", "Muda buena", 10 * MO, 2f),
        equipo("muda_noble", "Muda noble", 50 * MO, 3f),
        equipo("barda_cuero", "Barda de cuero", 50 * MO, 5f, notas = "+3 a la CA de la montura."),
        equipo("barda_metal", "Barda de metal", 500 * MO, 6f, notas = "+5 a la CA de la montura."),
        equipo("silla_montar", "Silla de montar", 25 * MO, 6f),
        equipo("brida", "Brida", 1 * MO, 2f),
        equipo("pienso", "Pienso", 5, 2f)
    )

    val todos: List<ArticuloDef> = armas + municion + armaduras + escudos + cascos + equipo

    fun porId(id: String?): ArticuloDef? = todos.firstOrNull { it.id == id }

    /** Monturas y animales (p. 55) con el VE que pueden cargar. */
    data class MonturaDef(val id: String, val nombre: String, val precioMc: Int, val capacidadVe: Int)

    val monturas: List<MonturaDef> = listOf(
        MonturaDef("burro", "Burro", 8 * MO, 24),
        MonturaDef("caballo_monta", "Caballo de monta", 75 * MO, 38),
        MonturaDef("caballo_guerra", "Caballo de guerra", 250 * MO, 70),
        MonturaDef("mula", "Mula", 30 * MO, 50),
        MonturaDef("poni", "Poni", 30 * MO, 12),
        MonturaDef("perro", "Perro", 3 * MP, 4),
        MonturaDef("perro_guardian", "Perro guardián", 25 * MO, 5)
    )
}
