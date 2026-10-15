# Room TodoList — CRUD con MVVM + Room + Navigation Compose

App de ejemplo: lista de tareas con crear, ver detalle, editar y eliminar (con modal de confirmación).
Docs relacionadas: `INITIAL.md` (configuración inicial) y `GUIA_CRUD.md` (guía paso a paso para devs que vienen de React).

## 1. Cómo se mueven los datos

La regla es una sola: **Room es la fuente de la verdad**. La UI nunca toca la DB; observa estado y dispara eventos.

```mermaid
flowchart LR
    subgraph UI["Vistas (Compose)"]
        LV["ListView"]
        CV["CreateView"]
        DV["DetailView"]
        UV["UpdateView"]
    end
    VM["TaskViewModel\nStateFlow + viewModelScope"]
    REPO["TaskRepository\ngetAll · getById · save · updateById · deleteById"]
    DAO["TaskDao\n@Query Flow/suspend · @Insert · @Update · @Delete"]
    DB[("Room SQLite\ntasks")]

    LV <-->|tasks: StateFlow<br>toggleDone| VM
    CV -->|save| VM
    DV -->|loadById · deleteById| VM
    UV -->|loadById · updateById| VM
    VM <-->|Flow / suspend| REPO
    REPO <-->|Entity| DAO
    DAO <-->|SQL| DB
```

### Lectura reactiva (la lista se actualiza sola)

```mermaid
sequenceDiagram
    participant LV as ListView
    participant VM as TaskViewModel
    participant R as TaskRepository
    participant DAO as TaskDao
    participant DB as Room

    LV->>VM: collectAsStateWithLifecycle(tasks)
    VM->>R: getAll()
    R->>DAO: getAll()
    DAO->>DB: SELECT * FROM tasks ...
    DB-->>DAO: Flow<List<Task>>
    DAO-->>R: Flow<List<Task>>
    R-->>VM: stateIn(...) → StateFlow
    VM-->>LV: recomposición con la lista
    Note over LV,DB: Cualquier INSERT/UPDATE/DELETE<br>reemite el Flow y la lista se redibuja sola.<br>No hay refetch manual.
```

### Escritura (crear / editar / eliminar)

```mermaid
sequenceDiagram
    participant V as Vista (Create/Update/Detail)
    participant VM as TaskViewModel
    participant R as TaskRepository
    participant DAO as TaskDao
    participant DB as Room

    V->>VM: save(title) / updateById(id,...) / deleteById(id)
    Note over VM: viewModelScope.launch { ... }
    VM->>R: save / updateById / deleteById
    R->>DAO: getById(id) + save/update/delete(Entity)
    DAO->>DB: INSERT / UPDATE / DELETE
    DB-->>DAO: ok
    DAO-->>R: ok
    R-->>VM: ok
    VM-->>V: onDone() → la vista navega atrás
    Note over V,DB: El cambio en la tabla dispara el Flow<br>de lectura y ListView se actualiza sola.
```

## 2. Mapa de navegación (el "router")

```mermaid
flowchart TD
    LIST["list\nListView"]
    CREATE["create\nCreateView"]
    DETAIL["detail/{id}\nDetailView"]
    UPDATE["update/{id}\nUpdateView"]

    LIST -->|FAB +| CREATE
    LIST -->|click en TaskCard| DETAIL
    DETAIL -->|Editar| UPDATE
    DETAIL -->|Eliminar + ConfirmModal| LIST
    CREATE -->|Guardar → popBackStack| LIST
    UPDATE -->|Actualizar → popBackStack| LIST
```

Definido en `navigation/Routes.kt` y `navigation/AppNavHost.kt` con `NavController` + `NavHost`:
`navigate()` ≈ ir, `popBackStack()` ≈ volver. Por las rutas solo viaja el `id`; la entidad siempre se recarga desde Room.

## 3. Directorios del proyecto

```mermaid
flowchart TD
    ROOT["com.rojas.room"]
    ROOT --> DATA["data/\nDTO + DAO + DB + Repository"]
    ROOT --> UI["ui/\nViewModel + 4 vistas"]
    ROOT --> COMP["components/\npiezas reusables sin ViewModel"]
    ROOT --> NAV["navigation/\nRoutes + AppNavHost"]
    ROOT --> MAIN["MainActivity.kt\nsolo monta el router"]
    ROOT --> APP["TaskApplication.kt\nsingleton de la DB"]

    DATA --> E["TaskEntity.kt · TaskDao.kt<br>AppDatabase.kt · TaskRepository.kt"]
    UI --> V["TaskViewModel.kt<br>list/ · create/ · detail/ · update/"]
    COMP --> C["TaskCard.kt · AppButton.kt<br>variant=primary|secondary|danger<br>ConfirmModal.kt"]
    NAV --> N["Routes.kt · AppNavHost.kt"]
```

Árbol real:

```
app/src/main/java/com/rojas/room/
├── MainActivity.kt          # solo monta RoomTheme + AppNavHost (como App.tsx)
├── TaskApplication.kt        # singleton Room (una sola instancia de la DB)
├── data/                    # MODEL — lo único que habla SQL
│   ├── TaskEntity.kt        # DTO: @Entity, fila de la tabla tasks
│   ├── TaskDao.kt           # DAO: @Query Flow/suspend, @Insert, @Update, @Delete
│   ├── AppDatabase.kt       # @Database, version = 1
│   └── TaskRepository.kt    # CRUD público: getAll, getById, save, updateById, deleteById
├── ui/                      # VIEWMODEL + VISTAS
│   ├── TaskViewModel.kt     # estado (tasks, selected) + acciones, sin NavController
│   ├── list/ListView.kt     # LazyColumn de TaskCard + FAB
│   ├── create/CreateView.kt # input + Guardar
│   ├── detail/DetailView.kt # muestra + Editar / Eliminar con ConfirmModal
│   └── update/UpdateView.kt # precarga por id + Actualizar
├── components/              # presentacionales: solo props + callbacks
│   ├── TaskCard.kt
│   ├── AppButton.kt
│   └── ConfirmModal.kt
└── navigation/              # router oficial
    ├── Routes.kt
    └── AppNavHost.kt
```

## 4. Cómo persiste la data entre las vistas

Hay 3 mecanismos, de más a menos importante:

### 4.1 Room es la persistencia real

Todo vive en SQLite (`tasks.db`). Moverse entre vistas no mueve datos: cada vista pide lo suyo a la DB.
`DetailView` y `UpdateView` reciben solo el `id` por la ruta (`detail/{id}`) y cargan la entidad con `vm.loadById(id)` → `repo.getById(id)` → `SELECT ... WHERE id = :id`.
Si editas en `UpdateView` y vuelves, `DetailView` muestra lo nuevo porque relee de la DB, no porque le hayan pasado el objeto.

### 4.2 Un solo ViewModel compartido

`MainActivity` crea **una** instancia de `TaskViewModel` (scope de la Activity) y se la pasa a `AppNavHost`, que la reparte a las 4 vistas.
Resultado: `tasks: StateFlow` y `selected: StateFlow` son los mismos en todas las pantallas. La lista observada con `collectAsStateWithLifecycle()` sobrevive a la navegación y a la **rotación** (el ViewModel no se destruye con el cambio de configuración).

### 4.3 Estado local solo para lo efímero

El texto del `TextField`, el `Checkbox` de edición o el booleano que abre el modal (`showConfirm`) viven en `remember { mutableStateOf() }` dentro de cada vista.
Se pierden al salir de la pantalla, y eso está bien: son estado de UI, no datos. Lo que debe sobrevivir está en Room + ViewModel.

```mermaid
flowchart TD
    subgraph Sobrevive["Sobrevive a navegación y rotación"]
        ROOM[("Room SQLite")]
        VM["TaskViewModel\ntasks / selected"]
    end
    subgraph Efimero["Muere al salir de la pantalla"]
        TF["TextField title"]
        CB["Checkbox done"]
        MO["showConfirm del modal"]
    end
    DETAIL["DetailView / UpdateView"]
    DETAIL -->|loadById id| VM
    VM -->|getById| ROOM
    ROOM -->|Entity| VM
    VM -->|selected| DETAIL
    DETAIL -.->|remember| TF
    DETAIL -.->|remember| CB
    DETAIL -.->|remember| MO
```

> Límite honesto: si el sistema mata el proceso en segundo plano, el `TextField` a medio escribir se pierde (no usamos `SavedStateHandle`). Los datos guardados en Room están a salvo; es el comportamiento esperado para este ejemplo simple.
