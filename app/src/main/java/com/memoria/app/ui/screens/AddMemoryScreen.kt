package com.memoria.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import coil.compose.AsyncImage
import com.memoria.app.data.model.EmotionTag
import com.memoria.app.ui.theme.*
import com.memoria.app.viewmodel.MemoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMemoryScreen(
    viewModel: MemoryViewModel,
    imagePath: String,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    var title           by remember { mutableStateOf("") }
    var description     by remember { mutableStateOf("") }
    var location        by remember { mutableStateOf("") }
    var selectedEmotion by remember { mutableStateOf(EmotionTag.JOY) }
    var titleError      by remember { mutableStateOf(false) }
    var isSaving        by remember { mutableStateOf(false) }
    var saveSuccess     by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsState()

    val successScale by animateFloatAsState(
        targetValue  = if (saveSuccess) 1.2f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "successScale",
        finishedListener = { if (saveSuccess) onSaved() }
    )

    LaunchedEffect(uiState.isLoading) {
        if (isSaving && !uiState.isLoading && uiState.error == null) {
            saveSuccess = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Recuerdo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                AsyncImage(
                    model              = imagePath,
                    contentDescription = "Imagen del recuerdo",
                    modifier           = Modifier.fillMaxSize(),
                    contentScale       = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                            )
                        )
                )
                Text(
                    "✦ Mantén presionado en galería para favorita",
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    style    = MaterialTheme.typography.labelSmall,
                    color    = Color.White.copy(alpha = 0.8f)
                )
            }

            Spacer(Modifier.height(20.dp))

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {

                OutlinedTextField(
                    value           = title,
                    onValueChange   = { title = it; titleError = false },
                    label           = { Text("Título *") },
                    placeholder     = { Text("¿Qué recuerdo es este?") },
                    modifier        = Modifier.fillMaxWidth(),
                    isError         = titleError,
                    supportingText  = if (titleError) {
                        { Text("El título es obligatorio", color = MemorIAColors.Error) }
                    } else null,
                    leadingIcon     = { Icon(Icons.Filled.Edit, contentDescription = null) },
                    singleLine      = true,
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MemorIAColors.IndigoAccent,
                        unfocusedBorderColor = MemorIAColors.NeutralBorder
                    )
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value           = description,
                    onValueChange   = { description = it },
                    label           = { Text("Descripción") },
                    placeholder     = { Text("Cuéntame sobre este momento...") },
                    modifier        = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                    leadingIcon     = { Icon(Icons.Filled.Notes, contentDescription = null) },
                    maxLines        = 5,
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MemorIAColors.IndigoAccent,
                        unfocusedBorderColor = MemorIAColors.NeutralBorder
                    )
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value           = location,
                    onValueChange   = { location = it },
                    label           = { Text("Ubicación") },
                    placeholder     = { Text("¿Dónde fue?") },
                    modifier        = Modifier.fillMaxWidth(),
                    leadingIcon     = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                    singleLine      = true,
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = MemorIAColors.IndigoAccent,
                        unfocusedBorderColor = MemorIAColors.NeutralBorder
                    )
                )

                Spacer(Modifier.height(20.dp))

                Text(
                    "¿Cómo te hace sentir?",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(10.dp))

                val emotionList = EmotionTag.values().toList()
                val rows = emotionList.chunked(4)
                rows.forEach { row ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { emotion ->
                            EmotionButton(
                                emotion  = emotion,
                                selected = selectedEmotion == emotion,
                                modifier = Modifier.weight(1f),
                                onClick  = { selectedEmotion = emotion }
                            )
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (title.isBlank()) { titleError = true; return@Button }
                        isSaving = true
                        viewModel.saveMemory(
                            title       = title,
                            description = description,
                            imagePath   = imagePath,
                            videoPath   = null,
                            emotionTag  = selectedEmotion,
                            location    = location
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).scale(successScale),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = if (saveSuccess) MemorIAColors.Success else MemorIAColors.IndigoAccent
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    AnimatedContent(
                        targetState = when {
                            saveSuccess       -> "success"
                            uiState.isLoading -> "saving"
                            else              -> "idle"
                        },
                        label = "saveBtn"
                    ) { state ->
                        when (state) {
                            "success" -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Check, null)
                                Spacer(Modifier.width(8.dp))
                                Text("¡Guardado!", fontWeight = FontWeight.Bold)
                            }
                            "saving"  -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(20.dp), Color.White, 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Guardando...")
                            }
                            else      -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Save, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Guardar Recuerdo", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                uiState.error?.let { err ->
                    Spacer(Modifier.height(8.dp))
                    Text(err, color = MemorIAColors.Error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmotionButton(
    emotion: EmotionTag,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = if (selected) emotionColor(emotion).copy(alpha = 0.3f) else MemorIAColors.NeutralCard,
        label = "emotionBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) emotionColor(emotion) else MemorIAColors.NeutralBorder,
        label = "emotionBorder"
    )
    var scale by remember { mutableStateOf(1f) }
    val animScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "emotionScale",
        finishedListener = { scale = 1f }
    )

    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier
                .aspectRatio(1f)
                .scale(animScale)
                .combinedClickable(
                    onClick    = { scale = 0.85f; onClick() },
                    onLongClick = { showTooltip = true }
                ),
            shape  = RoundedCornerShape(12.dp),
            color  = bgColor,
            border = BorderStroke(1.5.dp, borderColor)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(emotionEmoji(emotion), fontSize = 24.sp)
            }
        }
        if (selected || showTooltip) {
            Text(
                emotionLabel(emotion),
                style    = MaterialTheme.typography.labelSmall,
                color    = emotionColor(emotion),
                modifier = Modifier.padding(top = 2.dp)
            )
            if (showTooltip) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1500)
                    showTooltip = false
                }
            }
        }
    }
}

fun emotionLabel(tag: EmotionTag): String = when (tag) {
    EmotionTag.JOY       -> "Alegría"
    EmotionTag.LOVE      -> "Amor"
    EmotionTag.NOSTALGIA -> "Nostalgia"
    EmotionTag.ADVENTURE -> "Aventura"
    EmotionTag.PEACE     -> "Paz"
    EmotionTag.SURPRISE  -> "Sorpresa"
    EmotionTag.GRATITUDE -> "Gratitud"
}