package com.example.daedaluslink

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.graphicsLayer
import com.example.daedaluslink.ConnectConfigDatabase.Companion.resetDatabase
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {

    // Initialize ViewModel
    private val connectConfigViewModel: ConnectConfigViewModel by viewModels()

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter", "UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        resetDatabase(applicationContext) // TODO(remove for production) for development purposes only.

        actionBar?.hide()
        setContent {
            val navController = rememberNavController()

            val currentDestination by navController.currentBackStackEntryAsState()
            val showBottomBar = currentDestination?.destination?.route != "landing" &&
                    currentDestination?.destination?.route != "addConnectConfig"

            Scaffold(
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar(containerColor = Color.White) {
                            val items = listOf("control", "debug", "settings")
                            val icons = listOf(Icons.Default.PlayArrow, Icons.Default.Info, Icons.Default.Settings)

                            items.forEachIndexed { index, screen ->
                                NavigationBarItem(
                                    colors = NavigationBarItemColors(
                                        selectedIconColor = Color.Black,
                                        selectedIndicatorColor = Color.White,
                                        selectedTextColor = Color.Black,
                                        unselectedIconColor = Color.Gray,
                                        unselectedTextColor = Color.Gray,
                                        disabledIconColor = Color.Gray,
                                        disabledTextColor = Color.Gray
                                    ),
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
                // Use ViewModel directly from Compose using viewModel()
                NavHost(navController, startDestination = "landing") {
                    composable("landing") { LandingScreen(navController) }
                    composable("control") { HomeScreen(navController) }
                    composable("debug") { DashboardScreen(navController) }
                    composable("settings") { NotificationScreen(navController) }
                    composable("addConnectConfig") { AddConnectConfigScreen(navController, connectConfigViewModel) }
                }
            }
        }
    }
}

// icons for the user to choose from for the connect config
val allIcons = listOf(
    IconItem.MaterialIcon(Icons.Default.Info),
    IconItem.MaterialIcon(Icons.Default.PlayArrow),
    IconItem.CustomIcon(R.drawable.r2d2),
    IconItem.CustomIcon(R.drawable.r2d2)
    // Add more icons as needed
)

sealed class IconItem {
    data class MaterialIcon(val icon: ImageVector) : IconItem()
    data class CustomIcon(val resourceId: Int) : IconItem()  // Represents drawable resource ID
}

object IconMapper {

    // Mapping from icon ID to IconItem
    fun getIconById(iconId: String): IconItem {
        return when (iconId) {
            "info" -> IconItem.MaterialIcon(Icons.Default.Info) // Example of mapping an ID to a Material icon
            "PlayArrow" -> IconItem.MaterialIcon(Icons.Default.PlayArrow) // Another example for MaterialIcon
            "r2d2" -> IconItem.CustomIcon(R.drawable.r2d2) // Example of mapping an ID to a custom icon
            "r2d22" -> IconItem.CustomIcon(R.drawable.r2d2) // Another example for custom icons
            else -> IconItem.MaterialIcon(Icons.Default.Warning) // Default icon if the ID is unrecognized
        }
    }

    // Mapping from IconItem to ID (store this ID in the database)
    fun getIconId(iconItem: IconItem): String {
        return when (iconItem) {
            is IconItem.MaterialIcon -> when (iconItem.icon) {
                Icons.Default.Info -> "info"
                Icons.Default.PlayArrow -> "PlayArrow"
                else -> "default" // Default if the icon doesn't match
            }
            is IconItem.CustomIcon -> when (iconItem.resourceId) {
                R.drawable.r2d2 -> "r2d2"
                R.drawable.r2d2 -> "r2d22"
                else -> "default"
            }
        }
    }
}

@Composable
fun DisplayIcon(icon: IconItem, modifier: Modifier = Modifier) {
    when (icon) {
        is IconItem.MaterialIcon -> Icon(
            icon.icon,
            contentDescription = null,
            modifier = modifier
        )
        is IconItem.CustomIcon -> {
            // Use a Box to control the size of the custom icon
            Box(
                modifier = modifier
                    .size(48.dp) // Set the size for custom icons
            ) {
                Image(
                    painter = painterResource(id = icon.resourceId),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize() // Ensures the image fills the Box
                )
            }
        }
    }
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
                    onClick = { navController.navigate("addConnectConfig") },
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
                    onClick = { navController.navigate("addConnectConfig") },
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
            .clickable { navController.navigate("control") }) {

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

@Composable
fun AddConnectConfigScreen(navController: NavController, viewModel: ConnectConfigViewModel) {
    var configName by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf("WiFi") } // Default connection type
    var address by remember { mutableStateOf("") }
    var heartbeat by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<IconItem>(IconItem.MaterialIcon(Icons.Default.Info)) } // Default icon
    val configs by viewModel.allConfigs.collectAsState(initial = emptyList())

    Scaffold(
        topBar = { /* No topBar here, it's effectively removed */ },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Config Name Input
            OutlinedTextField(
                value = configName,
                onValueChange = { configName = it },
                label = { Text("Configuration Name", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Black,
                    unfocusedIndicatorColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // LazyRow for selecting icons from the list
            Text("Select an Icon:", fontWeight = FontWeight.Bold, color = Color.Black)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allIcons) { icon ->
                    IconButton(
                        onClick = { selectedIcon = icon },
                        modifier = Modifier
                            .size(48.dp)
                            .then(
                                if (selectedIcon == icon) Modifier
                                    .graphicsLayer { alpha = 1f } // Selected icon is normal
                                else Modifier
                                    .graphicsLayer { alpha = 0.4f } // Non-selected icons are grayed out
                            )
                    ) {
                        DisplayIcon(icon, modifier = Modifier.size(48.dp)) // Display the icon
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Connection type selection (Radio buttons for WiFi and Bluetooth)
            Text("Select Connection Type:", fontWeight = FontWeight.Bold, color = Color.Black)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedOption == "WiFi",
                    onClick = { selectedOption = "WiFi" },
                    colors = RadioButtonDefaults.colors(selectedColor = Color.Black, unselectedColor = Color.Gray)
                )
                Text("WiFi", color = Color.Black, modifier = Modifier.clickable { selectedOption = "WiFi" })
                Spacer(modifier = Modifier.width(16.dp))
                RadioButton(
                    selected = selectedOption == "Bluetooth",
                    onClick = { selectedOption = "Bluetooth" },
                    colors = RadioButtonDefaults.colors(selectedColor = Color.Black, unselectedColor = Color.Gray)
                )
                Text("Bluetooth", color = Color.Black, modifier = Modifier.clickable { selectedOption = "Bluetooth" })
            }
            Spacer(modifier = Modifier.height(8.dp))

            // IP/MAC Address Input Field
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("IP/MAC Address", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Black,
                    unfocusedIndicatorColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Heartbeat Frequency Input Field
            OutlinedTextField(
                value = heartbeat,
                onValueChange = { heartbeat = it },
                label = { Text("Heartbeat Frequency (Hz)", color = Color.Gray) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Black,
                    unfocusedIndicatorColor = Color.Gray
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val iconId = IconMapper.getIconId(selectedIcon) // Get the ID of the selected icon
                        val config = ConnectConfig(
                            name = configName,
                            connectionType = selectedOption,
                            address = address,
                            heartbeatFrequency = heartbeat.toIntOrNull() ?: 0,
                            iconId = iconId // Save the icon ID in the DB
                        )
                        viewModel.insertConfig(config)
                        navController.navigate("landing")
                    },
                    modifier = Modifier
                        .padding(0.dp)
                        .align(Alignment.Center) // Center the button horizontally
                        .fillMaxWidth(), // Button width set to 80% of the screen width
                    colors = ButtonDefaults.buttonColors(Color.Black), // Black button color
                    shape = RectangleShape // Rectangular shape // Center the button horizontally
                ) {
                    Text("Save new config", color = Color.White,
                    style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Saved Configurations:", fontWeight = FontWeight.Bold, color = Color.Black)

            // Display saved configurations with icons
            LazyColumn {
                items(configs) { config ->
                    val icon = IconMapper.getIconById(config.iconId) // Get the icon using the saved ID
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.LightGray)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Name: ${config.name}", color = Color.Black) // Display config name
                            DisplayIcon(icon, modifier = Modifier.size(48.dp)) // Display the icon
                            Text("Type: ${config.connectionType}", color = Color.Black)
                            Text("Address: ${config.address}", color = Color.Black)
                            Text("Heartbeat: ${config.heartbeatFrequency} Hz", color = Color.Black)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.deleteConfig(config) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Delete", color = Color.White)
                            }
                        }
                    }
                }
            }
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
