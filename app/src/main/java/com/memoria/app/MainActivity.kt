package com.memoria.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.memoria.app.ui.screens.*
import com.memoria.app.ui.theme.MemorIATheme
import com.memoria.app.viewmodel.MemoryViewModel
import com.memoria.app.viewmodel.MemoryViewModelFactory

private const val TAG = "MainActivity"

object Routes {
    const val HOME         = "home"
    const val CAMERA       = "camera"
    const val PROCESS      = "process/{imagePath}"
    const val GALLERY      = "gallery"
    const val DETAIL       = "detail/{memoryId}"
    const val VIDEO_PLAYER = "video/{memoryId}"
    const val VIDEO_DEMO   = "video_demo"
    const val TIMER        = "timer"
    const val ADD_MEMORY   = "add/{imagePath}"

    fun process(p: String)    = "process/${enc(p)}"
    fun detail(id: Long)      = "detail/$id"
    fun videoPlayer(id: Long) = "video/$id"
    fun addMemory(p: String)  = "add/${enc(p)}"
    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
    fun dec(s: String): String = java.net.URLDecoder.decode(s, "UTF-8")
}

class MainActivity : ComponentActivity() {
    private val viewModel: MemoryViewModel by viewModels { MemoryViewModelFactory(application) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
        setContent {
            MemorIATheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MemorIANavHost(viewModel)
                }
            }
        }
    }

    override fun onResume()  { super.onResume();  Log.d(TAG, "onResume") }
    override fun onPause()   { super.onPause();   Log.d(TAG, "onPause") }
    override fun onDestroy() { super.onDestroy(); Log.d(TAG, "onDestroy") }
}

@Composable
fun MemorIANavHost(viewModel: MemoryViewModel) {
    val nav = rememberNavController()

    NavHost(
        navController       = nav,
        startDestination    = Routes.HOME,
        enterTransition     = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(320, easing = FastOutSlowInEasing)) + fadeIn(tween(320)) },
        exitTransition      = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(320, easing = FastOutSlowInEasing)) + fadeOut(tween(200)) },
        popEnterTransition  = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(320, easing = FastOutSlowInEasing)) + fadeIn(tween(320)) },
        popExitTransition   = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(320, easing = FastOutSlowInEasing)) + fadeOut(tween(200)) }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel     = viewModel,
                onOpenCamera  = { nav.navigate(Routes.CAMERA) },
                onOpenGallery = { nav.navigate(Routes.GALLERY) },
                onOpenTimer   = { nav.navigate(Routes.TIMER) },
                onOpenVideo   = { nav.navigate(Routes.VIDEO_DEMO) },
                onOpenDetail  = { id -> nav.navigate(Routes.detail(id)) }
            )
        }

        composable(Routes.CAMERA) {
            CameraScreen(
                onImageCaptured = { path ->
                    nav.navigate(Routes.process(path)) { popUpTo(Routes.CAMERA) { inclusive = true } }
                },
                onBack = { nav.popBackStack() }
            )
        }

        composable(Routes.PROCESS, listOf(navArgument("imagePath") { type = NavType.StringType })) { bs ->
            val path = Routes.dec(bs.arguments?.getString("imagePath") ?: "")
            ImageProcessScreen(
                viewModel = viewModel,
                imagePath = path,
                onSave    = { saved -> nav.navigate(Routes.addMemory(saved)) { popUpTo(Routes.HOME) } },
                onBack    = { nav.popBackStack() }
            )
        }

        composable(Routes.GALLERY) {
            GalleryScreen(
                viewModel    = viewModel,
                onOpenDetail = { id -> nav.navigate(Routes.detail(id)) },
                onBack       = { nav.popBackStack() }
            )
        }

        composable(Routes.DETAIL, listOf(navArgument("memoryId") { type = NavType.LongType })) { bs ->
            val id = bs.arguments?.getLong("memoryId") ?: -1L
            MemoryDetailScreen(
                viewModel   = viewModel,
                memoryId    = id,
                onPlayVideo = { nav.navigate(Routes.videoPlayer(it)) },
                onBack      = { nav.popBackStack() }
            )
        }

        composable(Routes.VIDEO_PLAYER, listOf(navArgument("memoryId") { type = NavType.LongType })) { bs ->
            val id = bs.arguments?.getLong("memoryId") ?: -1L
            VideoPlayerScreen(viewModel = viewModel, memoryId = id, onBack = { nav.popBackStack() })
        }

        composable(Routes.VIDEO_DEMO) {
            VideoPlayerScreen(viewModel = viewModel, memoryId = -1L, onBack = { nav.popBackStack() })
        }

        composable(Routes.TIMER) {
            TimerScreen(viewModel = viewModel, onBack = { nav.popBackStack() })
        }

        composable(Routes.ADD_MEMORY, listOf(navArgument("imagePath") { type = NavType.StringType })) { bs ->
            val path = Routes.dec(bs.arguments?.getString("imagePath") ?: "")
            AddMemoryScreen(
                viewModel = viewModel,
                imagePath = path,
                onSaved   = { nav.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } } },
                onBack    = { nav.popBackStack() }
            )
        }
    }
}