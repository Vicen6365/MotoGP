package com.claw.motogp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    var expandedIndex by remember { mutableStateOf(-1) }
    var loadingContentIndex by remember { mutableStateOf(-1) }
    val scope = rememberCoroutineScope()

    fun loadNews() {
        isLoading = true
        errorMsg = ""
        expandedIndex = -1
        loadingContentIndex = -1
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
                Text("MotoGP · soymotero.net + motogp.com", color = Color.White,
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
                    Text("Cargando noticias...", color = MotoGPTextSecondary)
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
                    Text("Toca una noticia para leer el contenido completo",
                        color = MotoGPTextMuted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                }

                itemsIndexed(news) { index, article ->
                    NewsCard(
                        article = article,
                        isExpanded = expandedIndex == index,
                        isLoadingContent = loadingContentIndex == index,
                        onClick = {
                            if (expandedIndex == index) {
                                expandedIndex = -1
                            } else {
                                expandedIndex = index
                                if (article.content.isEmpty()) {
                                    loadingContentIndex = index
                                    scope.launch {
                                        val content = withContext(Dispatchers.IO) {
                                            Scraper.fetchArticleContent(article.url)
                                        }
                                        news = news.toMutableList().also {
                                            it[index] = it[index].copy(content = content)
                                        }
                                        loadingContentIndex = -1
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun NewsCard(
    article: NewsArticle,
    isExpanded: Boolean,
    isLoadingContent: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MotoGPSurface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                // Source badge column
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (article.source == "soymotero") Color(0xFF2E7D32).copy(alpha = 0.2f)
                                else Color(0xFF1565C0).copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (article.source == "soymotero") "SM" else "MG",
                            color = if (article.source == "soymotero") Color(0xFF2E7D32)
                                    else Color(0xFF1565C0),
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
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (article.snippet.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = article.snippet,
                            color = MotoGPTextSecondary,
                            fontSize = 12.sp,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    Row {
                        Text(
                            text = if (article.source == "soymotero") "SOYMOTERO" else "MOTOGP.COM",
                            color = if (article.source == "soymotero") Color(0xFF2E7D32)
                                    else Color(0xFF1565C0),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (article.date.isNotEmpty()) {
                            Text(" · ${article.date}", color = MotoGPTextMuted, fontSize = 10.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (isExpanded) "▼ Contraer" else "▼ Leer más",
                            color = MotoGPRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Expandable content section
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Divider(color = MotoGPBg, thickness = 1.dp)
                    Spacer(Modifier.height(12.dp))

                    if (isLoadingContent) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = MotoGPRed,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Cargando artículo...", color = MotoGPTextSecondary, fontSize = 12.sp)
                        }
                    } else if (article.content.isNotEmpty()) {
                        Text(
                            text = article.content,
                            color = MotoGPTextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                    } else {
                        Text(
                            text = "No se pudo cargar el contenido.",
                            color = MotoGPTextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
