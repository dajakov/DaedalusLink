package com.example.daedaluslink

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter",
        "UnusedMaterial3ScaffoldPaddingParameter"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()

            val currentDestination by navController.currentBackStackEntryAsState()
            val showBottomBar = currentDestination?.destination?.route != "landing" // Hide on Landing

            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar {
                            val items = listOf("home", "dashboard", "notifications")
                            val icons = listOf(Icons.Default.Home, Icons.AutoMirrored.Filled.List, Icons.Default.Notifications)

                            items.forEachIndexed { index, screen ->
                                NavigationBarItem(
                                    icon = { Icon(icons[index], contentDescription = screen) },
                                    label = { Text(screen) },
                                    selected = navController.currentDestination?.route == screen,
                                    onClick = { navController.navigate(screen) }
                                )
                            }
                        }
                    }
                }
            ) {
                NavHost(navController, startDestination = "landing") {
                    composable("landing") { LandingScreen(navController) }
                    composable("home") { HomeScreen(navController) }
                    composable("dashboard") { DashboardScreen(navController) }
                    composable("notifications") { NotificationScreen(navController) }
                }
            }
        }
    }
}

@Composable
fun LandingScreen(navController: NavController) {
    Column (

    ){
        Canvas(modifier = Modifier.fillMaxSize()
            .clickable { navController.navigate("home") }) {
            val screenWidth = size.width
            val screenHeight = size.height

            drawCircle(color = Color.Black, radius = 300f, center = this.center)
            drawCircle(color = Color.White, radius = 250f, center = this.center)
            drawRect(color = Color.White,
                topLeft = Offset(screenWidth*0.4f, screenHeight*0.2f),
                size = Size(screenWidth*0.2f, screenHeight*0.3f)
            )
            drawRect(color = Color.Black,
                topLeft = Offset(screenWidth*0.475f, screenHeight*0.3f),
                size = Size(screenWidth*0.05f, screenHeight*0.18f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Home") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Button(onClick = { /* Handle click */ }) {
                Text("Click Me")
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Dashboard") }) }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(onClick = { /* Do something */ }) {
                Text("Dashboard Button")
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(navController: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Notifications") }) }
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(onClick = { /* Do something */ }) {
                Text("Notifications Button")
            }
        }
    }
}
