package com.claw.motogp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlagCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.claw.motogp.ui.screens.CalendarScreen
import com.claw.motogp.ui.screens.CircuitsScreen
import com.claw.motogp.ui.screens.NewsScreen
import com.claw.motogp.ui.screens.StandingsScreen
import com.claw.motogp.ui.screens.WeekendScreen
import com.claw.motogp.ui.theme.MotoGPBg
import com.claw.motogp.ui.theme.MotoGPColorScheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MotoGPTheme {
                MotoGPApp()
            }
        }
    }
}

@Composable
fun MotoGPTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MotoGPColorScheme) {
        content()
    }
}

data class NavItem(val label: String, val icon: ImageVector)

@Composable
fun MotoGPApp() {
    var selectedTab by remember { mutableIntStateOf(0) }
    val items = listOf(
        NavItem("Finde", Icons.Filled.FlagCircle),
        NavItem("Clasif.", Icons.Filled.EmojiEvents),
        NavItem("Noticias", Icons.Filled.List),
        NavItem("Circuitos", Icons.Filled.Place),
        NavItem("Calendario", Icons.Filled.CalendarMonth)
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MotoGPBg,
                contentColor = Color.White,
                tonalElevation = 0.dp
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (selectedTab == index) MaterialTheme.colorScheme.primary
                                       else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary
                                       else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MotoGPBg)
        ) {
            when (selectedTab) {
                0 -> WeekendScreen()
                1 -> StandingsScreen()
                2 -> NewsScreen()
                3 -> CircuitsScreen()
                4 -> CalendarScreen()
            }
        }
    }
}
