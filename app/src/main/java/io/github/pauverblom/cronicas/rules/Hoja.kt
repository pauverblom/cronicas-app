package io.github.pauverblom.cronicas.rules

import io.github.pauverblom.cronicas.data.Caracteristicas
import io.github.pauverblom.cronicas.data.Compania
import io.github.pauverblom.cronicas.data.EspacioOverride
import io.github.pauverblom.cronicas.data.Estado
import io.github.pauverblom.cronicas.data.Objeto
import io.github.pauverblom.cronicas.data.Personaje
import io.github.pauverblom.cronicas.data.Tesoro
import io.github.pauverblom.cronicas.rules.data.Categoria
import io.github.pauverblom.cronicas.rules.data.ClaseDef
import io.github.pauverblom.cronicas.rules.data.Clases
import io.github.pauverblom.cronicas.rules.data.EspecieDef
import io.github.pauverblom.cronicas.rules.data.Especies
import io.github.pauverblom.cronicas.rules.data.TipoArma
import io.github.pauverblom.cronicas.rules.data.Alineamientos

/** Una característica ya calculada: puntuación con el modificador de especie y su modificador final. */
data class CaracteristicaHoja(
    val atributo: Atributo,
    val base: Int,
    val puntuacion: Int,
    val mod: Int,
    val modForzado: Boolean,
    val primaria: Boolean,
    /** Bonificador a tiradas de característica y salvación: mod. + nivel. */
    val bonusTirada: Int,
    val nd: Int
)

data class DesgloseCa(
    val base: Int,
    val armadura: Int,
    val des: Int,
    val escudo: Int,
    val magia: Int,
    val otros: Int,
    val casco: Int,
    val total: Int,
    val forzada: Boolean,
    /** Nombres de lo que se lleva puesto, para el «Tipo» de la ficha. */
    val nombreArmadura: String? = null,
    val nombreEscudo: String? = null,
    val nombreCasco: String? = null
)

data class LineaAtaque(val modo: String, val ba: Int, val dano: String, val alcance: Int? = null)

data class AtaqueHoja(
    val objetoId: Long?,
    val nombre: String,
    val lineas: List<LineaAtaque>,
    val notas: String = "",
    val aviso: String? = null,
    val equipado: Boolean = false
)

data class CargaHoja(
    val limite: Int,
    val total: Float,
    val veMonedas: Int,
    val banda: Carga.Banda,
    /** Uso de cada contenedor: id -> VE dentro. */
    val usoContenedores: Map<Long, Float>,
    /** Carga de cada montura: id -> VE cargado. */
    val usoCompanias: Map<Long, Float>
)

data class MagiaHoja(
    val clase: ClaseDef,
    val nivelLanzador: Int,
    /** Espacios por nivel de conjuro 0..9 tras aplicar los forzados a mano. */
    val espacios: List<Int>,
    val espaciosCalculados: List<Int>,
    val forzados: Set<Int>
)

data class HojaDerivada(
    val clase: ClaseDef?,
    val secundaria: ClaseDef?,
    val especie: EspecieDef?,
    val nombreClase: String,
    val nombreEspecie: String,
    val nivel: Int,
    val nivelSecundario: Int?,
    val caracteristicas: List<CaracteristicaHoja>,
    val dado: String,
    val dadoForzado: Boolean,
    val ba: Int,
    val baForzado: Boolean,
    val ca: DesgloseCa,
    val movimientoBase: Int,
    val movimiento: Int,
    val movimientoForzado: Boolean,
    val carga: CargaHoja,
    val ataques: List<AtaqueHoja>,
    val magia: List<MagiaHoja>,
    val xpSiguiente: Int?,
    val puedeSubir: Boolean,
    val avisos: List<String>
) {
    fun car(a: Atributo): CaracteristicaHoja = caracteristicas.first { it.atributo == a }
    fun mod(a: Atributo): Int = car(a).mod
    val esLanzador: Boolean get() = magia.isNotEmpty()
}

object Hoja {

    fun calcular(
        p: Personaje,
        c: Caracteristicas,
        e: Estado,
        objetos: List<Objeto> = emptyList(),
        tesoro: Tesoro? = null,
        companias: List<Compania> = emptyList(),
        espaciosForzados: List<EspacioOverride> = emptyList()
    ): HojaDerivada {
        val clase = Clases.porId(p.claseId)
        val secundaria = Clases.porId(p.claseSecundariaId)?.takeIf { clase != null && it.id != clase.id }
        val especie = Especies.porId(p.especieId)
        val nivel = p.nivel.coerceAtLeast(1)
        val nivelSec = secundaria?.let { Progresion.nivelSecundario(nivel) }

        val caracts = Atributo.entries.map { a ->
            val base = c.base(a)
            val puntuacion = (base + (especie?.mods?.get(a) ?: 0)).coerceIn(1, 25)
            val forzado = c.overrideMod(a)
            val mod = forzado ?: Modifiers.modifier(puntuacion)
            val primaria = c.primaria(a)
            CaracteristicaHoja(a, base, puntuacion, mod, forzado != null, primaria, mod + nivel, Salvaciones.nd(primaria))
        }
        fun mod(a: Atributo) = caracts.first { it.atributo == a }.mod
        fun punt(a: Atributo) = caracts.first { it.atributo == a }.puntuacion
        fun prim(a: Atributo) = caracts.first { it.atributo == a }.primaria

        // ── Dado de golpe y BA ──
        val dadoCalculado = clase?.let {
            val (d, fijos) = Progresion.dado(it, secundaria)
            when {
                nivel <= 10 -> "d$d"
                nivel <= 12 -> "d$d (+$fijos PG/nivel)"
                else -> "d$d (sin PG por nivel)"
            }
        } ?: "—"
        val baCalculado = clase?.let { Progresion.ba(it, secundaria, nivel) } ?: 0
        val ba = e.baOverride ?: baCalculado

        // ── Carga ──
        val sueltos = objetos.filter { it.contenedorId == null && it.companiaId == null }
        fun veDe(o: Objeto): Float =
            if (Categoria.of(o.categoria) == Categoria.CASCO && o.equipado) 0f else o.cantidad * o.ve
        val totalMonedas = tesoro?.let { it.cobre + it.plata + it.electro + it.oro + it.platino } ?: 0
        val veMonedas = Carga.veMonedas(totalMonedas)
        val totalVe = sueltos.sumOf { veDe(it).toDouble() }.toFloat() + veMonedas
        val limite = Carga.limite(punt(Atributo.FUE), prim(Atributo.FUE), prim(Atributo.CON))
        val banda = Carga.banda(totalVe, limite)
        val usoContenedores = objetos.filter { it.contenedorId != null }
            .groupBy { it.contenedorId!! }
            .mapValues { (_, l) -> l.sumOf { (it.cantidad * it.ve).toDouble() }.toFloat() }
        val usoCompanias = objetos.filter { it.companiaId != null && it.contenedorId == null }
            .groupBy { it.companiaId!! }
            .mapValues { (_, l) -> l.sumOf { (it.cantidad * it.ve).toDouble() }.toFloat() }
        val carga = CargaHoja(limite, totalVe, veMonedas, banda, usoContenedores, usoCompanias)

        // ── Clase de armadura ──
        val llevados = objetos.filter { it.companiaId == null }
        val equipados = llevados.filter { it.equipado }
        val esMistico = clase?.id == "mistico"
        val armadura = equipados.filter { Categoria.of(it.categoria) == Categoria.ARMADURA }.maxByOrNull { it.ca + it.bonoMagico }
        val escudo = equipados.filter { Categoria.of(it.categoria) == Categoria.ESCUDO }.maxByOrNull { it.ca + it.bonoMagico }
        val casco = equipados.filter { Categoria.of(it.categoria) == Categoria.CASCO }.maxByOrNull { it.ca + it.bonoMagico }
        // Los bonificadores mágicos a la CA de otros objetos no se acumulan: cuenta solo el mayor.
        val magia = equipados.filter {
            Categoria.of(it.categoria) !in setOf(Categoria.ARMADURA, Categoria.ESCUDO, Categoria.CASCO, Categoria.ARMA)
        }.maxOfOrNull { it.ca } ?: 0
        val caBase = if (esMistico) Clases.mistico(nivel).ca else 10
        val caArmadura = armadura?.let { it.ca + it.bonoMagico } ?: 0
        val caDes = if (banda == Carga.Banda.SOBRECARGADO) 0 else mod(Atributo.DES)
        val caEscudo = escudo?.let { it.ca + it.bonoMagico } ?: 0
        val caCasco = casco?.let { it.ca + it.bonoMagico } ?: 0
        val caCalculada = caBase + caArmadura + caDes + caEscudo + magia.coerceAtLeast(0) + e.caOtros
        val ca = DesgloseCa(
            caBase, caArmadura, caDes, caEscudo, magia.coerceAtLeast(0), e.caOtros, caCasco,
            e.caOverride ?: caCalculada, e.caOverride != null,
            armadura?.nombre, escudo?.nombre, casco?.nombre
        )

        // ── Movimiento ──
        val movBase = e.movOverride ?: when {
            esMistico -> Clases.mistico(nivel).movimiento
            especie != null -> especie.movimiento
            else -> 10
        }
        val movimiento = Carga.movimiento(movBase, banda)

        // ── Ataques ──
        val clasesArmas = listOfNotNull(clase, secundaria)
        fun entrenado(o: Objeto): Boolean {
            val id = o.catalogId ?: return true
            if (clasesArmas.isEmpty()) return true
            return clasesArmas.any { cl -> (cl.armas == null || id in cl.armas) && id !in cl.armasProhibidas }
        }
        val fue = mod(Atributo.FUE)
        val des = mod(Atributo.DES)
        val ataques = mutableListOf<AtaqueHoja>()
        val pequeno = especie?.pequeno == true
        val danoSinArmas = if (esMistico) Clases.mistico(nivel).principal else if (pequeno) "1d2" else "1d3"
        ataques += AtaqueHoja(
            objetoId = null,
            nombre = "Sin armas",
            lineas = listOf(LineaAtaque("Cuerpo a cuerpo", maxOf(0, ba + fue), sumarDano(danoSinArmas, fue))),
            notas = if (esMistico) {
                Clases.mistico(nivel).secundario?.let { "Ataque secundario: ${sumarDano(it, fue)}" } ?: ""
            } else "Daño atenuado"
        )
        for (o in llevados.filter { Categoria.of(it.categoria) == Categoria.ARMA }) {
            val penal = if (entrenado(o)) 0 else 4
            val tipo = TipoArma.of(o.tipoArma)
            fun dano(f: String, bonus: Int) = sumarDano(f, bonus + o.bonoMagico)
            val lineas = mutableListOf<LineaAtaque>()
            val baCuerpo = maxOf(0, ba + fue - penal) + o.bonoMagico
            val baDistancia = maxOf(0, ba + des - penal) + o.bonoMagico
            if (tipo != TipoArma.DISTANCIA) {
                lineas += LineaAtaque(if (o.formulaDanoDosManos.isNotBlank()) "Una mano" else "Cuerpo a cuerpo", baCuerpo, dano(o.formulaDano, fue))
                if (o.formulaDanoDosManos.isNotBlank()) {
                    lineas += LineaAtaque("Dos manos", baCuerpo, dano(o.formulaDanoDosManos, fue))
                }
            }
            if (tipo == TipoArma.ARROJADIZA) {
                // Las armas arrojadizas suman FUE al daño
                lineas += LineaAtaque("Arrojada", baDistancia, dano(o.formulaDano, fue), o.alcance)
            }
            if (tipo == TipoArma.DISTANCIA) {
                lineas += LineaAtaque("Distancia", baDistancia, dano(o.formulaDano, 0), o.alcance)
            }
            ataques += AtaqueHoja(
                objetoId = o.id,
                nombre = if (o.bonoMagico != 0) "${o.nombre} ${Modifiers.format(o.bonoMagico)}" else o.nombre,
                lineas = lineas,
                notas = listOfNotNull(
                    "Dos manos".takeIf { o.dosManos },
                    o.alcance?.let { "Alcance $it m (medio -2 hasta ${it * 2} m, largo -6 hasta ${it * 3} m)" }
                        ?.takeIf { tipo != TipoArma.CUERPO },
                    o.notas.takeIf { it.isNotBlank() }
                ).joinToString(" · "),
                aviso = if (penal > 0) "Arma no permitida para la clase: -4 al ataque" else null,
                equipado = o.equipado
            )
        }

        // ── Conjuros ──
        val forzados = espaciosForzados.associate { it.nivel to it.total }
        val magiaHoja = buildList {
            listOfNotNull(clase?.let { it to nivel }, secundaria?.let { it to nivelSec!! }).forEach { (cl, nl) ->
                val tabla = cl.tablaConjuros ?: return@forEach
                val atr = cl.atributoConjuros ?: Atributo.INT
                val calc = EspaciosConjuro.espacios(tabla, nl, punt(atr))
                // Los espacios forzados a mano se aplican a la primera clase lanzadora
                val aplicar = if (isEmpty()) forzados else emptyMap()
                val finales = calc.mapIndexed { lvl, v -> aplicar[lvl] ?: v }
                add(MagiaHoja(cl, nl, finales, calc, aplicar.keys))
            }
        }

        // ── Experiencia ──
        val xpSiguiente = clase?.let { Progresion.xpParaNivel(it, secundaria, nivel + 1) }
            ?.takeIf { nivel < Progresion.NIVEL_MAXIMO }
        val puedeSubir = xpSiguiente != null && p.experiencia >= xpSiguiente

        // ── Avisos ──
        val avisos = buildList {
            if (clase != null) {
                armadura?.catalogId?.let { id ->
                    if (clasesArmas.none { it.armaduras == null || id in it.armaduras }) {
                        add("La armadura ${armadura.nombre} no está permitida para la clase: puede perder sus habilidades de clase")
                    }
                }
                escudo?.catalogId?.let { id ->
                    if (clasesArmas.none { it.escudos == null || id in it.escudos }) {
                        add("El escudo ${escudo.nombre} no está permitido para la clase")
                    }
                }
                if (!prim(clase.principal)) add("${clase.principal.nombre} debería ser primaria para un ${clase.nombre.lowercase()}")
            }
            Alineamientos.aviso(clase, p.alineamiento)?.let { add(it) }
            Carga.efectos(banda)?.let { add("Carga ${banda.etiqueta.lowercase()}: $it") }
        }

        return HojaDerivada(
            clase = clase,
            secundaria = secundaria,
            especie = especie,
            nombreClase = clase?.nombre ?: p.claseNombre.ifBlank { "Sin clase" },
            nombreEspecie = especie?.nombre ?: p.especieNombre.ifBlank { "Sin especie" },
            nivel = nivel,
            nivelSecundario = nivelSec,
            caracteristicas = caracts,
            dado = e.dgOverride ?: dadoCalculado,
            dadoForzado = e.dgOverride != null,
            ba = ba,
            baForzado = e.baOverride != null,
            ca = ca,
            movimientoBase = movBase,
            movimiento = movimiento,
            movimientoForzado = e.movOverride != null,
            carga = carga,
            ataques = ataques,
            magia = magiaHoja,
            xpSiguiente = xpSiguiente,
            puedeSubir = puedeSubir,
            avisos = avisos
        )
    }
}
