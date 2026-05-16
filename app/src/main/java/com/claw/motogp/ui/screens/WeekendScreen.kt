package com.claw.motogp.ui.screens

import androidx.compose.foundation.background
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MotoGPBg)
    ) {
        // Header
        MotoGPHeader(weekend?.name ?: "Cargando...", isToday = false)

        // Refresh button
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

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MotoGPRed)
                    Spacer(Modifier.height(12.dp))
                    Text("Cargando datos...", color = MotoGPTextSecondary)
                }
            }
        } else if (errorMsg.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Circuit info
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MotoGPSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            if (weekend!!.circuit.isNotEmpty()) {
                                Text("🏁 ${weekend!!.circuit}", color = MotoGPTextSecondary, fontSize = 14.sp)
                            }
                            Text(
                                "Horarios CEST 🇪🇸",
                                color = MotoGPRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // Sessions by day
                val friday = weekend!!.sessions.filter { it.day == "Viernes" }
                val saturday = weekend!!.sessions.filter { it.day == "Sábado" }
                val sunday = weekend!!.sessions.filter { it.day == "Domingo" }

                if (friday.isNotEmpty()) {
                    item { DayHeader("Viernes") }
                    items(friday) { SessionCard(it) }
                }
                if (saturday.isNotEmpty()) {
                    item { DayHeader("Sábado") }
                    items(saturday) { SessionCard(it) }
                }
                if (sunday.isNotEmpty()) {
                    item { DayHeader("Domingo") }
                    items(sunday) { SessionCard(it) }
                }
            }
        }
    }
}

@Composable
fun MotoGPHeader(title: String, isToday: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MotoGPRed)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Text(
                text = if (isToday) "HOY" else "🏁 FIN DE SEMANA",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                text = title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
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
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
fun SessionCard(session: Session) {
    val sessionColor = when {
        session.shortName.contains("FP", true) || session.shortName == "Practice" -> ColorPractice
        session.shortName.startsWith("Q", true) -> ColorQualifying
        session.shortName == "Sprint" -> ColorSprint
        session.shortName == "Race" -> ColorRace
        session.shortName == "WU" -> ColorWarmUp
        else -> MotoGPTextSecondary
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MotoGPSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time badge
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(sessionColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = session.time,
                    color = sessionColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.shortName.uppercase(),
                    color = sessionColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = session.name,
                    color = MotoGPTextSecondary,
                    fontSize = 13.sp
                )
            }

            if (session.isCompleted) {
                Text("✓ LISTO", color = MotoGPSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("⏳", fontSize = 16.sp)
            }
        }
    }
}
