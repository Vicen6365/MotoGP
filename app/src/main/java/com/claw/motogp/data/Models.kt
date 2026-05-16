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
    val q1Results: List<QualifyingResult> = emptyList(),
    val q2Results: List<QualifyingResult> = emptyList(),
    val sprintResults: List<RaceResult> = emptyList(),
    val raceResults: List<RaceResult> = emptyList()
)

data class QualifyingResult(
    val position: Int,
    val rider: String,
    val team: String,
    val time: String
)

data class RaceResult(
    val position: Int,
    val rider: String,
    val team: String,
    val time: String,
    val gap: String = ""
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
