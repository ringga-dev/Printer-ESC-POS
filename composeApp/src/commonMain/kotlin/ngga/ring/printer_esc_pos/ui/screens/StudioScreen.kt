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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel
import ngga.ring.printer.util.preview.PreviewBlock
import ngga.ring.printer.util.escpos.TextAlignment

@Composable
fun StudioScreen(viewModel: PrinterViewModel) {
    val config by viewModel.config.collectAsState()
    val printStatus by viewModel.printStatus.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val previewBlocks by viewModel.previewBlocks.collectAsState()
    val receiptScrollState = rememberScrollState()

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Text(
            text = "Studio",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            letterSpacing = (-1).sp
        )
        Text(
            text = "Rendering preview & hardware test",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(32.dp))

        // Receipt Preview Area
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val previewWidth = if (config.paperWidth == 58) 250.dp else 320.dp
            
            ElevatedCard(
                modifier = Modifier
                    .width(previewWidth)
                    .fillMaxHeight()
                    .offset(y = 10.dp)
                    .blur(if (printStatus !is PrintStatus.Idle) 8.dp else 0.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 24.dp)
                        .verticalScroll(receiptScrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    previewBlocks.forEach { block ->
                        RenderPreviewBlock(block)
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }

            // Print Status Overlay
            if (printStatus !is PrintStatus.Idle) {
                StatusDialog(status = printStatus, onDismiss = { viewModel.resetPrintStatus() })
            }
        }

        Spacer(Modifier.height(24.dp))

        // Actions
        val isReady = config.address?.isNotEmpty() == true || config.connectionType == "VIRTUAL"
        Row(
            modifier = Modifier.fillMaxWidth().height(64.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.printExpertTest() },
                enabled = isReady,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Stars, null)
                Spacer(Modifier.width(8.dp))
                Text("EXPERT TEST", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { viewModel.printTestPage() },
                enabled = isReady,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Bolt, null)
                Spacer(Modifier.width(8.dp))
                Text("REGULAR", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RenderPreviewBlock(block: PreviewBlock) {
    when (block) {
        is PreviewBlock.Text -> {
            Text(
                text = block.text,
                color = Color.Black,
                fontWeight = if (block.isBold) FontWeight.Black else FontWeight.Normal,
                fontSize = if (block.isBig) 18.sp else 11.sp,
                textAlign = when (block.alignment) {
                    TextAlignment.CENTER -> TextAlign.Center
                    TextAlignment.RIGHT -> TextAlign.Right
                    else -> TextAlign.Left
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        is PreviewBlock.KeyValue -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(block.key, color = Color.Gray, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text(
                    block.value, 
                    color = Color.Black, 
                    fontWeight = if (block.isBold) FontWeight.Bold else FontWeight.Normal, 
                    fontSize = 11.sp,
                    textAlign = TextAlign.Right
                )
            }
        }
        is PreviewBlock.Divider -> {
            Text(
                block.char.toString().repeat(32),
                color = Color.LightGray,
                maxLines = 1,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        is PreviewBlock.Barcode -> {
            Column(
                horizontalAlignment = when (block.alignment) {
                    TextAlignment.CENTER -> Alignment.CenterHorizontally
                    TextAlignment.RIGHT -> Alignment.End
                    else -> Alignment.Start
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Icon(Icons.Default.ViewWeek, null, Modifier.size(width = 120.dp, height = 40.dp), Color.Black)
                Text(block.content, color = Color.Gray, fontSize = 8.sp)
            }
        }
        is PreviewBlock.QRCode -> {
            Column(
                horizontalAlignment = when (block.alignment) {
                    TextAlignment.CENTER -> Alignment.CenterHorizontally
                    TextAlignment.RIGHT -> Alignment.End
                    else -> Alignment.Start
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Icon(Icons.Default.QrCode2, null, Modifier.size(80.dp), Color.Black)
                Text("SCAN TO VERIFY", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }
        is PreviewBlock.Image -> {
            Icon(Icons.Default.Image, null, Modifier.size(48.dp).padding(vertical = 8.dp), Color.Gray)
        }
        PreviewBlock.Space -> {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun StatusDialog(status: PrintStatus, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.padding(24.dp).fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when(status) {
                PrintStatus.Processing, PrintStatus.Connecting, PrintStatus.Sending -> {
                    CircularProgressIndicator(modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = when(status) {
                            PrintStatus.Processing -> "Preparing engine..."
                            PrintStatus.Connecting -> "Connecting to hardware..."
                            else -> "Sending stream data..."
                        },
                        textAlign = TextAlign.Center
                    )
                }
                PrintStatus.Success -> {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary)
                    Text("PRINT SUCCESS", fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 16.dp))
                    Button(onClick = onDismiss, Modifier.padding(top = 24.dp).fillMaxWidth()) { Text("BACK") }
                }
                is PrintStatus.Error -> {
                    Icon(Icons.Default.Error, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Text("PRINT FAILED", fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 16.dp))
                    Text(status.message, fontSize = 11.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(onClick = onDismiss, Modifier.padding(top = 24.dp).fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("RETRY") }
                }
                else -> {}
            }
        }
    }
}
