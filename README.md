# MemorIA — Gestor de Recuerdos Multimedia 📸✨

> *Captura. Preserva. Revive.*

Una app Android nativa premium en **Kotlin + Jetpack Compose** que cubre **todos los requisitos de la práctica** con una estética innovadora y arquitectura profesional.

---

## 🎨 Concepto y Diseño

**MemorIA** es un gestor de recuerdos multimedia con inteligencia emocional. La app asocia cada recuerdo con una emoción (Alegría, Amor, Nostalgia, Aventura, Paz, Sorpresa, Gratitud) y presenta la galería con una interfaz oscura premium en paleta **Índigo Profundo + Oro Cálido**.

---

## 📋 Cobertura de Requisitos

### 1. ✅ Captura de Fotografía (`CameraScreen.kt`)
- **CameraX** con `PreviewView`, `ImageCapture` y ciclo de vida vinculado
- Solicitud de permisos con `Accompanist Permissions` (rationale + denied states)
- Soporte frontal/trasera + flash on/off
- Animación de obturador (shutter flash blanco)
- Guardado en `filesDir/captures/MEMORIA_timestamp.jpg`
- Preview thumbnail en tiempo real al capturar
- Manejo completo de errores `ImageCaptureException` (cámara cerrada, I/O, cámara inválida)
- Confirmación visual al guardar (navegación directa al procesador)

### 2. ✅ Procesamiento y Conversión (`ImageProcessScreen.kt` + `ImageProcessor.kt`)
- **9 filtros/transformaciones**: Original, Grises, Sepia, Vintage, Cálido, Frío, Rotar 90°, Voltear horizontal, Comprimir 50%
- **Filtros con `ColorMatrix`**: Grayscale, Sepia, Vintage, Warm, Cool
- **Transformaciones geométricas**: Rotación, flip, escala
- **3 formatos de salida**: JPEG, PNG, WEBP_LOSSY
- Slider de calidad (20–100%) para formatos con pérdida
- **Comparación antes/después** con divisor arrastrable (drag gesture)
- Información técnica: dimensiones, tamaño KB original/procesada
- Guardado en `filesDir/processed/processed_timestamp.ext`
- Procesamiento en `Dispatchers.IO` (coroutine, no bloquea UI)

### 3. ✅ Reproducción de Video (`VideoPlayerScreen.kt`)
- **Media3/ExoPlayer** con control de ciclo de vida
- Controles: ▶️ Play, ⏸ Pause, ⏹ Stop/Restart, ⏪ -10s, ⏩ +10s
- **Barra de progreso personalizada** (`Slider` con tema dorado)
- Etiquetas de tiempo formato `MM:SS`
- Indicador de buffering (`CircularProgressIndicator`)
- **Pantalla completa** con toggle
- Soporte video local (URI File) y remoto (HTTP URL)
- Video demo automático (Big Buck Bunny) si no hay video del recuerdo
- Auto-ocultar controles tras 3s de reproducción
- `DisposableEffect` garantiza `exoPlayer.release()` al salir

### 4. ✅ Animaciones y Datos Basados en Tiempo

**Animación 1 — Splash Screen** (`SplashActivity.kt`):
- Logo con spring bounce (`DampingRatioMediumBouncy`)
- Texto con fade-in tween 800ms
- Tagline con slide-up offset 700ms
- Pulsación infinita del logo (`InfiniteTransition`)
- Puntos de carga animados con delay escalonado

**Animación 2 — Timer Ring** (`TimerScreen.kt`):
- Anillo SVG que rota continuamente (`LinearEasing`, 3000ms)
- Arco de progreso que avanza según segundos % 60
- Gradiente sweep dinámico (Índigo→Oro→Índigo)

**Animación 3 — Glow pulsante** (`TimerScreen.kt`):
- `animateFloatAsState` para alpha del glow radial cuando corre
- Escala pulsante del display cuando el cronómetro está activo

**Datos basados en tiempo**:
- Cronómetro con `delay(1_000)` en coroutine de ViewModel
- `TimerState` con start/pause/reset/lap
- Tiempo formateado `HH:MM:SS` / `MM:SS`

**Otras animaciones**:
- FAB con spring bounce en Home
- Items de galería con entrada escalonada `index * 50ms`
- `AnimatedContent` para botones (idle/saving/saved)
- Transiciones de navegación slide + fade

### 5. ✅ Procesadores y Eventos UI

| Evento | Dónde |
|--------|-------|
| `onClick` | FAB, botones, chips, cards de memoria |
| `onLongClick` | Cards en galería → toggle favorita |
| Input de texto | Título, Descripción, Ubicación (con validación) |
| Cambio de estado | Switch comparación, Toggle flash, Selector emoción |
| Navegación | NavHost con 8 destinos, animaciones slide/fade |
| Drag gesture | Divisor de comparación en ImageProcessScreen |
| Slider | Calidad de imagen, progreso de video, cronómetro |

**UX destacado**:
- Validación de título vacío con `isError` + `supportingText`
- Long-press en emotion chips muestra label descriptivo
- Auto-ocultación de controles de video
- Snackbar de errores con dismiss
- `AnimatedVisibility` en todos los estados condicionales
- Feedback háptico en FAB (via click animation)

### 6. ✅ Lifecycle y Rendimiento

**Lifecycle**:
```
SplashActivity  → onCreate, onPause, onDestroy
MainActivity    → onCreate, onResume, onPause, onDestroy
MemoryViewModel → init, onCleared (cancela timerJob)
ExoPlayer       → DisposableEffect → release()
CameraX         → bindToLifecycle → auto-unbind
```

**Logs** (todos los componentes críticos):
```kotlin
Log.d(TAG, "Camera bound: lensFacing=$lensFacing")
Log.e(TAG, "Image processing failed", e)
Log.d(TAG, "ExoPlayer released")
Log.d(TAG, "Memory saved with id=$id")
```

**Manejo de excepciones**:
- `try-catch` en cada operación de BD, cámara e imagen
- `ImageCaptureException` con mensajes localizados
- `onError` en ExoPlayer listener
- `Result<T>` en `ImageProcessor.processImage()`

**Rendimiento**:
- Imágenes cargadas con `inSampleSize` calculado dinámicamente
- Procesamiento en `Dispatchers.IO` (coroutine)
- `StateFlow` + `collect` — sin polling, push-based
- `DisposableEffect` libera ExoPlayer al destruir composable
- `timerJob?.cancel()` en `onCleared()`
- Coil maneja caché de imágenes automáticamente
- `LazyColumn` / `LazyVerticalGrid` — solo renderiza lo visible

---

## 🏗️ Arquitectura

```
MVVM + Repository Pattern + Room + StateFlow

UI Layer (Compose Screens)
    ↕ StateFlow
ViewModel Layer (MemoryViewModel)
    ↕ suspend fun / Flow
Repository Layer (MemoryRepository)
    ↕ Room DAO
Local DB (MemorIADatabase / SQLite)
```

---

## 📁 Estructura de Archivos

```
app/src/main/
├── AndroidManifest.xml
├── java/com/memoria/app/
│   ├── MainActivity.kt              # NavHost + Lifecycle
│   ├── data/
│   │   ├── model/Memory.kt          # Entidades + UI states
│   │   └── repository/
│   │       └── MemoryRepository.kt  # Room DB + DAO + Repo
│   ├── utils/
│   │   └── ImageProcessor.kt        # Filtros + conversión
│   ├── viewmodel/
│   │   └── MemoryViewModel.kt       # Estado + lógica + timer
│   └── ui/
│       ├── theme/Theme.kt           # Colores + tipografía
│       └── screens/
│           ├── SplashActivity.kt    # Splash animado
│           ├── HomeScreen.kt        # Galería home + stats
│           ├── CameraScreen.kt      # CameraX + permisos
│           ├── ImageProcessScreen.kt # Filtros + conversión
│           ├── VideoPlayerScreen.kt # ExoPlayer + controles
│           ├── TimerScreen.kt       # Cronómetro animado
│           ├── AddMemoryScreen.kt   # Form + emotion picker
│           └── GalleryAndDetailScreen.kt # Galería + detalle
└── res/
    ├── drawable/ic_splash_logo.xml
    ├── values/{strings,themes,colors}.xml
    └── xml/{file_paths,backup_rules,data_extraction_rules}.xml
```

---

## 🛠️ Stack Tecnológico

| Librería | Uso |
|----------|-----|
| **Jetpack Compose** | UI declarativa 100% |
| **Material 3** | Design system |
| **CameraX** | Captura de foto |
| **Media3/ExoPlayer** | Reproducción de video |
| **Room** | Base de datos local |
| **Coil** | Carga de imágenes |
| **Accompanist Permissions** | Gestión de permisos |
| **Navigation Compose** | Navegación multi-pantalla |
| **DataStore** | Preferencias |
| **Kotlin Coroutines + Flow** | Async + reactive state |

---

## 🚀 Cómo Compilar

```bash
# Clonar / abrir en Android Studio Hedgehog+
# Gradle sync automático
# Run en emulador API 26+ o dispositivo físico

# Mínimo: API 26 (Android 8.0)
# Target: API 34 (Android 14)
```

---

## 🎯 Aspectos Innovadores

1. **Etiquetas emocionales** — cada recuerdo tiene una emoción asociada con color y emoji
2. **Comparación drag-to-reveal** — desliza para comparar original/procesada en tiempo real
3. **Timer con anillo SVG animado** — visualización artística del cronómetro
4. **Galería con entrada escalonada** — items aparecen con delay individual (staggered)
5. **Paleta Índigo + Oro** — identidad visual premium, no genérica
6. **Splash screen con spring physics** — rebote natural del logo
7. **8 pantallas conectadas** con transiciones slide bidireccionales

---

*MemorIA — Desarrollado con Kotlin y Jetpack Compose — Práctica de Desarrollo Mobile*
