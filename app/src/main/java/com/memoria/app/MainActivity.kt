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
    const val TIMER        = "timer"
    const val ADD_MEMORY   = "add/{imagePath}"

    fun process(imagePath: String)   = "process/${encode(imagePath)}"
    fun detail(memoryId: Long)       = "detail/$memoryId"
    fun videoPlayer(memoryId: Long)  = "video/$memoryId"
    fun addMemory(imagePath: String) = "add/${encode(imagePath)}"

    private fun encode(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
}

class MainActivity : ComponentActivity() {

    private val viewModel: MemoryViewModel by viewModels {
        MemoryViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")

        setContent {
            MemorIATheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color    = MaterialTheme.colorScheme.background
                ) {
                    MemorIANavHost(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause — saving state")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy — releasing resources")
    }
}


@Composable
fun MemorIANavHost(viewModel: MemoryViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec  = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(350))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec  = tween(350, easing = FastOutSlowInEasing)
            ) + fadeIn(tween(350))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(350, easing = FastOutSlowInEasing)
            ) + fadeOut(tween(200))
        }
    ) {

        composable(Routes.HOME) {
            HomeScreen(
                viewModel   = viewModel,
                onOpenCamera  = { navController.navigate(Routes.CAMERA) },
                onOpenGallery = { navController.navigate(Routes.GALLERY) },
                onOpenTimer   = { navController.navigate(Routes.TIMER) },
                onOpenDetail  = { id -> navController.navigate(Routes.detail(id)) }
            )
        }

        composable(Routes.CAMERA) {
            CameraScreen(
                onImageCaptured = { path ->
                    navController.navigate(Routes.process(path)) {
                        popUpTo(Routes.CAMERA) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PROCESS,
            arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
        ) { backStack ->
            val encoded = backStack.arguments?.getString("imagePath") ?: ""
            val imagePath = java.net.URLDecoder.decode(encoded, "UTF-8")
            ImageProcessScreen(
                viewModel  = viewModel,
                imagePath  = imagePath,
                onSave     = { savedPath ->
                    navController.navigate(Routes.addMemory(savedPath)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.GALLERY) {
            GalleryScreen(
                viewModel     = viewModel,
                onOpenDetail  = { id -> navController.navigate(Routes.detail(id)) },
                onBack        = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("memoryId") { type = NavType.LongType })
        ) { backStack ->
            val memoryId = backStack.arguments?.getLong("memoryId") ?: -1L
            MemoryDetailScreen(
                viewModel    = viewModel,
                memoryId     = memoryId,
                onPlayVideo  = { id -> navController.navigate(Routes.videoPlayer(id)) },
                onBack       = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.VIDEO_PLAYER,
            arguments = listOf(navArgument("memoryId") { type = NavType.LongType })
        ) { backStack ->
            val memoryId = backStack.arguments?.getLong("memoryId") ?: -1L
            VideoPlayerScreen(
                viewModel = viewModel,
                memoryId  = memoryId,
                onBack    = { navController.popBackStack() }
            )
        }

        composable(Routes.TIMER) {
            TimerScreen(
                viewModel = viewModel,
                onBack    = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.ADD_MEMORY,
            arguments = listOf(navArgument("imagePath") { type = NavType.StringType })
        ) { backStack ->
            val encoded   = backStack.arguments?.getString("imagePath") ?: ""
            val imagePath = java.net.URLDecoder.decode(encoded, "UTF-8")
            AddMemoryScreen(
                viewModel = viewModel,
                imagePath = imagePath,
                onSaved   = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
