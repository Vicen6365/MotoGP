package com.claw.motogp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claw.motogp.data.*
import com.claw.motogp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CalendarScreen() {
    var calendar by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val completedRounds = remember { Scraper.computeCompletedRounds() }

    fun loadData() {
        isLoading = true
        errorMsg = ""
        scope.launch {
            try {
                val cal = withContext(Dispatchers.IO) { Scraper.fetchSchedule() }
                calendar = cal
            } catch (e: Exception) {
                errorMsg = "Error al cargar: ${e.message}"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

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
                .padding(20.dp)
        ) {
            Column {
                Text("📅 CALENDARIO", color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("MotoGP 2026 · 22 carreras", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Refresh button
        Button(
            onClick = { loadData() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MotoGPRed),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Actualizando..." else "↻ Actualizar calendario", color = Color.White)
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MotoGPRed)
                    Spacer(Modifier.height(12.dp))
                    Text("Cargando calendario...", color = MotoGPTextSecondary)
                }
            }
        } else if (errorMsg.isNotEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMsg, color = MotoGPAccent, modifier = Modifier.padding(16.dp))
                    Button(onClick = { loadData() }, colors = ButtonDefaults.buttonColors(MotoGPRed)) {
                        Text("Reintentar", color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Legend
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(MotoGPTextMuted))
                            Spacer(Modifier.width(6.dp))
                            Text("Completado", color = MotoGPTextMuted, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(MotoGPRed))
                            Spacer(Modifier.width(6.dp))
                            Text("Pendiente", color = MotoGPTextMuted, fontSize = 11.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF9800)))
                            Spacer(Modifier.width(6.dp))
                            Text("Actual", color = MotoGPTextMuted, fontSize = 11.sp)
                        }
                    }
                }

                items(calendar) { gp ->
                    CalendarCard(gp, completedRounds, onAddToCalendar = {
                        addToCalendar(context, gp)
                    })
                }
            }
        }
    }
}

@Composable
fun CalendarCard(event: CalendarEvent, completedRounds: Int, onAddToCalendar: () -> Unit) {
    val isCompleted = event.isCompleted
    val isNext = !isCompleted && event.round == (completedRounds + 1)
    val borderColor = when {
        isNext -> Color(0xFFFF9800)
        isCompleted -> MotoGPTextMuted
        else -> MotoGPRed
    }

    val cardModifier = if (isNext) {
        Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFFFF9800), RoundedCornerShape(10.dp))
    } else {
        Modifier.fillMaxWidth()
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MotoGPSurface.copy(alpha = 0.6f) else MotoGPSurface
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = cardModifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Round number
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isCompleted) MotoGPTextMuted.copy(alpha = 0.2f) else MotoGPRed.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "R${event.round}",
                    color = if (isCompleted) MotoGPTextMuted else MotoGPRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.name,
                    color = if (isCompleted) MotoGPTextMuted else MotoGPTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = event.circuit,
                    color = MotoGPTextMuted,
                    fontSize = 12.sp
                )
                Row {
                    Text(
                        text = if (event.dateRange.isNotEmpty()) event.dateRange else "TBD",
                        color = if (isCompleted) MotoGPTextMuted else MotoGPTextSecondary,
                        fontSize = 11.sp
                    )
                    if (isNext) {
                        Text(" · Siguiente", color = Color(0xFFFF9800), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (isCompleted) {
                    Text("✓", color = MotoGPSuccess, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                } else {
                    FilledTonalButton(
                        onClick = onAddToCalendar,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MotoGPRed.copy(alpha = 0.2f),
                            contentColor = MotoGPRed
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("+ 📅", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

private fun addToCalendar(context: android.content.Context, event: CalendarEvent) {
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
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.US)

        // Race at 14:00 CEST (12:00 UTC)
        val startTime = sdf.parse("${dateStr}T12:00")?.time ?: return
        val endTime = startTime + 2 * 60 * 60 * 1000L // 2 hours

        // Use ACTION_INSERT with CalendarContract — abre el calendario nativo
        val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
            data = android.provider.CalendarContract.Events.CONTENT_URI
            putExtra(android.provider.CalendarContract.Events.TITLE, "🏁 MotoGP: ${event.name}")
            putExtra(android.provider.CalendarContract.Events.DESCRIPTION, "MotoGP 2026 - ${event.circuit}")
            putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTime)
            putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, endTime)
            putExtra(android.provider.CalendarContract.Events.ALL_DAY, false)
            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback: open Google Calendar web
        try {
            val name = java.net.URLEncoder.encode("MotoGP: ${event.name}", "UTF-8")
            val details = java.net.URLEncoder.encode("MotoGP 2026 - ${event.circuit}", "UTF-8")
            val webUrl = "https://calendar.google.com/calendar/render?action=TEMPLATE&text=$name&details=$details"
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse(webUrl)
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
