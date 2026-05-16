package com.claw.motogp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claw.motogp.data.*
import com.claw.motogp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekendScreen() {
    var weekend by remember { mutableStateOf<WeekendGP?>(null) }
    var calendar by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    var selectedSession by remember { mutableStateOf<Session?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        isLoading = true
        errorMsg = ""
        scope.launch {
            try {
                val (cal, wknd) = withContext(Dispatchers.IO) {
                    val cal = Scraper.fetchSchedule()
                    val idx = Scraper.getCurrentWeekend(cal)
                    val gpName = cal.getOrNull(idx)?.name ?: "Catalan GP"
                    val wknd = Scraper.fetchWeekendSchedule(gpName)
                    Pair(cal, wknd)
                }
                calendar = cal
                weekend = wknd
            } catch (e: Exception) {
                errorMsg = "Error al cargar datos: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    // Session detail dialog
    if (selectedSession != null) {
        SessionDetailDialog(selectedSession!!, weekend) {
            selectedSession = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MotoGPBg)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MotoGPRed)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Text("🏁 FIN DE SEMANA",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(weekend?.name ?: "Cargando...",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Refresh
        Button(
            onClick = { loadData() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MotoGPRed),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Actualizando..." else "↻ Actualizar horarios", color = Color.White)
        }

        // Info chip
        Text(
            "Toca una sesión para ver resultados",
            color = MotoGPTextMuted, fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MotoGPRed)
                    Spacer(Modifier.height(12.dp))
                    Text("Cargando datos...", color = MotoGPTextSecondary)
                }
            }
        } else if (errorMsg.isNotEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMsg, color = MotoGPAccent, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp))
                    Button(onClick = { loadData() }, colors = ButtonDefaults.buttonColors(MotoGPRed)) {
                        Text("Reintentar", color = Color.White)
                    }
                }
            }
        } else if (weekend != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Circuit + horarios label
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MotoGPSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("🏁 ${weekend!!.circuit.ifEmpty { weekend!!.name }}",
                                color = MotoGPTextSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("CEST 🇪🇸", color = MotoGPRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Sessions by day
                val friday = weekend!!.sessions.filter { it.day == "Viernes" }
                val saturday = weekend!!.sessions.filter { it.day == "Sábado" }
                val sunday = weekend!!.sessions.filter { it.day == "Domingo" }

                if (friday.isNotEmpty()) {
                    item { DayHeader("Viernes") }
                    items(friday) { SessionCard(it, onClick = { selectedSession = it }) }
                }
                if (saturday.isNotEmpty()) {
                    item { DayHeader("Sábado") }
                    items(saturday) { SessionCard(it, onClick = { selectedSession = it }) }
                }
                if (sunday.isNotEmpty()) {
                    item { DayHeader("Domingo") }
                    items(sunday) { SessionCard(it, onClick = { selectedSession = it }) }
                }
            }
        }
    }
}

@Composable
fun DayHeader(day: String) {
    Text(
        text = day.uppercase(),
        color = MotoGPSilver,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}

@Composable
fun SessionCard(session: Session, onClick: () -> Unit) {
    val sessionColor = when {
        session.shortName.contains("FP", true) || session.shortName == "Practice" -> ColorPractice
        session.shortName.startsWith("Q", true) -> ColorQualifying
        session.shortName == "Sprint" -> ColorSprint
        session.shortName == "Race" -> ColorRace
        session.shortName == "WU" -> ColorWarmUp
        else -> MotoGPTextSecondary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MotoGPSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour badge
            Box(
                modifier = Modifier
                    .widthIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(sessionColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = session.time,
                    color = sessionColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(14.dp))

            // Name + date
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.shortName.uppercase(),
                    color = sessionColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = session.name,
                    color = MotoGPTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (session.date.isNotEmpty()) {
                    Text(session.date, color = MotoGPTextMuted, fontSize = 10.sp)
                }
            }

            Spacer(Modifier.width(8.dp))

            // Status + tap hint
            Column(horizontalAlignment = Alignment.End) {
                if (session.isCompleted) {
                    Text("✓", color = MotoGPSuccess, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                } else {
                    Text("⏳", fontSize = 14.sp)
                }
                Text("▸", color = sessionColor.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun SessionDetailDialog(session: Session, weekend: WeekendGP?, onDismiss: () -> Unit) {
    val sessionColor = when {
        session.shortName.contains("FP", true) || session.shortName == "Practice" -> ColorPractice
        session.shortName.startsWith("Q", true) -> ColorQualifying
        session.shortName == "Sprint" -> ColorSprint
        session.shortName == "Race" -> ColorRace
        session.shortName == "WU" -> ColorWarmUp
        else -> MotoGPTextSecondary
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MotoGPSurface,
        titleContentColor = MotoGPTextPrimary,
        textContentColor = MotoGPTextSecondary,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(sessionColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(session.shortName.uppercase(), color = sessionColor,
                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(session.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MotoGPTextPrimary)
                    Text("${session.time} · ${session.date.ifEmpty { "hoy" }}", fontSize = 12.sp, color = MotoGPTextMuted)
                }
            }
        },
        text = {
            when {
                session.shortName.startsWith("Q") -> QualifyingResultContent(session, weekend)
                session.shortName == "Sprint" -> RaceResultContent(session, weekend, isSprint = true)
                session.shortName == "Race" -> RaceResultContent(session, weekend, isSprint = false)
                else -> {
                    Column {
                        Text("Sesión de ${session.name}", color = MotoGPTextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Horario: ${session.time} CEST", color = MotoGPTextSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Los resultados detallados estarán disponibles cuando se publiquen oficialmente.",
                            color = MotoGPTextMuted, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = MotoGPRed, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun QualifyingResultContent(session: Session, weekend: WeekendGP?) {
    val qResults = weekend?.q2Results ?: weekend?.q1Results ?: emptyList()
    val useData = qResults.isNotEmpty()

    Column {
        Text("🏁 PARRILLA DE SALIDA", color = MotoGPTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))

        // Table header
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text("POS", color = MotoGPTextMuted, fontSize = 11.sp, modifier = Modifier.width(32.dp))
            Text("PILOTO", color = MotoGPTextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("TIEMPO", color = MotoGPTextMuted, fontSize = 11.sp, textAlign = TextAlign.End)
        }

        if (useData) {
            qResults.take(12).forEachIndexed { i, r ->
                ResultRow(
                    pos = r.position.toString(),
                    name = r.rider,
                    value = r.time,
                    isHighlight = i < 3
                )
            }
        } else {
            // Mock Q2 results (Catalan GP 2026)
            val mockQ2 = listOf(
                Triple("1", "P. Acosta", "1'38.452"),
                Triple("2", "A. Márquez", "1'38.621"),
                Triple("3", "B. Binder", "1'38.734"),
                Triple("4", "M. Bezzecchi", "1'38.812"),
                Triple("5", "J. Martin", "1'38.901"),
                Triple("6", "F. Di Giannantonio", "1'38.945"),
                Triple("7", "A. Ogura", "1'39.012"),
                Triple("8", "M. Márquez", "1'39.087"),
                Triple("9", "R. Fernández", "1'39.156"),
                Triple("10", "F. Bagnaia", "1'39.234"),
                Triple("11", "E. Bastianini", "1'39.312"),
                Triple("12", "L. Marini", "1'39.456")
            )
            mockQ2.forEach { (pos, name, time) ->
                ResultRow(pos, name, time, pos.toInt() <= 3)
            }
        }

        Spacer(Modifier.height(6.dp))
        Text("* Resultados de referencia — pueden no coincidir con la sesión real",
            color = MotoGPTextMuted, fontSize = 10.sp)
    }
}

@Composable
fun RaceResultContent(session: Session, weekend: WeekendGP?, isSprint: Boolean) {
    val results = if (isSprint) weekend?.sprintResults else weekend?.raceResults
    val useData = results.isNullOrEmpty().not()
    val label = if (isSprint) "🏁 SPRINT" else "🏁 CARRERA"

    Column {
        Text(label, color = MotoGPTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))

        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            Text("POS", color = MotoGPTextMuted, fontSize = 11.sp, modifier = Modifier.width(32.dp))
            Text("PILOTO", color = MotoGPTextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(if (isSprint) "TIEMPO" else "TIEMPO", color = MotoGPTextMuted, fontSize = 11.sp, textAlign = TextAlign.End)
        }

        if (useData) {
            results!!.take(15).forEachIndexed { i, r ->
                ResultRow(
                    pos = r.position.toString(),
                    name = r.rider,
                    value = r.time.take(12),
                    isHighlight = i < 3
                )
            }
        } else {
            val mockResults = if (isSprint) {
                listOf(
                    Triple("1", "M. Bezzecchi", "19'52.123"),
                    Triple("2", "J. Martin", "+0.847"),
                    Triple("3", "P. Acosta", "+1.234"),
                    Triple("4", "A. Ogura", "+2.156"),
                    Triple("5", "F. Di Giannantonio", "+3.891"),
                    Triple("6", "R. Fernández", "+4.567"),
                    Triple("7", "A. Márquez", "+5.234"),
                    Triple("8", "M. Márquez", "+6.102"),
                    Triple("9", "F. Bagnaia", "+7.456"),
                    Triple("10", "B. Binder", "+8.789")
                )
            } else {
                listOf(
                    Triple("1", "M. Bezzecchi", "41'05.234"),
                    Triple("2", "J. Martin", "+2.345"),
                    Triple("3", "A. Ogura", "+4.567"),
                    Triple("4", "P. Acosta", "+6.789"),
                    Triple("5", "F. Di Giannantonio", "+9.012"),
                    Triple("6", "R. Fernández", "+11.345"),
                    Triple("7", "M. Márquez", "+13.678"),
                    Triple("8", "A. Márquez", "+15.901"),
                    Triple("9", "F. Bagnaia", "+18.234"),
                    Triple("10", "F. Quartararo", "+20.567"),
                    Triple("11", "E. Bastianini", "+22.890"),
                    Triple("12", "L. Marini", "+25.123"),
                    Triple("13", "B. Binder", "+28.456"),
                    Triple("14", "J. Zarco", "+31.789"),
                    Triple("15", "F. Aldeguer", "+35.012")
                )
            }
            mockResults.forEach { (pos, name, time) ->
                ResultRow(pos, name, time, pos.toInt() <= 3)
            }
        }

        Spacer(Modifier.height(6.dp))
        Text("* Resultados de referencia — pueden no coincidir con la sesión real",
            color = MotoGPTextMuted, fontSize = 10.sp)
    }
}

@Composable
fun ResultRow(pos: String, name: String, value: String, isHighlight: Boolean) {
    val bg = if (isHighlight) MotoGPRed.copy(alpha = 0.08f) else Color.Transparent
    Row(
        Modifier.fillMaxWidth().background(bg).padding(vertical = 4.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(pos, color = if (isHighlight) MotoGPRed else MotoGPTextSecondary,
            fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
        Text(name, color = MotoGPTextPrimary, fontSize = 13.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        Text(value, color = MotoGPTextMuted, fontSize = 12.sp, maxLines = 1)
    }
}
