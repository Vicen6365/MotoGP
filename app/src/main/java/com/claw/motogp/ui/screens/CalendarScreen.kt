package com.claw.motogp.ui.screens

import android.content.Intent
import android.net.Uri
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
                    CalendarCard(gp, onAddToCalendar = {
                        addToCalendar(context, gp)
                    })
                }
            }
        }
    }
}

@Composable
fun CalendarCard(event: CalendarEvent, onAddToCalendar: () -> Unit) {
    val isCompleted = event.isCompleted
    val isNext = !isCompleted && event.round == (Scraper().computeCompletedRounds() + 1)
    val borderColor = when {
        isNext -> Color(0xFFFF9800)
        isCompleted -> MotoGPTextMuted
        else -> MotoGPRed
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isCompleted) MotoGPSurface.copy(alpha = 0.6f) else MotoGPSurface
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isNext) Modifier.border(
                    1.5.dp, Color(0xFFFF9800), RoundedCornerShape(10.dp)
                ) else Modifier
            )
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
                Text(
                    text = if (event.dateRange.isNotEmpty()) event.dateRange else "TBD",
                    color = if (isCompleted) MotoGPTextMuted else MotoGPTextSecondary,
                    fontSize = 11.sp
                )
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
        // Build date from known race schedule
        val raceDates = mapOf(
            "Thailand GP" to "20260301", "Brazil GP" to "20260322",
            "Americas GP" to "20260329", "Spanish GP" to "20260426",
            "French GP" to "20260510", "Catalan GP" to "20260517",
            "Italian GP" to "20260531", "Hungarian GP" to "20260607",
            "Czech GP" to "20260621", "Dutch GP" to "20260628",
            "German GP" to "20260712", "British GP" to "20260809",
            "Aragon GP" to "20260830", "San Marino GP" to "20260913",
            "Austrian GP" to "20260920", "Japanese GP" to "20261004",
            "Indonesian GP" to "20261011", "Australian GP" to "20261025",
            "Malaysian GP" to "20261101", "Qatar GP" to "20261108",
            "Portuguese GP" to "20261122", "Valencia GP" to "20261129"
        )
        val dateStr = raceDates[event.name] ?: return
        val year = dateStr.substring(0, 4)
        val month = dateStr.substring(4, 6)
        val day = dateStr.substring(6, 8)

        // Race start: 14:00 CEST = 12:00 UTC
        val startMillis = "${year}${month}${day}T120000Z"
        // End: ~2h later
        val endMillis = "${year}${month}${day}T140000Z"

        val uri = Uri.parse("content://com.android.calendar/time/$startMillis/$endMillis")

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback: just open calendar app
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("content://com.android.calendar/time/")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }
}
