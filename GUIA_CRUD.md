# Guía CRUD TodoList — Room + MVVM + Navigation Compose

> Para devs que vienen de React. Sin código aplicado al proyecto: esta guía es el paso a paso a seguir.
> Estado previo requerido: `INITIAL.md` (SDK 37, KSP, Room 2.7.1 — se subió desde 2.6.1 porque el `room-compiler` 2.6.1 fallaba en KSP con `unexpected jvm signature V`, Lifecycle).

## 0. Mapa mental React → Compose (léelo una vez)

| React | Compose / Android | Idea |
|---|---|---|
| `props` | parámetros de `@Composable` | `TaskCard(task, onClick)` === `<TaskCard task onClick />` |
| `useState` | `remember { mutableStateOf() }` / `collectAsStateWithLifecycle()` | estado local vs estado del ViewModel |
| `useEffect` + fetch | `Flow` de Room + `StateFlow` en ViewModel | la lista se actualiza sola, no hay que refetchear |
| `Context / custom hook` | `ViewModel` | lógica y estado fuera del componente visual |
| `React Router` (`Routes`, `useNavigate`, `useParams`) | `NavController` + `NavHost` + rutas (`useParams` ≈ `backStackEntry.arguments`) | ver Paso 6 |
| `Modal` condicional | `AlertDialog` con `if (showDialog)` | mismo patrón: booleano que monta/desmonta |

Regla de oro MVVM (igual que en React bien estructurado): **la vista no toca la DB**. La vista observa estado y dispara eventos.

```
View (ListView, DetailView…)
 ↕ estado (StateFlow) / eventos (fun save(), deleteById()…)
ViewModel
 ↕ suspend fun / Flow
Repository
 ↕ Entity / DAO
Room (SQLite)
```

## 1. Estructura de paquetes a crear

Dentro de `com.rojas.room`:

```
data/
  TaskEntity.kt      // DTO = @Entity (la tabla)
  TaskDao.kt         // DAO = queries
  AppDatabase.kt     // @Database
  TaskRepository.kt  // expone getAll, getById, save, updateById, deleteById
ui/
  TaskViewModel.kt   // o uno por pantalla: TaskListViewModel / TaskFormViewModel
  list/ListView.kt
  detail/DetailView.kt
  create/CreateView.kt
  update/UpdateView.kt
components/
  TaskCard.kt        // reutilizable, como componente React
  AppButton.kt       // variant = primary | secondary | danger
  ConfirmModal.kt    // diálogo de confirmación
navigation/
  Routes.kt          // rutas tipadas (tu "router")
  AppNavHost.kt      // NavHost central
```

`components/` es tu `src/components/` de React: piezas tontas, reusables, sin ViewModel dentro. Reciben datos + callbacks.

## 2. Paso 1 — Dependencia del router (único cambio de Gradle pendiente)

En `gradle/libs.versions.toml` agrega versión + librería (usa la estable que veas en la doc; a modo de ejemplo `2.9.x`):

```toml
[versions]
navigationCompose = "2.9.3"

[libraries]
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
```

En `app/build.gradle.kts`:

```kotlin
implementation(libs.androidx.navigation.compose)
```

Por qué: `NavController`/`NavHost` es el router oficial. `Sync Now` después.

## 3. Paso 2 — DTO: `data/TaskEntity.kt`

El DTO es tu "modelo de datos plano", como un `interface Task { id, title, done }` en TypeScript.

- Anótala `@Entity(tableName = "tasks")`.
- `@PrimaryKey(autoGenerate = true) val id: Long = 0`.
- Campos mínimos: `title: String`, `done: Boolean = false`, opcional `createdAt: Long = System.currentTimeMillis()`.
- Buena práctica: es `data class` inmutable (`val`). Para editar, se usa `copy()` — igual que el spread `{...task, title}` en React.

## 4. Paso 3 — DAO: `data/TaskDao.kt` (buena práctica Room)

Elegiste buena práctica Room: `@Update`/`@Delete` por entidad, `@Query` solo para leer. Así Room valida el SQL en compilación y aprovecha caché.

```kotlin
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAll(): Flow<List<TaskEntity>>   // reactivo: como una suscripción

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)
}
```

Notas:
- `getAll()` devuelve `Flow`: cada cambio en la tabla reemite la lista solo. No necesitas `refetch` como en React Query manual.
- Todo lo demás es `suspend`: se llama desde corrutina (`viewModelScope.launch`), nunca desde el hilo principal.
- ¿Y `updateById` / `deleteById`? No van en el DAO como `@Query` crudo: se exponen en el Repository/ViewModel (Paso 5), que primero resuelve la entidad por id y luego llama a `update`/`delete`. Mantienes el CRUD pedido (`getAll, getById, updateById, deleteById, save`) en la API pública, pero el SQL peligroso queda encapsulado.

## 5. Paso 4 — `data/AppDatabase.kt`

```kotlin
@Database(entities = [TaskEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}
```

- Singleton en una clase `Application` o en un `object`/contenedor simple (para no meter Hilt en un ejemplo simple). Igual que crear una sola instancia del cliente API en React.
- Si cambias el schema, sube `version` y define migración o `fallbackToDestructiveMigration()` solo en desarrollo.

## 6. Paso 5 — `data/TaskRepository.kt`: aquí vive tu CRUD pedido

El Repository es como tu capa `api/tasks.ts` en React: la única que habla con la fuente de datos.

```kotlin
class TaskRepository(private val dao: TaskDao) {
    fun getAll(): Flow<List<TaskEntity>> = dao.getAll()
    suspend fun getById(id: Long) = dao.getById(id)
    suspend fun save(title: String): Long =
        dao.save(TaskEntity(title = title))
    suspend fun updateById(id: Long, title: String, done: Boolean) {
        dao.getById(id)?.let { dao.update(it.copy(title = title, done = done)) }
    }
    suspend fun deleteById(id: Long) {
        dao.getById(id)?.let { dao.delete(it) }
    }
}
```

Por qué así: el ViewModel nunca ve SQL ni `copy()` sueltos por la UI; la UI solo llama `updateById/deleteById/save`.

## 7. Paso 6 — ViewModel: `ui/TaskViewModel.kt`

Equivale a tu custom hook `useTasks()`: expone estado + acciones.

- Lista: `val tasks: StateFlow<List<TaskEntity>> = repo.getAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())`.
- Detalle/form: `val selected: StateFlow<TaskEntity?>` que se carga con `getById(id)`.
- Acciones: `fun save(title)`, `fun updateById(...)`, `fun deleteById(id, onDone: () -> Unit)` dentro de `viewModelScope.launch { ... }`.
- Buena práctica: el ViewModel no recibe `NavController`. Navegar es decisión de la vista: el ViewModel avisa (callback / evento) y la vista navega. Igual que un hook no debe importar el router.

## 8. Paso 7 — Router: `navigation/Routes.kt` + `AppNavHost.kt`

Tu `react-router-dom`, pero tipado con Kotlinx Serialization o rutas string simples. Para ejemplo simple, rutas string:

```kotlin
object Routes {
    const val LIST = "list"
    const val CREATE = "create"
    const val DETAIL = "detail/{id}"      // useParams().id
    const val UPDATE = "update/{id}"
    fun detail(id: Long) = "detail/$id"
    fun update(id: Long) = "update/$id"
}
```

`AppNavHost.kt` (tu `<Routes>`):

```kotlin
@Composable
fun AppNavHost(nav: NavHostController, vm: TaskViewModel) {
    NavHost(navController = nav, startDestination = Routes.LIST) {
        composable(Routes.LIST) { ListView(onOpen = { nav.navigate(Routes.detail(it)) }, onCreate = { nav.navigate(Routes.CREATE) }) }
        composable(Routes.CREATE) { CreateView(onDone = { nav.popBackStack() }) }
        composable(Routes.DETAIL) { backStack ->
            val id = backStack.arguments?.getString("id")?.toLongOrNull() ?: return@composable
            DetailView(id = id, onEdit = { nav.navigate(Routes.update(id)) }, onDeleted = { nav.popBackStack() })
        }
        composable(Routes.UPDATE) { backStack ->
            val id = backStack.arguments?.getString("id")?.toLongOrNull() ?: return@composable
            UpdateView(id = id, onDone = { nav.popBackStack() })
        }
    }
}
```

Equivalencias: `nav.navigate(...)` ≈ `navigate(...)`, `nav.popBackStack()` ≈ `navigate(-1)`, `backStack.arguments` ≈ `useParams()`.

## 9. Paso 8 — `components/` (Card, Button, Modal)

### 9.1 `TaskCard.kt` — reutilizable

Props: `(task: TaskEntity, onClick: () -> Unit, onToggle: () -> Unit)`. Por dentro un `Card` + `Checkbox` + `Text`. Se usa en `ListView`. Igual que un `<TaskCard/>` presentacional: sin ViewModel dentro.

### 9.2 `AppButton.kt` — con `variant`

```kotlin
enum class ButtonVariant { PRIMARY, SECONDARY, DANGER }

@Composable
fun AppButton(text: String, variant: ButtonVariant = ButtonVariant.PRIMARY, onClick: () -> Unit)
```

Mapeo: `PRIMARY → Button`, `SECONDARY → OutlinedButton`, `DANGER → Button` con `containerColor = MaterialTheme.colorScheme.error`. Es tu `<Button variant="danger">` de React: misma API de props, distinto render.

### 9.3 `ConfirmModal.kt`

```kotlin
@Composable
fun ConfirmModal(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit)
```

Por dentro `AlertDialog` con `AppButton("Eliminar", DANGER)` + `AppButton("Cancelar", SECONDARY)`. Se muestra con `if (showConfirm)` — montaje condicional idéntico al modal de React.

## 10. Paso 9 — Las 4 vistas

### 10.1 `ListView` — `ui/list/ListView.kt`

- Observa `vm.tasks.collectAsStateWithLifecycle()`.
- `LazyColumn` de `TaskCard(task, onClick = { onOpen(task.id) })`.
- `FloatingActionButton` → `onCreate()`. Singles responsibility: solo lista y delega.

### 10.2 `CreateView` — `ui/create/CreateView.kt`

- Estado local del input: `var title by remember { mutableStateOf("") }` (tu `useState("")`).
- `AppButton("Guardar", PRIMARY)` → `vm.save(title); onDone()`. Validar `title.isNotBlank()` antes.

### 10.3 `DetailView` — `ui/detail/DetailView.kt` + modal de eliminar

- Carga con `LaunchedEffect(id) { vm.loadById(id) }` (tu `useEffect([id])`).
- Muestra título + estado + `AppButton("Editar", SECONDARY)` y `AppButton("Eliminar", DANGER)`.
- Eliminar NO borra directo: abre el modal:
  ```kotlin
  var showConfirm by remember { mutableStateOf(false) }
  AppButton("Eliminar", DANGER) { showConfirm = true }
  if (showConfirm) ConfirmModal(onConfirm = { vm.deleteById(id); onDeleted() }, onDismiss = { showConfirm = false })
  ```
  Este es el requisito del modal de confirmación.

### 10.4 `UpdateView` — `ui/update/UpdateView.kt`

- Precarga con `getById(id)`, edita en estado local, guarda con `vm.updateById(id, title, done)`.
- `Checkbox` para `done` + `TextField` para título + `AppButton("Actualizar", PRIMARY)`.

## 11. Paso 10 — `MainActivity.kt`: cableado mínimo

1. Crea el `NavHostController` con `rememberNavController()`.
2. Obtén el ViewModel con `viewModel()` (con un `ViewModelProvider.Factory` simple que inyecte el Repository; en ejemplo simple basta una fábrica manual, sin Hilt).
3. Llama a `AppNavHost(nav, vm)` dentro de tu `RoomTheme`.

Nada de lógica de negocio en la Activity: es tu `App.tsx` que solo monta el router.

## 12. Checklist de buenas prácticas (criterio de corrección)

- [ ] UI sin SQL ni `dao` importado fuera de `data/`.
- [ ] DAO con `Flow` + `suspend`; sin llamadas bloqueantes en el hilo principal.
- [ ] CRUD público (`getAll, getById, save, updateById, deleteById`) en Repository/ViewModel.
- [ ] `components/` sin ViewModel: solo props + callbacks.
- [ ] `AppButton` con `variant` y `TaskCard` reused al menos en `ListView`.
- [ ] `DetailView` elimina solo vía `ConfirmModal`.
- [ ] Navegación solo en capa `navigation/` + vistas; ViewModel sin `NavController`.
- [ ] Probar en API 37: crear → listar → detalle → editar → eliminar con modal → rotar pantalla sin perder estado.

## 13. Orden de implementación sugerido (1 sesión)

1. Entity → DAO → Database → Repository (compila sin UI).
2. ViewModel (prueba `getAll/save` con Logs o test simple).
3. `Routes` + `AppNavHost` con pantallas vacías (`Text("List")`, etc.) y navega entre ellas.
4. `components/` (Button, Card, Modal).
5. Las 4 vistas en orden: List → Create → Detail (+modal) → Update.
6. Cableado final en `MainActivity` + pasada del checklist en API 37.
