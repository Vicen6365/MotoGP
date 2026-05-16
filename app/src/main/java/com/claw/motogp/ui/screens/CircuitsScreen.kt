package com.claw.motogp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.painterResource
import com.claw.motogp.data.CircuitData
import com.claw.motogp.data.CircuitInfo
import com.claw.motogp.data.CircuitRecord
import com.claw.motogp.data.Scraper
import com.claw.motogp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CircuitsScreen() {
    var selectedCircuit by remember { mutableStateOf<CircuitInfo?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().background(MotoGPBg)) {
        // Header
        Box(
            modifier = Modifier.fillMaxWidth().background(MotoGPRed).padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Text("🏁 CIRCUITOS 2026",
                    color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("22 trazados",
                    color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Refresh records bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Toca un circuito para ver su trazado y datos",
                color = MotoGPTextMuted, fontSize = 11.sp,
                modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    isRefreshing = true
                    refreshMsg = ""
                    scope.launch {
                        try {
                            val records = withContext(Dispatchers.IO) {
                                Scraper.fetchCircuitRecords()
                            }
                            if (records.isEmpty()) {
                                refreshMsg = "No se pudieron actualizar"
                            } else {
                                CircuitData.updatedRecords.clear()
                                CircuitData.updatedRecords.putAll(records)
                                refreshMsg = "✓ ${records.size} circuitos actualizados"
                            }
                        } catch (e: Exception) {
                            refreshMsg = "Error: ${e.message}"
                        } finally {
                            isRefreshing = false
                        }
                    }
                },
                enabled = !isRefreshing,
                colors = ButtonDefaults.buttonColors(containerColor = MotoGPRed),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text(
                    if (isRefreshing) " ↻" else "↻ Récords",
                    color = Color.White, fontSize = 12.sp
                )
            }
        }

        // Refresh status message
        if (refreshMsg.isNotEmpty()) {
            Text(refreshMsg,
                color = MotoGPSuccess, fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
        }

        // Circuit list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(CircuitData.circuits) { circuit ->
                CircuitCard(circuit, onClick = { selectedCircuit = circuit })
            }
        }
    }

    // Detail dialog
    if (selectedCircuit != null) {
        CircuitDetailDialog(selectedCircuit!!, onDismiss = { selectedCircuit = null })
    }
}

@Composable
fun CircuitCard(circuit: CircuitInfo, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MotoGPSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Flag + round
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MotoGPSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(circuit.flag, fontSize = 24.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(circuit.gpName, color = MotoGPTextPrimary,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(circuit.country, color = MotoGPTextMuted, fontSize = 12.sp)
            }

            Spacer(Modifier.width(4.dp))

            // Length + turns chip
            Column(horizontalAlignment = Alignment.End) {
                Text(circuit.length, color = MotoGPTextSecondary,
                    fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text("${circuit.turns} curvas", color = MotoGPTextMuted, fontSize = 11.sp)
            }

            Spacer(Modifier.width(8.dp))

            Text("▸", color = MotoGPRed.copy(alpha = 0.6f), fontSize = 16.sp)
        }
    }
}

@Composable
fun CircuitDetailDialog(circuit: CircuitInfo, onDismiss: () -> Unit) {
    // Get live record data if available
    val liveRecord = CircuitData.updatedRecords[circuit.name]
    val displayLap = liveRecord?.lap ?: circuit.recordLap
    val displayHolder = liveRecord?.holder ?: circuit.recordHolder
    val displayYear = liveRecord?.year ?: circuit.recordYear

    var isRefreshing by remember { mutableStateOf(false) }
    var refreshMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MotoGPSurface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 16.dp)
            ) {
                // Header with close
                Box(
                    modifier = Modifier.fillMaxWidth().background(MotoGPRed.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(circuit.flag, fontSize = 32.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(circuit.gpName, color = MotoGPTextPrimary,
                                fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(circuit.country, color = MotoGPTextMuted, fontSize = 13.sp)
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Text("✕", color = MotoGPTextMuted, fontSize = 18.sp)
                        }
                    }
                }

                // Track map image — BIGGER with white background (maps are black-on-transparent)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .border(1.dp, Color.Black.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = circuit.mapResId),
                        contentDescription = "${circuit.name} track map",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp),
                        contentScale = ContentScale.Fit
                    )
                }

                // Circuit name
                Text(circuit.name, color = MotoGPTextPrimary,
                    fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(Modifier.height(8.dp))

                // Stats grid (2x2)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    StatBox("Longitud", circuit.length, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    StatBox("Curvas", "${circuit.turns}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    StatBox("Tipo", circuit.layoutType, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    StatBox("Récord", displayLap, Modifier.weight(1f))
                }

                // Record holder (live)
                if (displayHolder != "—") {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MotoGPRed.copy(alpha = 0.08f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🏆", fontSize = 20.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Récord de vuelta: ${displayHolder}",
                                color = MotoGPRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("${displayLap} (${displayYear})",
                                color = MotoGPTextSecondary, fontSize = 12.sp)
                        }
                        // "Updated" badge for fetched records
                        if (liveRecord != null) {
                            Text("LIVE", color = MotoGPSuccess, fontSize = 10.sp,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Description
                Spacer(Modifier.height(8.dp))
                Text(circuit.description,
                    color = MotoGPTextSecondary, fontSize = 13.sp, lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp))

                Spacer(Modifier.height(12.dp))

                // Refresh button for this circuit's record
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            isRefreshing = true
                            refreshMsg = ""
                            scope.launch {
                                try {
                                    val records = withContext(Dispatchers.IO) {
                                        Scraper.fetchCircuitRecords()
                                    }
                                    if (records.isEmpty()) {
                                        refreshMsg = "No se pudo obtener el récord"
                                    } else if (records.containsKey(circuit.name)) {
                                        CircuitData.updatedRecords[circuit.name] = records[circuit.name]!!
                                        refreshMsg = "✓ Récord actualizado"
                                    } else {
                                        refreshMsg = "Circuito no encontrado en la fuente"
                                    }
                                } catch (e: Exception) {
                                    refreshMsg = "Error de conexión"
                                } finally {
                                    isRefreshing = false
                                }
                            }
                        },
                        enabled = !isRefreshing,
                        colors = ButtonDefaults.buttonColors(containerColor = MotoGPRed),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            if (isRefreshing) "↻ Actualizando..." else "↻ Actualizar récord",
                            color = Color.White, fontSize = 13.sp
                        )
                    }
                }

                if (refreshMsg.isNotEmpty()) {
                    Text(refreshMsg,
                        color = if (refreshMsg.startsWith("✓")) MotoGPSuccess else MotoGPAccent,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                }
            }
        }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MotoGPSurfaceVariant)
            .padding(12.dp)
    ) {
        Column {
            Text(label, color = MotoGPTextMuted, fontSize = 11.sp)
            Text(value, color = MotoGPTextPrimary, fontSize = 16.sp,
                fontWeight = FontWeight.Bold)
        }
    }
}
