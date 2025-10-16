package com.dajakov.daedaluslink

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLayoutDirection

val sharedState = SharedState() // Global shared state

class MainActivity : ComponentActivity() {
    private val connectConfigViewModel: ConnectConfigViewModel by viewModels()
    private val linkConfigViewModel: LinkConfigViewModel by viewModels()
    private val debugViewModel: DebugViewModel by viewModels()

    private lateinit var analyticsLogger: AnalyticsLogger
    private lateinit var webSocketManager: WebSocketManager

    @SuppressLint("UnusedMaterialScaffoldPaddingParameter",
        "UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        analyticsLogger = getAnalyticsProvider(applicationContext).getLogger()
        webSocketManager = WebSocketManager(analyticsLogger)

//        resetDatabase(applicationContext) //TODO(remove for production) for development purposes only.

        actionBar?.hide()

        setContent {
            DaedalusLinkTheme {
                // Control status bar appearance
                val view = LocalView.current
                val primaryColor = MaterialTheme.colorScheme.primary
                // Determine if the primary color is "dark".
                // A simple heuristic: if luminance is < 0.5, it's dark.
                // Adjust this threshold if needed for your specific color scheme.
                val isPrimaryColorDark = primaryColor.toArgb().let { color ->
                    // Extract RGB components
                    val red = android.graphics.Color.red(color)
                    val green = android.graphics.Color.green(color)
                    val blue = android.graphics.Color.blue(color)
                    // Calculate luminance (simplified formula)
                    (0.299 * red + 0.587 * green + 0.114 * blue) / 255 < 0.5
                }

                SideEffect {
                    val window = this.window
                    window.statusBarColor = AndroidColor.TRANSPARENT
                    WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isPrimaryColorDark
                }

                val navController = rememberNavController()

                val currentDestination by navController.currentBackStackEntryAsState()
                val showBottomBar = currentDestination?.destination?.route?.let { route ->
                    !route.startsWith("loading") && route != "landing"
                            && route != "addConnectConfig" && route != "appSettings"
                } ?: true

                Column(modifier = Modifier.fillMaxSize()) {
                    // Draw colored background behind status bar
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsTopHeight(WindowInsets.statusBars) // Use specific inset height
                            .background(MaterialTheme.colorScheme.primary) // Use theme color
                    )

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        // Apply padding to avoid content overlapping with system bars if Scaffold is not fullscreen
                        // For edge-to-edge, top padding is handled by the Spacer, bottom by navigation gestures or Scaffold's bottomBar padding
                        contentWindowInsets = WindowInsets(0,0,0,0), // Let content go edge to edge within scaffold
                        bottomBar = {
                            if (showBottomBar) {
                                NavigationBar(containerColor = MaterialTheme.colorScheme.primary) {
                                    val items = listOf("control", "debug", "settings")
                                    val iconsList = listOf(Icons.Default.PlayArrow, Icons.Default.Info,
                                        Icons.Default.Settings)

                                    items.forEachIndexed { index, screen ->
                                        NavigationBarItem(
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            icon = { Icon(iconsList[index],
                                                contentDescription = screen) },
                                            label = { Text(screen) },
                                            selected = navController.currentDestination?.route == screen,
                                            onClick = { navController.navigate(screen) }
                                        )
                                    }
                                }
                            }
                        }
                    ) {paddingValues ->
                        NavHost(navController, startDestination = "landing",
                            // Apply padding from Scaffold, but remove top padding as Spacer handles it
                            modifier = Modifier.padding(PaddingValues(
                                top = 0.dp, // Top padding is now handled by the Spacer
                                bottom = paddingValues.calculateBottomPadding(),
                                start = paddingValues.calculateLeftPadding(LocalLayoutDirection.current),
                                end = paddingValues.calculateRightPadding(LocalLayoutDirection.current)
                            ))) {
                            composable("landing") { LandingScreen(navController,
                                connectConfigViewModel, linkConfigViewModel) }
                            composable("loading/{configIndex}/{linkIndex}") { backStackEntry ->
                                val configIndex = backStackEntry.arguments?.getString("configIndex")
                                    ?.toIntOrNull() ?: 0
                                val linkIndex = backStackEntry.arguments?.getString("linkIndex")
                                    ?.toIntOrNull() ?: 0

                                // Pass the MainActivity's webSocketManager instance to LoadingScreen
                                LoadingScreen(navController, connectConfigViewModel, configIndex,
                                    linkConfigViewModel, linkIndex, debugViewModel, webSocketManager)
                            }
                            composable("appSettings") { AppSettingsScreen(navController) }
                            composable("control") { ControlScreen(navController, webSocketManager) } // Pass webSocketManager
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
    val iconsList = listOf(IconItem.MaterialIcon(Icons.Filled.Add)) +
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
        .background(MaterialTheme.colorScheme.primary)
        // Apply system bar padding to the content of LandingScreen,
        // so it doesn't draw under the status bar (already colored) or nav bar
        .padding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom).asPaddingValues())
        ) {
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
            iconsList.forEachIndexed { index, iconItem ->
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

        if (selectedIndex.intValue == 0 || selectedIndex.intValue == iconsList.size - 1) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (selectedIndex.intValue == 0) {
                            navController.navigate("addConnectConfig")
                        } else { // This implies selectedIndex.intValue == iconsList.size - 1
                            navController.navigate("appSettings")
                        }
                    },
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
            val iconDropdown = if (mExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown

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
                        imageVector = iconDropdown,
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
                    onClick = {                         val actualConfigId = configIDs.getOrNull(selectedIndex.intValue - 1)
                        if (actualConfigId != null) {
                           navController.navigate("loading/$actualConfigId/$linkIndex")
                        } else {
                            // Handle error: No valid config selected, or placeholder was clicked
                            println("Error: No valid robot configuration selected for connection.")
                        }
                    },
                    modifier = Modifier
                        .padding(0.dp)
                        .align(Alignment.Center)
                        .fillMaxWidth(0.9f),
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary),
                    shape = RectangleShape
                ) {
                    Text(
                        "Connect to Robot",
                        color = MaterialTheme.colorScheme.onSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        var canvasHeight by remember { mutableFloatStateOf(0f) }
        val noConfigElementSelected = selectedIndex.intValue == 0 || selectedIndex.intValue == iconsList.size - 1

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
                        val actualConfigId = configIDs.getOrNull(selectedIndex.intValue - 1)
                        if (actualConfigId != null) {
                           navController.navigate("loading/$actualConfigId/$linkIndex")
                        } else {
                             println("Error: No valid robot configuration selected for connection from canvas click.")
                        }
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
                text = "1.3.0-beta", // TODO: Use BuildConfig.VERSION_NAME
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsScreen(navController: NavController) {
    var isDarkMode by remember { mutableStateOf(false) } // TODO: Replace with actual theme state
    val uriHandler = LocalUriHandler.current

    Scaffold(containerColor = MaterialTheme.colorScheme.primary,
        topBar = {
            TopAppBar(
                title = { Text("App Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // This already includes status bar padding due to enableEdgeToEdge and Scaffold defaults
                .padding(16.dp)
        ) {
            // --- General Settings ---
            item {
                Text(
                    text = "General",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
//            item {
//                Row(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(vertical = 8.dp),
//                    verticalAlignment = Alignment.CenterVertically,
//                    horizontalArrangement = Arrangement.SpaceBetween
//                ) {
//                    Text("Dark Mode", color = MaterialTheme.colorScheme.onBackground)
//                    Switch(
//                        checked = isDarkMode,
//                        onCheckedChange = { isDarkMode = it },
//                        colors = SwitchDefaults.colors(
//                            checkedThumbColor = MaterialTheme.colorScheme.primary,
//                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
//                            uncheckedThumbColor = MaterialTheme.colorScheme.outline,
//                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
//                        )
//                    )
//                }
//            }

            // --- App Information ---
            item {
                Text(
                    text = "App Information",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                InfoRow("App Version", "1.3.0-beta")
            }
            item {
                InfoRow("Build Number", "130") // Example build number, TODO: Use BuildConfig.VERSION_CODE
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )
            }

            // --- Contact & Support ---
            item {
                Text(
                    text = "Contact & Support",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp, top = 16.dp),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                InfoRow("Developer", "dajakov")
            }
            item {
                InfoRow("Contact Email", "contact@dajakov.com") // Placeholder
            }
            item {
                Text(
                    text = "Privacy Policy",
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clickable {
                            try {
                                uriHandler.openUri("https://dajakov.com/projects/daedalusLink/privacy") // Placeholder URL
                            } catch (e: Exception) {
                                // Handle error, e.g., show a toast
                            }
                        },
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }
//            item {
//                 Text(
//                    text = "Report an Issue",
//                    modifier = Modifier
//                        .padding(vertical = 8.dp)
//                        .clickable {
//                             try {
//                                uriHandler.openUri("https://dajakov.com/report-issue") // Placeholder URL
//                            } catch (e: Exception) {
//                                // Handle error, e.g., show a toast
//                            }
//                        },
//                    color = MaterialTheme.colorScheme.tertiary,
//                    fontWeight = FontWeight.Bold
//                )
//                HorizontalDivider(
//                    modifier = Modifier.padding(vertical = 16.dp),
//                    thickness = DividerDefaults.Thickness,
//                    color = DividerDefaults.color
//                )
//            }
             item {
                Spacer(modifier = Modifier.height(50.dp)) // Add some space at the bottom
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground)
        Text(value, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
fun LoadingScreen(navController: NavController, connectConfigViewModel: ConnectConfigViewModel,
                  configIndex: Int, linkConfigViewModel: LinkConfigViewModel, linkIndexArgument: Int,
                  debugViewModel: DebugViewModel, webSocketMngr: WebSocketManager) { // Renamed linkIndex to linkIndexArgument, added webSocketMngr
    BackHandler { }

    val connectConfigs by connectConfigViewModel.allConfigs.collectAsState(initial = emptyList())
    // It's safer to not proceed if connectConfigs is empty early on, though find should handle it.
    if (connectConfigs.isEmpty() && configIndex != 0) { // Check if configIndex is valid if list is empty
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: No configurations loaded.", color = MaterialTheme.colorScheme.onError)
            Button(onClick = { navController.navigate("landing") }) { Text("Go Back") }
        }
        return
    }

    val activeConfig = connectConfigs.find { it.id == configIndex }

    if (activeConfig == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: Configuration not found for ID $configIndex.", color = MaterialTheme.colorScheme.onError)
            Button(onClick = { navController.navigate("landing") }) { Text("Go Back") }
        }
        return
    }

    val ipAddress = activeConfig.address
    val robotName = activeConfig.name
    val heartbeatFrequency = activeConfig.heartbeatFrequency.toLong()

    var debugText by remember { mutableStateOf("Initializing...") }
    var connectionSuccess by remember { mutableStateOf(true) }
    var showExitButton by remember { mutableStateOf(false) }
    var steps by remember { mutableStateOf(listOf<String>()) }

    fun updateSteps(step: String, isSameLine: Boolean = false) {
        steps = if (isSameLine && steps.isNotEmpty()) {
            val lastStep = steps.last() + " | " + step
            steps.dropLast(1) + lastStep
        } else {
            steps + step
        }
    }

    suspend fun performPing(ip: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val cleanIp = ip.substringBefore(":")
                val address = InetAddress.getByName(cleanIp)
                address.isReachable(2000)
            } catch (_: IOException) {
                false
            }
        }
    }

    LaunchedEffect(Unit) {
        updateSteps("Pinging $ipAddress...")
        debugText = "Connecting to $robotName... "
        val pingResult = performPing(ipAddress)
        var webSocketResult = false

        if (pingResult) {
            updateSteps("✅", true)
        } else {
            updateSteps("❌ Ping Failed", true)
            connectionSuccess = false
            showExitButton = true
        }

        if (pingResult) {
            updateSteps("Connecting to WebSocket... ")
            // Corrected: Removed analyticsLogger from the call, as webSocketMngr has it via constructor
            webSocketResult = webSocketMngr.connectToWebSocket(
                "ws://$ipAddress", sharedState, heartbeatFrequency, debugViewModel, robotName
            )

            if (!webSocketResult) {
                updateSteps("❌ Connection Failed", true)
                connectionSuccess = false
                showExitButton = true
            } else {
                updateSteps("✅", true)
                connectionSuccess = true
            }
        }

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
                updateSteps("❌ No JSON Response", true)
                connectionSuccess = false
                showExitButton = true
            }
        }

        delay(1000) // Shorter delay to see final status before navigating or showing exit

        if (connectionSuccess) {
            navController.navigate("control") { popUpTo("landing") { inclusive = false } }
        } else {
            // Ensure exit button is shown if any step failed and connectionSuccess is false
            showExitButton = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!showExitButton) { // Only show progress if not yet failed
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                Spacer(modifier = Modifier.height(16.dp))
            }
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
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { navController.navigate("landing") { popUpTo("landing") { inclusive = true } } },
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.secondary),
                    shape = RectangleShape
                ) {
                    Text(
                        "Exit to Landing",
                        color = MaterialTheme.colorScheme.onSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddConnectConfigScreen(navController: NavController, viewModel: ConnectConfigViewModel) {
    var configName by remember { mutableStateOf("") }
    var selectedOption by remember { mutableStateOf("WiFi") }
    var address by remember { mutableStateOf("") }
    var heartbeat by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf<IconItem>(IconItem
        .MaterialIcon(Icons.Default.Info)) } // Default icon
    val configs by viewModel.allConfigs.collectAsState(initial = emptyList())

    Scaffold(containerColor = MaterialTheme.colorScheme.primary,
        topBar = {
            TopAppBar(
                title = { Text("Add Connect Config") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }){ paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // This already includes status bar padding
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
                            iconId = iconId
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
                    val iconToDisplay = IconMapper.getIconById(config.iconId)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DisplayIcon(
                                iconToDisplay,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Name: ${config.name}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Type: ${config.connectionType}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "Address: ${config.address}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "Heartbeat: ${config.heartbeatFrequency} Hz",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontSize = 12.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.deleteConfig(config) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(
                                    horizontal = 8.dp,
                                    vertical = 4.dp
                                )
                            ) {
                                Text(
                                    "Delete",
                                    color = MaterialTheme.colorScheme.onError,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
