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
    val scope = rememberCoroutineScope()
    val completedRounds = remember { Scraper.computeCompletedRounds() }
    val champInfo = remember { ChampionshipInfo.calculate(completedRounds) }

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val (r, m) = withContext(Dispatchers.IO) { Scraper.fetchStandings() }
                riders = r
                manufacturers = m
            } catch (_: Exception) {}
            isLoading = false
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
                .padding(16.dp)
        ) {
            Column {
                Text("CLASIFICACIÓN MUNDIAL",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("MotoGP 2026",
                    color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Ronda $completedRounds de ${champInfo.totalRounds} · Quedan ${champInfo.remainingPointsAvailable} pts",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp))
            }
        }

        // Tabs
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
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MotoGPRed)
            }
        } else if (selectedTab == 0) {
            RiderStandingsTable(riders)
        } else {
            ManufacturerStandingsTable(manufacturers)
        }
    }
}

@Composable
fun RiderStandingsTable(riders: List<RiderStanding>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(riders) { rider ->
            val posColor = when (rider.position) {
                1 -> MotoGPGold; 2 -> MotoGPSilver; 3 -> MotoGPBronze; else -> MotoGPTextSecondary
            }
            val bgColor = if (rider.position <= 3) MotoGPSurfaceVariant else MotoGPSurface

            Card(
                colors = CardDefaults.cardColors(containerColor = bgColor),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Position (circular badge)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(posColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${rider.position}",
                            color = posColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(Modifier.width(12.dp))

                    // Name + team
                    Column(modifier = Modifier.weight(1f)) {
                        Text(rider.rider,
                            color = MotoGPTextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(rider.team,
                            color = MotoGPTextMuted, fontSize = 11.sp)
                    }

                    // Gap
                    Text(rider.deficit,
                        color = MotoGPTextMuted, fontSize = 12.sp,
                        modifier = Modifier.padding(end = 12.dp))

                    // Points
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${rider.points}",
                            color = MotoGPTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("pts", color = MotoGPTextMuted, fontSize = 10.sp)
                    }
                }

                // Progress bar
                if (riders.isNotEmpty()) {
                    val maxPts = riders.first().points.coerceAtLeast(1)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
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
            }
        }
    }
}

@Composable
fun ManufacturerStandingsTable(manufacturers: List<ManufacturerStanding>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
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
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Position badge
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(50))
                            .background(posColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${mfr.position}",
                            color = posColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(Modifier.width(12.dp))

                    // Color indicator + name
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(mfrColor)
                    )
                    Spacer(Modifier.width(10.dp))

                    Text(mfr.manufacturer,
                        color = MotoGPTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        modifier = Modifier.weight(1f))

                    // Points
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${mfr.points}",
                            color = MotoGPTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("pts", color = MotoGPTextMuted, fontSize = 10.sp)
                    }
                }

                // Progress bar
                if (manufacturers.isNotEmpty()) {
                    val maxPts = manufacturers.first().points.coerceAtLeast(1)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
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
            }
        }
    }
}
