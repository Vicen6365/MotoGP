package com.claw.motogp.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Scraper {
    private const val SCHEDULE_URL = "https://www.autosport.com/motogp/schedule/2026/"
    private const val STANDINGS_URL = "https://www.motorsport.com/motogp/standings/2026/"
    private const val TIMEOUT = 10000

    private val dateFormats = listOf(
        SimpleDateFormat("dd MMM yyyy", Locale.US),
        SimpleDateFormat("d MMM yyyy", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US)
    )

    fun getMonthNumber(month: String): Int = when (month.lowercase().take(3)) {
        "jan" -> 1; "feb" -> 2; "mar" -> 3; "apr" -> 4; "may" -> 5; "jun" -> 6
        "jul" -> 7; "aug" -> 8; "sep" -> 9; "oct" -> 10; "nov" -> 11; "dec" -> 12
        else -> 1
    }

    fun getCurrentWeekend(calendars: List<CalendarEvent>): Int {
        val now = Date()
        for ((i, gp) in calendars.withIndex()) {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US)
            try {
                val parts = gp.dateRange.split(" - ")
                val dateParts = parts[0].split(" ")
                val day = dateParts[0].replace("rd","").replace("th","").replace("st","").replace("nd","")
                val month = dateParts[1].take(3)
                val year = "2026"
                val start = sdf.parse("$day $month $year") ?: continue
                val endParts = parts.getOrElse(1) { dateParts[1] }.split(" ")
                val eDay = endParts[0].replace("rd","").replace("th","").replace("st","").replace("nd","")
                val eMonth = endParts.getOrElse(1) { dateParts[1] }.take(3)
                val end = sdf.parse("$eDay $eMonth $year") ?: continue
                // Extend end by 3 days for race weekend
                val cal = java.util.Calendar.getInstance().apply { time = end; add(java.util.Calendar.DAY_OF_MONTH, 3) }
                if (now.after(start) && now.before(cal.time)) return i
            } catch (_: Exception) {}
        }
        // If no current, find the next one
        for ((i, gp) in calendars.withIndex()) {
            if (!gp.isCompleted) return i
        }
        return calendars.lastIndex
    }

    fun fetchSchedule(): List<CalendarEvent> {
        return try {
            val doc = Jsoup.connect(SCHEDULE_URL).timeout(TIMEOUT).get()
            parseSchedule(doc)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseSchedule(doc: Document): List<CalendarEvent> {
        val events = mutableListOf<CalendarEvent>()
        val text = doc.body().text()
        val gpNames = listOf(
            "Thailand GP", "Brazil GP", "Americas GP", "Spanish GP",
            "French GP", "Catalan GP", "Italian GP", "Hungarian GP",
            "Czech GP", "Dutch GP", "German GP", "British GP",
            "Aragon GP", "San Marino GP", "Austrian GP", "Japanese GP",
            "Indonesian GP", "Australian GP", "Malaysian GP",
            "Qatar GP", "Portuguese GP", "Valencia GP"
        )
        val gpSlugs = listOf(
            "thailand", "brazil", "americas", "spain",
            "french", "catalunya", "italy", "hungary",
            "czech", "dutch", "germany", "british",
            "aragon", "san-marino", "austria", "japan",
            "indonesia", "australia", "malaysia",
            "qatar", "portugal", "valencia"
        )
        val gpCircuits = listOf(
            "Chang International Circuit",
            "Autodromo Internacional Ayrton Senna",
            "Circuit of the Americas",
            "Circuito de Jerez",
            "Le Mans Circuit Bugatti",
            "Circuit de Barcelona-Catalunya",
            "Mugello Circuit",
            "Balaton Park",
            "Brno Circuit",
            "TT Circuit Assen",
            "Sachsenring",
            "Silverstone Circuit",
            "MotorLand Aragon",
            "Misano World Circuit",
            "Red Bull Ring",
            "Twin Ring Motegi",
            "Mandalika Street Circuit",
            "Phillip Island Circuit",
            "Sepang International Circuit",
            "Losail International Circuit",
            "Algarve International Circuit",
            "Circuit Ricardo Tormo"
        )

        // Parse dates from text
        val datePattern = Regex("(\\d{1,2}\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec))")
        val raceDayPattern = Regex("(\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec))\\s+\\d{1,2}:\\d{2}")

        val raceDays = raceDayPattern.findAll(text).map { it.groupValues[1] }.toList()

        for ((i, name) in gpNames.withIndex()) {
            val circuit = gpCircuits.getOrElse(i) { "" }
            val dateRange = if (i < raceDays.size) raceDays[i] else ""
            val isCompleted = isGpCompleted(name)
            events.add(CalendarEvent(
                name = name,
                circuit = circuit,
                dateRange = dateRange,
                startDate = dateRange,
                endDate = dateRange,
                isCompleted = isCompleted,
                round = i + 1
            ))
        }
        return events
    }

    private fun isGpCompleted(name: String): Boolean {
        val order = listOf(
            "Thailand GP" to "2026-03-01", "Brazil GP" to "2026-03-22",
            "Americas GP" to "2026-03-29", "Spanish GP" to "2026-04-26",
            "French GP" to "2026-05-10", "Catalan GP" to "2026-05-17",
            "Italian GP" to "2026-05-31", "Hungarian GP" to "2026-06-07",
            "Czech GP" to "2026-06-21", "Dutch GP" to "2026-06-28",
            "German GP" to "2026-07-12", "British GP" to "2026-08-09",
            "Aragon GP" to "2026-08-30", "San Marino GP" to "2026-09-13",
            "Austrian GP" to "2026-09-20", "Japanese GP" to "2026-10-04",
            "Indonesian GP" to "2026-10-11", "Australian GP" to "2026-10-25",
            "Malaysian GP" to "2026-11-01", "Qatar GP" to "2026-11-08",
            "Portuguese GP" to "2026-11-22", "Valencia GP" to "2026-11-29"
        )
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return try {
            val raceDate = sdf.parse(order.find { it.first == name }?.second ?: "2027-01-01")
            raceDate?.before(Date()) ?: false
        } catch (_: Exception) { false }
    }

    fun fetchWeekendSchedule(gpName: String): WeekendGP {
        return try {
            val doc = Jsoup.connect(SCHEDULE_URL).timeout(TIMEOUT).get()
            parseWeekendSchedule(doc, gpName)
        } catch (e: Exception) {
            WeekendGP(name = gpName, circuit = "", country = "", dateRange = "",
                sessions = getDefaultSessions())
        }
    }

    fun getDefaultSessions(): List<Session> {
        return listOf(
            Session("Free Practice 1", "FP1", "Viernes", "", "10:45"),
            Session("Practice", "Practice", "Viernes", "", "15:00"),
            Session("Free Practice 2", "FP2", "Sábado", "", "10:10"),
            Session("Qualifying 1", "Q1", "Sábado", "", "10:50"),
            Session("Qualifying 2", "Q2", "Sábado", "", "11:15"),
            Session("Sprint", "Sprint", "Sábado", "", "15:00"),
            Session("Warm Up", "WU", "Domingo", "", "09:40"),
            Session("Race", "Race", "Domingo", "", "14:00")
        )
    }

    fun parseWeekendSchedule(doc: Document, gpName: String): WeekendGP {
        val text = doc.body().text()
        val gpPrefix = gpName.replace("GP", "").trim()

        val sessionNames = listOf(
            "FREE PRACTICE 1", "PRACTICE", "FREE PRACTICE 2",
            "QUALIFYING 1", "QUALIFYING 2", "SPRINT", "WARM UP", "RACE"
        )
        val shortNames = listOf("FP1", "Practice", "FP2", "Q1", "Q2", "Sprint", "WU", "Race")
        val days = listOf("Viernes", "Viernes", "Sábado", "Sábado", "Sábado", "Sábado", "Domingo", "Domingo")

        // Find section for this GP and extract session data
        val gpIdx = listOf(
            "Thailand", "Brazil", "Americas", "Spanish", "French", "Catalan",
            "Italian", "Hungarian", "Czech", "Dutch", "German", "British",
            "Aragon", "San Marino", "Austrian", "Japanese", "Indonesian",
            "Australian", "Malaysian", "Qatar", "Portuguese", "Valencia"
        ).indexOfFirst { gpName.contains(it, ignoreCase = true) }

        val sessions = mutableListOf<Session>()
        val gpDates = mutableListOf<String>()

        // Find dates in text
        val dateRegex = Regex("(\\d{1,2}\\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec))")

        // Parse all sessions from text
        for (s in sessionNames) {
            val matcher = Regex("$s[^\\d]*(\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec))[^\\d]*(\\d{1,2}:\\d{2})").find(text)
            if (matcher != null) {
                val idx = sessionNames.indexOf(s)
                sessions.add(Session(
                    name = s.replace("FREE PRACTICE", "Free Practice")
                        .replace("QUALIFYING", "Qualifying")
                        .replace("WARM UP", "Warm Up"),
                    shortName = shortNames[idx],
                    day = days[idx],
                    date = matcher.groupValues[1],
                    time = matcher.groupValues[2],
                    isCompleted = false
                ))
            }
        }

        if (sessions.isEmpty()) {
            return WeekendGP(name = gpName, circuit = "", country = "", dateRange = "",
                sessions = getDefaultSessions())
        }

        val circuitMap = mapOf(
            "Thailand" to "Chang International Circuit",
            "Catalan" to "Circuit de Barcelona-Catalunya",
            "Spanish" to "Circuito de Jerez",
            "French" to "Le Mans Circuit Bugatti",
            "Italian" to "Mugello Circuit"
        )
        val circuit = circuitMap.entries.find { gpName.contains(it.key, ignoreCase = true) }?.value ?: ""

        return WeekendGP(
            name = gpName,
            circuit = circuit,
            country = "",
            dateRange = sessions.firstOrNull()?.date ?: "",
            sessions = sessions
        )
    }

    fun fetchStandings(): Pair<List<RiderStanding>, List<ManufacturerStanding>> {
        return try {
            val doc = Jsoup.connect(STANDINGS_URL).timeout(TIMEOUT).get()
            parseStandings(doc)
        } catch (e: Exception) {
            Pair(getHardcodedRiders(), getHardcodedManufacturers())
        }
    }

    fun parseStandings(doc: Document): Pair<List<RiderStanding>, List<ManufacturerStanding>> {
        val riders = mutableListOf<RiderStanding>()
        val manufacturers = mutableListOf<ManufacturerStanding>()

        val text = doc.body().text()

        // Parse rider standings from table
        val riderPattern = Regex("(\\d+)\\s+([A-Z]\\.\\s*[A-Za-zÀ-ÿ]+)\\s*([A-Za-zÀ-ÿ0-9\\s]+?)\\s+(\\d{2,3})")
        var pos = 1
        for (match in riderPattern.findAll(text)) {
            val rider = match.groupValues[2].trim()
            val team = match.groupValues[3].trim()
            val points = match.groupValues[4].toIntOrNull() ?: 0
            if (riders.size < 26) {
                riders.add(RiderStanding(pos, rider, team, "", points))
                pos++
            }
        }

        // If parsing failed, use hardcoded data
        if (riders.isEmpty()) return Pair(getHardcodedRiders(), getHardcodedManufacturers())

        // Parse manufacturers from remaining text
        val manuPattern = Regex("(Aprilia|Ducati|KTM|Honda|Yamaha)(?:.*?)(\\d{2,3})")
        for (match in manuPattern.findAll(text)) {
            val mfr = match.groupValues[1]
            val pts = match.groupValues[2].toIntOrNull() ?: 0
            if (manufacturers.none { it.manufacturer == mfr }) {
                manufacturers.add(ManufacturerStanding(manufacturers.size + 1, mfr, pts))
            }
        }

        if (manufacturers.isEmpty()) {
            manufacturers.addAll(getHardcodedManufacturers())
        }

        return Pair(riders, manufacturers)
    }

    fun fetchQ1Results(doc: Document): List<QualifyingResult> {
        return emptyList() // Needs session-specific URL
    }

    fun computeRiderHistory(standings: List<RiderStanding>): List<RiderHistory> {
        // Simplified: return current points as single data point
        return standings.take(5).map { RiderHistory(it.rider, listOf(it.points)) }
    }

    fun computeManufacturerHistory(standings: List<ManufacturerStanding>): List<ManufacturerHistory> {
        return standings.map { ManufacturerHistory(it.manufacturer, listOf(it.points)) }
    }

    private fun getHardcodedRiders(): List<RiderStanding> = listOf(
        RiderStanding(1, "M. Bezzecchi", "Aprilia Racing", "Aprilia", 128),
        RiderStanding(2, "J. Martin", "Aprilia Racing", "Aprilia", 127),
        RiderStanding(3, "F. Di Giannantonio", "VR46 Racing", "Ducati", 84),
        RiderStanding(4, "P. Acosta", "Red Bull KTM", "KTM", 83),
        RiderStanding(5, "A. Ogura", "Trackhouse Racing", "Aprilia", 67),
        RiderStanding(6, "R. Fernández", "Trackhouse Racing", "Aprilia", 62),
        RiderStanding(7, "M. Marquez", "Ducati Team", "Ducati", 57),
        RiderStanding(8, "A. Marquez", "Gresini Racing", "Ducati", 55),
        RiderStanding(9, "F. Bagnaia", "Ducati Team", "Ducati", 43),
        RiderStanding(10, "E. Bastianini", "Tech 3", "KTM", 39),
        RiderStanding(11, "L. Marini", "Honda HRC", "Honda", 33),
        RiderStanding(12, "J. Zarco", "Team LCR", "Honda", 29),
        RiderStanding(13, "B. Binder", "Red Bull KTM", "KTM", 28),
        RiderStanding(14, "F. Aldeguer", "Gresini Racing", "Ducati", 27),
        RiderStanding(15, "F. Morbidelli", "VR46 Racing", "Ducati", 27),
        RiderStanding(16, "F. Quartararo", "Yamaha Racing", "Yamaha", 26),
        RiderStanding(17, "D. Moreira", "Team LCR", "Honda", 10),
        RiderStanding(18, "J. Mir", "Honda HRC", "Honda", 8),
        RiderStanding(19, "A. Rins", "Yamaha Racing", "Yamaha", 7),
        RiderStanding(20, "T. Razgatlioglu", "Pramac Racing", "Yamaha", 4),
        RiderStanding(21, "J. Miller", "Pramac Racing", "Yamaha", 1),
        RiderStanding(22, "M. Viñales", "Tech 3", "KTM", 0)
    )

    private fun getHardcodedManufacturers(): List<ManufacturerStanding> = listOf(
        ManufacturerStanding(1, "Aprilia", 255),
        ManufacturerStanding(2, "Ducati", 211),
        ManufacturerStanding(3, "KTM", 150),
        ManufacturerStanding(4, "Honda", 69),
        ManufacturerStanding(5, "Yamaha", 38)
    )

    fun computeCompletedRounds(): Int {
        val raceDates = listOf(
            "2026-03-01", "2026-03-22", "2026-03-29", "2026-04-26",
            "2026-05-10", "2026-05-17"
        )
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        var count = 0
        val now = Date()
        for (d in raceDates) {
            try {
                if (sdf.parse(d)?.before(now) == true) count++
            } catch (_: Exception) {}
        }
        return count
    }
}
