package com.memoria.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.memoria.app.ui.theme.*
import com.memoria.app.viewmodel.MemoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    viewModel: MemoryViewModel,
    onBack: () -> Unit
) {
    val timerState by viewModel.timerState.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Restart),
        label = "ringRotation"
    )

    val glowAlpha by animateFloatAsState(
        targetValue  = if (timerState.isRunning) 0.6f else 0f,
        animationSpec = tween(600), label = "glowAlpha"
    )
    val pulseScale by if (timerState.isRunning) {
        infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 1.05f,
            animationSpec = infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse"
        )
    } else { remember { mutableStateOf(1f) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cronómetro") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Volver") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally) {

            Spacer(Modifier.height(24.dp))

            Box(modifier = Modifier.size(280.dp).scale(pulseScale), contentAlignment = Alignment.Center) {

                Box(modifier = Modifier.size(280.dp).alpha(glowAlpha).background(
                    Brush.radialGradient(listOf(MemorIAColors.IndigoAccent.copy(0.3f), Color.Transparent)),
                    androidx.compose.foundation.shape.CircleShape
                ))

                Canvas(modifier = Modifier.size(280.dp).rotate(ringRotation)) {
                    val strokeWidth = 6.dp.toPx()
                    drawArc(color = MemorIAColors.NeutralBorder, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(strokeWidth))
                    val sweep = ((timerState.elapsedSeconds % 60) / 60f) * 360f
                    drawArc(
                        brush = Brush.sweepGradient(listOf(MemorIAColors.IndigoAccent, MemorIAColors.GoldBright, MemorIAColors.IndigoAccent)),
                        startAngle = -90f, sweepAngle = sweep, useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = formatTimerDisplay(timerState.elapsedSeconds),
                        fontSize   = 48.sp,
                        fontWeight = FontWeight.Black,
                        color      = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = (-1).sp
                    )
                    if (timerState.isRunning) {
                        Spacer(Modifier.height(4.dp))
                        Surface(shape = RoundedCornerShape(50), color = MemorIAColors.Success.copy(0.15f)) {
                            Text("⬤  CORRIENDO", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall, color = MemorIAColors.Success, letterSpacing = 1.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {

                if (timerState.elapsedSeconds > 0) {
                    OutlinedButton(onClick = viewModel::resetTimer, border = androidx.compose.foundation.BorderStroke(1.dp, MemorIAColors.Error)) {
                        Icon(Icons.Filled.RestartAlt, null, tint = MemorIAColors.Error)
                        Spacer(Modifier.width(4.dp))
                        Text("Reset", color = MemorIAColors.Error)
                    }
                }

                val btnColor by animateColorAsState(
                    targetValue  = if (timerState.isRunning) MemorIAColors.GoldBright else MemorIAColors.IndigoAccent,
                    animationSpec = tween(300), label = "btnColor"
                )
                var btnPressed by remember { mutableStateOf(false) }
                val btnScale by animateFloatAsState(
                    targetValue  = if (btnPressed) 0.9f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "btnScale", finishedListener = { btnPressed = false }
                )
                Button(
                    onClick = { btnPressed = true; if (timerState.isRunning) viewModel.pauseTimer() else viewModel.startTimer() },
                    modifier = Modifier.scale(btnScale).height(52.dp).widthIn(min = 130.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = btnColor),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    AnimatedContent(targetState = timerState.isRunning, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "playPause") { running ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (running) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, tint = if (running) MemorIAColors.IndigoDeep else Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text(if (running) "Pausar" else "Iniciar", color = if (running) MemorIAColors.IndigoDeep else Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (timerState.isRunning) {
                    OutlinedButton(onClick = viewModel::addLap) {
                        Icon(Icons.Filled.Flag, null); Spacer(Modifier.width(4.dp)); Text("Vuelta")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            if (timerState.laps.isNotEmpty()) {
                Text("Vueltas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    itemsIndexed(timerState.laps.reversed()) { index, lap ->
                        LapRow(lapNumber = timerState.laps.size - index, lapTime = lap, isLast = index == 0)
                    }
                }
            }

            if (timerState.elapsedSeconds > 0) {
                Spacer(Modifier.height(16.dp))
                Card(modifier = Modifier.padding(16.dp).fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MemorIAColors.NeutralCard)) {
                    Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatItem("Tiempo total", formatTimerDisplay(timerState.elapsedSeconds))
                        StatItem("Vueltas", "${timerState.laps.size}")
                        if (timerState.laps.isNotEmpty()) StatItem("Mejor vuelta", formatTimerDisplay(timerState.laps.min()))
                    }
                }
            }
        }
    }
}

@Composable
fun LapRow(lapNumber: Int, lapTime: Long, isLast: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(visible = visible, enter = slideInVertically { -it } + fadeIn()) {
        Column {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Vuelta $lapNumber", style = MaterialTheme.typography.bodyMedium, color = if (isLast) MemorIAColors.GoldBright else MaterialTheme.colorScheme.onBackground)
                Text(formatTimerDisplay(lapTime), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = if (isLast) MemorIAColors.GoldBright else MemorIAColors.IndigoAccent)
            }
            Divider(color = MemorIAColors.NeutralBorder, thickness = 0.5.dp)
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MemorIAColors.IndigoAccent)
        Text(label,  style = MaterialTheme.typography.labelSmall, color = MemorIAColors.NeutralHint)
    }
}

fun formatTimerDisplay(totalSeconds: Long): String {
    val hours   = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}