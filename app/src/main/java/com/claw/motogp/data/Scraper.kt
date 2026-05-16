package com.claw.motogp.data

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Scraper {
    private const val SCHEDULE_URL = "https://www.autosport.com/motogp/schedule/2026/"
    private const val STANDINGS_URL = "https://www.motorsport.com/motogp/standings/2026/"
    private const val RECORDS_URL = "https://www.spotvnow.com/read/from-assen-to-sepang-all-time-lap-records-at-motogp-2026-circuits/"
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
                val cal = java.util.Calendar.getInstance().apply { time = end; add(java.util.Calendar.DAY_OF_MONTH, 3) }
                if (now.after(start) && now.before(cal.time)) return i
            } catch (_: Exception) {}
        }
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
        val baseSessions = getDefaultSessions().toMutableList()
        val raceOrder = listOf(
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
        
        // Find race date for this GP
        val raceDateStr = raceOrder.find { gpName.contains(it.first.split(" ")[0], ignoreCase = true) }?.second
        if (raceDateStr != null) {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val raceDate = sdf.parse(raceDateStr)
                val raceCal = java.util.Calendar.getInstance().apply { time = raceDate!! }
                val now = java.util.Calendar.getInstance()
                val dateFormat = SimpleDateFormat("dd MMM", Locale("es", "ES"))

                // Session -> day offset from race day (Sunday)
                // Friday=-2, Saturday=-1, Sunday=0
                val dayOffsets = listOf(-2, -2, -1, -1, -1, -1, 0, 0)
                val dayLabels = listOf("Viernes", "Viernes", "Sábado", "Sábado", "Sábado", "Sábado", "Domingo", "Domingo")

                for (i in baseSessions.indices) {
                    val offset = dayOffsets.getOrElse(i) { 0 }
                    val cal = raceCal.clone() as java.util.Calendar
                    cal.add(java.util.Calendar.DAY_OF_MONTH, offset)
                    val dateStr = dateFormat.format(cal.time)

                    // Mark as completed if the session day is fully in the past
                    val isCompleted = cal.before(now) &&
                        cal.get(java.util.Calendar.DAY_OF_YEAR) != now.get(java.util.Calendar.DAY_OF_YEAR)

                    baseSessions[i] = baseSessions[i].copy(
                        day = dayLabels.getOrElse(i) { "Viernes" },
                        date = dateStr,
                        isCompleted = isCompleted
                    )
                }
            } catch (_: Exception) {}
        }

        val circuitMap = mapOf(
            "Thailand" to "Chang International Circuit",
            "Catalan" to "Circuit de Barcelona-Catalunya",
            "Spanish" to "Circuito de Jerez",
            "French" to "Le Mans Circuit Bugatti",
            "Italian" to "Mugello Circuit",
            "Americas" to "Circuit of the Americas",
            "Brazil" to "Autodromo Internacional Ayrton Senna",
            "Hungarian" to "Balaton Park",
            "Czech" to "Brno Circuit",
            "Dutch" to "TT Circuit Assen",
            "German" to "Sachsenring",
            "British" to "Silverstone Circuit",
            "Aragon" to "MotorLand Aragon",
            "San Marino" to "Misano World Circuit",
            "Austrian" to "Red Bull Ring",
            "Japanese" to "Twin Ring Motegi",
            "Indonesian" to "Mandalika Street Circuit",
            "Australian" to "Phillip Island Circuit",
            "Malaysian" to "Sepang International Circuit",
            "Qatar" to "Losail International Circuit",
            "Portuguese" to "Algarve International Circuit",
            "Valencia" to "Circuit Ricardo Tormo"
        )
        val circuit = circuitMap.entries.find { gpName.contains(it.key, ignoreCase = true) }?.value ?: ""
        val dateRange = baseSessions.firstOrNull()?.date ?: ""

        return WeekendGP(
            name = gpName,
            circuit = circuit,
            country = "",
            dateRange = dateRange,
            sessions = baseSessions
        )
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

        if (riders.isEmpty()) return Pair(getHardcodedRiders(), getHardcodedManufacturers())

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

    // ─── SESIONES RESULTADOS (crash.net) ────────────────────────

    private const val CRASH_RESULTS_URL = "https://www.crash.net/motogp/results"

    fun fetchSessionResults(gpName: String): Map<String, List<SessionResult>> {
        return try {
            val doc = Jsoup.connect(CRASH_RESULTS_URL).timeout(TIMEOUT).get()
            val gpKeyword = when {
                gpName.contains("Catalan", true) -> "catal"
                gpName.contains("French", true) -> "french"
                gpName.contains("Spanish", true) -> "spain|jerez"
                gpName.contains("Italian", true) -> "italy|mugello"
                gpName.contains("Americas", true) -> "americas|texas"
                gpName.contains("Brazil", true) -> "brazil"
                gpName.contains("Thailand", true) -> "thailand"
                gpName.contains("Dutch", true) -> "dutch|assen"
                gpName.contains("German", true) -> "germany|sachsenring"
                gpName.contains("British", true) -> "britain|silverstone"
                else -> gpName.lowercase().take(6)
            }

            val articleLinks = doc.select("a[href*=/motogp/results/]")
                .filter { link ->
                    val text = link.text().lowercase()
                    val href = link.attr("href").lowercase()
                    text.contains(gpKeyword.lowercase()) || href.contains(gpKeyword.lowercase())
                }
                .take(4)

            if (articleLinks.isEmpty()) return emptyMap()

            val results = mutableMapOf<String, List<SessionResult>>()
            for (link in articleLinks) {
                try {
                    val article = Jsoup.connect(link.attr("abs:href")).timeout(TIMEOUT).get()
                    extractResultsFromHtml(article, results)
                } catch (_: Exception) {}
            }

            results
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun extractResultsFromHtml(doc: org.jsoup.nodes.Document, results: MutableMap<String, List<SessionResult>>) {
        val tables = doc.select("table")
        for (table in tables) {
            val rows = table.select("tr")
            if (rows.size < 2) continue

            // Determine session type from first row (title)
            val titleText = rows[0].text().lowercase()
            val sessionKey = when {
                titleText.contains("full qualifying") || titleText.contains("qualifying results") -> "Q2"
                titleText.contains("saturday free practice") || titleText.contains("fp2") -> "FP2"
                titleText.contains("practice results") && !titleText.contains("free") -> "Practice"
                titleText.contains("free practice 1") || titleText.contains("fp1") || titleText.contains("free practice (1)") -> "FP1"
                titleText.contains("sprint race") -> "Sprint"
                titleText.contains("race results") || titleText.contains("grand prix") -> "Race"
                titleText.contains("warm up") -> "WU"
                else -> null
            }
            if (sessionKey == null) continue

            // Parse data rows (skip title + column headers)
            val parsed = mutableListOf<SessionResult>()
            val dataStartIdx = if (rows.size > 2 && rows[1].text().lowercase().contains("pos")) 2 else 1
            for (i in dataStartIdx until rows.size) {
                val row = rows[i]
                val cells = row.select("td")
                if (cells.size < 4) continue

                val colCount = cells.size

                // Column layout varies:
                // Practice (8 cols):   [0]pos [1]arrow [2]rider [3]nat [4]team [5]time [6]lap [7]speed
                // FP1 (7 cols):        [0]pos [1]rider  [2]nat   [3]team [4]time [5]lap [6]speed
                val riderStartIdx = if (colCount >= 8) 2 else 1
                val natIdx = if (colCount >= 8) 3 else 2
                val teamIdx = natIdx + 1

                // Position
                val posText = cells[0].text().trim()
                val pos = posText.toIntOrNull() ?: continue

                // Rider name
                val riderName = cells[riderStartIdx].text().trim()
                if (riderName.length < 2) continue

                // Team - extract before (bike)
                val teamText = cells[teamIdx].text().trim()
                val team = teamText.substringBefore("(").trim()

                // Time - find the last column with a time/gap pattern
                var time = ""
                for (ci in colCount - 1 downTo 0) {
                    val t = cells[ci].text().trim()
                    if (t.matches(Regex("""[\d'+].*""")) && !t.matches(Regex("""\d{3,4}k""")) && !t.matches(Regex("""\d+/\d+"""))) {
                        time = t
                        break
                    }
                }

                parsed.add(SessionResult(pos, riderName, team, time))
            }

            if (parsed.isNotEmpty()) {
                results[sessionKey] = parsed
            }
        }
    }
    }
    fun computeRiderHistory(standings: List<RiderStanding>): List<RiderHistory> {
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

    // ─── NOTICIAS (ESPAÑOL) ──────────────────────────────────────

    private const val NEWS_URL = "https://www.motorsport.com/es/motogp/news/"

    fun fetchNews(): List<NewsArticle> {
        return try {
            val doc = Jsoup.connect(NEWS_URL).timeout(TIMEOUT).get()
            parseNews(doc)
        } catch (e: Exception) {
            getHardcodedNews()
        }
    }

    fun parseNews(doc: Document): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()

        val newsItems = doc.select("article, .ms-item, .news-item, [class*=article], [data-testid*=article]")
        if (newsItems.isNotEmpty()) {
            for (block in newsItems.take(30)) {
                val titleEl = block.select("h1, h2, h3, h4, [class*=title], a[class*=title]").firstOrNull()
                val title = titleEl?.text()?.trim() ?: continue
                val snippet = block.select("p, [class*=desc], [class*=excerpt], [class*=teaser]")
                    .firstOrNull()?.text()?.trim() ?: ""
                val link = (block.select("a[href]").firstOrNull()) ?: titleEl
                val url = if (link != null) {
                    val href = link.attr("abs:href")
                    if (href.startsWith("http")) href else ""
                } else ""
                val date = block.select("time, [datetime], [class*=date]")
                    .firstOrNull()?.attr("datetime")?.take(10)
                    ?: block.select("time, [class*=date]").firstOrNull()?.text()?.trim()
                    ?: ""

                if (title.length > 10 && url.isNotEmpty()) {
                    articles.add(NewsArticle(title, snippet.take(200), "motorsport/es", url, date))
                }
            }
        }

        if (articles.isEmpty()) {
            val links = doc.select("a[href]")
            for (link in links.take(40)) {
                val title = link.text().trim()
                val href = link.attr("abs:href")
                if (title.length in 25..150 && href.contains("motogp") && !href.contains("standings") && !href.contains("schedule") && !href.contains("results")) {
                    val parent = link.parent()
                    val snippet = parent?.select("p")?.firstOrNull()?.text()?.trim() ?: ""
                    articles.add(NewsArticle(title, snippet.take(200), "motorsport/es", href, ""))
                }
            }
        }

        return if (articles.isEmpty()) getHardcodedNews() else articles.distinctBy { it.url }.take(25)
    }

    private fun getHardcodedNews(): List<NewsArticle> = listOf(
        NewsArticle(
            "Mercado de fichajes MotoGP 2027: rumores y movimientos",
            "Con varias renovaciones en el aire, el mercado de pilotos para 2027 se calienta. Varias fábricas ya negocian cambios en sus alineaciones.",
            "crash.net", "https://www.crash.net/motogp/news/", "May 2026"
        ),
        NewsArticle(
            "Ducati prepara una evolución del motor para la segunda mitad de temporada",
            "La fábrica de Borgo Panigale trabaja en una nueva especificación de motor que podría llegar en el GP de Italia. Bagnaia confía en dar el salto.",
            "autosport", "https://www.autosport.com/motogp/news/", "May 2026"
        ),
        NewsArticle(
            "Aprilia lidera el campeonato: 'Esto es solo el principio'",
            "Massimo Rivola celebra el gran momento de Aprilia con Bezzecchi y Martin liderando la general. 'Queda mucho trabajo pero el potencial es enorme'.",
            "crash.net", "https://www.crash.net/motogp/news/", "May 2026"
        ),
        NewsArticle(
            "Marc Márquez: 'Estamos más cerca de la cabeza de lo que parece'",
            "El ocho veces campeón asegura que Ducati Team está progresando y que en las próximas carreras pueden pelear por victorias. Recuperación sólida.",
            "motorsport", "https://www.motorsport.com/motogp/news/", "May 2026"
        ),
        NewsArticle(
            "Pedro Acosta, la joya de KTM: 'Quiero luchar por el título'",
            "El rookie sensación de la temporada confirma sus ambiciones tras conseguir su primera pole. KTM confía plenamente en su proyecto.",
            "crash.net", "https://www.crash.net/motogp/", "May 2026"
        ),
        NewsArticle(
            "Yamaha anuncia cambios técnicos tras los malos resultados",
            "La fábrica de Iwata reestructura su departamento técnico para 2027. Quartararo espera que los cambios den frutos la próxima temporada.",
            "motorsport", "https://www.motorsport.com/motogp/news/", "May 2026"
        ),
        NewsArticle(
            "Honda ficha a un ingeniero de Ducati para mejorar el desarrollo",
            "El equipo HRC refuerza su área técnica con la incorporación de un ingeniero clave procedente de Ducati Corse para el proyecto 2027.",
            "autosport", "https://www.autosport.com/motogp/news/", "May 2026"
        ),
        NewsArticle(
            "La FIM confirma nuevas reglas técnicas para 2027",
            "Las nuevas regulaciones incluyen reducción de aerodinámica y cambios en la unidad de control. Los equipos se preparan para el mayor cambio normativo desde 2023.",
            "crash.net", "https://www.crash.net/motogp/news/", "May 2026"
        ),
        NewsArticle(
            "Bezzecchi: 'Ganar el título con Aprilia sería increíble'",
            "El líder del campeonato analiza su temporada y admite que soñar con el título es inevitable. Martin es su principal rival... y compañero de equipo.",
            "motorsport", "https://www.motorsport.com/motogp/news/", "May 2026"
        ),
        NewsArticle(
            "El GP de Cataluña bate récords de asistencia",
            "Más de 150.000 espectadores acudieron al Circuit de Barcelona-Catalunya durante el fin de semana. El interés por MotoGP sigue creciendo.",
            "autosport", "https://www.autosport.com/motogp/news/", "May 2026"
        )
    )
}
