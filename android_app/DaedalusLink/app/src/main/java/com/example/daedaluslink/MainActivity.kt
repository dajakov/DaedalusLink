package com.example.daedaluslink

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterialScaffoldPaddingParameter",
        "UnusedMaterial3ScaffoldPaddingParameter"
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        actionBar?.hide() // Why is this needed? From where does the action bar come from???

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

sealed class IconItem {
    data class MaterialIcon(val icon: ImageVector) : IconItem()
    data class CustomIcon(val resourceId: Int) : IconItem()  // Represents drawable resource ID
}

@Composable
fun CustomIcon(resourceId: Int, selectedIndex: MutableState<Int>, index: Int) {
    // Use the painterResource to load the drawable image
    val painter = painterResource(id = resourceId)

    // Apply color filter for tinting the image based on the selected state
    val tintColor = if (index == selectedIndex.value) Color.Black else Color.Gray

    // Use the Image composable to display the loaded image with color filter
    Image(
        painter = painter,
        contentDescription = "Custom Icon",
        modifier = Modifier
            .padding(8.dp)
            .size(if (index == selectedIndex.value) 120.dp else 100.dp)
            .then(
                Modifier.clickable {
                    selectedIndex.value = index
                }
            ),
        colorFilter = ColorFilter.tint(tintColor) // Apply color filter for tint
    )
}

@Composable
fun LandingScreen(navController: NavController) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

    // Convert dp to px using LocalDensity inside the composable context
    val density = LocalDensity.current
    val screenWidthPx = with(density) { screenWidth.toPx() }
    val screenHeightPx = with(density) { screenHeight.toPx() }
    val diameter = screenWidthPx * 0.4f

    val selectedIndex = remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    // Sample Material icons list
    val icons = listOf(
        IconItem.MaterialIcon(Icons.Filled.Add),
        IconItem.CustomIcon(R.drawable.r2d2),
        IconItem.MaterialIcon(Icons.Filled.Settings),
    )

    LaunchedEffect(selectedIndex.intValue) {
        val targetOffset = with(density) {
            val size = if (selectedIndex.intValue == 0) 120.dp else 100.dp
            (selectedIndex.intValue * size.toPx()) - (screenWidthPx / 2) + (size.toPx() / 2)
        }
        scrollState.animateScrollTo(targetOffset.toInt())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier
            .horizontalScroll(scrollState)
            .padding(vertical = 16.dp)
        ) {
            icons.forEachIndexed { index, iconItem ->
                val size = if (index == selectedIndex.intValue) 120.dp else 100.dp

                // Icon button
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(size)
                        .clickable {
                            selectedIndex.intValue = index
                        }
                ) {
                    when (iconItem) {
                        is IconItem.MaterialIcon -> {
                            // Render Material Icon
                            Icon(
                                imageVector = iconItem.icon,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                tint = if (index == selectedIndex.intValue) Color.Black else Color.Gray
                            )
                        }
                        is IconItem.CustomIcon -> {
                            // Render Custom Icon (Image from resources)
                            CustomIcon(resourceId = iconItem.resourceId, selectedIndex = selectedIndex, index = index)
                        }
                    }
                }
            }
        }

        Canvas(modifier = Modifier
            .fillMaxSize()
            .clickable { navController.navigate("home") }) {

            drawArc(
                color = Color.Black,
                topLeft = Offset(screenWidthPx * 0.5f - diameter / 2, screenHeightPx * 0.5f - diameter / 2),
                size = Size(diameter, diameter),
                startAngle = 300f,
                sweepAngle = 300f,
                style = Stroke(30f),
                useCenter = false
            )

            drawRect(
                color = Color.Black,
                topLeft = Offset(screenWidthPx * 0.5f - 15f, screenHeightPx * 0.5f - diameter * 0.7f),
                size = Size(30f, diameter * 0.7f),
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        topBar = { /* No topBar here, it's effectively removed */ },
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
        topBar = { /* No topBar here, it's effectively removed */ },
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
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
        topBar = { /* No topBar here, it's effectively removed */ },
    ) {
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)) {
            Button(onClick = { /* Do something */ }) {
                Text("Notifications Button")
            }
        }
    }
}
