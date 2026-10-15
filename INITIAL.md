# INITIAL.md — Configuración inicial Room + MVVM (TodoList)

> Alcance: solo configuración inicial. No se implementó código de negocio (sin Entity, DAO, Database, Repository, ViewModel ni pantalla).

Proyecto base: `Empty Activity` en Kotlin + Compose, paquete `com.rojas.room`.
Objetivo pedido: ejemplo simple de TodoList con Room, arquitectura MVVM y capas DTO + DAO, en Android 37 (API 37).

## 1. Estado de partida (antes de tocar nada)

- `app/build.gradle.kts`: `compileSdk = release(36) + minorApiLevel 1`, `targetSdk = 36`, `minSdk = 27`, sin Room, sin KSP aplicado.
- `gradle/libs.versions.toml`: sin entradas de Room ni de `lifecycle-viewmodel-compose` / `lifecycle-runtime-compose`.
- El alias del plugin KSP (`com.google.devtools.ksp`) ya existía declarado en el catálogo, pero no estaba aplicado en el módulo `:app`.

## 2. Cambios realizados (qué, dónde, antes → después, por qué)

### 2.1 `app/build.gradle.kts:7-9` — `compileSdk` 36.1 → 37

Antes:
```kotlin
compileSdk {
    version = release(36) { minorApiLevel = 1 }
}
```
Después:
```kotlin
compileSdk = 37
```
Por qué: pediste dejar el proyecto en Android 37. `compileSdk` define contra qué versión del SDK compila el código (APIs disponibles). Se alinea con `targetSdk = 37` para no mezclar comportamientos. Requisito: tener instalada la plataforma SDK 37 en el SDK Manager; si no está, el Sync falla y hay que instalarla.

### 2.2 `app/build.gradle.kts:11-14` — `targetSdk` 36 → 37

Antes: `targetSdk = 36`. Después: `targetSdk = 37`. `minSdk = 27` no se tocó.

Por qué se cambia: lo pediste (Android 37) y además conviene que `targetSdk` acompañe a `compileSdk`. `targetSdk` no es solo "número": le dice al sistema "esta app ya conoce y acepta los cambios de comportamiento de esa versión", por lo que el sistema deja de aplicar modos de compatibilidad viejos.

Por qué aparece el aviso del editor:
El mensaje `It looks like you just edited the targetSdk from 36 to 37...` es una inspección informativa estándar del IDE, no un error de compilación. Aparece siempre que se sube `targetSdk` porque subirlo puede activar nuevos comportamientos en tiempo de ejecución (permisos, notificaciones, trabajo en segundo plano, edge-to-edge, restricciones de APIs, etc.). El IDE te pide que revises la documentación de cambios de comportamiento y que uses el Android SDK Upgrade Assistant antes de dar el cambio por bueno. Verlo es lo esperado; no significa que el cambio esté mal.

Qué se revisó:
- El cambio fue intencional y está documentado aquí (era requisito tuyo: Android 37).
- No hay en el proyecto, a nivel de configuración, APIs obsoletas evidentes que choquen con 37: `MainActivity`, `AndroidManifest.xml` y dependencias Compose/Material3 se mantienen igual.
- Lo que queda pendiente (no ejecutable desde este entorno, requiere el IDE con GUI): correr `Android Studio → Tools → Android SDK Upgrade Assistant`, seleccionar el salto 36 → 37, aplicar sus recomendaciones y probar en emulador/dispositivo API 37. No se pudo verificar la lista exacta de cambios de comportamiento de API 37 desde aquí, así que esa revisión queda como paso obligatorio antes de publicar.

Decisión: se mantiene `targetSdk = 37`. Si en tu máquina el Upgrade Assistant reporta algo que te obligue a un ajuste (permiso, `windowSoftInputMode`, backup rules, etc.), lo documentamos como enmienda a este archivo.

### 2.3 `app/build.gradle.kts:1-5` — se aplica el plugin KSP

Antes: solo `android.application` + `kotlin.compose`. Después: se suma `alias(libs.plugins.ksp)`.

Por qué: Room genera su implementación mediante anotaciones (`@Entity`, `@Dao`, `@Database`). Ese código lo genera `room-compiler` vía KSP. Sin aplicar el plugin, las dependencias Room quedan a medio instalar y el `@Dao` nunca se implementa. El alias ya existía en el catálogo (`libs.versions.toml:39`); solo faltaba aplicarlo en `:app`.

### 2.4 `gradle/libs.versions.toml:8-9` — nuevas versiones

- `room = "2.6.1"`: versión estable y muy documentada, adecuada para un ejemplo de curso. Fija una sola versión para `room-runtime`, `room-ktx` y `room-compiler` para que no se mezclen.
- `lifecycleViewmodelCompose = "2.11.0"`: se iguala a `lifecycleRuntimeKtx = "2.11.0"` para evitar mezclar versiones de Lifecycle.

### 2.5 `gradle/libs.versions.toml:30-34` + `app/build.gradle.kts:46-51` — dependencias Room + MVVM

| Dependencia | Para qué |
|---|---|
| `androidx.room:room-runtime` (`implementation`) | Motor Room / SQLite. Sin esto no hay base de datos. |
| `androidx.room:room-ktx` (`implementation`) | Soporte Kotlin: `suspend` y `Flow` en el DAO. Es lo que permite observar la lista de tareas reactivamente. |
| `androidx.room:room-compiler` (`ksp`, no `implementation`) | Procesador de anotaciones. Genera el código del DAO/DB. Va como `ksp(...)` porque solo se usa en compilación, no se empaqueta. |
| `androidx.lifecycle:lifecycle-viewmodel-compose` (`implementation`) | `viewModel()` en Compose. Es el puente View ↔ ViewModel del MVVM. |
| `androidx.lifecycle:lifecycle-runtime-compose` (`implementation`) | `collectAsStateWithLifecycle()`. Observa el `StateFlow` del ViewModel respetando el ciclo de vida. |

Por qué `ksp(...)` y no `implementation(...)` para el compiler: si se pone como `implementation`, el artefacto del compilador terminaría en el APK sin aportar nada en ejecución. Como `ksp`, solo actúa en tiempo de compilación.

Lo que NO se tocó y por qué:
- `minSdk = 27`: se conserva para no reducir compatibilidad; Room y Lifecycle lo soportan.
- `namespace`, `applicationId`, `versionCode/Name`: identidad del proyecto, nada que ver con Room.
- `AndroidManifest.xml`: Room local no necesita permisos (no es red ni almacenamiento externo). No se agrega nada.
- `compileOptions Java 11`, `buildFeatures.compose`: se dejan como estaban.

## 3. Conceptos (DTO, DAO, MVVM) — resumen para el ejemplo TodoList

- **DTO (Data Transfer Object):** objeto solo de datos que viaja entre capas. En este ejemplo será la `@Entity Task(id, titulo, hecho)`: representa una fila de la tabla y circula DB → Repository → ViewModel → UI. No tiene lógica SQL ni de pantalla.
- **DAO (Data Access Object):** interfaz `@Dao` con `@Insert`, `@Update`, `@Delete` y `@Query`. Es la única capa que escribe SQL. Room la implementa por ti (vía KSP). La UI nunca toca SQL; solo llama al DAO indirectamente vía Repository/ViewModel.
- **MVVM:** `Model` = Room + Repository (datos); `ViewModel` = estado (`StateFlow<List<Task>>`) y eventos (`agregar/toggle/borrar`), sobrevive a rotación; `View` = Compose que solo observa estado y emite eventos.

## 4. Verificación pendiente (hacer en tu máquina)

1. SDK Manager → instalar `Android SDK Platform 37` si falta.
2. `Sync Now` en Android Studio.
3. `Tools → Android SDK Upgrade Assistant` → revisar salto 36 → 37.
4. Probar en emulador API 37: agregar / marcar / borrar tarea (cuando exista el código), más rotación de pantalla.
