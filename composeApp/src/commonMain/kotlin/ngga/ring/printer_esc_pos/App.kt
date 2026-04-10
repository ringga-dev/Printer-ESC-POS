package ngga.ring.printer_esc_pos

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ngga.ring.printer.NggaPrinter
import ngga.ring.printer.model.*
import kotlinx.coroutines.flow.collectLatest
import ngga.ring.printer.manager.PrinterPermissionManager
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val printer = remember { NggaPrinter() }
    val scope = rememberCoroutineScope()
    
    // Global Config State
    var printerConfig by remember { 
        mutableStateOf(PrinterConfig(name = "Not Selected", connectionType = "BLUETOOTH", address = "")) 
    }
    var paperWidth by remember { mutableStateOf(58) }
    var cpl by remember { mutableStateOf(32) }
    
    // UI State
    var selectedTab by remember { mutableStateOf(0) }
    var discoveredPrinters by remember { mutableStateOf(emptyList<DiscoveredPrinter>()) }
    var discoveryLog by remember { mutableStateOf("Ready to scan...") }
    var discoveryMode by remember { mutableStateOf("BLUETOOTH") }
    var refreshScanTrigger by remember { mutableStateOf(0) }
    
    // Permission State
    val permissionManager = remember { PrinterPermissionManager() }
    var hasBluetoothPermission by remember { mutableStateOf(permissionManager.hasPermissions("BLUETOOTH")) }
    
    // Start Discovery Effect
    LaunchedEffect(selectedTab, discoveryMode, hasBluetoothPermission, refreshScanTrigger) {
        if (selectedTab == 0 && (discoveryMode != "BLUETOOTH" || hasBluetoothPermission)) {
            discoveryLog = "Preparing $discoveryMode scan..."
            println("NggaPrinter: Starting scan for $discoveryMode (Trigger: $refreshScanTrigger)")
            printer.connectorFactory.discovery(discoveryMode) { log ->
                discoveryLog = log
            }.collectLatest { devices ->
                println("NggaPrinter: Observed ${devices.size} devices for $discoveryMode")
                discoveredPrinters = devices
            }
        }
    }
    
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF64B5F6),
            secondary = Color(0xFF81C784),
            tertiary = Color(0xFFFFB74D),
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E)
        )
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { 
                        Text("NggaPrinter Pro", fontWeight = FontWeight.Bold, letterSpacing = 1.sp) 
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Search, null) },
                        label = { Text("Search") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Print, null) },
                        label = { Text("Studio") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = { Icon(Icons.Default.Settings, null) },
                        label = { Text("Config") }
                    )
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF121212), Color(0xFF1E1E1E))
                        )
                    )
            ) {
                // Simplified render without AnimatedContent
                Box(Modifier.fillMaxSize()) {
                    when (selectedTab) {
                        0 -> {
                            if (discoveryMode == "BLUETOOTH" && !hasBluetoothPermission) {
                                PermissionGate(onGranted = { hasBluetoothPermission = true })
                            } else {
                                DiscoveryScreen(
                                    devices = discoveredPrinters, 
                                    log = discoveryLog, 
                                    selected = printerConfig,
                                    currentMode = discoveryMode,
                                    onModeChange = { discoveryMode = it },
                                    onRefresh = { refreshScanTrigger++ }
                                ) { 
                                    printerConfig = it 
                                }
                            }
                        }
                        1 -> ReceiptScreen(printer, printerConfig, paperWidth, cpl, scope)
                        2 -> ConfigScreen(paperWidth, cpl, onUpdateWidth = { paperWidth = it }, onUpdateCpl = { cpl = it })
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveryScreen(
    devices: List<DiscoveredPrinter>,
    log: String,
    selected: PrinterConfig,
    currentMode: String,
    onModeChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onSelect: (PrinterConfig) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        HeaderSection("Printer Discovery", "Search for Bluetooth, USB, or Network printers")
        
        Spacer(Modifier.height(16.dp))
        
        // Protocol Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("BLUETOOTH", "USB", "NETWORK").forEach { mode ->
                FilterChip(
                    selected = currentMode == mode,
                    onClick = { onModeChange(mode) },
                    label = { Text(mode, fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            when(mode) {
                                "NETWORK" -> Icons.Default.Router
                                "USB" -> Icons.Default.SettingsEthernet
                                else -> Icons.Default.Bluetooth
                            },
                             null,
                             modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            color = Color.Black.copy(alpha = 0.3f)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (log.contains("Scanning") || log.contains("Starting") || log.contains("Checking")) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    text = log,
                    modifier = Modifier.weight(1f),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                Text(
                    text = "${devices.size} found",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
        ) {
            Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(12.dp))
            Text("SCAN FOR PRINTERS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        Spacer(Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Diagnostic Item: Always show this to prove the list is working
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().alpha(0.6f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray.copy(alpha = 0.2f))
                ) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                        Spacer(Modifier.width(10.dp))
                        Text("Discovery active: Monitoring hardware ports", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            if (devices.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.AutoMode, null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.3f))
                            Spacer(Modifier.height(16.dp))
                            Text("No $currentMode detected", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }
            }

            items(devices) { device ->
                val isSelected = selected.address == device.address
                Card(
                    onClick = { 
                        onSelect(PrinterConfig(device.name, device.connectionType, device.address, device.port)) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) 
                                        else MaterialTheme.colorScheme.surface
                    ),
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Icon(
                                when(device.connectionType) {
                                    "NETWORK" -> Icons.Default.Router
                                    "USB" -> Icons.Default.SettingsEthernet
                                    else -> Icons.Default.Bluetooth
                                }, 
                                null,
                                modifier = Modifier.padding(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(20.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(device.address, color = LocalContentColor.current.copy(alpha = 0.6f), fontSize = 13.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptScreen(
    printer: NggaPrinter, 
    config: PrinterConfig, 
    width: Int, 
    cpl: Int, 
    scope: kotlinx.coroutines.CoroutineScope
) {
    var printStatus by remember { mutableStateOf<PrintStatus>(PrintStatus.Idle) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            HeaderSection("Receipt Studio", "Live simulation for ${width}mm paper")
        
        Spacer(Modifier.height(24.dp))
        
        // Simulated Preview (Width dynamic based on selection)
        val previewPadding = when(width) {
            58 -> 48.dp
            72 -> 24.dp
            else -> 0.dp
        }

        Card(
            modifier = Modifier.padding(horizontal = previewPadding).fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("NGGAPRINTER PRO", color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text("Professional Thermal Suite", color = Color.Gray, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Selected:", color = Color.Black, fontSize = 10.sp)
                    Text("${config.name} (${config.connectionType})", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
                Spacer(Modifier.height(8.dp))
                repeat(4) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Item Line ${it + 1}", color = Color.Black, fontSize = 12.sp)
                        Text("${(it + 1) * 10}.000", color = Color.Black, fontSize = 12.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Color.LightGray)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("TOTAL AMOUNT", color = Color.Black, fontWeight = FontWeight.Bold)
                    Text("100.000", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.QrCode2, null, modifier = Modifier.size(60.dp), tint = Color.Black)
                Text("Verification QR", color = Color.Black, fontSize = 9.sp)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        val isReady = !config.address.isNullOrEmpty()
        Row(Modifier.fillMaxWidth()) {
            Button(
                enabled = isReady,
                onClick = {
                    scope.launch {
                        printer.printTestPage(config, cpl).collect {status ->
                            printStatus = status
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Default.Print, null)
                Spacer(Modifier.width(12.dp))
                Text("Print Receipt", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.width(12.dp))

            // Advanced Test Print Button
            OutlinedButton(
                enabled = isReady,
                onClick = {
                    scope.launch {
                        printer.printTestPage(config, cpl).collect { status ->
                            printStatus = status
                        }
                    }
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.History, null)
            }
        }
    }

    // Status Overlay
    if (printStatus !is PrintStatus.Idle) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.padding(32.dp).fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (printStatus) {
                        PrintStatus.Processing -> {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Processing...", fontWeight = FontWeight.Bold)
                            Text("Generating receipt layout", fontSize = 12.sp, color = Color.Gray)
                        }
                        PrintStatus.Connecting -> {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("Connecting...", fontWeight = FontWeight.Bold)
                            Text("Establishing link with ${config.name}", fontSize = 12.sp, color = Color.Gray)
                        }
                        PrintStatus.Sending -> {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp), color = MaterialTheme.colorScheme.secondary)
                            Spacer(Modifier.height(16.dp))
                            Text("Printing...", fontWeight = FontWeight.Bold)
                            Text("Transmitting ESC/POS commands", fontSize = 12.sp, color = Color.Gray)
                        }
                        PrintStatus.Success -> {
                            Text("✅", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text("Print Successful!", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { printStatus = PrintStatus.Idle },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Done")
                            }
                        }
                        is PrintStatus.Error -> {
                            Text("❌", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text("Print Failed", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Text((printStatus as PrintStatus.Error).message, fontSize = 12.sp, color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { printStatus = PrintStatus.Idle },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Back")
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
}

@Composable
fun ConfigScreen(width: Int, cpl: Int, onUpdateWidth: (Int) -> Unit, onUpdateCpl: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        HeaderSection("Hardware Configuration", "Tune layout based on paper physics")
        
        Spacer(Modifier.height(24.dp))
        
        Text("Select Paper Variant", fontWeight = FontWeight.Bold, color = Color.White)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(58, 72, 80).forEach { w ->
                val isSelected = width == w
                FilterChip(
                    selected = isSelected,
                    onClick = { 
                        onUpdateWidth(w)
                        // Auto-adjust CPL for convenience
                        onUpdateCpl(if (w == 58) 32 else if (w == 72) 38 else 42)
                    },
                    label = { Text("${w}mm", modifier = Modifier.padding(horizontal = 8.dp)) },
                    leadingIcon = {
                        if (isSelected) Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                    }
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text("Characters Per Line (CPL)", fontWeight = FontWeight.Bold, color = Color.White)
        Slider(
            value = cpl.toFloat(),
            onValueChange = { onUpdateCpl(it.toInt()) },
            valueRange = 30f..48f,
            steps = 18
        )
        Text("Current: $cpl CPL", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        
        Spacer(Modifier.weight(1f))
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Help, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(16.dp))
                Text("CPL determines how many characters fit in one line. Standard 58mm uses 32 CPL, while 80mm uses 42-48 CPL.", fontSize = 12.sp, color = LocalContentColor.current.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun PermissionGate(onGranted: () -> Unit) {
    val permissionManager = remember { PrinterPermissionManager() }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Permissions Required", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Bluetooth and Location permissions are needed to discover nearby thermal printers.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = Color.Gray
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                permissionManager.requestPermissions("BLUETOOTH") { granted ->
                    if (granted) onGranted()
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("Grant Permissions", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HeaderSection(title: String, subtitle: String) {
    Column {
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, color = Color.White)
        Text(subtitle, color = Color.Gray, fontSize = 13.sp)
    }
}