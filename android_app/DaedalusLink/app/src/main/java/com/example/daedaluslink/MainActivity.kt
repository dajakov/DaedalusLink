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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetAddress
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
import co.yml.charts.axis.AxisData
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.GridLines
import co.yml.charts.ui.linechart.model.IntersectionPoint
import co.yml.charts.ui.linechart.model.Line
import co.yml.charts.ui.linechart.model.LineChartData
import co.yml.charts.ui.linechart.model.LinePlotData
import co.yml.charts.ui.linechart.model.LineStyle
import co.yml.charts.ui.linechart.model.SelectionHighlightPoint
import co.yml.charts.ui.linechart.model.SelectionHighlightPopUp
import co.yml.charts.ui.linechart.model.ShadowUnderLine
import co.yml.charts.common.extensions.formatToSinglePrecision
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

val sharedState = SharedState()

var webSocketManager = WebSocketManager()

class MainActivity : ComponentActivity() {
    // Initialize ViewModel
    private val connectConfigViewModel: ConnectConfigViewModel by viewModels()
    private val linkConfigViewModel: LinkConfigViewModel by viewModels()
    private val debugViewModel: DebugViewModel by viewModels()

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter", "UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

//        resetDatabase(applicationContext) // TODO(remove for production) for development purposes only.

        actionBar?.hide()
        setContent {
            val navController = rememberNavController()

            val currentDestination by navController.currentBackStackEntryAsState()
            val showBottomBar = currentDestination?.destination?.route?.let { route ->
                !route.startsWith("loading") && route != "landing" && route != "addConnectConfig"
            } ?: true

            Column(modifier = Modifier.fillMaxSize()) {
                // 🔷 Draw colored background behind status bar
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                        .background(Color.Black) // or any color that matches your theme
                )

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
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
                    NavHost(navController, startDestination = "landing") {
                        composable("landing") { LandingScreen(navController, connectConfigViewModel, linkConfigViewModel) }
                        composable("loading/{configIndex}/{linkIndex}") { backStackEntry ->
                            val configIndex = backStackEntry.arguments?.getString("configIndex")?.toIntOrNull() ?: 0
                            val linkIndex = backStackEntry.arguments?.getString("linkIndex")?.toIntOrNull() ?: 0

                            LoadingScreen(navController, connectConfigViewModel, configIndex, linkConfigViewModel, linkIndex, debugViewModel)
                        }
                        composable("control") { ControlScreen(navController) }
                        composable("debug") { DebugScreen(navController, debugViewModel) }
                        composable("settings") { SettingsScreen(navController) }
                        composable("addConnectConfig") { AddConnectConfigScreen(navController, connectConfigViewModel) }
                    }
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
    IconItem.CustomIcon(R.drawable.hexapod),
    IconItem.CustomIcon(R.drawable.siggi)
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
            "info" -> IconItem.MaterialIcon(Icons.Default.Info)
            "PlayArrow" -> IconItem.MaterialIcon(Icons.Default.PlayArrow)
            "r2d2" -> IconItem.CustomIcon(R.drawable.r2d2)
            "hexapod" -> IconItem.CustomIcon(R.drawable.hexapod)
            "siggi" -> IconItem.CustomIcon(R.drawable.siggi)
            else -> IconItem.MaterialIcon(Icons.Default.Warning) // Default icon if the ID is unrecognized
        }
    }

    // Mapping from IconItem to ID
    fun getIconId(iconItem: IconItem): String {
        return when (iconItem) {
            is IconItem.MaterialIcon -> when (iconItem.icon) {
                Icons.Default.Info -> "info"
                Icons.Default.PlayArrow -> "PlayArrow"
                else -> "default" // Default if the icon doesn't match
            }
            is IconItem.CustomIcon -> when (iconItem.resourceId) {
                R.drawable.r2d2 -> "r2d2"
                R.drawable.hexapod -> "hexapod"
                R.drawable.siggi -> "siggi"
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
fun LandingScreen(navController: NavController, connectConfigViewModel: ConnectConfigViewModel,
                  linkConfigViewModel: LinkConfigViewModel) {
    sharedState.clear()

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    // Convert dp to px using LocalDensity inside the composable context
    val density = LocalDensity.current
    val screenWidthPx = with(density) { screenWidth.toPx() }
    val diameter = screenWidthPx * 0.4f

    // Track the selected config index (-1 for Auto-Pull)
    var linkIndex by remember { mutableIntStateOf(-1) }

    val selectedIndex = remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()


    val connectConfigs by connectConfigViewModel.allConfigs.collectAsState(initial = emptyList())

    val dynamicIcons = connectConfigs.map { config ->
        IconMapper.getIconById(config.iconId)
    }

    // Create final icons list with "Add" and "Settings"
    val icons = listOf(IconItem.MaterialIcon(Icons.Filled.Add)) +
            dynamicIcons +
            listOf(IconItem.MaterialIcon(Icons.Filled.Settings))

    // Get the names corresponding to the icons
    val texts = listOf("Add...") +
            connectConfigs.map { it.name } +
            listOf("Settings")

    val configIDs = connectConfigs.map { it.id}

    LaunchedEffect(selectedIndex.intValue) {
        val targetOffset = with(density) {
            val size = if (selectedIndex.intValue == 0) 120.dp else 100.dp
            (selectedIndex.intValue * size.toPx()) - (screenWidthPx / 2) + (size.toPx() / 2)
        }
        scrollState.animateScrollTo(targetOffset.toInt())
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.White),) {
        Box(
            modifier = Modifier
                .fillMaxWidth() // Take up the full width
                .padding(top = 16.dp) // Padding from the top
        ) {
            Text(
                text = texts.getOrElse(selectedIndex.intValue) { "Unnamed" },  // Dynamically change text based on selected icon
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
                            Icon(
                                imageVector = iconItem.icon,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                tint = if (index == selectedIndex.intValue) Color.Black else Color.Gray
                            )
                        }

                        is IconItem.CustomIcon -> {
                            CustomIcon(
                                resourceId = iconItem.resourceId,
                                selectedIndex = selectedIndex,
                                index = index
                            )
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
            var mExpanded by remember { mutableStateOf(false) }

            // Collect available LinkConfigs from ViewModel
            val linkConfigs by linkConfigViewModel.allConfigs.collectAsState(initial = emptyList())

            // Create a list of config names + Auto-Pull
            val configNames = linkConfigs.map { it.name } + "Auto-Pull from Robot"

            val icon = if (mExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown

            Column(Modifier.padding(15.dp)) {
                Text("Select Link Config", modifier = Modifier.padding(bottom = 5.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .clickable { mExpanded = !mExpanded }
                        .padding(16.dp)
                ) {
                    Text(
                        text = configNames.getOrNull(linkIndex) ?: "[Auto-Pull from Robot]",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )

                    Icon(
                        imageVector = icon,
                        contentDescription = "Dropdown icon",
                        modifier = Modifier.align(Alignment.CenterEnd).padding(5.dp)
                    )
                }

                DropdownMenu(
                    expanded = mExpanded,
                    onDismissRequest = { mExpanded = false },
                    modifier = Modifier.width(200.dp).padding(top = 8.dp)
                ) {
                    configNames.forEachIndexed { index, name ->
                        DropdownMenuItem(
                            onClick = {
                                linkIndex = if (index == configNames.lastIndex) -1 else index
                                mExpanded = false
                            },
                            text = { Text(text = name) }
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
        val noConfigElementSelected = selectedIndex.intValue == 0 || selectedIndex.intValue == icons.size - 1
        Canvas(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                // Get the size of the canvas (height in this case)
                canvasHeight = coordinates.size.height.toFloat()
            }
            .let { baseModifier ->
                if (!noConfigElementSelected) {
                    baseModifier.clickable {
                        navController.navigate("loading/${(configIDs.getOrNull(selectedIndex.intValue - 1))}/${linkIndex}")
                    }
                } else {
                    baseModifier
                }
            }
        ) {
            // Calculate the bottom position for the arc and rect
            val bottomOffset = canvasHeight - diameter // Bottom of the canvas
            val color = if (noConfigElementSelected) Color.Gray else Color.Black

            // Arc: Position the y-coordinate relative to the bottom
            drawArc(
                color = color,
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
                color = color,
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
                text = "1.0.0-alpha", // Dynamic version text can go here
                color = Color.Gray,  // Grayed-out text
                fontSize = 12.sp,     // Small font size
                modifier = Modifier
                    .align(Alignment.Center)  // Align to the center of the Box
            )
        }
    }
}

@Composable
fun LoadingScreen(navController: NavController, connectConfigViewModel: ConnectConfigViewModel,
                  configIndex: Int, linkConfigViewModel: LinkConfigViewModel, linkIndex: Int,
                  debugViewModel: DebugViewModel) {
    BackHandler { }

    val connectConfigs by connectConfigViewModel.allConfigs.collectAsState(initial = emptyList())
    if (connectConfigs.isEmpty()) {
        // 🔹 Show loading UI while waiting for data
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val activeConfig = connectConfigs.find { it.id == configIndex }

    val ipAddress = activeConfig?.address
    val robotName = activeConfig?.name
    val heartbeatFrequency = activeConfig?.heartbeatFrequency!!.toLong()

    var debugText by remember { mutableStateOf("Initializing...") }
    var connectionSuccess by remember { mutableStateOf(true) }
    var showExitButton by remember { mutableStateOf(false) }
    var steps by remember { mutableStateOf(listOf<String>()) }

    // Function to update steps in the UI (with the option for same line or newline)
    fun updateSteps(step: String, isSameLine: Boolean = false) {
        steps = if (isSameLine) {
            // Append the step to the last entry, keeping it on the same line
            if (steps.isNotEmpty()) {
                val lastStep = steps.last() + " | " + step
                steps.dropLast(1) + lastStep
            } else {
                listOf(step)
            }
        } else {
            // Add the step as a new entry (new line)
            steps + step
        }
    }

    // Function to perform a ping to an IP
    suspend fun performPing(ipAddress: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cleanIp = ipAddress.substringBefore(":") // ✅ Remove port if present
                val address = InetAddress.getByName(cleanIp)
                address.isReachable(2000) // ✅ Timeout: 2 seconds
            } catch (e: IOException) {
                false
            }
        }
    }

    LaunchedEffect(Unit) {
        // Step 1: Ping the IP address
        updateSteps("Pinging $ipAddress...")
        debugText = "Connecting to $robotName... "
        val pingResult = ipAddress?.let { performPing(it) }
        var webSocketResult = false

        if (pingResult == true) {
            updateSteps("✅", true)
        } else {
            updateSteps("❌", true)
            connectionSuccess = false
            showExitButton = true // Show exit button if ping fails
        }

        // Step 2: Try to connect to WebSocket
        if (pingResult == true) {
            updateSteps("Connecting to WebSocket... ")

            webSocketResult = webSocketManager.connectToWebSocket(
                "ws://$ipAddress", sharedState, heartbeatFrequency, debugViewModel
            )

            if (!webSocketResult) {
                updateSteps("❌", true)
                connectionSuccess = false
                showExitButton = true // Show exit button if WebSocket fails
            } else {
                updateSteps("✅", true)
                connectionSuccess = true
            }
        }

        // Step 3: Wait for a valid JSON file
        if (webSocketResult) {
            updateSteps("Waiting for JSON file... ")
            val jsonReceived = withTimeoutOrNull(5000) {
                while (!sharedState.isJsonReceived) {
                    delay(500)
                }
                true // JSON received within time
            }

            if (jsonReceived == true) {
                updateSteps("✅", true)
            } else {
                updateSteps("❌", true)
                connectionSuccess = false
                showExitButton = true // Show exit button if JSON reception fails
            }
        }

        delay(2000)

        if (connectionSuccess) {
            navController.navigate("control")
        }
    }

    // UI for loading screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = debugText,
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
            // Display the steps list under the progress indicator
            steps.forEach { step ->
                Text(
                    text = step,
                    color = Color.Black,
                    fontSize = 14.sp
                )
            }

            // Show exit button if there's a failure
            if (showExitButton) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate("landing") },
                    modifier = Modifier
                        .padding(0.dp)
                        .fillMaxWidth(0.9f), // Button width set to 80% of the screen width
                    colors = ButtonDefaults.buttonColors(Color.Black), // Black button color
                    shape = RectangleShape // Rectangular shape
                ) {
                    Text(
                        "Exit",
                        color = Color.White, // Text color set to white to contrast the black button
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
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
                                    .graphicsLayer {
                                        alpha = 0.4f
                                    } // Non-selected icons are grayed out
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

@Composable
fun GridLayout(content: @Composable (Pair<Float, Float>) -> Unit) {
    val screenHeight = LocalConfiguration.current.screenHeightDp - 48 // Exclude bottom nav (adjust as needed)
    val screenWidth = LocalConfiguration.current.screenWidthDp

    val columns = 8
    val rows = 14

    val cellWidth = screenWidth / columns.toFloat()
    val cellHeight = screenHeight / rows.toFloat()

    val gridSize = Pair(cellWidth, cellHeight)

    Box(modifier = Modifier.fillMaxSize()) {
        content(gridSize) // Pass gridSize to children
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ControlScreen(navController: NavController) {
    BackHandler {
        webSocketManager.disconnect()
        navController.navigate("landing")
    }

    val receivedJsonData = sharedState.receivedJsonData

    @Composable
    fun ButtonElement(
        element: InterfaceData,
        gridSize: Pair<Float, Float>,
        onPress: (String) -> Unit,
        onRelease: (String) -> Unit
    ) {
        val (cellWidth, cellHeight) = gridSize
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        LaunchedEffect(isPressed) {
            if (isPressed) {
                onPress(element.pressCommand)
            } else {
                onRelease(element.pressCommand)
            }
        }

        Box(
            modifier = Modifier
                .absoluteOffset(
                    x = (element.position[0] * cellWidth + 2).dp,
                    y = (element.position[1] * cellHeight + 2).dp
                )
                .size(
                    width = (element.size[0] * cellWidth - 4).dp,
                    height = (element.size[1] * cellHeight - 4).dp
                )
                .padding(0.dp)
        ) {
            Button(
                onClick = {}, // handle press/release manually
                interactionSource = interactionSource,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp),
                colors = ButtonDefaults.buttonColors(Color.Black),
                shape = RectangleShape
            ) {
                Text(element.label, color = Color.White)
            }
        }
    }

    @Composable
    fun JoystickElement(element: InterfaceData, gridSize: Pair<Float, Float>, onMove: (Byte, Byte) -> Unit) {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val joystickRadius = 80f
        var offsetX by remember { mutableFloatStateOf(0f) }
        var offsetY by remember { mutableFloatStateOf(0f) }

        val (cellWidth, cellHeight) = gridSize

        Box(
            modifier = Modifier
                .absoluteOffset(
                    x = (element.position[0] * cellWidth + 2).dp,
                    y = (element.position[1] * cellHeight + 2).dp
                )
                .size(
                    width = (element.size[0] * cellWidth - 4).dp,
                    height = (element.size[1] * cellHeight - 4).dp
                )
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFC7C7C7)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(screenWidth, screenWidth)
                    .width((element.size[0] * cellWidth - 2).dp)
                    .height((element.size[0] * cellWidth - 2).dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                offsetX = 0f
                                offsetY = 0f
                                onMove(0, 0) // Send (0, 0) on drag end
                            },
                            onDrag = { _, dragAmount ->
                                // Update offsets while ensuring they are within bounds
                                offsetX = (offsetX + dragAmount.x).coerceIn(
                                    -screenWidth.toPx() / 2 + joystickRadius,
                                    screenWidth.toPx() / 2 - joystickRadius
                                )
                                offsetY = (offsetY + dragAmount.y).coerceIn(
                                    -screenWidth.toPx() / 2 + joystickRadius,
                                    screenWidth.toPx() / 2 - joystickRadius
                                )

                                // Normalize the offsets to the range [-128, 127]
                                val normalizedX = ((offsetX / (screenWidth.toPx() / 2 - joystickRadius)) * 127).toInt().coerceIn(-128, 127).toByte()
                                val normalizedY = ((offsetY / (screenWidth.toPx() / 2 - joystickRadius)) * 127).toInt().coerceIn(-128, 127).toByte()

                                // Pass normalized values to the callback
                                onMove(normalizedX, normalizedY)
                            }
                        )
                    }
            ) {
                val canvasCenter = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    color = Color.Black,
                    radius = joystickRadius,
                    center = canvasCenter + Offset(offsetX, offsetY)
                )
            }
        }
    }

    @Composable
    fun DynamicUI(jsonString: String) {
        // Deserialize JSON into LinkConfig object
        val config = remember { json.decodeFromString<LinkConfig>(jsonString) }

        GridLayout { gridSize ->
            Box(modifier = Modifier.fillMaxSize()) {
                config.interfaceData.forEach { element ->
                    when (element.type) {
                        "button" -> ButtonElement(
                            element,
                            gridSize,
                            onPress = { command -> webSocketManager.sendCommand(command) },
                            onRelease = { command -> webSocketManager.sendCommand("!${command}") } // Or another logic
                        )
                        "joystick" -> JoystickElement(element, gridSize,
                            onMove = { x, y ->
                                webSocketManager.sendMovementCommand(
                                    x,
                                    y
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        if (receivedJsonData.isNotEmpty()) {
            DynamicUI(receivedJsonData)
        } else {
            Text("No JSON file received!")
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun DebugScreen(navController: NavController, debugViewModel: DebugViewModel) {
    val debugData = debugViewModel.debugData

    BackHandler {
        navController.navigate("landing")
    }

    Scaffold (Modifier.background(Color.White)){
        Column(
            Modifier
                .background(Color.White)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Debug Charts", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))

            if (debugData.isEmpty()) {
                Text("Waiting for debug data...")
            } else {
                debugData.forEach { (key, points) ->
                    if (points.isNotEmpty()) {
                        Text(text = key.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleMedium)
                        RpmChartScreen(rpmData = points)
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RpmChartScreen(rpmData: List<Point>) {
    if (rpmData.isEmpty()) return

    val steps = 5
    val yMin = rpmData.minOf { it.y }
    val yMax = rpmData.maxOf { it.y }
    val yRange = yMax - yMin
    val safeRange = if (yRange == 0f) 1f else yRange // avoid division by zero

    val yAxisData = AxisData.Builder()
        .steps(steps)
        .backgroundColor(Color.White)
        .labelAndAxisLinePadding(20.dp)
        .labelData { i ->
            val yScale = safeRange / steps
            ((i * yScale) + yMin).formatToSinglePrecision()
        }
        .build()

    val xAxisData = AxisData.Builder()
        .axisStepSize(100.dp)
        .backgroundColor(Color.White)
        .steps(rpmData.size - 1)
        .labelData { "" } // Empty string hides the labels
        .labelAndAxisLinePadding(0.dp)
        .build()

    val lineChartData = LineChartData(
        linePlotData = LinePlotData(
            lines = listOf(
                Line(
                    dataPoints = rpmData,
                    lineStyle = LineStyle(),
                    intersectionPoint = IntersectionPoint(),
                    selectionHighlightPoint = SelectionHighlightPoint(),
                    shadowUnderLine = ShadowUnderLine(),
                    selectionHighlightPopUp = SelectionHighlightPopUp()
                )
            )
        ),
        xAxisData = xAxisData,
        yAxisData = yAxisData,
        gridLines = GridLines(),
        backgroundColor = Color.White
    )

    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        LineChart(
            modifier = Modifier
                .background(Color.White)
                .fillMaxWidth()
                .height(250.dp),
            lineChartData = lineChartData
        )
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun SettingsScreen(navController: NavController) {
    BackHandler {
        navController.navigate("landing") // Instead of going back, navigate to Home
    }

    Scaffold (
        Modifier.background(Color.White),
        topBar = { /* No topBar here, it's effectively removed */ },
    ) {
//        Column(modifier = Modifier
//            .fillMaxSize()
//            .padding(16.dp)) {
//            Button(onClick = { /* Do something */ }) {
//                Text("Notifications Button")
//            }
//        }
    }
}
