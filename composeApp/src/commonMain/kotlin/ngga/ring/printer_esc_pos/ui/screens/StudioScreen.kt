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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ngga.ring.printer.model.PrintStatus
import ngga.ring.printer_esc_pos.viewmodel.PrinterViewModel
import ngga.ring.printer.util.preview.PreviewBlock
import ngga.ring.printer.util.escpos.TextAlignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import ngga.ring.printer_esc_pos.util.rememberImagePicker
import androidx.compose.ui.graphics.ImageBitmap
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.logo_ringga_dev
import kotlinproject.composeapp.generated.resources.logo_ringga_dev_classic
import org.jetbrains.compose.resources.*
import kotlinx.coroutines.launch

@Composable
fun StudioScreen(viewModel: PrinterViewModel) {
    val config by viewModel.config.collectAsState()
    val printStatus by viewModel.printStatus.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val previewBlocks by viewModel.previewBlocks.collectAsState()
    val receiptScrollState = rememberScrollState()

    val baseFontSize = 10.sp

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

        // Branding & Logo Section
        val logoPreview by viewModel.logoPreview.collectAsState()
        val imagePicker = rememberImagePicker { image, preview ->
            viewModel.setLogo(image, preview)
        }
        
        Text("BRANDING", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalButton(
                onClick = { imagePicker() },
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.AddPhotoAlternate, null)
                Spacer(Modifier.width(8.dp))
                Text(if (logoPreview == null) "SET LOGO" else "CHANGE LOGO")
            }
            
            if (logoPreview != null) {
                IconButton(onClick = { viewModel.clearLogo() }) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        // Sample Logos Selection
        Text("SAMPLE LOGOS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val scope = rememberCoroutineScope()
            
            // Simplified approach: clickable icons that load bytes on demand
            
            OutlinedCard(
                onClick = { 
                    scope.launch {
                        val bytes = Res.readBytes("drawable/logo_ringga_dev.png")
                        val bitmap = bytes.decodeToImageBitmap()
                        viewModel.setLogo(bytes, bitmap)
                    }
                },
                modifier = Modifier.size(60.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(Res.drawable.logo_ringga_dev),
                        contentDescription = "New Logo",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            OutlinedCard(
                onClick = { 
                    scope.launch {
                        val bytes = Res.readBytes("drawable/logo_ringga_dev_classic.png")
                        val bitmap = bytes.decodeToImageBitmap()
                        viewModel.setLogo(bytes, bitmap)
                    }
                },
                modifier = Modifier.size(60.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(Res.drawable.logo_ringga_dev_classic),
                        contentDescription = "Classic Logo",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

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
                // Calculate effective padding based on left margin
                // paperWidth (mm) to dots (approx 8 dots/mm)
                val totalDots = (config.paperWidth - 10) * 8 
                val dotsPerChar = totalDots.toDouble() / config.characterPerLine.toDouble()
                
                val effectiveWidth = if (config.autoCenter) {
                    (totalDots - (2 * config.leftMargin)).coerceAtLeast(1).toDouble()
                } else {
                    totalDots.toDouble()
                }

                val effectiveChars = if (config.autoCenter) {
                    (effectiveWidth / dotsPerChar).toInt().coerceAtLeast(1)
                } else {
                    config.characterPerLine
                }

                val actualTextWidthDots = effectiveChars.toDouble() * dotsPerChar
                val centeringPaddingDots = if (config.autoCenter) {
                    ((effectiveWidth - actualTextWidthDots) / 2.0).toInt().coerceAtLeast(0)
                } else {
                    0
                }

                val totalLeftMarginDots = config.leftMargin + centeringPaddingDots
                val marginRatio = totalLeftMarginDots.toFloat() / totalDots.coerceAtLeast(384).toFloat()
                val marginPadding = (previewWidth.value * marginRatio).dp
                
                val contentWidth = if (config.autoCenter) {
                    previewWidth - (marginPadding * 2)
                } else {
                    previewWidth - marginPadding
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(start = marginPadding)
                            .width(contentWidth)
                            .fillMaxHeight()
                            .verticalScroll(receiptScrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        previewBlocks.forEach { block ->
                            RenderPreviewBlock(block)
                        }
                        Spacer(Modifier.height(40.dp))
                    }
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
    val monoFont = FontFamily.Monospace
    
    when (block) {
        is PreviewBlock.Text -> {
            Text(
                text = block.text,
                color = if (block.isInverted) Color.White else Color.Black,
                fontWeight = if (block.isBold) FontWeight.Black else FontWeight.Normal,
                fontSize = 11.sp * block.heightMultiplier,
                fontFamily = monoFont,
                textDecoration = if (block.isUnderline) TextDecoration.Underline else TextDecoration.None,
                textAlign = when (block.alignment) {
                    TextAlignment.CENTER -> TextAlign.Center
                    TextAlignment.RIGHT -> TextAlign.Right
                    else -> TextAlign.Left
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        if (block.isInverted) drawRect(color = Color.Black)
                    }
                    .padding(vertical = (2.dp * block.heightMultiplier))
                    .graphicsLayer {
                        scaleX = block.widthMultiplier.toFloat() / block.heightMultiplier.toFloat()
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
            )
        }
        is PreviewBlock.KeyValue -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp)
                    .drawBehind {
                        if (block.isInverted) drawRect(color = Color.Black)
                    },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    block.key, 
                    color = if (block.isInverted) Color.White else Color.Gray, 
                    fontSize = 10.sp, 
                    fontFamily = monoFont,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    block.value, 
                    color = if (block.isInverted) Color.White else Color.Black, 
                    fontWeight = if (block.isBold) FontWeight.Bold else FontWeight.Normal, 
                    fontSize = 10.sp,
                    fontFamily = monoFont,
                    textAlign = TextAlign.Right
                )
            }
        }
        is PreviewBlock.Divider -> {
            Text(
                text = block.char.toString().repeat(64),
                color = Color.LightGray,
                maxLines = 1,
                fontFamily = monoFont,
                letterSpacing = 2.sp,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
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
                Text(block.content, color = Color.Gray, fontSize = 9.sp, fontFamily = monoFont)
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
                Text("SCAN TO VERIFY", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = monoFont)
            }
        }
        is PreviewBlock.Image -> {
            Column(
                horizontalAlignment = when (block.alignment) {
                    TextAlignment.CENTER -> Alignment.CenterHorizontally
                    TextAlignment.RIGHT -> Alignment.End
                    else -> Alignment.Start
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                val bitmap = block.previewData as? ImageBitmap
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier
                            .width((block.width / 2.5).dp)
                            .height((block.height / 2.5).dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(width = (block.width / 2.5).dp, height = (block.height / 2.5).dp)
                            .drawBehind { drawRect(Color.LightGray.copy(alpha = 0.2f)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, null, Modifier.size(20.dp), Color.Gray)
                            Text("${block.width}x${block.height}", fontSize = 8.sp, color = Color.Gray, fontFamily = monoFont)
                        }
                    }
                }
            }
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
