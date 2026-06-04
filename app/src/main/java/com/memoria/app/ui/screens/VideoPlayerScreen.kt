package com.memoria.app.ui.screens

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.memoria.app.ui.theme.*
import com.memoria.app.viewmodel.MemoryViewModel
import kotlinx.coroutines.delay
import java.io.File

private const val TAG = "VideoPlayerScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    viewModel: MemoryViewModel,
    memoryId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val memories by viewModel.memories.collectAsState()
    val memory = memories.find { it.id == memoryId }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().also {
            Log.d(TAG, "ExoPlayer created")
        }
    }

    var isPlaying    by remember { mutableStateOf(false) }
    var isBuffering  by remember { mutableStateOf(false) }
    var currentPos   by remember { mutableStateOf(0L) }
    var duration     by remember { mutableStateOf(0L) }
    var isFullscreen by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }

    LaunchedEffect(isPlaying, controlsVisible) {
        if (isPlaying && controlsVisible) {
            delay(3_000)
            controlsVisible = false
        }
    }

    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPos = exoPlayer.currentPosition
            duration   = exoPlayer.duration.takeIf { it > 0 } ?: 0L
            isPlaying  = exoPlayer.isPlaying
            delay(500)
        }
    }

    LaunchedEffect(memory) {
        val videoPath = memory?.videoPath
        if (!videoPath.isNullOrBlank()) {
            val mediaItem = if (videoPath.startsWith("http")) {
                MediaItem.fromUri(videoPath)
            } else {
                MediaItem.fromUri(android.net.Uri.fromFile(File(videoPath)))
            }
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            Log.d(TAG, "Media loaded: $videoPath")
        } else {
            val demoUri = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
            exoPlayer.setMediaItem(MediaItem.fromUri(demoUri))
            exoPlayer.prepare()
            Log.d(TAG, "Demo video loaded")
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                Log.d(TAG, "isPlaying: $playing")
            }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e(TAG, "Player error: ${error.message}")
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            Log.d(TAG, "ExoPlayer released")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = if (isFullscreen) Modifier.fillMaxSize()
            else Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        controlsVisible = true
                    }
            )

            AnimatedVisibility(
                visible = isBuffering,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn(), exit = fadeOut()
            ) {
                CircularProgressIndicator(color = Color.White)
            }

            AnimatedVisibility(
                visible  = controlsVisible,
                modifier = Modifier.fillMaxSize(),
                enter    = fadeIn(tween(200)),
                exit     = fadeOut(tween(500))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    ) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Volver", tint = Color.White)
                    }

                    IconButton(
                        onClick = { isFullscreen = !isFullscreen },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        Icon(
                            if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                            contentDescription = "Pantalla completa",
                            tint = Color.White
                        )
                    }

                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VideoControlButton(size = 44.dp, onClick = {
                            exoPlayer.seekTo((currentPos - 10_000).coerceAtLeast(0))
                        }) {
                            Icon(Icons.Filled.Replay10, contentDescription = "Retroceder",
                                tint = Color.White, modifier = Modifier.size(24.dp))
                        }

                        VideoControlButton(size = 64.dp, onClick = {
                            if (isPlaying) exoPlayer.pause() else exoPlayer.play()
                        }) {
                            if (isBuffering) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }

                        VideoControlButton(size = 44.dp, onClick = {
                            exoPlayer.seekTo(0)
                            exoPlayer.pause()
                        }) {
                            Icon(Icons.Filled.Stop, contentDescription = "Parar",
                                tint = Color.White, modifier = Modifier.size(24.dp))
                        }

                        VideoControlButton(size = 44.dp, onClick = {
                            exoPlayer.seekTo((currentPos + 10_000).coerceAtMost(duration.coerceAtLeast(1)))
                        }) {
                            Icon(Icons.Filled.Forward10, contentDescription = "Adelantar",
                                tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(currentPos), style = MaterialTheme.typography.labelSmall, color = Color.White)
                            Text(formatTime(duration),   style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        }
                        Spacer(Modifier.height(4.dp))
                        Slider(
                            value         = if (duration > 0) currentPos.toFloat() / duration.toFloat() else 0f,
                            onValueChange = { fraction -> exoPlayer.seekTo((fraction * duration).toLong()) },
                            modifier      = Modifier.fillMaxWidth(),
                            colors        = SliderDefaults.colors(
                                thumbColor         = MemorIAColors.GoldBright,
                                activeTrackColor   = MemorIAColors.GoldBright,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            )
                        )
                    }
                }
            }
        }

        if (!isFullscreen && memory != null) {
            val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
            val videoHeightDp = screenWidthDp * 9f / 16f

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = videoHeightDp)
                    .padding(16.dp)
            ) {
                Text(memory.title, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = Color.White)
                if (memory.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(memory.description, style = MaterialTheme.typography.bodyMedium,
                        color = MemorIAColors.NeutralText)
                }
            }
        }
    }
}

@Composable
fun VideoControlButton(size: Dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue  = if (pressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "controlBtn",
        finishedListener = { pressed = false }
    )
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .background(Color.Black.copy(alpha = 0.5f), androidx.compose.foundation.shape.CircleShape)
            .clickable { pressed = true; onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val seconds = ms / 1000
    val minutes = seconds / 60
    val secs    = seconds % 60
    return "$minutes:${secs.toString().padStart(2, '0')}"
}