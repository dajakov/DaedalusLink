package com.example.daedaluslink

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    // Text options based on selected icon
    val texts = listOf(
        "Add...",
        "R2D2",
        "Settings"
    )

    LaunchedEffect(selectedIndex.intValue) {
        val targetOffset = with(density) {
            val size = if (selectedIndex.intValue == 0) 120.dp else 100.dp
            (selectedIndex.intValue * size.toPx()) - (screenWidthPx / 2) + (size.toPx() / 2)
        }
        scrollState.animateScrollTo(targetOffset.toInt())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth() // Take up the full width
                .padding(top = 16.dp) // Padding from the top
        ) {
            Text(
                text = texts[selectedIndex.intValue],  // Dynamically change text based on selected icon
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .align(Alignment.Center), // Center text horizontally and vertically within Box
                color = Color.Black
            )
        }

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

        if (selectedIndex.intValue == 0 || selectedIndex.intValue == icons.size - 1) {
            Box(
                modifier = Modifier.fillMaxWidth() // Ensure it takes full width
            ) {
                Button(
                    onClick = { /* Handle button click */ },
                    modifier = Modifier
                        .padding(15.dp)
                        .align(Alignment.Center) // Center the button horizontally
                        .fillMaxWidth(), // Button width set to 80% of the screen width
                    colors = ButtonDefaults.buttonColors(Color.Black), // Black button color
                    shape = RectangleShape // Rectangular shape
                ) {
                    if (selectedIndex.intValue == 0){
                        Text(
                            "Add a new Robot",
                            color = Color.White, // Text color set to white to contrast the black button
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            "App settings",
                            color = Color.White, // Text color set to white to contrast the black button
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            // Declaring a boolean value to store the expanded state of the TextField
            var mExpanded by remember { mutableStateOf(false) }

            // Create a list of cities
            val mVersionNumbers = listOf("Tatooine", "Coruscant", "Hoth")

            // Create a string value to store the selected city
            var mSelectedText by remember { mutableStateOf(mVersionNumbers.last()) } // Default to the last element

            var mTextFieldSize by remember { mutableStateOf(Size.Zero) }

            // Up Icon when expanded and down icon when collapsed
            val icon = if (mExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown

            Column(Modifier.padding(15.dp)) {
                // Label for the dropdown
                Text(
                    text = "Select config",
                    modifier = Modifier.padding(bottom = 5.dp) // Space between label and dropdown
                )

                // Visual Box around dropdown and the selected value
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp)) // Box with border and rounded corners
                        .clickable { mExpanded = !mExpanded } // Toggle dropdown when clicked
                        .padding(16.dp) // Padding inside the box
                ) {
                    // Display the currently selected value
                    Text(
                        text = mSelectedText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )

                    // Icon to indicate dropdown (arrow)
                    Icon(
                        imageVector = icon,
                        contentDescription = "Dropdown icon",
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(5.dp)
                    )
                }

                // Dropdown menu
                DropdownMenu(
                    expanded = mExpanded,
                    onDismissRequest = { mExpanded = false }, // Close menu when clicked outside
                    modifier = Modifier
                        .width(200.dp) // Set width of dropdown menu
                        .padding(top = 8.dp) // Add space between box and dropdown
                ) {
                    mVersionNumbers.forEach { label ->
                        DropdownMenuItem(
                            onClick = {
                                mSelectedText = label
                                mExpanded = false // Close dropdown after selection
                            },
                            text = { Text(text = label) }
                        )
                    }
                }
            }
            Box(
                modifier = Modifier.fillMaxWidth() // Ensure it takes full width
            ) {
                Button(
                    onClick = { /* Handle button click */ },
                    modifier = Modifier
                        .padding(0.dp)
                        .align(Alignment.Center) // Center the button horizontally
                        .fillMaxWidth(0.9f), // Button width set to 80% of the screen width
                    colors = ButtonDefaults.buttonColors(Color.Black), // Black button color
                    shape = RectangleShape // Rectangular shape
                ) {
                    Text(
                        "Add a new config",
                        color = Color.White, // Text color set to white to contrast the black button
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        var canvasHeight by remember { mutableFloatStateOf(0f) }
        Canvas(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                // Get the size of the canvas (height in this case)
                canvasHeight = coordinates.size.height.toFloat()
            }
            .clickable { navController.navigate("home") }) {

            // Calculate the bottom position for the arc and rect
            val bottomOffset = canvasHeight - diameter // Bottom of the canvas

            // Arc: Position the y-coordinate relative to the bottom
            drawArc(
                color = Color.Black,
                topLeft = Offset(
                    screenWidthPx * 0.5f - diameter / 2,  // Center horizontally
                    bottomOffset - diameter * 0.3f        // Position vertically relative to the bottom
                ),
                size = Size(diameter, diameter),
                startAngle = 300f,
                sweepAngle = 300f,
                style = Stroke(30f),
                useCenter = false
            )

            // Rect: Position the y-coordinate relative to the bottom
            drawRect(
                color = Color.Black,
                topLeft = Offset(
                    screenWidthPx * 0.5f - 15f,  // Center horizontally
                    bottomOffset - diameter * 0.5f // Position vertically relative to the bottom
                ),
                size = Size(30f, diameter * 0.7f)
            )
        }

        // Version Text Box at the bottom
        Box(
            modifier = Modifier
                .height(40.dp)  // Fixed height for the version text container
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "1.0.0-dev", // Dynamic version text can go here
                color = Color.Gray,  // Grayed-out text
                fontSize = 12.sp,     // Small font size
                modifier = Modifier
                    .align(Alignment.Center)  // Align to the center of the Box
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
