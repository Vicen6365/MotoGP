package com.claw.motogp.ui.screens

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claw.motogp.data.*
import com.claw.motogp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CalendarScreen() {
    val context = LocalContext.current
    var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var expandedGp by remember { mutableStateOf<Int?>(null) }
    val scope = rememberCoroutineScope()

    fun loadData() {
        isLoading = true
        scope.launch {
            events = withContext(Dispatchers.IO) { Scraper.fetchSchedule() }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Column(modifier = Modifier.fillMaxSize().background(MotoGPBg)) {
        Box(
            modifier = Modifier.fillMaxWidth().background(MotoGPRed).padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Text("📅 CALENDARIO 2026",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("22 Grandes Premios",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick = { loadData() },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MotoGPRed),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Cargando..." else "↻ Actualizar calendario", color = Color.White)
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MotoGPRed)
            }
        } else if (events.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se pudo cargar el calendario", color = MotoGPTextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Legend
                val currentIndex = events.indexOfFirst { !it.isCompleted }

                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(MotoGPSuccess))
                            Spacer(Modifier.width(4.dp))
                            Text("Pasado", color = MotoGPTextMuted, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(MotoGPRed))
                            Spacer(Modifier.width(4.dp))
                            Text("Actual", color = MotoGPTextMuted, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(MotoGPSurfaceVariant))
                            Spacer(Modifier.width(4.dp))
                            Text("Futuro", color = MotoGPTextMuted, fontSize = 11.sp)
                        }
                    }
                }

                itemsIndexed(events) { i, event ->
                    val isExpanded = expandedGp == i
                    GpCard(
                        event = event,
                        isExpanded = isExpanded,
                        isCurrent = i == currentIndex,
                        onToggle = { expandedGp = if (isExpanded) null else i },
                        onAddSession = { sessionName -> addSessionToCalendar(context, event, sessionName) }
                    )
                }
            }
        }
    }
}

@Composable
fun GpCard(
    event: CalendarEvent,
    isExpanded: Boolean,
    isCurrent: Boolean,
    onToggle: () -> Unit,
    onAddSession: (String) -> Unit
) {
    val bgColor = when {
        event.isCompleted -> MotoGPSurface
        isCurrent -> MotoGPSurfaceVariant
        else -> MotoGPSurface.copy(alpha = 0.5f)
    }
    val accentColor = when {
        event.isCompleted -> MotoGPSuccess
        isCurrent -> MotoGPRed
        else -> MotoGPTextMuted
    }
    val roundBg = when {
        event.isCompleted -> MotoGPSuccess.copy(alpha = 0.15f)
        isCurrent -> MotoGPRed.copy(alpha = 0.15f)
        else -> MotoGPTextMuted.copy(alpha = 0.08f)
    }
    val statusText = when {
        event.isCompleted -> "✓"
        isCurrent -> "◉"
        else -> "▸"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column {
            // Header row (always visible)
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Round number
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(roundBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        event.round.toString(),
                        color = accentColor,
                        fontWeight = FontWeight.Bold, fontSize = 16.sp
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(event.name,
                        color = MotoGPTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        if (event.dateRange.isNotEmpty()) "${event.dateRange} · ${event.circuit}"
                        else event.circuit,
                        color = MotoGPTextMuted, fontSize = 11.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }

                // Status badge
                Text(statusText, color = accentColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            // Expanded sessions panel
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                    Divider(color = MotoGPTextMuted.copy(alpha = 0.2f))
                    Spacer(Modifier.height(8.dp))
                    Text("SESIONES", color = MotoGPTextMuted, fontSize = 10.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(Modifier.height(4.dp))

                    GpSessions.all.forEach { session ->
                        GpSessionRow(
                            session = session,
                            onAdd = { onAddSession(session.shortName) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GpSessionRow(session: CalendarSession, onAdd: () -> Unit) {
    val sessionColor = when {
        session.shortName.contains("FP", true) || session.shortName == "Practice" -> ColorPractice
        session.shortName.startsWith("Q", true) -> ColorQualifying
        session.shortName == "Sprint" -> ColorSprint
        session.shortName == "Race" -> ColorRace
        session.shortName == "WU" -> ColorWarmUp
        else -> MotoGPTextSecondary
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Session badge
        Box(
            modifier = Modifier
                .width(42.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(sessionColor.copy(alpha = 0.12f))
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(session.shortName, color = sessionColor,
                fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }

        Spacer(Modifier.width(8.dp))

        Text(session.fullName, color = MotoGPTextSecondary,
            fontSize = 13.sp, modifier = Modifier.weight(1f),
            maxLines = 1, overflow = TextOverflow.Ellipsis)

        Text(session.time, color = MotoGPTextMuted,
            fontSize = 12.sp, modifier = Modifier.width(40.dp))

        Spacer(Modifier.width(6.dp))

        // Add to calendar button
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(MotoGPRed.copy(alpha = 0.15f))
                .clickable { onAdd() }
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text("+📅", color = MotoGPRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun addSessionToCalendar(context: android.content.Context, event: CalendarEvent, sessionShortName: String) {
    try {
        val raceDates = mapOf(
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
        val dateStr = raceDates[event.name] ?: return

        // Find session time
        val sessionTime = GpSessions.all.find {
            it.shortName == sessionShortName
        }?.time ?: "14:00"

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US)
        val startTime = sdf.parse("${dateStr} ${sessionTime}")?.time ?: return
        val endTime = when (sessionShortName) {
            "Race" -> startTime + 2 * 60 * 60 * 1000L
            "Sprint" -> startTime + 60 * 60 * 1000L
            else -> startTime + 45 * 60 * 1000L
        }

        // Adjust for CEST to UTC: subtract 2 hours
        val startUtc = startTime - 2 * 60 * 60 * 1000L
        val endUtc = endTime - 2 * 60 * 60 * 1000L

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "🏁 MotoGP: ${event.name} - $sessionShortName")
            putExtra(CalendarContract.Events.DESCRIPTION,
                "MotoGP 2026 - ${event.circuit}\n${event.name} - Sesión: $sessionShortName")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startUtc)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endUtc)
            putExtra(CalendarContract.Events.ALL_DAY, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val name = java.net.URLEncoder.encode("MotoGP: ${event.name} - $sessionShortName", "UTF-8")
            val details = java.net.URLEncoder.encode("MotoGP 2026 - ${event.circuit}", "UTF-8")
            val webUrl = "https://calendar.google.com/calendar/render?action=TEMPLATE&text=$name&details=$details"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse(webUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
