package ngga.ring.printer_esc_pos.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ngga.ring.printer_esc_pos.ui.components.GlassCard
import ngga.ring.printer_esc_pos.ui.components.SectionHeader
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(viewModel: PrinterViewModel) {
    val config by viewModel.config.collectAsState()
    val showVirtual by viewModel.showVirtual.collectAsState()
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp
        )
        Text(
            text = "Hardware calibration & localization",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // Discovery Options
        SectionHeader("Discovery Options", "Configure how devices are found")
        Spacer(Modifier.height(16.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Demo Mode", fontWeight = FontWeight.Bold)
                    Text("Show simulated printers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = showVirtual,
                    onCheckedChange = { viewModel.toggleVirtual(it) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        // Paper Configuration
        SectionHeader("Paper Configuration", "Define physical output limits")
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(58, 80).forEach { width ->
                val isSelected = config.paperWidth == width
                ElevatedCard(
                    onClick = {
                        viewModel.updateConfig(config.copy(
                            paperWidth = width,
                            characterPerLine = if (width == 80) 48 else 32,
                            paperWidthDots = if (width == 80) 576 else 384
                        ))
                    },
                    modifier = Modifier.weight(1f).height(80.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "${width}mm",
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Internationalization
        SectionHeader("Localization", "Charset & Encoding settings")
        Spacer(Modifier.height(16.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = config.charsetName,
                    onValueChange = { viewModel.updateConfig(config.copy(charsetName = it)) },
                    label = { Text("Charset Name (e.g. UTF-8, GBK)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                
                OutlinedTextField(
                    value = config.escPosCodePage.toInt().toString(16).uppercase(),
                    onValueChange = { 
                        val newPage = it.toIntOrNull(16)?.toByte() ?: 0
                        viewModel.updateConfig(config.copy(escPosCodePage = newPage))
                    },
                    label = { Text("ESC/POS Code Page (Hex)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    prefix = { Text("0x") }
                )
            }
        }
        
        Spacer(Modifier.height(100.dp)) // Extra space for navigation bar
    }
}
