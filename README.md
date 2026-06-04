# MemorIA — Gestor de Recuerdos Multimedia

> Captura. Preserva. Revive.

App Android nativa en Kotlin + Jetpack Compose que cubre todos los requisitos de la practica con arquitectura profesional MVVM y diseno premium en paleta Indigo + Oro.

---

## Concepto y Diseno

MemorIA es un gestor de recuerdos multimedia con etiquetas emocionales. La app asocia cada recuerdo con una emocion (Alegria, Amor, Nostalgia, Aventura, Paz, Sorpresa, Gratitud) y presenta la galeria con una interfaz oscura premium.

---

## Cobertura de Requisitos

### 1. Captura de Fotografia — CameraScreen.kt

- CameraX con PreviewView, ImageCapture y ciclo de vida vinculado
- Solicitud de permisos con Accompanist Permissions, incluyendo flujo de rationale y estado denegado
- Soporte camara frontal y trasera, flash on/off
- Animacion de obturador al capturar
- Guardado en filesDir/captures/MEMORIA_timestamp.jpg
- Preview thumbnail en tiempo real al capturar
- Manejo completo de errores ImageCaptureException: camara cerrada, I/O, camara invalida
- Confirmacion visual al guardar con navegacion directa al procesador

### 2. Procesamiento y Conversion — ImageProcessScreen.kt + ImageProcessor.kt

- 11 filtros y transformaciones: Original, Grises, Sepia, Vintage, Calido, Frio, Brillo, Contraste, Rotar 90, Voltear, Comprimir
- Filtros con ColorMatrix y transformaciones geometricas
- 3 formatos de salida: JPEG, PNG, WEBP
- Slider de calidad 20-100% para formatos con perdida
- Comparacion antes/despues con divisor arrastrable mediante drag gesture
- Informacion tecnica: dimensiones y tamano original/procesada en KB
- Guardado en filesDir/processed/
- Procesamiento en Dispatchers.IO mediante coroutine, sin bloquear la UI

Decision tecnica: BitmapFactory.decodeFile() ignora los metadatos EXIF de orientacion. Se usa ExifInterface para leer TAG_ORIENTATION y aplicar la rotacion correctiva antes de cualquier procesamiento, garantizando que las fotos tomadas en vertical se muestren correctamente.

### 3. Reproduccion de Video — VideoPlayerScreen.kt

- Media3/ExoPlayer con control de ciclo de vida
- Controles: Play, Pause, Stop con reinicio, retroceso 10s, avance 10s
- Barra de progreso personalizada con Slider en tema dorado
- Etiquetas de tiempo en formato MM:SS
- Indicador de buffering con CircularProgressIndicator
- Pantalla completa con toggle
- Soporte video local via URI de archivo y video remoto via URL HTTP
- Video demo local incluido en assets de la app, sin necesidad de internet
- Auto-ocultar controles tras 3.5 segundos de reproduccion
- DisposableEffect garantiza exoPlayer.release() al destruir el composable

### 4. Animaciones y Datos Basados en Tiempo

Animacion 1 — Splash Screen (SplashActivity.kt):
- Logo con spring bounce usando DampingRatioMediumBouncy
- Texto con fade-in tween 800ms
- Tagline con slide-up 700ms de delay
- Pulsacion infinita del logo con InfiniteTransition
- Puntos de carga animados con delay escalonado

Animacion 2 — Anillo del cronometro (TimerScreen.kt):
- Anillo que rota continuamente con LinearEasing cada 3000ms
- Arco de progreso que avanza segun segundos mod 60
- Gradiente sweep dinamico de Indigo a Oro

Animacion 3 — Glow pulsante (TimerScreen.kt):
- animateFloatAsState para el alpha del glow radial cuando el cronometro esta activo
- Escala pulsante del display durante la ejecucion

Datos basados en tiempo:
- Cronometro con delay(1000) en coroutine del ViewModel
- TimerState con start, pause, reset y laps
- Tiempo formateado como HH:MM:SS o MM:SS segun duracion

Otras animaciones:
- FAB con spring bounce en Home
- Items de galeria con entrada escalonada index * 40ms
- AnimatedContent en botones de guardado: idle, saving, saved
- Transiciones de navegacion slide horizontal y fade entre las 8 pantallas

### 5. Procesadores y Eventos UI

| Evento | Donde |
|---|---|
| onClick | FAB, botones, chips, cards de memoria |
| onLongClick | Cards en galeria para toggle de favorita |
| Input de texto | Titulo, Descripcion, Ubicacion con validacion |
| Cambio de estado | Switch comparacion, Toggle flash, Selector emocion |
| Navegacion | NavHost con 8 destinos y animaciones |
| Drag gesture | Divisor de comparacion en ImageProcessScreen |
| Slider | Calidad de imagen y progreso de video |

Validacion de titulo vacio con isError y supportingText. AnimatedContent en boton guardar con tres estados. Snackbar de errores con dismiss. AnimatedVisibility en todos los estados condicionales.

### 6. Lifecycle y Rendimiento

Lifecycle:

```
SplashActivity  — onCreate, onPause, onDestroy
MainActivity    — onCreate, onResume, onPause, onDestroy
MemoryViewModel — init, onCleared cancela timerJob
ExoPlayer       — DisposableEffect garantiza release()
CameraX         — bindToLifecycle con auto-unbind
```

Logs en todos los componentes criticos con Log.d y Log.e por TAG. Try-catch en cada operacion de base de datos, camara e imagen. Result en ImageProcessor.processImage().

Rendimiento:
- inSampleSize calculado dinamicamente al cargar bitmaps, maximo 1920px
- Procesamiento en Dispatchers.IO
- StateFlow sin polling, actualizacion por push
- LazyColumn y LazyVerticalGrid renderizan solo lo visible
- Coil gestiona cache de imagenes automaticamente

---

## Arquitectura

```
MVVM + Repository Pattern + Room + StateFlow

UI Layer (Compose Screens)
    — StateFlow —
ViewModel Layer (MemoryViewModel)
    — suspend fun / Flow —
Repository Layer (MemoryRepository)
    — Room DAO —
Local DB (MemorIADatabase / SQLite)
```

La UI nunca accede directamente a la base de datos. El ViewModel expone StateFlow que la UI observa reactivamente. El Repository es el unico punto de verdad para los datos.

---

## Estructura de Archivos

```
app/src/main/
    AndroidManifest.xml
    assets/
        demo.mp4
    java/com/memoria/app/
        MainActivity.kt
        data/
            model/Memory.kt
            repository/MemoryRepository.kt
        utils/
            ImageProcessor.kt
        viewmodel/
            MemoryViewModel.kt
        ui/
            theme/Theme.kt
            screens/
                SplashActivity.kt
                HomeScreen.kt
                CameraScreen.kt
                ImageProcessScreen.kt
                VideoPlayerScreen.kt
                TimerScreen.kt
                AddMemoryScreen.kt
                GalleryAndDetailScreen.kt
    res/
        drawable/ic_launcher.xml
        values/strings.xml
        values/themes.xml
        values/colors.xml
        xml/file_paths.xml
        xml/backup_rules.xml
        xml/data_extraction_rules.xml
```

---

## Stack Tecnologico

| Libreria | Version | Uso |
|---|---|---|
| Kotlin | 1.9.22 | Lenguaje oficial Android |
| Jetpack Compose | BOM 2024.02 | UI declarativa |
| Material 3 | Incluido | Design system |
| CameraX | 1.3.2 | Captura de fotografia |
| Media3/ExoPlayer | 1.3.0 | Reproduccion de video |
| Room | 2.6.1 | Base de datos local con Flow reactivo |
| Navigation Compose | 2.7.7 | Navegacion con animaciones |
| Coil | 2.6.0 | Carga de imagenes con cache |
| ExifInterface | 1.3.7 | Correccion de orientacion EXIF |
| Accompanist Permissions | 0.34.0 | Gestion de permisos en Compose |
| Kotlin Coroutines y Flow | Incluido | Programacion asincrona |

---

## Compilacion

```
minSdk:    26 (Android 8.0)
targetSdk: 34 (Android 14)
Gradle:    8.7
AGP:       8.4.2
JDK:       17
```

Abrir en Android Studio Hedgehog o superior, sincronizar Gradle y ejecutar en emulador API 26+ o dispositivo fisico.

---

MemorIA — Desarrollado con Kotlin y Jetpack Compose — Practica 3 Desarrollo de Aplicaciones Moviles
