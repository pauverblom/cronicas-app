package io.github.pauverblom.cronicas.data

/** Conjuro inicial del grimorio del personaje de ejemplo. */
data class SpellSeed(
    val nombre: String,
    val nivel: Int,
    val tl: Int,
    val componentes: String,
    val alcance: String,
    val duracion: String,
    val ts: String,
    val rc: String,
    val descripcion: String,
    val notaTactica: String = "",
    val verificar: Boolean = false,
    val preparado: Int = 0
)

object SeedData {

    /** Crea el personaje de ejemplo (Ghitano) y el catálogo de conjuros si la base de datos está vacía. */
    suspend fun seedIfEmpty(repo: Repository) {
        if (repo.personajeCount() > 0) return

        val pid = repo.insertPersonaje(
            Personaje(
                nombre = "Ghitano",
                clase = "Mago / Asesino",
                especie = "Enano",
                nivel = 1,
                experiencia = 0,
                xpSiguiente = 2075,
                alineamiento = "Legal",
                deidad = "Vallon",
                lugarOrigen = "Salmanasar",
                descripcion = "Orden Cladonia",
                personalidad = "En sus ratos libres le gusta divertirse con otros aventureros, tomándose las aventuras como vacaciones.",
                trasfondo = "Se dedica a encubrir o solucionar los trapos sucios de ciertos cargos en la Orden Cladonia.",
                altura = "1,1 m",
                peso = "57 kg",
                edad = "Media",
                genero = "Hombre"
            )
        )

        repo.upsertCaracteristicas(
            Caracteristicas(
                personajeId = pid,
                fue = 9, des = 16, con = 9, int_ = 16, sab = 9, car = 6,
                desPrimaria = true,
                intPrimaria = true,
                desOverrideMod = 1,
                conOverrideMod = 1
            )
        )

        repo.upsertEstadoCombate(
            EstadoCombate(
                personajeId = pid,
                pgActuales = 4,
                pgMaximos = 4,
                dadoGolpe = "d4",
                ba = 0,
                caArmadura = 0,
                caOverride = 11,
                tipoArmadura = "Ninguna",
                movMarchar = 12,
                movCorrer = 24
            )
        )

        repo.insertArma(Arma(personajeId = pid, nombre = "Espada corta", formulaDano = "1d6", tipo = "cuerpo", ve = 2))
        repo.insertArma(Arma(personajeId = pid, nombre = "Látigo", formulaDano = "1d3", tipo = "cuerpo", notas = "Alcance 4 m (+DES)", ve = 2))

        repo.upsertEspaciosConjuro(EspaciosConjuro(personajeId = pid, nivel = 0, total = 4))
        repo.upsertEspaciosConjuro(EspaciosConjuro(personajeId = pid, nivel = 1, total = 3))

        repo.upsertTesoro(Tesoro(personajeId = pid))

        val spells = listOf(
            SpellSeed(
                nombre = "Detectar magia",
                nivel = 0,
                tl = 1,
                componentes = "V",
                alcance = "Radio 15 m",
                duracion = "1 min/nivel",
                ts = "No",
                rc = "No",
                descripcion = "Percibes un brillo tenue alrededor de criaturas y objetos mágicos dentro de un radio de 15 metros. Las barreras físicas gruesas (muros de piedra, metal) bloquean la detección. No identifica el tipo de magia ni el efecto concreto, solo su presencia.",
                notaTactica = "Úsalo antes de entrar en una sala sospechosa o para escanear botín. A nivel 1 dura 1 minuto: entra, mira rápido y sal. No gasta espacio de nivel 1."
            ),
            SpellSeed(
                nombre = "Luces danzantes",
                nivel = 0,
                tl = 1,
                componentes = "V, S",
                alcance = "50 m",
                duracion = "1 turno",
                ts = "Ver texto",
                rc = "No",
                descripcion = "Creas hasta cuatro luces del tamaño de antorchas, cuatro esferas luminosas, o una silueta humana difusa que brilla. Las luces se mueven bajo tu control dentro del alcance mientras mantengas la concentración. No causan daño, pero pueden distraer o iluminar zonas.",
                notaTactica = "Mándalas por un pasillo oscuro para provocar que los enemigos disparen a las luces en vez de a ti. También sirven para simular un grupo de antorchas y asustar o confundir."
            ),
            SpellSeed(
                nombre = "Mano mágica",
                nivel = 0,
                tl = 1,
                componentes = "V, S",
                alcance = "7 m",
                duracion = "Concentración",
                ts = "No",
                rc = "Sí",
                descripcion = "Creas una mano espectral que puede mover un objeto de hasta 3 kg dentro del alcance mientras mantienes la concentración. Si el objeto sale del rango de 7 metros, cae. La mano no puede atacar, activar objetos mágicos ni transportar más del límite de peso.",
                notaTactica = "Perfecta para tirar de palancas, recuperar llaves al otro lado de una reja o mover objetos sospechosos sin tocarlos. A 7 m de alcance, mantente lejos de trampas."
            ),
            SpellSeed(
                nombre = "Mensaje",
                nivel = 0,
                tl = 1,
                componentes = "V",
                alcance = "150 m",
                duracion = "1 asalto/nivel",
                ts = "No",
                rc = "No",
                descripcion = "Susurras un mensaje breve y señalas al objetivo, que lo oye dentro de su cabeza y puede responder en un susurro que solo tú oyes. El alcance es de 150 metros; no requiere línea de visión directa, pero sí un camino libre de barreras gruesas de piedra.",
                notaTactica = "Coordina con el grupo sin alertar a los enemigos. Ideal si el ladrón va por delante explorando o si necesitas pedir ayuda en silencio. A nivel 1 dura 1 asalto: sé breve."
            ),
            SpellSeed(
                nombre = "Dormir",
                nivel = 1,
                tl = 1,
                componentes = "V, S",
                alcance = "Área 20 m",
                duracion = "5 asaltos/nivel",
                ts = "Niega",
                rc = "No",
                descripcion = "Sueño mágico que afecta a criaturas de hasta 4+1 DG en un área. Afecta primero a las de menos DG. Los no-muertos y criaturas inmunes a encantamiento son inmunes. Las criaturas dormidas pueden ser despertadas con daño o una acción vigorosa.",
                notaTactica = "Tu conjuro estrella a nivel 1. Apunta a grupos de enemigos débiles (goblins, bandidos). Recuerda: afecta primero a los más débiles, así que no lo uses si hay aliados de bajo nivel en el área. Tras dormir enemigos, el grupo puede atarlos o neutralizarlos."
            ),
            SpellSeed(
                nombre = "Escudo",
                nivel = 1,
                tl = 1,
                componentes = "V, S",
                alcance = "Personal",
                duracion = "1 turno/nivel",
                ts = "No",
                rc = "No",
                descripcion = "Un campo de fuerza invisible te rodea, otorgándote un bonificador de +4 a la CA contra todos los ataques. Funciona como escudo pero sin ocupar la mano. Los proyectiles mágicos (como Proyectil mágico) son bloqueados automáticamente.",
                notaTactica = "Lánzalo antes de entrar en combate si sabes que te van a atacar. Con tu CA base de 11, sube a 15: marca la diferencia. Además bloquea Proyectil mágico enemigo, que de otro modo no tiene salvación."
            ),
            SpellSeed(
                nombre = "Descarga eléctrica",
                nivel = 1,
                tl = 1,
                componentes = "V, S",
                alcance = "Toque",
                duracion = "Ver texto",
                ts = "No",
                rc = "Sí",
                descripcion = "El lanzador es capaz de transmitir una potente descarga eléctrica al contacto con su mano. El daño producido es de 1d8+1 por nivel del lanzador. Por ejemplo, un lanzador de nivel 3 causará 1d8+3 puntos de daño. Un objetivo que trate activamente de evitar ser tocado por la mano electrificada forzará al lanzador a realizar una tirada de ataque de toque. El conjuro termina una vez sea aplicada la descarga con éxito. La descarga se puede transmitir a través de objetos metálicos.",
                notaTactica = "Tu único conjuro de daño directo a nivel 1 junto con Proyectil mágico. Requiere toque, así que es arriesgado para un mago. Úsalo cuando el enemigo ya esté en cuerpo a cuerpo contigo o tras una emboscada."
            ),
            SpellSeed(
                nombre = "Proyectil mágico",
                nivel = 1,
                tl = 1,
                componentes = "V, S",
                alcance = "50 m",
                duracion = "Instantánea",
                ts = "No",
                rc = "Sí",
                descripcion = "Lanzas un proyectil de energía mágica que impacta automáticamente a un objetivo dentro del alcance causando 1d4+1 puntos de daño. No requiere tirada de ataque. A niveles superiores se crean proyectiles adicionales.",
                notaTactica = "Daño garantizado sin tirada de ataque: perfecto contra enemigos con CA alta. A nivel 1 es un solo proyectil de 1d4+1 (2-5 de daño). Úsalo para rematar o contra objetivos difíciles de golpear."
            ),
            SpellSeed(
                nombre = "Invisibilidad",
                nivel = 2,
                tl = 1,
                componentes = "V, S",
                alcance = "Toque",
                duracion = "Hasta que ataque o 1 turno/nivel",
                ts = "No",
                rc = "Sí",
                descripcion = "El objetivo tocado (tú mismo u otro) se vuelve invisible junto con su equipo. La invisibilidad termina si el sujeto ataca, lanza un conjuro ofensivo o realiza una acción hostil. No silencia ruidos ni borra huellas.",
                notaTactica = "Conjuro de nivel 2: no lo tendrás preparable hasta subir. Cuando lo tengas, es tu mejor herramienta de supervivencia y exploración. Recuerda que atacar rompe el efecto, así que planifica."
            )
        )

        for (spell in spells) {
            val cid = repo.insertConjuro(
                Conjuro(
                    nombre = spell.nombre,
                    nivel = spell.nivel,
                    tl = spell.tl,
                    componentes = spell.componentes,
                    alcance = spell.alcance,
                    duracion = spell.duracion,
                    ts = spell.ts,
                    rc = spell.rc,
                    descripcion = spell.descripcion,
                    notaTactica = spell.notaTactica,
                    esPropio = true,
                    verificar = spell.verificar
                )
            )
            repo.upsertConjuroPersonaje(
                ConjuroPersonaje(
                    personajeId = pid,
                    conjuroId = cid,
                    conocido = true,
                    preparadoVeces = spell.preparado,
                    gastadoVeces = 0
                )
            )
        }

        SpellCatalog.seedCatalog(repo)
    }
}
