package com.claw.motogp.data

data class Session(
    val name: String,       // ej: "Free Practice 1", "Qualifying 1", "Race"
    val shortName: String,  // ej: "FP1", "Q1", "RACE"
    val day: String,        // ej: "Viernes", "Sábado", "Domingo"
    val date: String,       // ej: "16 May"
    val time: String,       // ej: "10:45"
    val isCompleted: Boolean = false
)

data class WeekendGP(
    val name: String,
    val circuit: String,
    val country: String,
    val dateRange: String,
    val isCurrent: Boolean = false,
    val sessions: List<Session> = emptyList(),
    val sessionResults: Map<String, List<SessionResult>> = emptyMap() // shortName -> results
)

data class SessionResult(
    val position: Int,
    val rider: String,
    val team: String,
    val time: String,
    val laps: String = ""
)

data class RiderStanding(
    val position: Int,
    val rider: String,
    val team: String,
    val bike: String,
    val points: Int,
    val deficit: String = ""
)

data class TeamStanding(
    val position: Int,
    val team: String,
    val points: Int
)

data class ManufacturerStanding(
    val position: Int,
    val manufacturer: String,
    val points: Int
)

data class ChampionshipInfo(
    val totalRounds: Int = 22,
    val completedRounds: Int,
    val pointsPerRaceWin: Int = 37,  // 25 race + 12 sprint
    val raceWinPoints: Int = 25,
    val sprintWinPoints: Int = 12,
    val totalPointsAvailable: Int,
    val remainingPointsAvailable: Int
) {
    companion object {
        fun calculate(completedRounds: Int): ChampionshipInfo {
            val totalRounds = 22
            val perRound = 37  // 25 race + 12 sprint
            val total = totalRounds * perRound
            val remaining = (totalRounds - completedRounds) * perRound
            return ChampionshipInfo(
                completedRounds = completedRounds,
                totalPointsAvailable = total,
                remainingPointsAvailable = remaining
            )
        }
    }
}

data class CalendarEvent(
    val name: String,
    val circuit: String,
    val dateRange: String,
    val startDate: String,
    val endDate: String,
    val isCompleted: Boolean,
    val round: Int
)

// Series history for evolution chart
data class RiderHistory(
    val rider: String,
    val pointsByRound: List<Int>
)

data class ManufacturerHistory(
    val manufacturer: String,
    val pointsByRound: List<Int>
)

// News
data class NewsArticle(
    val title: String,
    val snippet: String,
    val source: String,
    val url: String,
    val date: String
)

// Session definition for calendar (FP1, Practice, Q1, Q2, Sprint, Race)
data class CalendarSession(
    val shortName: String,
    val fullName: String,
    val time: String // HH:MM format
)

// Circuit info for the circuits tab
data class CircuitInfo(
    val name: String,
    val gpName: String,
    val country: String,
    val flag: String,
    val length: String,
    val turns: Int,
    val layoutType: String,
    val recordLap: String,
    val recordHolder: String,
    val recordYear: String,
    val mapResId: Int,
    val description: String
)

object CircuitData {
    val circuits: List<CircuitInfo> = listOf(
        CircuitInfo("Chang International Circuit",
            "Thailand GP", "Tailandia", "🇹🇭",
            "4.554 km", 12, "Mixto",
            "1'29.715", "J. Martin", "2025",
            R.drawable.circuit_thailand,
            "Circuito moderno con curvas variadas y rectas largas. El calor extremo es un factor clave."),
        CircuitInfo("Autódromo Termas de Río Hondo",
            "Brazil GP", "Argentina", "🇦🇷",
            "4.805 km", 14, "Rápido",
            "1'37.569", "M. Marquez", "2014",
            R.drawable.circuit_argentina,
            "Trazado fluido con curvas enlazadas. Favorito de los pilotos por su fluidez."),
        CircuitInfo("Circuit of the Americas",
            "Americas GP", "EE.UU.", "🇺🇸",
            "5.513 km", 20, "Técnico",
            "2'03.025", "F. Bagnaia", "2025",
            R.drawable.circuit_americas,
            "Circuito con la subida más pronunciada de MotoGP. Curvas muy variadas."),
        CircuitInfo("Circuito de Jerez - Ángel Nieto",
            "Spanish GP", "España", "🇪🇸",
            "4.423 km", 13, "Mixto",
            "1'36.993", "P. Acosta", "2025",
            R.drawable.circuit_jerez,
            "Circuito histórico con la famosa curva 'Dry Sack'. Ambiente único."),
        CircuitInfo("Bugatti Circuit (Le Mans)",
            "French GP", "Francia", "🇫🇷",
            "4.185 km", 14, "Stop & Go",
            "1'31.247", "J. Martin", "2024",
            R.drawable.circuit_lemans,
            "Alternancia de rectas largas y curvas lentas. Frenada crítica."),
        CircuitInfo("Circuit de Barcelona-Catalunya",
            "Catalan GP", "España", "🇪🇸",
            "4.657 km", 14, "Mixto",
            "1'38.069", "P. Acosta", "2026",
            R.drawable.circuit_catalunya,
            "Circuito polivalente con curvas de media y alta velocidad. Muy usado para test."),
        CircuitInfo("Mugello Circuit",
            "Italian GP", "Italia", "🇮🇹",
            "5.245 km", 15, "Rápido",
            "1'44.470", "F. Bagnaia", "2025",
            R.drawable.circuit_mugello,
            "Trazado de alta velocidad. Recta larguísima de más de 1 km."),
        CircuitInfo("Balaton Park Circuit",
            "Hungarian GP", "Hungría", "🇭🇺",
            "4.547 km", 15, "Técnico",
            "—", "—", "—",
            R.drawable.circuit_hungary,
            "Nuevo circuito en 2026. Diseño moderno con curvas técnicas al lado del lago Balaton."),
        CircuitInfo("TT Circuit Assen",
            "Dutch GP", "Países Bajos", "🇳🇱",
            "4.542 km", 18, "Fluido",
            "1'33.101", "F. Quartararo", "2022",
            R.drawable.circuit_assen,
            "La 'Catedral' del motociclismo. Curvas rápidas y peralte natural."),
        CircuitInfo("Sachsenring",
            "German GP", "Alemania", "🇩🇪",
            "3.671 km", 13, "Técnico",
            "1'20.113", "J. Martin", "2025",
            R.drawable.circuit_sachsenring,
            "El circuito más corto de MotoGP. Curva a izquierdas Waterfall muy famosa."),
        CircuitInfo("Silverstone Circuit",
            "British GP", "Reino Unido", "🇬🇧",
            "5.900 km", 18, "Rápido",
            "1'58.095", "M. Marquez", "2019",
            R.drawable.circuit_silverstone,
            "El circuito más largo del calendario. Curvas de alta velocidad."),
        CircuitInfo("Red Bull Ring",
            "Austrian GP", "Austria", "🇦🇹",
            "4.318 km", 10, "Stop & Go",
            "1'22.913", "J. Martin", "2025",
            R.drawable.circuit_austria,
            "Pocas curvas pero muy intensas. Grandes rectas con cambios de elevación."),
        CircuitInfo("Motorland Aragón",
            "Aragon GP", "España", "🇪🇸",
            "5.078 km", 17, "Mixto",
            "1'46.196", "M. Marquez", "2024",
            R.drawable.circuit_aragon,
            "Diseño moderno con curvas de todos los tipos. Cuenta con puente."),
        CircuitInfo("Misano World Circuit",
            "San Marino GP", "Italia", "🇮🇹",
            "4.226 km", 16, "Mixto",
            "1'30.700", "J. Martin", "2025",
            R.drawable.circuit_misano,
            "Cerca de la costa adriática. Curvas técnicas con una zona muy rápida."),
        CircuitInfo("Mobility Resort Motegi",
            "Japanese GP", "Japón", "🇯🇵",
            "4.801 km", 14, "Mixto",
            "1'44.116", "P. Acosta", "2025",
            R.drawable.circuit_motegi,
            "Circuito japonés con curvas muy técnicas. Zona de bosque."),
        CircuitInfo("Mandalika International Street Circuit",
            "Indonesian GP", "Indonesia", "🇮🇩",
            "4.301 km", 17, "Rápido",
            "1'30.170", "J. Martin", "2024",
            R.drawable.circuit_mandalika,
            "Circuito callejero junto a la playa. Muy rápido con curvas abiertas."),
        CircuitInfo("Phillip Island Grand Prix Circuit",
            "Australian GP", "Australia", "🇦🇺",
            "4.448 km", 12, "Rápido",
            "1'28.246", "F. Bagnaia", "2024",
            R.drawable.circuit_phillip_island,
            "Curvas rápidas en sentido antihorario. Viento y frío factor clave."),
        CircuitInfo("Sepang International Circuit",
            "Malaysian GP", "Malasia", "🇲🇾",
            "5.543 km", 15, "Mixto",
            "1'57.783", "J. Martin", "2025",
            R.drawable.circuit_sepang,
            "Ancho y largo circuito. Calor y humedad extremos."),
        CircuitInfo("Lusail International Circuit",
            "Qatar GP", "Qatar", "🇶🇦",
            "5.380 km", 16, "Stop & Go",
            "1'52.637", "F. Bagnaia", "2025",
            R.drawable.circuit_qatar,
            "Carrera nocturna con focos. Arena y curvas anchas."),
        CircuitInfo("Autódromo Internacional do Algarve",
            "Portuguese GP", "Portugal", "🇵🇹",
            "4.653 km", 15, "Mixto",
            "1'39.555", "J. Mir", "2021",
            R.drawable.circuit_portimao,
            "Desniveles y curvas ciegas. Una de las subidas más bestias del calendario."),
        CircuitInfo("Circuit Ricardo Tormo",
            "Valencia GP", "España", "🇪🇸",
            "4.005 km", 14, "Técnico",
            "1'29.699", "B. Binder", "2025",
            R.drawable.circuit_valencia,
            "Circuito urbano lento y técnico. Cierre de temporada. Aprieta las tuercas."),
        CircuitInfo("Automotodrom Brno",
            "Czech GP", "República Checa", "🇨🇿",
            "5.403 km", 14, "Fluido",
            "1'55.454", "J. Martin", "2025",
            R.drawable.circuit_brno,
            "Trazado natural con grandes cambios de elevación. Curvas a ciegas.")
    )
}

// All GP sessions with standard times
object GpSessions {
    val all = listOf(
        CalendarSession("FP1", "Entrenos Libres 1", "10:45"),
        CalendarSession("Practice", "Practice", "15:00"),
        CalendarSession("FP2", "Entrenos Libres 2", "10:10"),
        CalendarSession("Q1", "Clasificación 1", "10:50"),
        CalendarSession("Q2", "Clasificación 2", "11:15"),
        CalendarSession("Sprint", "Sprint", "15:00"),
        CalendarSession("WU", "Warm Up", "09:40"),
        CalendarSession("Race", "Carrera", "14:00")
    )
}
