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

@Composable
fun StandingsScreen() {
    var riders by remember { mutableStateOf<List<RiderStanding>>(emptyList()) }
    var manufacturers by remember { mutableStateOf<List<ManufacturerStanding>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCharts by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val completedRounds = remember { Scraper.computeCompletedRounds() }
    val champInfo = remember { ChampionshipInfo.calculate(completedRounds) }

    fun loadData() {
        scope.launch {
            try {
                val (r, m) = withContext(Dispatchers.IO) { Scraper.fetchStandings() }
                riders = r
                manufacturers = m
            } catch (_: Exception) {}
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
                Text("🏆 CLASIFICACIÓN", color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("MotoGP 2026", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Ronda $completedRounds de ${champInfo.totalRounds} · Quedan ${champInfo.remainingPointsAvailable} pts",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        // Tab selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Pilotos", "Marcas").forEachIndexed { i, title ->
                FilterChip(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    label = { Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MotoGPRed,
                        selectedLabelColor = Color.White
                    )
                )
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = {
                showCharts = !showCharts
                if (!showCharts) loadData() // Refresh when going back to table
            }) {
                Text(if (showCharts) "📊 Tabla" else "📈 Evolución", color = MotoGPRed, fontSize = 12.sp)
            }
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MotoGPRed)
            }
        } else if (riders.isEmpty() && manufacturers.isEmpty() && selectedTab == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Sin conexión", color = MotoGPTextMuted, fontSize = 16.sp)
            }
        } else if (selectedTab == 0) {
            if (showCharts) {
                EvolutionChart(riders.map { it.rider }, riders.take(5).map { listOf(it.points) })
            } else {
                RiderStandingsTable(riders, champInfo)
            }
        } else {
            if (showCharts) {
                EvolutionChart(manufacturers.map { it.manufacturer }, manufacturers.map { listOf(it.points) })
            } else {
                ManufacturerStandingsTable(manufacturers)
            }
        }
    }
}

@Composable
fun RiderStandingsTable(riders: List<RiderStanding>, champInfo: ChampionshipInfo) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Text("#", color = MotoGPTextMuted, fontSize = 10.sp, modifier = Modifier.width(22.dp))
                Text("Piloto", color = MotoGPTextMuted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text("Vic", color = MotoGPTextMuted, fontSize = 10.sp, modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
                Text("Pod", color = MotoGPTextMuted, fontSize = 10.sp, modifier = Modifier.width(26.dp), textAlign = TextAlign.Center)
                Text("Pts", color = MotoGPTextMuted, fontSize = 10.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
            }
        }

        items(riders) { rider ->
            val posColor = when (rider.position) {
                1 -> MotoGPGold; 2 -> MotoGPSilver; 3 -> MotoGPBronze; else -> MotoGPTextSecondary
            }
            val bgColor = if (rider.position <= 3) MotoGPSurfaceVariant else MotoGPSurface

            Card(
                colors = CardDefaults.cardColors(containerColor = bgColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Position
                        Text(
                            text = "${rider.position}",
                            color = posColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.width(22.dp)
                        )

                        // Rider name + team + country flag
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val flag = flagEmoji(rider.countryIso)
                                Text("$flag ", fontSize = 14.sp)
                                Text(rider.rider, color = MotoGPTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Text(rider.team, color = MotoGPTextMuted, fontSize = 10.sp)
                        }

                        // Wins
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
                            Text("${rider.wins}", color = if (rider.wins > 0) MotoGPSuccess else MotoGPTextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("V", color = MotoGPTextMuted, fontSize = 9.sp)
                        }

                        // Podiums
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(26.dp)) {
                            Text("${rider.podiums}", color = if (rider.podiums > 0) MotoGPGold else MotoGPTextMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("P", color = MotoGPTextMuted, fontSize = 9.sp)
                        }

                        // Points
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${rider.points}", color = MotoGPTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.width(40.dp))
                        }
                    }

                    // Gap bar + last positions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Progress bar
                        if (riders.isNotEmpty()) {
                            val maxPts = riders.first().points.coerceAtLeast(1)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MotoGPProgressBg)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = rider.points.toFloat() / maxPts)
                                        .background(MotoGPRed)
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // Gap
                        Text(rider.deficit.ifEmpty { "-" },
                            color = MotoGPTextMuted, fontSize = 10.sp,
                            modifier = Modifier.width(56.dp), textAlign = TextAlign.End)

                        // Last positions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.width(44.dp)
                        ) {
                            val laps = rider.lastPositions.ifEmpty { emptyList() }
                            for (pos in laps) {
                                val lpColor = when {
                                    pos <= 3 -> MotoGPGold
                                    pos <= 7 -> MotoGPSuccess
                                    pos <= 12 -> MotoGPTextSecondary
                                    else -> MotoGPRed
                                }
                                Text("$pos", color = lpColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun flagEmoji(iso: String): String = when (iso.uppercase()) {
    "ES" -> "🇪🇸"; "IT" -> "🇮🇹"; "FR" -> "🇫🇷"; "JP" -> "🇯🇵"
    "ZA" -> "🇿🇦"; "AU" -> "🇦🇺"; "BR" -> "🇧🇷"; "US" -> "🇺🇸"
    "DE" -> "🇩🇪"; "GB" -> "🇬🇧"; "NL" -> "🇳🇱"; "TR" -> "🇹🇷"
    "CZ" -> "🇨🇿"; "AT" -> "🇦🇹"; "ID" -> "🇮🇩"; "MY" -> "🇲🇾"
    "AR" -> "🇦🇷"; "PT" -> "🇵🇹"; "IE" -> "🇮🇪"; "FI" -> "🇫🇮"
    "CO" -> "🇨🇴"; "BE" -> "🇧🇪"; "NZ" -> "🇳🇿"
    else -> ""
}

@Composable
fun ManufacturerStandingsTable(manufacturers: List<ManufacturerStanding>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(manufacturers) { mfr ->
            val posColor = when (mfr.position) {
                1 -> MotoGPGold; 2 -> MotoGPSilver; 3 -> MotoGPBronze; else -> MotoGPTextSecondary
            }
            val mfrColor = when (mfr.manufacturer.lowercase()) {
                "aprilia" -> Color(0xFF9C27B0)
                "ducati" -> Color(0xFFE91E63)
                "ktm" -> Color(0xFFFF6D00)
                "honda" -> Color(0xFF2196F3)
                "yamaha" -> Color(0xFF1E88E5)
                else -> MotoGPTextPrimary
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
                    Text("${mfr.position}", color = posColor, fontWeight = FontWeight.Bold,
                        fontSize = 18.sp, modifier = Modifier.width(32.dp))

                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(mfrColor)
                    )
                    Spacer(Modifier.width(12.dp))

                    Text(mfr.manufacturer, color = MotoGPTextPrimary, fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, modifier = Modifier.weight(1f))

                    // Power bar
                    if (manufacturers.isNotEmpty()) {
                        val maxPts = manufacturers.first().points.coerceAtLeast(1)
                        Box(
                            modifier = Modifier
                                .width(60.dp)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(MotoGPProgressBg)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = mfr.points.toFloat() / maxPts)
                                    .background(mfrColor)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))

                    Text("${mfr.points}", color = MotoGPTextPrimary, fontWeight = FontWeight.Bold,
                        fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
fun EvolutionChart(
    labels: List<String>,
    points: List<List<Int>>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MotoGPSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("📊 Puntos actuales", color = MotoGPTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(Modifier.height(16.dp))
                    val maxPts = points.flatten().maxOrNull()?.coerceAtLeast(1) ?: 1
                    val colors = listOf(MotoGPRed, MotoGPGold, MotoGPSuccess, Color(0xFF4FC3F7), Color(0xFFFF9800))
                    labels.take(5).forEachIndexed { i, label ->
                        val pts = points.getOrNull(i)?.lastOrNull() ?: 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(label, color = MotoGPTextPrimary, fontSize = 13.sp, modifier = Modifier.width(80.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MotoGPProgressBg)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction = pts.toFloat() / maxPts)
                                        .background(colors[i % colors.size])
                                )
                            }
                            Text("$pts", color = MotoGPTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
