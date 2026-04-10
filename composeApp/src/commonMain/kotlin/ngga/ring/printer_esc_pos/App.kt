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
import kotlinx.datetime.Clock

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
    val discoveredPrinters = remember { mutableStateListOf<DiscoveredPrinter>() }
    var discoveryLog by remember { mutableStateOf("Ready to scan...") }
    
    // Start Discovery Effect
    LaunchedEffect(selectedTab) {
        if (selectedTab == 0) {
            printer.connectorFactory.discovery("BLUETOOTH") { log ->
                discoveryLog = log
            }.collectLatest { devices ->
                discoveredPrinters.clear()
                discoveredPrinters.addAll(devices)
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
                        icon = { Icon(Icons.Default.SettingsInputAntenna, null) },
                        label = { Text("Discovery") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.Receipt, null) },
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
                AnimatedContent(targetState = selectedTab) { tab ->
                    when (tab) {
                        0 -> DiscoveryScreen(discoveredPrinters, discoveryLog, printerConfig) { 
                            printerConfig = it 
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
    onSelect: (PrinterConfig) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        HeaderSection("Printer Discovery", "Search for Bluetooth, USB, or Network printers")
        
        Spacer(Modifier.height(12.dp))
        
        Surface(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            color = Color.Black.copy(alpha = 0.3f)
        ) {
            Text(
                text = log,
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Icon(
                                when(device.connectionType) {
                                    "NETWORK" -> Icons.Default.NetworkPing
                                    "USB" -> Icons.Default.Usb
                                    else -> Icons.Default.Bluetooth
                                }, 
                                contentDescription = null, 
                                modifier = Modifier.padding(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(device.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(device.address, color = LocalContentColor.current.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
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
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        HeaderSection("Receipt Studio", "Live simulation for ${width}mm paper")
        
        Spacer(Modifier.height(24.dp))
        
        // Simulated Preview
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(16.dp)),
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
                Divider(color = Color.LightGray)
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
        Button(
            enabled = isReady,
            onClick = {
                scope.launch {
                    val business = BusinessInfo("NggaPrinter Pro", "Professional HQ", "v1.0.0", currencySymbol = "$")
                    val data = ReceiptData(
                        headerId = "INV-FINAL",
                        transactionId = "TX-PROFESSIONAL",
                        timestamp = Clock.System.now().toEpochMilliseconds(),
                        items = listOf(ReceiptItem("Professional SDK", 1.0, 100.0)),
                        totalAmount = 100.0,
                        verificationUrl = "https://github.com/ringga-dev/Printer-ESC-POS"
                    )
                    printer.printReceipt(config, business, data)
                }
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
            )
        ) {
            Icon(Icons.Default.AutoFixHigh, null)
            Spacer(Modifier.width(12.dp))
            Text(if (isReady) "Execute Print to ${config.name}" else "Select a Printer First", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ConfigScreen(width: Int, cpl: Int, onUpdateWidth: (Int) -> Unit, onUpdateCpl: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        HeaderSection("Hardware Configuration", "Tune layout based on paper physics")
        
        Spacer(Modifier.height(24.dp))
        
        Text("Paper Width", fontWeight = FontWeight.Bold, color = Color.White)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(58, 80).forEach { w ->
                FilterChip(
                    selected = width == w,
                    onClick = { onUpdateWidth(w) },
                    label = { Text("${w}mm") }
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
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text("Standard 58mm usually uses 32 CPL. 80mm uses 42-48 CPL.", fontSize = 12.sp, color = LocalContentColor.current.copy(alpha = 0.7f))
            }
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