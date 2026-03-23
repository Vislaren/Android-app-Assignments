# GradeMaster 📊

**GradeMaster** is a production-grade Android application built with **Kotlin + Jetpack Compose**
for processing, grading, and managing student score data from CSV, Excel, and Google Sheets.

---

## Architecture Overview

```
GradeMaster/
├── app/src/main/java/com/grademaster/
│   ├── data/
│   │   ├── local/                  # Room DB + DataStore
│   │   │   ├── GradeMasterDatabase.kt    (Room DB, StudentDao, VaultSessionDao)
│   │   │   └── AppPreferences.kt         (DataStore: theme, grade ranges, settings)
│   │   ├── model/                  # Data models & sealed classes
│   │   │   └── Student.kt                (Student, GradeRange, VaultSession, enums)
│   │   └── repository/             # Centralized data access
│   │       └── StudentRepository.kt
│   ├── di/
│   │   └── AppModule.kt            # Hilt dependency injection
│   ├── ui/
│   │   ├── components/             # Reusable Compose components
│   │   │   ├── StudentCard.kt            (Card, Avatar, GradeBadge, EditDialog)
│   │   │   └── ShimmerEffect.kt          (Shimmer, ShimmerList, VaultShimmer)
│   │   ├── navigation/
│   │   │   ├── NavItem.kt                (Sealed class: Home, Vault, Settings)
│   │   │   └── GradeMasterNavHost.kt     (NavHost + persistent BottomBar)
│   │   ├── screens/
│   │   │   ├── home/
│   │   │   │   ├── HomeViewModel.kt
│   │   │   │   └── HomeScreen.kt
│   │   │   ├── vault/
│   │   │   │   ├── VaultViewModel.kt
│   │   │   │   └── VaultScreen.kt
│   │   │   └── settings/
│   │   │       ├── SettingsViewModel.kt
│   │   │       └── SettingsScreen.kt
│   │   └── theme/
│   │       ├── Theme.kt                  (Light/Dark color schemes, GradeMasterColors)
│   │       └── TypographyShapes.kt
│   ├── util/
│   │   ├── GradeCalculator.kt      (Pure grade logic + ClassStats HOF)
│   │   ├── StudentTransformer.kt   (MANDATORY custom higher-order function)
│   │   ├── FileProcessor.kt        (XLSX, CSV, Google Sheets parser)
│   │   └── ExportEngine.kt         (CSV, Excel, PDF export + Share Intent)
│   ├── GradeMasterApplication.kt   (@HiltAndroidApp)
│   └── MainActivity.kt
└── app/src/test/java/com/grademaster/
    └── GradeMasterTest.kt          (Unit tests + main() demo)
```

---

## Technology Stack

| Layer              | Technology                                    |
|--------------------|-----------------------------------------------|
| Language           | Kotlin 2.1                                    |
| UI                 | Jetpack Compose + Material3                   |
| Architecture       | MVVM + Repository pattern                     |
| DI                 | Hilt (Dagger)                                 |
| Database           | Room (SQLite)                                 |
| Settings           | DataStore Preferences                         |
| Navigation         | Compose Navigation                            |
| File I/O (XLSX)    | Apache POI 5.3                                |
| File I/O (CSV)     | OpenCSV 5.9                                   |
| PDF Export         | iText 8                                       |
| Cloud              | Google Drive API v3                           |
| Async              | Kotlin Coroutines + Flow                      |
| Testing            | JUnit 4 + MockK + Coroutines Test             |

---

## Feature Details

### 1 · Home Screen
- **File Import** — pick `.csv` or `.xlsx` via Android file picker
- **Google Sheets** — paste a public spreadsheet URL; auto-converted to CSV export
- **Auto Column Detection** — identifies CA, Exam, Name columns by alias matching
- **Live Grade Calculation** — grades assigned from Settings ranges on import
- **Searchable List** — real-time filter by student name
- **Grade Filter Chips** — A / B / C / D / F / All
- **Stats Row** — Total · Passed · Failed · Average
- **Student Card** — expandable; click to reveal Edit & Delete icons
- **Edit Dialog** — modify CA + Exam only; total & grade auto-recalculate
- **Shimmer Loading** — animated skeleton while file is being processed
- **Theme Toggle** — Light/Dark switch at top of screen (requirement met)
- **Save to Vault** — persists current session to Room database
- **Export Sheet** — export as CSV, Excel, or PDF to device / Drive / Share

### 2 · Vault Screen
- **Session List** — all saved sessions with date, student count, average, pass rate
- **Session Detail** — drill into a session to see its full student list
- **Search** — filter sessions by title; filter students by name within a session
- **Delete** — remove sessions (cascades to students) or individual students
- **Export** — same export options as Home, scoped to the selected session
- **State Preserved** — navigating away and back does not reset the view

### 3 · Settings Screen
- **Grade Ranges** — tap any grade row to edit its min/max score; changes sync immediately
- **Reset** — restore default ranges (A≥70, B≥60, C≥50, D≥40, F<40)
- **Pass Mark Slider** — set the threshold for pass/fail statistics
- **Default Export Format** — CSV / Excel / PDF chip picker
- **Google Drive Toggle** — enable Drive upload destination
- **Theme Toggle** — mirrors Home screen toggle; persisted via DataStore

---

## Data Model

```kotlin
@Entity(tableName = "students")
data class Student(
    val id: Long,
    val sessionId: Long,       // FK → VaultSession
    val name: String,
    val caScore: Double,       // 0–30
    val examScore: Double,     // 0–70
    val finalScore: Double,    // caScore + examScore
    val grade: String          // A / B / C / D / F
) {
    fun validate(): ValidationResult       // OOP: encapsulated validation
    fun withGrade(ranges): Student         // immutable grade reassignment
}
```

---

## OOP & Functional Programming

### Custom Higher-Order Function (MANDATORY REQUIREMENT)
```kotlin
// StudentTransformer.kt
fun <T> applyTransformation(
    students: List<Student>,
    transformer: (Student) -> T   // ← accepts a lambda
): List<T> = students.map(transformer)

// Call site — lambda passed in:
val summaries = StudentTransformer.applyTransformation(students) { s ->
    "[${s.grade}] ${s.name} — ${s.finalScore}/100"
}
```

### Collection Operations
```kotlin
// Extension functions on List<Student>
fun List<Student>.passed(passMark: Double = 40.0)  = filter { it.finalScore >= passMark }
fun List<Student>.failed(passMark: Double = 40.0)  = filter { it.finalScore < passMark }
fun List<Student>.byGrade(grade: Grade)             = filter { it.grade == grade.label }
fun List<Student>.topN(n: Int)                      = sortedByDescending { it.finalScore }.take(n)
fun List<Student>.classAverage()                    = sumOf { it.finalScore } / size
fun <T> List<Student>.transformWith(t: (Student)->T) = map(t)   // HOF extension
```

---

## Unit Tests (Requirement 4)

`GradeMasterTest.kt` covers:

| Test Category        | Tests                                              |
|----------------------|----------------------------------------------------|
| **HOF λ**            | 5 tests — different lambdas passed to `applyTransformation` |
| **Collection filter**| passed / failed / byGrade / searchBy / topN / chained pipeline |
| **Collection map**   | extracting scores, name-grade pairs                |
| **groupBy**          | grade distribution map                             |
| **OOP Validation**   | valid student, CA>30, Exam>70, blank name          |
| **GradeCalculator**  | all default grades, custom ranges, class stats     |
| **Immutability**     | withGrade leaves original unchanged                |

### Run Tests
```bash
./gradlew :app:test
```

### Run main() Demo
```bash
# Compile the test file with the model and util classes, then run:
./gradlew :app:testDebugUnitTest --tests "com.grademaster.GradeMasterTest.main"
# Or call GradeMasterTest.main(arrayOf()) from any JVM entry point
```

---

## Export Engine

| Format   | Library     | Contents                                      |
|----------|-------------|-----------------------------------------------|
| CSV      | Built-in    | Header + rows + summary footer                |
| Excel    | Apache POI  | Students sheet + colour header + Summary sheet|
| PDF      | iText 8     | Title, stats row, colour-coded student table  |

**Destinations:** Internal Storage · Google Drive (OAuth) · System Share Intent

---

## Grade Ranges (Default)

| Grade | Range        | Colour  |
|-------|--------------|---------|
| A     | 70 – 100     | Green   |
| B     | 60 – 69.99   | Blue    |
| C     | 50 – 59.99   | Orange  |
| D     | 40 – 49.99   | Red     |
| F     | 0  – 39.99   | Dark Red|

Ranges are fully editable in Settings and stored in DataStore.
Every grade in the app recalculates automatically when ranges change.

---

## Theme

| Token            | Light                  | Dark                  |
|------------------|------------------------|-----------------------|
| Primary          | Green (#2E7D32)        | Green 80 (#81C784)    |
| Secondary        | Blue (#1565C0)         | Blue 80 (#90CAF9)     |
| Tertiary/Accent  | Thick Orange (#FF6D00) | Orange Light          |
| Background       | Off-White              | #121412               |
| Surface          | White                  | #1A1C1A               |

---

## Setup

1. Clone the repository
2. Open in Android Studio Ladybug (2024.2+) or newer
3. Sync Gradle — all dependencies resolve via Maven Central + Google
4. Run on device / emulator with API 26+
5. For Google Sheets import: ensure the sheet is publicly accessible ("Anyone with the link")
6. For Google Drive upload: configure OAuth client ID in `google-services.json`
