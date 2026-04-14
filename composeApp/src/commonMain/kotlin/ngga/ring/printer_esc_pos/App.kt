package ngga.ring.printer_esc_pos

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ngga.ring.printer.KmpPrinter
import ngga.ring.printer.manager.PrinterPermissionManager
import ngga.ring.printer.model.*
import ngga.ring.printer.util.ConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val printer = remember { KmpPrinter() }
    val scope = rememberCoroutineScope()
    
    // Global Config State
    var config by remember { 
        mutableStateOf(PrinterConfig(name = "Discovery Mode", connectionType = "BLUETOOTH", address = "")) 
    }
    var showVirtual by remember { mutableStateOf(false) }
    
    // UI State
    var selectedTab by remember { mutableStateOf(0) }
    var discoveredPrinters by remember { mutableStateOf(emptyList<DiscoveredPrinter>()) }
    var discoveryLog by remember { mutableStateOf("Ready to scan...") }
    var discoveryMode by remember { mutableStateOf("BLUETOOTH") }
    var refreshScanTrigger by remember { mutableStateOf(0) }
    
    val connectionState by printer.connectionState.collectAsState()
    
    // Permission State
    val permissionManager = remember { PrinterPermissionManager() }
    var hasBluetoothPermission by remember { mutableStateOf(permissionManager.hasPermissions("BLUETOOTH")) }
    
    // Start Discovery Effect
    LaunchedEffect(selectedTab, discoveryMode, hasBluetoothPermission, refreshScanTrigger, showVirtual) {
        if (selectedTab == 0 && (discoveryMode != "BLUETOOTH" || hasBluetoothPermission)) {
            val discoveryConfig = DiscoveryConfig(showVirtualDevices = showVirtual)
            printer.discovery(discoveryMode, discoveryConfig) { log ->
                discoveryLog = log
            }.collectLatest { devices ->
                discoveredPrinters = devices
            }
        }
    }
    
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF64B5F6),
            secondary = Color(0xFF81C784),
            background = Color(0xFF0F1115),
            surface = Color(0xFF1A1D23)
        )
    ) {
        Scaffold(
            topBar = {
                GlassyTopBar(connectionState)
            },
            bottomBar = {
                FancyNavigationBar(selectedTab) { selectedTab = it }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color(0xFF0F1115))
            ) {
                // Background Glow
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 100.dp, y = (-50).dp)
                        .blur(100.dp)
                        .alpha(0.1f)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                )

                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn(tween(300)) + slideInHorizontally { if (targetState > initialState) it else -it } togetherWith
                        fadeOut(tween(300)) + slideOutHorizontally { if (targetState > initialState) -it else it }
                    }
                ) { tab ->
                    when (tab) {
                        0 -> DiscoveryView(
                                devices = discoveredPrinters, 
                                log = discoveryLog, 
                                selected = config,
                                currentMode = discoveryMode,
                                onModeChange = { discoveryMode = it },
                                onRefresh = { refreshScanTrigger++ }
                            ) { config = it }
                        
                        1 -> ReceiptStudio(printer, config, scope)
                        
                        2 -> AdvancedSettings(
                                config = config, 
                                showVirtual = showVirtual,
                                onConfigChange = { config = it },
                                onToggleVirtual = { showVirtual = it }
                            )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassyTopBar(state: ConnectionState) {
    CenterAlignedTopAppBar(
        title = { 
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("KmpPrinter Pro", fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = (-0.5).sp)
                ConnectionBadge(state)
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun ConnectionBadge(state: ConnectionState) {
    val (color, text) = when(state) {
        is ConnectionState.Connected -> Color(0xFF81C784) to "CONNECTED: ${state.name}"
        ConnectionState.Connecting -> Color(0xFFFFB74D) to "CONNECTING..."
        is ConnectionState.Error -> Color(0xFFE57373) to "ERROR"
        else -> Color.Gray to "DISCONNECTED"
    }
    
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color.copy(alpha = 0.8f))
    }
}

@Composable
fun FancyNavigationBar(selected: Int, onSelect: (Int) -> Unit) {
    NavigationBar(
        containerColor = Color(0xFF1A1D23).copy(alpha = 0.8f),
        tonalElevation = 0.dp
    ) {
        NavButton(selected == 0, Icons.Default.Search, "Search") { onSelect(0) }
        NavButton(selected == 1, Icons.Default.ReceiptLong, "Studio") { onSelect(1) }
        NavButton(selected == 2, Icons.Default.Tune, "Config") { onSelect(2) }
    }
}

@Composable
fun RowScope.NavButton(selected: Boolean, icon: ImageVector, label: String, onClick: () -> Unit) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(icon, null, modifier = Modifier.size(24.dp)) },
        label = { Text(label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedIconColor = Color.Gray.copy(alpha = 0.5f),
            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        )
    )
}

@Composable
fun DiscoveryView(
    devices: List<DiscoveredPrinter>,
    log: String,
    selected: PrinterConfig,
    currentMode: String,
    onModeChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelect: (PrinterConfig) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Printer Discovery", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
        Text("Connect to nearby hardware via BLE, USB or LAN", color = Color.Gray, fontSize = 13.sp)
        
        Spacer(Modifier.height(24.dp))
        
        // Mode Selector
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("BLUETOOTH", "USB", "NETWORK").forEach { mode ->
                val isSelected = currentMode == mode
                InputChip(
                    selected = isSelected,
                    onClick = { onModeChange(mode) },
                    label = { Text(mode) },
                    trailingIcon = { if (isSelected) Icon(Icons.Default.Check, null) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        
        // Discovery Status
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (log.contains("Scanning")) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                else Icon(Icons.Default.Radar, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                
                Spacer(Modifier.width(12.dp))
                Text(log, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, null) }
            }
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(devices) { device ->
                val isSelected = selected.address == device.address
                PrinterCard(device, isSelected) {
                    onSelect(PrinterConfig(device.name, device.connectionType, device.address, device.port))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterCard(device: DiscoveredPrinter, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(48.dp), shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.05f)) {
                Icon(
                    when(device.connectionType) {
                        "NETWORK" -> Icons.Default.Lan
                        "USB" -> Icons.Default.Usb
                        else -> Icons.Default.Bluetooth
                    }, 
                    null, 
                    modifier = Modifier.padding(12.dp),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(device.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(device.address ?: "No Address", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ReceiptStudio(printer: KmpPrinter, config: PrinterConfig, scope: kotlinx.coroutines.CoroutineScope) {
    var printStatus by remember { mutableStateOf<PrintStatus>(PrintStatus.Idle) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Receipt Studio", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
        Text("Mockup simulation for ${config.paperWidth}mm paper", color = Color.Gray, fontSize = 13.sp)

        Spacer(Modifier.height(24.dp))
        
        // Realistic Preview
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val previewWidth = if (config.paperWidth == 58) 260.dp else 340.dp
            Card(
                modifier = Modifier.width(previewWidth).fillMaxHeight().blur(if(printStatus !is PrintStatus.Idle) 4.dp else 0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(Modifier.padding(24.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("KMPPRINTER PRO", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    Text("Multi-Platform Thermal Engine", color = Color.Gray, fontSize = 10.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("-".repeat(config.characterPerLine), color = Color.LightGray, maxLines = 1)
                    
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Hardware:", color = Color.Black, fontSize = 10.sp)
                        Text(config.name, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    repeat(3) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Professional Item ${it+1}", color = Color.Black, fontSize = 11.sp)
                            Text("Rp 25.000", color = Color.Black, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("-".repeat(config.characterPerLine), color = Color.LightGray, maxLines = 1)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TOTAL", color = Color.Black, fontWeight = FontWeight.Black)
                        Text("Rp 75.000", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                    
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.QrCode2, null, Modifier.size(64.dp), Color.Black)
                    Text("AUTHENTIC RECEIPT", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            if (printStatus !is PrintStatus.Idle) {
                StatusCard(printStatus) { printStatus = PrintStatus.Idle }
            }
        }

        val isReady = !config.address.isNullOrEmpty()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                enabled = isReady,
                onClick = {
                    scope.launch {
                        printer.newCommandBuilder(config).initialize().printRuler().build().let {
                            printer.printRaw(config, it).collect { printStatus = it }
                        }
                    }
                },
                modifier = Modifier.height(64.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.Straighten, null)
            }

            Button(
                enabled = isReady,
                onClick = {
                    scope.launch {
                        printer.printTestPage(config).collect { printStatus = it }
                    }
                },
                modifier = Modifier.weight(1f).height(64.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.ElectricBolt, null)
                Spacer(Modifier.width(12.dp))
                Text("TEST PRINT", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatusCard(status: PrintStatus, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            when(status) {
                PrintStatus.Processing -> {
                    CircularProgressIndicator()
                    Text("Preparing Engine...", Modifier.padding(top = 16.dp))
                }
                PrintStatus.Connecting -> {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.secondary)
                    Text("Connecting Hardware...", Modifier.padding(top = 16.dp))
                }
                PrintStatus.Sending -> {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Printing Data...", Modifier.padding(top = 16.dp))
                }
                PrintStatus.Success -> {
                    Text("SUCCESS", color = Color(0xFF81C784), fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Button(onClick = onDismiss, Modifier.padding(top = 24.dp).fillMaxWidth()) { Text("BACK") }
                }
                is PrintStatus.Error -> {
                    Text("FAILED", color = Color(0xFFE57373), fontWeight = FontWeight.Black, fontSize = 24.sp)
                    Text(status.message, color = Color.Gray, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Button(onClick = onDismiss, Modifier.padding(top = 24.dp).fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))) { Text("RETRY") }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun AdvancedSettings(
    config: PrinterConfig, 
    showVirtual: Boolean, 
    onConfigChange: (PrinterConfig) -> Unit,
    onToggleVirtual: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Hardware Settings", fontWeight = FontWeight.ExtraBold, fontSize = 28.sp)
        Text("Calibrate paper physics and discovery options", color = Color.Gray, fontSize = 13.sp)

        Spacer(Modifier.height(32.dp))
        
        SettingHeader("DISCOVERY OPTIONS")
        SwitchRow(
            title = "Demo Mode (Virtual Devices)", 
            subtitle = "Show simulated printers for testing", 
            checked = showVirtual, 
            onCheckedChange = onToggleVirtual
        )
        
        Spacer(Modifier.height(32.dp))
        
        SettingHeader("PAPER CONFIGURATION")
        PaperSelector(config.paperWidth) { 
            val newWidth = it
            onConfigChange(config.copy(
                paperWidth = newWidth, 
                characterPerLine = if (newWidth == 80) 48 else 32,
                paperWidthDots = if (newWidth == 80) 576 else 384
            ))
        }
        
        Spacer(Modifier.height(24.dp))
        
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text("CPL: ${config.characterPerLine}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Slider(
                    value = config.characterPerLine.toFloat(),
                    onValueChange = { onConfigChange(config.copy(characterPerLine = it.toInt())) },
                    valueRange = 20f..64f,
                    steps = 44
                )
            }
            Column(Modifier.weight(1f)) {
                Text("Dots: ${config.paperWidthDots}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Slider(
                    value = config.paperWidthDots.toFloat(),
                    onValueChange = { onConfigChange(config.copy(paperWidthDots = it.toInt())) },
                    valueRange = 200f..800f,
                    steps = 60
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.03f)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("PRO TIP: Hardware Calibration", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text("If your text is cut off or not centered, use the 'Print Ruler' tool in the Studio tab to find your printer's exact dot width.", fontSize = 11.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SettingHeader(title: String) {
    Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.05f))
}

@Composable
fun SwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.Gray, fontSize = 12.sp)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun PaperSelector(selected: Int, onSelect: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(58, 80).forEach { w ->
            val isSelected = selected == w
            Card(
                onClick = { onSelect(w) },
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.03f)
                ),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
            ) {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("${w}mm", fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
        }
    }
}