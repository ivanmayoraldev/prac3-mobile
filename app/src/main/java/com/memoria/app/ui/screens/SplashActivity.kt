package com.memoria.app.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.memoria.app.MainActivity
import com.memoria.app.ui.theme.MemorIAColors
import com.memoria.app.ui.theme.MemorIATheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "SplashActivity"

@SuppressLint("CustomSplashScreen")
class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        setContent {
            MemorIATheme(darkTheme = true) {
                SplashScreen()
            }
        }

        lifecycleScope.launch {
            delay(2_800)
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
}

@Composable
fun SplashScreen() {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        isVisible = true
    }

    val logoScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.3f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "logoScale"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 500),
        label = "textAlpha"
    )

    val taglineOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 20.dp,
        animationSpec = tween(durationMillis = 700, delayMillis = 700, easing = FastOutSlowInEasing),
        label = "taglineOffset"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors  = listOf(MemorIAColors.IndigoDark, MemorIAColors.NeutralDark),
                    radius  = 1200f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .scale(logoScale * pulseScale)
                    .size(120.dp)
                    .background(
                        brush  = Brush.radialGradient(
                            colors = listOf(MemorIAColors.IndigoAccent, MemorIAColors.IndigoMid)
                        ),
                        shape  = androidx.compose.foundation.shape.CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text  = "✦",
                    fontSize   = 52.sp,
                    color      = MemorIAColors.GoldBright
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text       = "MemorIA",
                modifier   = Modifier.alpha(textAlpha),
                fontSize   = 42.sp,
                fontWeight = FontWeight.Black,
                color      = Color.White,
                letterSpacing = (-1).sp,
                textAlign  = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text       = "Captura. Preserva. Revive.",
                modifier   = Modifier
                    .alpha(textAlpha)
                    .offset(y = taglineOffset),
                fontSize   = 15.sp,
                fontWeight = FontWeight.Light,
                color      = MemorIAColors.NeutralText,
                letterSpacing = 2.sp,
                textAlign  = TextAlign.Center
            )
        }

        AnimatedLoadingDots(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
                .alpha(textAlpha)
        )
    }
}

@Composable
fun AnimatedLoadingDots(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(3) { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue  = 1f,
                animationSpec = infiniteRepeatable(
                    animation  = tween(600),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .background(MemorIAColors.IndigoAccent, androidx.compose.foundation.shape.CircleShape)
            )
        }
    }
}
