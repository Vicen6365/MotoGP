package com.claw.motogp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
                    // Also load session results in background
                    val results = Scraper.fetchSessionResults(gpName)
                    Pair(cal, wknd.copy(sessionResults = results))
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
    if (selectedSession != null && weekend != null) {
        SessionDetailDialog2(selectedSession!!, weekend!!, onDismiss = { selectedSession = null },
            onRefresh = { session ->
                scope.launch {
                    val results = withContext(Dispatchers.IO) {
                        Scraper.fetchSessionResults(weekend!!.name)
                    }
                    weekend = weekend!!.copy(sessionResults = results)
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().background(MotoGPBg)
    ) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth().background(MotoGPRed).padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Text("🏁 FIN DE SEMANA",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text(weekend?.name ?: "Cargando...",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Refresh
        Button(
            onClick = { loadData() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MotoGPRed),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Actualizando..." else "↻ Actualizar horarios", color = Color.White)
        }

        // Info chip
        Text(
            "Toca una sesión para ver tiempos",
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
                // Circuit card
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
        color = MotoGPSilver, fontSize = 13.sp,
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
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MotoGPSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hour badge (adaptive width)
            Box(
                modifier = Modifier
                    .widthIn(min = 48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(sessionColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(session.time, color = sessionColor,
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
            }

            Spacer(Modifier.width(14.dp))

            // Name + date
            Column(modifier = Modifier.weight(1f)) {
                Text(session.shortName.uppercase(), color = sessionColor,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(session.name, color = MotoGPTextSecondary,
                    fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
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

// ─── SESSION DETAIL DIALOG WITH REAL DATA + REFRESH ─────────

@Composable
fun SessionDetailDialog2(
    session: Session,
    weekend: WeekendGP,
    onDismiss: () -> Unit,
    onRefresh: (Session) -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val sessionColor = when {
        session.shortName.contains("FP", true) || session.shortName == "Practice" -> ColorPractice
        session.shortName.startsWith("Q", true) -> ColorQualifying
        session.shortName == "Sprint" -> ColorSprint
        session.shortName == "Race" -> ColorRace
        session.shortName == "WU" -> ColorWarmUp
        else -> MotoGPTextSecondary
    }

    // Get results for this session from the WeekendGP cache
    val cachedResults = weekend.sessionResults[session.shortName] ?: emptyList()
    var displayedResults by remember(session.shortName, weekend) { mutableStateOf(cachedResults) }

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
                    Text("${session.time} · ${session.date.ifEmpty { "hoy" }}",
                        fontSize = 12.sp, color = MotoGPTextMuted)
                }
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp)) {
            if (isRefreshing) {
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MotoGPRed, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Actualizando...", color = MotoGPTextMuted, fontSize = 12.sp)
                    }
                }
            } else if (displayedResults.isEmpty()) {
                Column {
                    Text("Esta sesión aún no tiene resultados publicados.",
                        color = MotoGPTextSecondary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Prueba a tocar 'Actualizar' o revisa más tarde cuando la sesión haya terminado.",
                        color = MotoGPTextMuted, fontSize = 12.sp)
                }
            } else {
                SessionResultsTable(displayedResults, session.shortName)
            }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    isRefreshing = true
                    onRefresh(session)
                    // Refresh re-triggered from parent will set new weekend
                    // For immediate feedback, try fetching directly
                }) {
                    Text("↻ Actualizar", color = MotoGPRed, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = MotoGPTextMuted)
                }
            }
        }
    )
}

@Composable
fun SessionResultsTable(results: List<SessionResult>, sessionType: String) {
    val isQuali = sessionType.startsWith("Q")
    val isRace = sessionType == "Race" || sessionType == "Sprint"
    val label = when {
        sessionType == "FP1" || sessionType == "FP2" || sessionType == "Practice" -> "🏁 ENTRENOS"
        isQuali -> "🏁 CLASIFICACIÓN"
        sessionType == "Sprint" -> "🏁 SPRINT"
        sessionType == "Race" -> "🏁 CARRERA"
        else -> "🏁 RESULTADOS"
    }

    Column {
        Text(label, color = MotoGPTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))

        // Header row
        Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("#", color = MotoGPTextMuted, fontSize = 11.sp, modifier = Modifier.width(28.dp))
            Text("PILOTO", color = MotoGPTextMuted, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("TIEMPO", color = MotoGPTextMuted, fontSize = 11.sp, modifier = Modifier.width(88.dp),
                textAlign = TextAlign.End)
        }

        results.forEach { r ->
            val bg = if (r.position <= 3) MotoGPRed.copy(alpha = 0.08f) else Color.Transparent
            val textColor = if (r.position <= 3) MotoGPRed else MotoGPTextPrimary
            Row(
                Modifier.fillMaxWidth().background(bg).padding(vertical = 5.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(r.position.toString(), color = textColor, fontSize = 13.sp,
                    fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(r.rider, color = MotoGPTextPrimary, fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (r.team.isNotEmpty()) {
                        Text(r.team, color = MotoGPTextMuted, fontSize = 10.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Text(r.time, color = MotoGPTextSecondary, fontSize = 12.sp,
                    modifier = Modifier.width(88.dp), textAlign = TextAlign.End)
            }
        }

        Spacer(Modifier.height(6.dp))
        Text("* Datos de crash.net · Toca Actualizar para refrescar",
            color = MotoGPTextMuted, fontSize = 10.sp)
    }
}
