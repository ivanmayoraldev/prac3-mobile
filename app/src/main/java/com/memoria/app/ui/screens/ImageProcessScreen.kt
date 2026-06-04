package com.memoria.app.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.memoria.app.data.model.*
import com.memoria.app.ui.theme.*
import com.memoria.app.utils.ImageProcessor
import com.memoria.app.viewmodel.MemoryViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageProcessScreen(
    viewModel: MemoryViewModel,
    imagePath: String,
    onSave: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val state by viewModel.imageProcessState.collectAsState()

    var selectedFilter   by remember { mutableStateOf(ProcessingType.NONE) }
    var selectedFormat   by remember { mutableStateOf(ImageFormat.JPEG) }
    var quality          by remember { mutableStateOf(85f) }
    var dividerFraction  by remember { mutableStateOf(0.5f) }
    var showComparison   by remember { mutableStateOf(false) }
    var showSaveDialog   by remember { mutableStateOf(false) }
    var saveTitle        by remember { mutableStateOf("") }
    var saveSuccess      by remember { mutableStateOf(false) }

    val originalInfo = remember(imagePath) {
        val (w, h) = ImageProcessor.getImageDimensions(imagePath)
        val kb     = ImageProcessor.getFileSize(imagePath) / 1024
        Triple(w, h, kb)
    }
    val processedInfo = remember(state.processedPath) {
        state.processedPath?.let {
            val (w, h) = ImageProcessor.getImageDimensions(it)
            val kb     = ImageProcessor.getFileSize(it) / 1024
            Triple(w, h, kb)
        }
    }

    LaunchedEffect(imagePath) { viewModel.setSourceImage(imagePath) }

    val successScale by animateFloatAsState(
        targetValue  = if (saveSuccess) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "success",
        finishedListener = { if (saveSuccess) { saveSuccess = false; state.processedPath?.let { onSave(it) } } }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editor de Imagen", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Volver") }
                },
                actions = {
                    Surface(shape = RoundedCornerShape(50), color = MemorIAColors.IndigoMid) {
                        Text(
                            selectedFormat.label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MemorIAColors.IndigoAccent
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model              = imagePath,
                    contentDescription = "Original",
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Fit
                )

                if (state.processedPath != null) {
                    if (showComparison) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(dividerFraction)
                                .clip(RectangleShape)
                        ) {
                            AsyncImage(
                                model              = state.processedPath,
                                contentDescription = "Procesada",
                                modifier           = Modifier.fillMaxWidth().fillMaxHeight(),
                                contentScale       = ContentScale.Fit
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(2.dp)
                                .fillMaxWidth(dividerFraction)
                                .background(Color.White)
                        )

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .align(Alignment.CenterStart)
                                .offset(x = with(androidx.compose.ui.platform.LocalDensity.current) {
                                    val screenW = 360.dp
                                    (screenW * dividerFraction) - 16.dp
                                })
                                .background(Color.White, CircleShape)
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        dividerFraction = (dividerFraction + dragAmount / size.width)
                                            .coerceIn(0.05f, 0.95f)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.SwapHoriz, null, Modifier.size(20.dp), tint = Color.Black)
                        }

                        Text(
                            "ORIGINAL", modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                                .background(Color.Black.copy(0.6f), RoundedCornerShape(4.dp)).padding(4.dp, 2.dp),
                            style = MaterialTheme.typography.labelSmall, color = Color.White
                        )
                        Text(
                            selectedFilter.label.uppercase(), modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                                .background(MemorIAColors.IndigoAccent.copy(0.85f), RoundedCornerShape(4.dp)).padding(4.dp, 2.dp),
                            style = MaterialTheme.typography.labelSmall, color = Color.White
                        )
                    } else {
                        AsyncImage(
                            model              = state.processedPath,
                            contentDescription = "Procesada",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Fit
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
                            color    = MemorIAColors.IndigoAccent.copy(0.9f),
                            shape    = RoundedCornerShape(50)
                        ) {
                            Text(
                                "${selectedFilter.emoji} ${selectedFilter.label}",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style    = MaterialTheme.typography.labelMedium,
                                color    = Color.White
                            )
                        }
                    }
                }

                if (state.isProcessing) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MemorIAColors.IndigoAccent, strokeWidth = 3.dp)
                            Spacer(Modifier.height(10.dp))
                            Text("Aplicando ${selectedFilter.label}...", color = Color.White, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                if (saveSuccess) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.5f)), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier.size(80.dp).scale(successScale).background(MemorIAColors.Success, CircleShape),
                            contentAlignment = Alignment.Center
                        ) { Icon(Icons.Filled.Check, null, Modifier.size(40.dp), tint = Color.White) }
                    }
                }

                Text(
                    "${originalInfo.first}×${originalInfo.second} · ${originalInfo.third}KB",
                    modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
                        .background(Color.Black.copy(0.55f), RoundedCornerShape(4.dp)).padding(4.dp, 2.dp),
                    style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.8f)
                )
            }

            if (state.processedPath != null) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.CompareArrows, null, Modifier.size(18.dp), tint = MemorIAColors.IndigoAccent)
                        Text("Comparar original / procesada", style = MaterialTheme.typography.labelMedium)
                    }
                    Switch(
                        checked = showComparison, onCheckedChange = { showComparison = it },
                        colors  = SwitchDefaults.colors(checkedThumbColor = MemorIAColors.IndigoAccent, checkedTrackColor = MemorIAColors.IndigoMid)
                    )
                }

                processedInfo?.let { (w, h, kb) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoPill("${w}×${h}px", Icons.Filled.AspectRatio)
                        InfoPill("${kb}KB", Icons.Filled.Storage)
                        InfoPill(selectedFormat.label, Icons.Filled.Image)
                        InfoPill("Q$quality%", Icons.Filled.HighQuality)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            Divider(color = MemorIAColors.NeutralBorder, modifier = Modifier.padding(vertical = 8.dp))

            Text(
                "Filtros y efectos",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ProcessingType.values().toList()) { filter ->
                    FilterCard(
                        filter   = filter,
                        selected = selectedFilter == filter,
                        onClick  = {
                            selectedFilter = filter
                            if (filter != ProcessingType.NONE) {
                                viewModel.processImage(filter, selectedFormat, quality.toInt())
                            } else {
                                viewModel.resetImageProcess()
                                viewModel.setSourceImage(imagePath)
                            }
                        }
                    )
                }
            }

            Divider(color = MemorIAColors.NeutralBorder, modifier = Modifier.padding(vertical = 4.dp))

            Text(
                "Formato de salida",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold
            )
            Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ImageFormat.values().forEach { fmt ->
                    val sel = selectedFormat == fmt
                    val bg  by animateColorAsState(if (sel) MemorIAColors.IndigoAccent else MemorIAColors.NeutralCard, label = "fmt$fmt")
                    Surface(
                        onClick = { selectedFormat = fmt; if (state.processedPath != null) viewModel.processImage(selectedFilter, fmt, quality.toInt()) },
                        shape   = RoundedCornerShape(12.dp),
                        color   = bg,
                        border  = BorderStroke(1.5.dp, if (sel) MemorIAColors.IndigoAccent else MemorIAColors.NeutralBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(fmt.label, style = MaterialTheme.typography.labelLarge, color = if (sel) Color.White else MemorIAColors.NeutralText, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                            Text(".${fmt.ext}", style = MaterialTheme.typography.labelSmall, color = if (sel) Color.White.copy(0.7f) else MemorIAColors.NeutralHint)
                        }
                    }
                }
            }

            if (selectedFormat != ImageFormat.PNG) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.HighQuality, null, Modifier.size(18.dp), tint = MemorIAColors.IndigoAccent)
                            Text("Calidad de compresión", style = MaterialTheme.typography.labelMedium)
                        }
                        Text(
                            "${quality.toInt()}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MemorIAColors.IndigoAccent
                        )
                    }
                    Slider(
                        value         = quality,
                        onValueChange = { quality = it },
                        onValueChangeFinished = { if (state.processedPath != null) viewModel.processImage(selectedFilter, selectedFormat, quality.toInt()) },
                        valueRange    = 20f..100f,
                        colors        = SliderDefaults.colors(
                            activeTrackColor  = MemorIAColors.IndigoAccent,
                            thumbColor        = MemorIAColors.IndigoAccent
                        )
                    )
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Menor tamaño", style = MaterialTheme.typography.labelSmall, color = MemorIAColors.NeutralHint)
                        Text("Mayor calidad", style = MaterialTheme.typography.labelSmall, color = MemorIAColors.NeutralHint)
                    }
                }
            }

            Divider(color = MemorIAColors.NeutralBorder, modifier = Modifier.padding(vertical = 8.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (state.processedPath != null) {
                    Button(
                        onClick  = { showSaveDialog = true },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = MemorIAColors.IndigoAccent),
                        shape    = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Guardar imagen procesada como recuerdo", fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick  = { onSave(imagePath) },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    border   = BorderStroke(1.dp, MemorIAColors.NeutralBorder),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Image, null, tint = MemorIAColors.NeutralHint)
                    Spacer(Modifier.width(8.dp))
                    Text("Usar imagen original sin filtro", color = MemorIAColors.NeutralHint)
                }

                state.error?.let { err ->
                    Card(colors = CardDefaults.cardColors(containerColor = MemorIAColors.Error.copy(0.1f))) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Error, null, tint = MemorIAColors.Error, modifier = Modifier.size(16.dp))
                            Text(err, style = MaterialTheme.typography.bodySmall, color = MemorIAColors.Error)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            icon    = { Icon(Icons.Filled.Save, null, tint = MemorIAColors.IndigoAccent) },
            title   = { Text("Guardar recuerdo", fontWeight = FontWeight.Bold) },
            text    = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("La imagen procesada con filtro \"${selectedFilter.label}\" se guardará como un nuevo recuerdo.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(
                        value         = saveTitle,
                        onValueChange = { saveTitle = it },
                        label         = { Text("Título del recuerdo") },
                        placeholder   = { Text("Mi recuerdo con filtro ${selectedFilter.label}") },
                        modifier      = Modifier.fillMaxWidth(),
                        singleLine    = true,
                        colors        = OutlinedTextFieldDefaults.colors(focusedBorderColor = MemorIAColors.IndigoAccent)
                    )
                    processedInfo?.let { (w, h, kb) ->
                        Text(
                            "📐 ${w}×${h}px · 💾 ${kb}KB · 🖼 ${selectedFormat.label}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MemorIAColors.NeutralHint
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = saveTitle.ifBlank { "Recuerdo ${selectedFilter.label}" }
                        val processedPath = state.processedPath ?: return@Button
                        viewModel.saveMemory(
                            title       = title,
                            description = "Filtro: ${selectedFilter.label} · Formato: ${selectedFormat.label}",
                            imagePath   = imagePath,
                            videoPath   = null,
                            emotionTag  = com.memoria.app.data.model.EmotionTag.JOY
                        )
                        showSaveDialog = false
                        saveSuccess    = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MemorIAColors.IndigoAccent)
                ) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
fun FilterCard(filter: ProcessingType, selected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        targetValue  = if (selected) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "filterScale"
    )
    val bg by animateColorAsState(
        targetValue = if (selected) MemorIAColors.IndigoAccent else MemorIAColors.NeutralCard,
        label = "filterBg"
    )
    Surface(
        onClick = onClick,
        modifier = Modifier.scale(scale).width(72.dp),
        shape    = RoundedCornerShape(14.dp),
        color    = bg,
        border   = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MemorIAColors.IndigoAccent else MemorIAColors.NeutralBorder),
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Column(
            modifier            = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(filter.emoji, fontSize = 22.sp)
            Text(
                filter.label,
                style     = MaterialTheme.typography.labelSmall,
                color     = if (selected) Color.White else MemorIAColors.NeutralText,
                textAlign = TextAlign.Center,
                maxLines  = 2
            )
        }
    }
}

@Composable
fun InfoPill(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape = RoundedCornerShape(50), color = MemorIAColors.NeutralCard, border = BorderStroke(1.dp, MemorIAColors.NeutralBorder)) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, null, Modifier.size(12.dp), tint = MemorIAColors.IndigoAccent)
            Text(text, style = MaterialTheme.typography.labelSmall, color = MemorIAColors.NeutralText)
        }
    }
}