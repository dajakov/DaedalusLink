package com.dajakov.daedaluslink

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
import com.dajakov.daedaluslink.ui.theme.DaedalusLinkTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
// import androidx.compose.foundation.isSystemInDarkTheme // No longer directly used here
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.unit.IntOffset


val sharedState = SharedState()

var webSocketManager = WebSocketManager()

class MainActivity : ComponentActivity() {
    private val connectConfigViewModel: ConnectConfigViewModel by viewModels()
    private val linkConfigViewModel: LinkConfigViewModel by viewModels()
    private val debugViewModel: DebugViewModel by viewModels()

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter",
        "UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

//        resetDatabase(applicationContext) //TODO(remove for production) for development purposes only.

        actionBar?.hide()
        setContent {
            DaedalusLinkTheme {
                val navController = rememberNavController()

                val currentDestination by navController.currentBackStackEntryAsState()
                val showBottomBar = currentDestination?.destination?.route?.let { route ->
                    !route.startsWith("loading") && route != "landing"
                            && route != "addConnectConfig"
                } ?: true

                Column(modifier = Modifier.fillMaxSize()) {
                    // Draw colored background behind status bar
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(WindowInsets.statusBars.asPaddingValues()
                                .calculateTopPadding())
                            .background(Color.Black)
                    )

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar(containerColor = MaterialTheme.colorScheme.primary) {
                                    val items = listOf("control", "debug", "settings")
                                    val icons = listOf(Icons.Default.PlayArrow, Icons.Default.Info,
                                        Icons.Default.Settings)

                                    items.forEachIndexed { index, screen ->
                                        NavigationBarItem(
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            icon = { Icon(icons[index],
                                                contentDescription = screen) },
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
                            composable("landing") { LandingScreen(navController,
                                connectConfigViewModel, linkConfigViewModel) }
                            composable("loading/{configIndex}/{linkIndex}") { backStackEntry ->
                                val configIndex = backStackEntry.arguments?.getString("configIndex")
                                    ?.toIntOrNull() ?: 0
                                val linkIndex = backStackEntry.arguments?.getString("linkIndex")
                                    ?.toIntOrNull() ?: 0

                                LoadingScreen(navController, connectConfigViewModel, configIndex,
                                    linkConfigViewModel, linkIndex, debugViewModel)
                            }
                            composable("control") { ControlScreen(navController) }
                            composable("debug") { DebugScreen(navController, debugViewModel) }
                            composable("settings") { SettingsScreen(navController) }
                            composable("addConnectConfig") { AddConnectConfigScreen(navController,
                                connectConfigViewModel) }
                        }
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
            else -> IconItem.MaterialIcon(Icons.Default.Warning)
        }
    }

    // Mapping from IconItem to ID
    fun getIconId(iconItem: IconItem): String {
        return when (iconItem) {
            is IconItem.MaterialIcon -> when (iconItem.icon) {
                Icons.Default.Info -> "info"
                Icons.Default.PlayArrow -> "PlayArrow"
                else -> "default"
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
fun DisplayIcon(icon: IconItem, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    when (icon) {
        is IconItem.MaterialIcon -> Icon(
            imageVector = icon.icon,
            contentDescription = null,
            modifier = modifier,
            tint = tint
        )
        is IconItem.CustomIcon -> {
            Image(
                painter = painterResource(id = icon.resourceId),
                contentDescription = null,
                modifier = modifier, // Apply the modifier directly to the Image
                colorFilter = ColorFilter.tint(tint)
            )
        }
    }
}

@Composable
fun CustomIcon(resourceId: Int, selectedIndex: MutableState<Int>, index: Int) {
    val painter = painterResource(id = resourceId)

    val tintColor = if (index == selectedIndex.value) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.onSurfaceVariant

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
        colorFilter = ColorFilter.tint(tintColor)
    )
}

@Composable
fun LandingScreen(navController: NavController, connectConfigViewModel: ConnectConfigViewModel,
                  linkConfigViewModel: LinkConfigViewModel) {
    sharedState.clear()

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp

    val density = LocalDensity.current
    val screenWidthPx = with(density) { screenWidth.toPx() }
    val diameter = screenWidthPx * 0.4f

    var linkIndex by remember { mutableIntStateOf(-1) }
    val selectedIndex = remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    val connectConfigs by connectConfigViewModel.allConfigs.collectAsState(initial = emptyList())
    val dynamicIcons = connectConfigs.map { config ->
        IconMapper.getIconById(config.iconId)
    }
    val icons = listOf(IconItem.MaterialIcon(Icons.Filled.Add)) +
            dynamicIcons +
            listOf(IconItem.MaterialIcon(Icons.Filled.Settings))
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
        .background(MaterialTheme.colorScheme.primary),) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            Text(
                text = texts.getOrElse(selectedIndex.intValue) { "Unnamed" },
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.align(Alignment.Center),
                color = MaterialTheme.colorScheme.onPrimary
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
                        .clickable { selectedIndex.intValue = index }
                ) {
                    when (iconItem) {
                        is IconItem.MaterialIcon -> {
                            Icon(
                                imageVector = iconItem.icon,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                tint = if (index == selectedIndex.intValue) MaterialTheme.colorScheme.surfaceVariant
                                else MaterialTheme.colorScheme.onSurfaceVariant
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
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { navController.navigate("addConnectConfig") },
                    modifier = Modifier
                        .padding(15.dp)
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary),
                    shape = RectangleShape
                ) {
                    if (selectedIndex.intValue == 0){
                        Text(
                            "Add a new Robot",
                            color = MaterialTheme.colorScheme.onSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            "App settings",
                            color = MaterialTheme.colorScheme.onSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            var mExpanded by remember { mutableStateOf(false) }
            val linkConfigs by linkConfigViewModel.allConfigs.collectAsState(initial = emptyList())
            val configNames = linkConfigs.map { it.name } + "Auto-Pull from Robot"
            val icon = if (mExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown

            Column(Modifier.padding(15.dp)) {
                Text("Select Link Config", modifier = Modifier.padding(bottom = 5.dp), color = MaterialTheme.colorScheme.onPrimary)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(8.dp))
                        .clickable { mExpanded = !mExpanded }
                        .padding(16.dp)
                ) {
                    Text(
                        text = configNames.getOrNull(linkIndex) ?: "[Auto-Pull from Robot]",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Icon(
                        imageVector = icon,
                        contentDescription = "Dropdown icon",
                        modifier = Modifier.align(Alignment.CenterEnd).padding(5.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
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
                            text = { Text(text = name, color = MaterialTheme.colorScheme.onPrimary) }
                        )
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { navController.navigate("addConnectConfig") },
                    modifier = Modifier
                        .padding(0.dp)
                        .align(Alignment.Center)
                        .fillMaxWidth(0.9f),
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary),
                    shape = RectangleShape
                ) {
                    Text(
                        "Add a new config",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        var canvasHeight by remember { mutableFloatStateOf(0f) }
        val noConfigElementSelected = selectedIndex.intValue == 0 || selectedIndex.intValue == icons.size - 1

        val canvasDrawingColor = if (noConfigElementSelected) MaterialTheme.colorScheme.onSurfaceVariant
        else MaterialTheme.colorScheme.surfaceVariant

        Canvas(modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                canvasHeight = coordinates.size.height.toFloat()
            }
            .let { baseModifier ->
                if (!noConfigElementSelected) {
                    baseModifier.clickable {
                        navController.navigate("loading/${(configIDs
                            .getOrNull(selectedIndex.intValue - 1))}/${linkIndex}")
                    }
                } else {
                    baseModifier
                }
            }
        ) {
            val bottomOffset = canvasHeight - diameter

            drawArc(
                color = canvasDrawingColor,
                topLeft = Offset(
                    screenWidthPx * 0.5f - diameter / 2,
                    bottomOffset - diameter * 0.3f
                ),
                size = Size(diameter, diameter),
                startAngle = 300f,
                sweepAngle = 300f,
                style = Stroke(30f),
                useCenter = false
            )
            drawRect(
                color = canvasDrawingColor,
                topLeft = Offset(
                    screenWidthPx * 0.5f - 15f,
                    bottomOffset - diameter * 0.5f
                ),
                size = Size(30f, diameter * 0.7f)
            )
        }

        Box(
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = "1.2.0-alpha",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Center)
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

    fun updateSteps(step: String, isSameLine: Boolean = false) {
        steps = if (isSameLine) {
            if (steps.isNotEmpty()) {
                val lastStep = steps.last() + " | " + step
                steps.dropLast(1) + lastStep
            } else {
                listOf(step)
            }
        } else {
            steps + step
        }
    }

    // Function to perform a ping to an IP
    suspend fun performPing(ipAddress: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cleanIp = ipAddress.substringBefore(":")
                val address = InetAddress.getByName(cleanIp)
                address.isReachable(2000)
            } catch (_: IOException) {
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
            showExitButton = true
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
                showExitButton = true
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
                true
            }

            if (jsonReceived == true) {
                updateSteps("✅", true)
            } else {
                updateSteps("❌", true)
                connectionSuccess = false
                showExitButton = true
            }
        }

        delay(2000)

        if (connectionSuccess) {
            navController.navigate("control")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = debugText,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))
            steps.forEach { step ->
                Text(
                    text = step,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp
                )
            }

            if (showExitButton) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navController.navigate("landing") },
                    modifier = Modifier
                        .padding(0.dp)
                        .fillMaxWidth(0.9f), // Button width set to 80% of the screen width
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary), // Black button color
                    shape = RectangleShape // Rectangular shape
                ) {
                    Text(
                        "Exit",
                        color = MaterialTheme.colorScheme.onSecondary, // Text color set to white to contrast the black button
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
    var selectedOption by remember { mutableStateOf("WiFi") }
    var address by remember { mutableStateOf("") }
    var heartbeat by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<IconItem>(IconItem
        .MaterialIcon(Icons.Default.Info)) } // Default icon
    val configs by viewModel.allConfigs.collectAsState(initial = emptyList())

    Scaffold(containerColor = MaterialTheme.colorScheme.primary){ paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .background(MaterialTheme.colorScheme.primary)
        ) {
            OutlinedTextField(
                value = configName,
                onValueChange = { configName = it },
                label = { Text("Configuration Name", color = MaterialTheme.colorScheme.onPrimary) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text("Select an Icon:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
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
                                    .graphicsLayer { alpha = 1f }
                                else Modifier
                                    .graphicsLayer {
                                        alpha = 0.4f
                                    }
                            )
                    ) {
                        DisplayIcon(icon, modifier = Modifier.size(48.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Connection type selection (Radio buttons for WiFi and Bluetooth)
            Text("Select Connection Type:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selectedOption == "WiFi",
                    onClick = { selectedOption = "WiFi" },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text("WiFi", color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.clickable
                { selectedOption = "WiFi" })

                Spacer(modifier = Modifier.width(16.dp))

                RadioButton(
                    selected = selectedOption == "Bluetooth",
                    onClick = { selectedOption = "Bluetooth" },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.onPrimary,
                        unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant)
                )
                Text("Bluetooth", color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.clickable
                { selectedOption = "Bluetooth" })
            }
            Spacer(modifier = Modifier.height(8.dp))

            // IP/MAC Address Input Field
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("IP/MAC Address", color = MaterialTheme.colorScheme.onPrimary) },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Heartbeat Frequency Input Field
            OutlinedTextField(
                value = heartbeat,
                onValueChange = { heartbeat = it },
                label = { Text("Heartbeat Frequency (Hz)", color = MaterialTheme.colorScheme.onPrimary) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.primary,
                    unfocusedContainerColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        val iconId = IconMapper.getIconId(selectedIcon)
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
                        .align(Alignment.Center)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.onPrimary),
                    shape = RectangleShape
                ) {
                    Text("Save new config", color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.bodyMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Saved Configurations:", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)

            // Display saved configurations with icons
            LazyColumn {
                items(configs) { config ->
                    val icon = IconMapper.getIconById(config.iconId)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Name: ${config.name}", color = MaterialTheme.colorScheme.onPrimary)
                            DisplayIcon(icon, modifier = Modifier.size(48.dp))
                            Text("Type: ${config.connectionType}", color = MaterialTheme.colorScheme.onPrimary)
                            Text("Address: ${config.address}", color = MaterialTheme.colorScheme.onPrimary)
                            Text("Heartbeat: ${config.heartbeatFrequency} Hz",
                                color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.deleteConfig(config) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Delete", color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GridLayout(content: @Composable (cellSize: Pair<Dp, Dp>, offset: Pair<Dp, Dp>) -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    val cornerPadding = 8.dp
    val usableWidth = screenWidth - 2 * cornerPadding
    val usableHeight = screenHeight - 2 * cornerPadding

    val virtualColumns = 8
    val virtualRows = 14
    val cellSize = minOf(usableWidth / virtualColumns, usableHeight / virtualRows)

    val gridWidth = cellSize * virtualColumns

    val horizontalPadding = (screenWidth - gridWidth) / 2
    val verticalPadding = cornerPadding

    val circleColor = MaterialTheme.colorScheme.onPrimary

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .offset {
                    IntOffset(
                        x = with(density) { horizontalPadding.roundToPx() },
                        y = with(density) { verticalPadding.roundToPx() }
                    )
                }
        ) {
            for (row in 0..virtualRows) {
                for (col in 0..virtualColumns) {
                    drawCircle(
                        color = circleColor,
                        radius = 2.dp.toPx(),
                        center = Offset(
                            x = col * cellSize.toPx(),
                            y = row * cellSize.toPx()
                        )
                    )
                }
            }
        }

        content(Pair(cellSize, cellSize), Pair(horizontalPadding,
            verticalPadding))
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
        gridSize: Pair<Dp, Dp>,
        offset: Pair<Dp, Dp>,
        onPress: (String) -> Unit,
        onRelease: (String) -> Unit
    ) {
        val (cellWidth, cellHeight) = gridSize
        val (offsetX, offsetY) = offset
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()

        LaunchedEffect(isPressed) {
            if (isPressed) onPress(element.pressCommand)
            else onRelease(element.pressCommand)
        }

        Box(
            modifier = Modifier
                .absoluteOffset(
                    x = offsetX + element.position[0] * cellWidth,
                    y = offsetY + element.position[1] * cellHeight
                )
                .size(
                    width = element.size[0] * cellWidth,
                    height = element.size[1] * cellHeight
                )
        ) {
            Button(
                onClick = {},
                interactionSource = interactionSource,
                modifier = Modifier.fillMaxSize(),
                colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary),
                shape = RectangleShape
            ) {
                Text(element.label, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }

    @Composable
    fun JoystickElement(
        element: InterfaceData,
        gridSize: Pair<Dp, Dp>,
        offset: Pair<Dp, Dp>,
        onMove: (Byte, Byte) -> Unit
    ) {
        val (cellWidth, cellHeight) = gridSize
        val (offsetX, offsetY) = offset
        val density = LocalDensity.current

        val joystickSizeDp = element.size[0] * cellWidth
        val joystickRadiusPx = with(density) { 40.dp.toPx() }

        var offsetXInternal by remember { mutableFloatStateOf(0f) }
        var offsetYInternal by remember { mutableFloatStateOf(0f) }

        val circleColor = MaterialTheme.colorScheme.primary

        Box(
            modifier = Modifier
                .absoluteOffset(
                    x = offsetX + element.position[0] * cellWidth,
                    y = offsetY + element.position[1] * cellHeight
                )
                .size(
                    width = element.size[0] * cellWidth,
                    height = element.size[1] * cellHeight
                )
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFC7C7C7)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(joystickSizeDp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                offsetXInternal = 0f
                                offsetYInternal = 0f
                                onMove(0, 0)
                            },
                            onDrag = { _, dragAmount ->
                                val maxOffsetX = size.width / 2f - joystickRadiusPx
                                val maxOffsetY = size.height / 2f - joystickRadiusPx

                                offsetXInternal = (offsetXInternal + dragAmount.x)
                                    .coerceIn(-maxOffsetX, maxOffsetX)
                                offsetYInternal = (offsetYInternal + dragAmount.y)
                                    .coerceIn(-maxOffsetY, maxOffsetY)

                                val normalizedX = ((offsetXInternal / maxOffsetX) * 127).toInt()
                                    .coerceIn(-128, 127).toByte()
                                val normalizedY = ((offsetYInternal / maxOffsetY) * 127).toInt()
                                    .coerceIn(-128, 127).toByte()

                                onMove(normalizedX, normalizedY)
                            }
                        )
                    }
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    color = circleColor,
                    radius = joystickRadiusPx,
                    center = center + Offset(offsetXInternal, offsetYInternal)
                )
            }
        }
    }

    @Composable
    fun DynamicUI(jsonString: String) {
        val config = remember { json.decodeFromString<LinkConfig>(jsonString) }

        GridLayout { gridSize, offset ->
            Box(modifier = Modifier.fillMaxSize()) {
                config.interfaceData.forEach { element ->
                    when (element.type) {
                        "button" -> ButtonElement(
                            element, gridSize, offset,
                            onPress = { cmd -> webSocketManager.sendCommand(cmd) },
                            onRelease = { cmd -> webSocketManager.sendCommand("!$cmd") }
                        )
                        "joystick" -> JoystickElement(
                            element, gridSize, offset,
                            onMove = { x, y -> webSocketManager.sendMovementCommand(x, y) }
                        )
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
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

    Scaffold (Modifier.background(MaterialTheme.colorScheme.primary)){
        Column(
            Modifier
                .background(MaterialTheme.colorScheme.primary)
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
                        Text(text = key.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.titleMedium)
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
    val safeRange = if (yRange == 0f) 1f else yRange

    val yAxisData = AxisData.Builder()
        .steps(steps)
        .backgroundColor(MaterialTheme.colorScheme.primary)
        .labelAndAxisLinePadding(20.dp)
        .labelData { i ->
            val yScale = safeRange / steps
            ((i * yScale) + yMin).formatToSinglePrecision()
        }
        .build()

    val xAxisData = AxisData.Builder()
        .axisStepSize(100.dp)
        .backgroundColor(MaterialTheme.colorScheme.primary)
        .steps(rpmData.size - 1)
        .labelData { "" }
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
        backgroundColor = MaterialTheme.colorScheme.primary
    )

    Column(Modifier.fillMaxWidth().padding(8.dp)) {
        LineChart(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary)
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
        navController.navigate("landing")
    }

    Scaffold (
        Modifier.background(MaterialTheme.colorScheme.primary),
        topBar = { },
    ) {
    }
}
