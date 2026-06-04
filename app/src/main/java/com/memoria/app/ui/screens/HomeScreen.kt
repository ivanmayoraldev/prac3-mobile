package com.memoria.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MemoryViewModel,
    onOpenCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    val memories    by viewModel.memories.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val showFavs    by viewModel.showFavoritesOnly.collectAsState()
    val stats       by viewModel.stats.collectAsState()
    val uiState     by viewModel.uiState.collectAsState()

    var greetingVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { greetingVisible = true }

    val greetingAlpha by animateFloatAsState(
        targetValue  = if (greetingVisible) 1f else 0f,
        animationSpec = tween(800), label = "greetingAlpha"
    )
    val greetingOffset by animateDpAsState(
        targetValue  = if (greetingVisible) 0.dp else (-16).dp,
        animationSpec = tween(600, easing = FastOutSlowInEasing), label = "greetingOffset"
    )

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.error) {
        uiState.error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            var fabPressed by remember { mutableStateOf(false) }
            val fabScale by animateFloatAsState(
                targetValue  = if (fabPressed) 0.9f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "fabScale", finishedListener = { fabPressed = false }
            )
            ExtendedFloatingActionButton(
                onClick         = { fabPressed = true; onOpenCamera() },
                modifier        = Modifier.scale(fabScale),
                icon            = { Icon(Icons.Filled.CameraAlt, null) },
                text            = { Text("Capturar") },
                containerColor  = MemorIAColors.IndigoAccent,
                contentColor    = Color.White
            )
        }
    ) { padding ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(MemorIAColors.IndigoDark, MaterialTheme.colorScheme.background)))
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column(modifier = Modifier.alpha(greetingAlpha).offset(y = greetingOffset)) {
                        Text(greeting(), style = MaterialTheme.typography.labelLarge, color = MemorIAColors.IndigoAccent, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Tus Recuerdos", style = MaterialTheme.typography.headlineLarge, color = Color.White, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(4.dp))
                        Text("${stats.first} memorias · ${stats.second} favoritas", style = MaterialTheme.typography.bodyMedium, color = MemorIAColors.NeutralHint)
                    }
                }
            }

            item {
                SearchBar(
                    query    = searchQuery, onQuery = viewModel::setSearchQuery,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item { QuickActionsRow(onOpenCamera, onOpenGallery, onOpenTimer, showFavs, viewModel::toggleFavoritesFilter) }

            item {
                Text(
                    text     = if (showFavs) "✦ Favoritas" else "Recientes",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    style    = MaterialTheme.typography.titleMedium
                )
            }

            if (memories.isEmpty()) {
                item { EmptyStateCard(onOpenCamera) }
            }

            val chunked: List<List<Memory>> = memories.chunked(2)
            items(chunked.size) { index ->
                val row = chunked[index]
                Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { memory ->
                        MemoryCard(
                            memory     = memory,
                            modifier   = Modifier.weight(1f),
                            onClick    = { onOpenDetail(memory.id) },
                            onFavorite = { viewModel.toggleFavorite(memory) }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun QuickActionsRow(
    onCamera: () -> Unit, onGallery: () -> Unit, onTimer: () -> Unit,
    showFavs: Boolean, onToggleFavs: () -> Unit
) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionChip(Icons.Filled.CameraAlt,      "Cámara",     onCamera)
        QuickActionChip(Icons.Filled.PhotoLibrary,   "Galería",    onGallery)
        QuickActionChip(Icons.Filled.Timer,          "Cronómetro", onTimer)
        QuickActionChip(
            icon     = if (showFavs) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            label    = "Favoritas",
            onClick  = onToggleFavs,
            selected = showFavs
        )
    }
}

@Composable
fun QuickActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue  = if (pressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chipScale", finishedListener = { pressed = false }
    )
    Surface(
        onClick = { pressed = true; onClick() },
        modifier = Modifier.scale(scale),
        shape    = RoundedCornerShape(50),
        color    = if (selected) MemorIAColors.IndigoAccent else MemorIAColors.NeutralCard,
        border   = BorderStroke(1.dp, if (selected) MemorIAColors.IndigoAccent else MemorIAColors.NeutralBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, null, Modifier.size(16.dp), tint = if (selected) Color.White else MemorIAColors.IndigoAccent)
            Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) Color.White else MemorIAColors.NeutralText)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MemoryCard(memory: Memory, modifier: Modifier = Modifier, onClick: () -> Unit, onFavorite: () -> Unit) {
    var isLongPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue  = if (isLongPressed) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "cardScale", finishedListener = { isLongPressed = false }
    )
    Card(
        modifier = modifier.scale(scale).aspectRatio(0.75f).combinedClickable(onClick = onClick, onLongClick = { isLongPressed = true; onFavorite() }),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MemorIAColors.NeutralCard)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (memory.imagePath != null) {
                AsyncImage(model = memory.imagePath, contentDescription = memory.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(emotionColor(memory.emotionTag).copy(0.7f), MemorIAColors.NeutralCard))),
                    contentAlignment = Alignment.Center
                ) { Text(emotionEmoji(memory.emotionTag), fontSize = 40.sp) }
            }
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.75f)), startY = 100f)))
            if (memory.type == MemoryType.VIDEO || memory.type == MemoryType.MIXED) {
                Icon(Icons.Filled.PlayCircleFilled, "Video", Modifier.align(Alignment.Center).size(40.dp), tint = Color.White.copy(0.9f))
            }
            if (memory.isFavorite) {
                Icon(Icons.Filled.Favorite, "Fav", Modifier.align(Alignment.TopEnd).padding(8.dp).size(20.dp), tint = MemorIAColors.GoldBright)
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
                Text(memory.title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatDate(memory.createdAt), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
            }
        }
    }
}

@Composable
fun EmptyStateCard(onOpenCamera: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -12f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float"
    )
    Card(modifier = Modifier.fillMaxWidth().padding(24.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MemorIAColors.NeutralCard)) {
        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📸", fontSize = 64.sp, modifier = Modifier.offset(y = floatY.dp))
            Spacer(Modifier.height(16.dp))
            Text("Sin recuerdos aún", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Captura tu primer momento especial", style = MaterialTheme.typography.bodyMedium, color = MemorIAColors.NeutralHint)
            Spacer(Modifier.height(20.dp))
            Button(onClick = onOpenCamera, colors = ButtonDefaults.buttonColors(containerColor = MemorIAColors.IndigoAccent)) {
                Icon(Icons.Filled.CameraAlt, null); Spacer(Modifier.width(8.dp)); Text("Abrir Cámara")
            }
        }
    }
}

fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when { hour < 12 -> "BUENOS DÍAS"; hour < 19 -> "BUENAS TARDES"; else -> "BUENAS NOCHES" }
}

fun formatDate(timestamp: Long): String = SimpleDateFormat("d MMM yyyy", Locale("es")).format(Date(timestamp))

fun emotionColor(tag: EmotionTag): Color = when (tag) {
    EmotionTag.JOY       -> Color(0xFFFFD166)
    EmotionTag.LOVE      -> Color(0xFFFF6B8A)
    EmotionTag.NOSTALGIA -> Color(0xFF8A7BBD)
    EmotionTag.ADVENTURE -> Color(0xFF4ECDC4)
    EmotionTag.PEACE     -> Color(0xFF95D5B2)
    EmotionTag.SURPRISE  -> Color(0xFFFF9F1C)
    EmotionTag.GRATITUDE -> Color(0xFF9B7DFF)
}

fun emotionEmoji(tag: EmotionTag): String = when (tag) {
    EmotionTag.JOY       -> "😊"; EmotionTag.LOVE      -> "❤️"
    EmotionTag.NOSTALGIA -> "🌅"; EmotionTag.ADVENTURE -> "🌍"
    EmotionTag.PEACE     -> "🌿"; EmotionTag.SURPRISE  -> "✨"
    EmotionTag.GRATITUDE -> "🙏"
}