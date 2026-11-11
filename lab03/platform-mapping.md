# Lab-03 Challenge: Cross-Platform MVVM Mapping

## 1. Mapping JavaFX MVVM to Android Jetpack & WPF

| `ReportViewModel` Member / Concept | JavaFX 25 Implementation | Android Jetpack Architecture | .NET / WPF Architecture |
| --- | --- | --- | --- |
| **ViewModel Base** | Plain Java Class (`ReportViewModel`) | `androidx.lifecycle.ViewModel` | `INotifyPropertyChanged` / `ObservableObject` |
| **Selected Month** | `ObjectProperty<YearMonth>` | `MutableStateFlow<YearMonth?>` | `YearMonth` property with `RaisePropertyChanged` |
| **Branch Checkbox Flags** | `Map<String, BooleanProperty>` | `StateFlow<Set<String>>` (active branch set) | `Dictionary<string, bool>` or `bool` properties |
| **Product Filter** | `StringProperty` | `MutableStateFlow<String>` | `string` property with two-way XAML binding |
| **Branch Summaries** | `ObservableList<BranchSummary>` | `StateFlow<List<BranchSummary>>` | `ObservableCollection<BranchSummary>` |
| **Visible Products** | `FilteredList<ProductTotal>` | Derived `StateFlow` via `combine(products, filter)` | `ICollectionView` with `Filter` predicate |
| **Loading State** | `ReadOnlyBooleanProperty` | `StateFlow<Boolean>` | `bool IsLoading` with `RaisePropertyChanged` |
| **Progress Fraction** | `ReadOnlyDoubleProperty` | `StateFlow<Float>` (0.0f - 1.0f) | `double Progress` bound to `ProgressBar.Value` |
| **Validation Message** | `StringBinding` | Derived `StateFlow<String>` via `combine(...)` | `string ErrorMessage` / `IDataErrorInfo` |
| **Total Revenue Text** | `StringBinding` | Derived `StateFlow<String>` | Formatted `string` property or XAML `IValueConverter` |
| **Generate / Cancel** | `Command` (`DelegateCommand`) | UI Events / Methods (`generate()`, `cancel()`) | `ICommand` (`RelayCommand` / `AsyncRelayCommand`) |
| **Background Threading** | `BackgroundJobs` (`FxBackgroundJobs`) | `viewModelScope.launch(Dispatchers.IO)` | `async` / `await` (`Task.Run` on threadpool) |

---

## 2. Key Differences and Non-Transferable Elements

1. **Unidirectional Data Flow vs Bidirectional Binding**: While JavaFX supports automatic two-way bindings between controls and properties (`bindBidirectional`), modern Android Jetpack Compose adopts Unidirectional Data Flow (UDF) where UI controls never mutate ViewModel properties directly; instead, user interactions emit events (e.g., `onMonthSelected(YearMonth)`) and the ViewModel emits updated immutable state.
2. **State Atomicity**: JavaFX uses discrete observable property wrappers for each individual field, whereas Jetpack applications typically consolidate screen state into a single immutable data class (e.g., `data class ReportUiState(...)`) published through a single `StateFlow`.
3. **Lifecycle and Configuration Survival**: Android `ViewModel` instances are scoped to Activity/Fragment lifecycles and automatically survive configuration changes such as device rotation or screen resizing, whereas JavaFX ViewModels remain in memory only as long as the parent application or stage references them.
4. **Structured Concurrency**: In Android, coroutine cancellations propagate automatically when `viewModelScope` is cleared upon screen departure, avoiding orphaned background tasks without manual `JobHandle.cancel()` book-keeping.
5. **Collection Mutations**: WPF uses `ObservableCollection<T>` which fires granular `CollectionChanged` events for UI updates similar to JavaFX, while Jetpack Compose recomposes UI subtrees whenever the reference to an immutable list within `StateFlow` is updated.
