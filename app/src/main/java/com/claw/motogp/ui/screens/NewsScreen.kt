package com.claw.motogp.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claw.motogp.data.NewsArticle
import com.claw.motogp.data.Scraper
import com.claw.motogp.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun NewsScreen() {
    var news by remember { mutableStateOf<List<NewsArticle>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun loadNews() {
        isLoading = true
        errorMsg = ""
        scope.launch {
            try {
                val articles = withContext(Dispatchers.IO) { Scraper.fetchNews() }
                news = articles
            } catch (e: Exception) {
                errorMsg = "Error al cargar: ${e.message}"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadNews() }

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
                Text("📰 NOTICIAS", color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Text("MotoGP · Rumores · Mercado", color = Color.White,
                    fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Refresh button
        Button(
            onClick = { loadNews() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MotoGPRed),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Actualizando..." else "↻ Actualizar noticias", color = Color.White)
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MotoGPRed)
                    Spacer(Modifier.height(12.dp))
                    Text("Buscando noticias...", color = MotoGPTextSecondary)
                }
            }
        } else if (errorMsg.isNotEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(errorMsg, color = MotoGPAccent, modifier = Modifier.padding(16.dp))
                    Button(onClick = { loadNews() }, colors = ButtonDefaults.buttonColors(MotoGPRed)) {
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
                item {
                    Text("Toca una noticia para abrirla en el navegador",
                        color = MotoGPTextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                }

                items(news) { article ->
                    NewsCard(article, onClick = {
                        openInBrowser(context, article.url)
                    })
                }
            }
        }
    }
}

@Composable
fun NewsCard(article: NewsArticle, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MotoGPSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Source badge column
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(sourceColor(article.source).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sourceShort(article.source),
                        color = sourceColor(article.source),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = article.title,
                    color = MotoGPTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (article.snippet.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = article.snippet,
                        color = MotoGPTextSecondary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.height(6.dp))

                Row {
                    Text(
                        text = article.source.uppercase(),
                        color = sourceColor(article.source),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (article.date.isNotEmpty()) {
                        Text(" · ${article.date}", color = MotoGPTextMuted, fontSize = 10.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("Abrir →", color = MotoGPRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun sourceColor(source: String): Color = when (source.lowercase()) {
    "autosport" -> Color(0xFF00BCD4) // cyan
    "crash.net" -> Color(0xFFFF5722) // orange
    "motorsport" -> Color(0xFF8BC34A) // green
    else -> MotoGPTextMuted
}

private fun sourceShort(source: String): String = when (source.lowercase()) {
    "autosport" -> "AU"
    "crash.net" -> "CR"
    "motorsport" -> "MS"
    else -> "GP"
}

private fun openInBrowser(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {}
}
