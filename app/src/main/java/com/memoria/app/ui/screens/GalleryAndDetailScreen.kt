package com.memoria.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.memoria.app.data.model.*
import com.memoria.app.ui.theme.*
import com.memoria.app.viewmodel.MemoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: MemoryViewModel,
    onOpenDetail: (Long) -> Unit,
    onBack: () -> Unit
) {
    val memories    by viewModel.memories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showFavs    by viewModel.showFavoritesOnly.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Galería", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Volver") } },
                actions = {
                    IconButton(onClick = viewModel::toggleFavoritesFilter) {
                        Icon(
                            if (showFavs) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            null, tint = if (showFavs) MemorIAColors.GoldBright else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MemoriaSearchBar(
                query    = searchQuery,
                onQuery  = viewModel::setSearchQuery,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            if (memories.isEmpty()) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        if (searchQuery.isBlank()) "No hay recuerdos" else "Sin resultados para \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge, color = MemorIAColors.NeutralHint
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(memories) { index, memory ->
                        GalleryItem(
                            memory      = memory, index = index,
                            onClick     = { onOpenDetail(memory.id) },
                            onLongClick = { viewModel.toggleFavorite(memory) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GalleryItem(memory: Memory, index: Int, onClick: () -> Unit, onLongClick: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 40L); visible = true }

    AnimatedVisibility(
        visible = visible,
        enter   = scaleIn(initialScale = 0.85f, animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) + fadeIn()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().aspectRatio(0.8f)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick),
            shape  = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MemorIAColors.NeutralCard)
        ) {
            Box(Modifier.fillMaxSize()) {
                if (memory.imagePath != null) {
                    AsyncImage(
                        model = memory.processedImagePath ?: memory.imagePath,
                        contentDescription = memory.title,
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(emotionColor(memory.emotionTag).copy(0.5f), MemorIAColors.NeutralCard))),
                        contentAlignment = Alignment.Center
                    ) { Text(memory.emotionTag.emoji, fontSize = 48.sp) }
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)), startY = 200f)))
                if (memory.isFavorite) {
                    Icon(Icons.Filled.Favorite, null, Modifier.align(Alignment.TopEnd).padding(8.dp).size(18.dp), tint = MemorIAColors.GoldBright)
                }
                Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                    Text(memory.title, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(formatDate(memory.createdAt), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDetailScreen(
    viewModel: MemoryViewModel,
    memoryId: Long,
    onPlayVideo: (Long) -> Unit,
    onBack: () -> Unit
) {
    val memories by viewModel.memories.collectAsState()
    val memory   = memories.find { it.id == memoryId }
    var showDelete by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Volver", tint = Color.White) }
                },
                actions = {
                    memory?.let { m ->
                        IconButton(onClick = { viewModel.toggleFavorite(m) }) {
                            Icon(if (m.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                null, tint = if (m.isFavorite) MemorIAColors.GoldBright else Color.White)
                        }
                        IconButton(onClick = { showDelete = true }) {
                            Icon(Icons.Filled.Delete, null, tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { _ ->
        if (memory == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = MemorIAColors.IndigoAccent) }
            return@Scaffold
        }

        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            val imageOffset = (scrollState.value * 0.3f).toInt()
            Box(Modifier.fillMaxWidth().height(320.dp).clipToBounds()) {
                if (memory.imagePath != null) {
                    AsyncImage(
                        model = memory.processedImagePath ?: memory.imagePath,
                        contentDescription = memory.title,
                        modifier = Modifier.fillMaxWidth().height(340.dp).offset(y = (-imageOffset).dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MemorIAColors.IndigoDark, MemorIAColors.NeutralDark))),
                        contentAlignment = Alignment.Center
                    ) { Text(memory.emotionTag.emoji, fontSize = 80.sp) }
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.25f), Color.Transparent, Color.Black.copy(0.55f)))))
            }

            Column(Modifier.padding(20.dp)) {
                Surface(shape = RoundedCornerShape(50), color = emotionColor(memory.emotionTag).copy(0.15f)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(memory.emotionTag.emoji, fontSize = 14.sp)
                        Text(memory.emotionTag.label, style = MaterialTheme.typography.labelMedium, color = emotionColor(memory.emotionTag))
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(memory.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)

                if (memory.location.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.LocationOn, null, Modifier.size(16.dp), tint = MemorIAColors.NeutralHint)
                        Text(memory.location, style = MaterialTheme.typography.bodySmall, color = MemorIAColors.NeutralHint)
                    }
                }

                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Filled.AccessTime, null, Modifier.size(16.dp), tint = MemorIAColors.NeutralHint)
                    Text(formatDate(memory.createdAt), style = MaterialTheme.typography.bodySmall, color = MemorIAColors.NeutralHint)
                }

                if (memory.description.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(memory.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(0.85f))
                }

                if (memory.processedImagePath != null) {
                    Spacer(Modifier.height(16.dp))
                    Surface(shape = RoundedCornerShape(8.dp), color = MemorIAColors.IndigoAccent.copy(0.15f), border = BorderStroke(1.dp, MemorIAColors.IndigoAccent.copy(0.3f))) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.AutoFixHigh, null, Modifier.size(14.dp), tint = MemorIAColors.IndigoAccent)
                            Text("Imagen con filtro aplicado", style = MaterialTheme.typography.labelSmall, color = MemorIAColors.IndigoAccent)
                        }
                    }
                }

                if (memory.videoPath != null || memory.type == MemoryType.VIDEO || memory.type == MemoryType.MIXED) {
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick  = { onPlayVideo(memory.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = MemorIAColors.IndigoAccent)
                    ) {
                        Icon(Icons.Filled.PlayCircleFilled, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Reproducir Video", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            icon    = { Icon(Icons.Filled.Delete, null, tint = MemorIAColors.Error) },
            title   = { Text("Eliminar recuerdo") },
            text    = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteMemory(memory!!); showDelete = false; onBack() }) {
                    Text("Eliminar", color = MemorIAColors.Error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancelar") } }
        )
    }
}