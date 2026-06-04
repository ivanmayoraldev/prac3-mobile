package com.memoria.app.ui.screens

import android.content.res.AssetFileDescriptor
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.AssetDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.memoria.app.ui.theme.*
import com.memoria.app.viewmodel.MemoryViewModel
import kotlinx.coroutines.delay
import java.io.File

private const val TAG = "VideoPlayerScreen"

@Composable
fun VideoPlayerScreen(
    viewModel: MemoryViewModel,
    memoryId: Long,
    onBack: () -> Unit
) {
    val context  = LocalContext.current
    val memories by viewModel.memories.collectAsState()
    val memory   = memories.find { it.id == memoryId }

    var isPlaying    by remember { mutableStateOf(false) }
    var isBuffering  by remember { mutableStateOf(true) }
    var isReady      by remember { mutableStateOf(false) }
    var currentPos   by remember { mutableStateOf(0L) }
    var duration     by remember { mutableStateOf(0L) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var errorMsg     by remember { mutableStateOf<String?>(null) }

    val mediaItem = remember(memoryId) {
        val path = memory?.videoPath
        when {
            !path.isNullOrBlank() && path.startsWith("http") ->
                MediaItem.fromUri(android.net.Uri.parse(path))
            !path.isNullOrBlank() ->
                MediaItem.fromUri(android.net.Uri.fromFile(File(path)))
            else ->
                MediaItem.fromUri(android.net.Uri.parse("asset:///demo.mp4"))
        }
    }

    val exoPlayer = remember {
        val dataSourceFactory = DataSource.Factory {
            AssetDataSource(context)
        }
        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()
            .also { Log.d(TAG, "ExoPlayer created with asset support") }
    }

    LaunchedEffect(mediaItem) {
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        Log.d(TAG, "Media prepared: $mediaItem")
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> { isBuffering = true }
                    Player.STATE_READY     -> { isBuffering = false; isReady = true; duration = exoPlayer.duration.coerceAtLeast(0) }
                    Player.STATE_ENDED     -> { isPlaying = false; isBuffering = false }
                    Player.STATE_IDLE      -> { isBuffering = false }
                }
            }
            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Player error: ${error.message}")
                errorMsg = "Error al reproducir el video."
                isBuffering = false
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
            Log.d(TAG, "ExoPlayer released")
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(300)
            if (isReady) {
                currentPos = exoPlayer.currentPosition
                if (duration <= 0) duration = exoPlayer.duration.coerceAtLeast(0)
            }
        }
    }

    LaunchedEffect(isPlaying, showControls) {
        if (isPlaying && showControls) { delay(3_500); showControls = false }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player        = exoPlayer
                    useController = false
                    layoutParams  = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .let { if (isFullscreen) it.fillMaxHeight() else it.aspectRatio(16f / 9f) }
                .clickable { showControls = !showControls }
        )

        AnimatedVisibility(
            visible  = isBuffering && errorMsg == null,
            modifier = Modifier.fillMaxWidth()
                .let { if (isFullscreen) it.fillMaxHeight() else it.aspectRatio(16f / 9f) },
            enter = fadeIn(), exit = fadeOut()
        ) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(color = MemorIAColors.IndigoAccent, strokeWidth = 3.dp, modifier = Modifier.size(48.dp))
                    Text("Cargando video...", color = Color.White.copy(0.8f), style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        errorMsg?.let { msg ->
            Box(
                modifier = Modifier.fillMaxWidth()
                    .let { if (isFullscreen) it.fillMaxHeight() else it.aspectRatio(16f / 9f) },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color.Black.copy(0.85f)),
                    shape    = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.ErrorOutline, null, Modifier.size(48.dp), tint = MemorIAColors.Error)
                        Text("Error de reproducción", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(msg, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.7f))
                        Button(
                            onClick = {
                                errorMsg = null; isBuffering = true
                                exoPlayer.setMediaItem(mediaItem)
                                exoPlayer.prepare(); exoPlayer.play()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MemorIAColors.IndigoAccent)
                        ) { Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Reintentar") }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible  = showControls && errorMsg == null,
            modifier = Modifier.fillMaxWidth()
                .let { if (isFullscreen) it.fillMaxHeight() else it.aspectRatio(16f / 9f) },
            enter = fadeIn(tween(150)), exit = fadeOut(tween(400))
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(0.65f), Color.Transparent, Color.Transparent, Color.Black.copy(0.85f)))
                )
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Volver", tint = Color.White)
                    }
                    Text(
                        memory?.title ?: "Demo — MemorIA",
                        Modifier.weight(1f).padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1
                    )
                    IconButton(onClick = { isFullscreen = !isFullscreen }) {
                        Icon(if (isFullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen, null, tint = Color.White)
                    }
                }

                Row(
                    Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    VBtn(44.dp, { exoPlayer.seekTo((currentPos - 10_000).coerceAtLeast(0)) }) {
                        Icon(Icons.Filled.Replay10, null, Modifier.size(24.dp), tint = Color.White)
                    }
                    VBtn(68.dp, { if (isPlaying) exoPlayer.pause() else exoPlayer.play() }) {
                        Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, null, Modifier.size(38.dp), tint = Color.White)
                    }
                    VBtn(44.dp, { exoPlayer.seekTo(0); exoPlayer.pause(); showControls = true }) {
                        Icon(Icons.Filled.Stop, null, Modifier.size(24.dp), tint = Color.White)
                    }
                    VBtn(44.dp, { if (duration > 0) exoPlayer.seekTo((currentPos + 10_000).coerceAtMost(duration)) }) {
                        Icon(Icons.Filled.Forward10, null, Modifier.size(24.dp), tint = Color.White)
                    }
                }

                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), Arrangement.SpaceBetween) {
                        Text(fmtTime(currentPos), style = MaterialTheme.typography.labelMedium, color = Color.White)
                        if (duration > 0)
                            Text(fmtTime(duration), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(0.6f))
                    }
                    Slider(
                        value = if (duration > 0) (currentPos.toFloat() / duration.toFloat()).coerceIn(0f, 1f) else 0f,
                        onValueChange = { if (duration > 0) exoPlayer.seekTo((it * duration).toLong()) },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = SliderDefaults.colors(
                            thumbColor = MemorIAColors.GoldBright,
                            activeTrackColor = MemorIAColors.GoldBright,
                            inactiveTrackColor = Color.White.copy(0.25f)
                        )
                    )
                }
            }
        }

        if (!isFullscreen && memory != null) {
            val screenW = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
            Column(Modifier.fillMaxWidth().padding(top = screenW * 9f / 16f).padding(16.dp)) {
                Text(memory.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                if (memory.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(memory.description, style = MaterialTheme.typography.bodyMedium, color = MemorIAColors.NeutralText)
                }
            }
        }
    }
}

@Composable
fun VBtn(size: Dp, onClick: () -> Unit, content: @Composable () -> Unit) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.82f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "vbtn", finishedListener = { pressed = false }
    )
    Box(
        Modifier.size(size).scale(scale).background(Color.White.copy(0.15f), CircleShape)
            .clickable { pressed = true; onClick() },
        contentAlignment = Alignment.Center
    ) { content() }
}

fun fmtTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000; val m = s / 60; val sec = s % 60
    return "$m:${sec.toString().padStart(2, '0')}"
}