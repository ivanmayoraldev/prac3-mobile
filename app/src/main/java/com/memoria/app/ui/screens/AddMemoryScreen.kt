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
        targetValue  = if (saveSuccess) 1.15f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "success", finishedListener = { if (saveSuccess) onSaved() }
    )

    LaunchedEffect(uiState.isLoading) {
        if (isSaving && !uiState.isLoading && uiState.error == null) saveSuccess = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Recuerdo", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {

            Box(Modifier.fillMaxWidth().height(200.dp)) {
                AsyncImage(model = imagePath, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.4f)))))
                Text(
                    "✦ Mantén pulsado en galería para marcar como favorita",
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.85f)
                )
            }

            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                OutlinedTextField(
                    value           = title,
                    onValueChange   = { title = it; titleError = false },
                    label           = { Text("Título *") },
                    placeholder     = { Text("¿Qué recuerdo es este?") },
                    isError         = titleError,
                    supportingText  = if (titleError) { { Text("El título es obligatorio", color = MemorIAColors.Error) } } else null,
                    leadingIcon     = { Icon(Icons.Filled.Edit, null) },
                    modifier        = Modifier.fillMaxWidth(),
                    singleLine      = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MemorIAColors.IndigoAccent, unfocusedBorderColor = MemorIAColors.NeutralBorder)
                )

                OutlinedTextField(
                    value           = description,
                    onValueChange   = { description = it },
                    label           = { Text("Descripción") },
                    placeholder     = { Text("Cuéntame sobre este momento...") },
                    leadingIcon     = { Icon(Icons.Filled.Notes, null) },
                    modifier        = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                    maxLines        = 5,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MemorIAColors.IndigoAccent, unfocusedBorderColor = MemorIAColors.NeutralBorder)
                )

                OutlinedTextField(
                    value           = location,
                    onValueChange   = { location = it },
                    label           = { Text("Ubicación") },
                    placeholder     = { Text("¿Dónde fue?") },
                    leadingIcon     = { Icon(Icons.Filled.LocationOn, null) },
                    modifier        = Modifier.fillMaxWidth(),
                    singleLine      = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MemorIAColors.IndigoAccent, unfocusedBorderColor = MemorIAColors.NeutralBorder)
                )

                Text("¿Cómo te hace sentir?", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                val emotions = EmotionTag.values().toList()
                emotions.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { emotion ->
                            EmotionPicker(
                                emotion  = emotion,
                                selected = selectedEmotion == emotion,
                                modifier = Modifier.weight(1f),
                                onClick  = { selectedEmotion = emotion }
                            )
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (title.isBlank()) { titleError = true; return@Button }
                        isSaving = true
                        viewModel.saveMemory(title = title, description = description, imagePath = imagePath, emotionTag = selectedEmotion, location = location)
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp).scale(successScale),
                    colors   = ButtonDefaults.buttonColors(containerColor = if (saveSuccess) MemorIAColors.Success else MemorIAColors.IndigoAccent),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    AnimatedContent(
                        targetState = when { saveSuccess -> "ok"; uiState.isLoading -> "saving"; else -> "idle" },
                        label = "saveBtn"
                    ) { state ->
                        when (state) {
                            "ok"     -> Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Check, null); Spacer(Modifier.width(8.dp)); Text("¡Guardado!", fontWeight = FontWeight.Bold) }
                            "saving" -> Row(verticalAlignment = Alignment.CenterVertically) { CircularProgressIndicator(Modifier.size(20.dp), Color.White, 2.dp); Spacer(Modifier.width(8.dp)); Text("Guardando...") }
                            else     -> Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Save, null); Spacer(Modifier.width(8.dp)); Text("Guardar Recuerdo", fontWeight = FontWeight.Bold) }
                        }
                    }
                }

                uiState.error?.let { Text(it, color = MemorIAColors.Error, style = MaterialTheme.typography.bodySmall) }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EmotionPicker(emotion: EmotionTag, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    var scale by remember { mutableStateOf(1f) }
    val animScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "emoScale", finishedListener = { scale = 1f }
    )
    val bg by animateColorAsState(targetValue = if (selected) emotionColor(emotion).copy(0.3f) else MemorIAColors.NeutralCard, label = "emoBg")
    val border by animateColorAsState(targetValue = if (selected) emotionColor(emotion) else MemorIAColors.NeutralBorder, label = "emoBorder")

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.aspectRatio(1f).scale(animScale)
                .combinedClickable(onClick = { scale = 0.85f; onClick() }),
            shape  = RoundedCornerShape(12.dp),
            color  = bg,
            border = BorderStroke(1.5.dp, border)
        ) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { Text(emotion.emoji, fontSize = 24.sp) }
        }
        if (selected) {
            Text(emotion.label, style = MaterialTheme.typography.labelSmall, color = emotionColor(emotion), modifier = Modifier.padding(top = 2.dp))
        }
    }
}