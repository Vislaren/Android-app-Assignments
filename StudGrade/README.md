# Student Grade Calculator
### Kotlin · Compose for Desktop · Material 3

A full-featured desktop application for calculating and managing student grades, built with idiomatic Kotlin and Jetpack Compose for Desktop.

---

## 🗂 Project Structure

```
src/main/kotlin/
├── Main.kt                          ← Entry point + console HOF demo
├── model/
│   └── Student.kt                   ← Data class with calculateTotal() & asCsvRow()
├── logic/
│   ├── GradingLogic.kt              ← processStudents(), buildGradingLogic(), filterDistinction()
│   └── ExcelParser.kt               ← Apache POI .xlsx reader
└── ui/
    ├── AppTheme.kt                  ← Material 3 color schemes (light/dark)
    ├── AppShell.kt                  ← Scaffold + NavigationRail + shared state
    ├── components/
    │   └── Components.kt            ← StudentCard, SearchBar, StatsRow, GradeBadge
    └── views/
        ├── HomeView.kt              ← File import, process, preview, export
        ├── VaultView.kt             ← Session storage with search & delete
        └── SettingsView.kt          ← Configurable grading ranges
```

---

## 🚀 Getting Started

### Prerequisites
- JDK 17+
- Gradle 8+

### Run
```bash
./gradlew run
```

### Package (native installer)
```bash
./gradlew packageDmg       # macOS
./gradlew packageMsi       # Windows
./gradlew packageDeb       # Linux
```

---

## 📋 CSV Format

The app expects CSV files with these columns (header row optional):

```
Name, ID, CA Score, Exam Score
Alice Johnson, STU001, 28, 65
Bob Smith, STU002, 22.5, 58
```

**Validation rules:**
- CA Score: 0 – 30
- Exam Score: 0 – 70
- Final Score = CA + Exam (max 100)

---

## 🏗 Architecture Highlights

### `Student` Data Class
```kotlin
data class Student(
    val name: String, val id: String,
    val caScore: Double, val examScore: Double,
    val finalScore: Double = 0.0, val grade: String = ""
) {
    fun calculateTotal(): Student  // validates & sums scores
    fun asCsvRow(): String         // comma-separated export row
}
```

### Higher-Order Function
```kotlin
fun processStudents(
    list: List<Student>,
    gradingLogic: (Double) -> String   // lambda injected from Settings
): List<Student>
```

### State Management
- `remember { mutableStateListOf() }` — Vault storage
- `derivedStateOf { }` — search filtering, grading lambda
- All grading logic flows from `SettingsView` → `AppShell` → `HomeView`

---

## 🎨 Design

| Role | Color |
|------|-------|
| Primary | Green `#2E7D32` |
| Secondary | Blue `#1565C0` |
| Accent (Delete/Calculate) | Orange `#E65100` |
| Background (Light) | `#F8FFF8` |

---

## 📦 Dependencies

| Library | Purpose |
|---------|---------|
| `compose.desktop.currentOs` | UI framework |
| `compose.material3` | Design system |
| `compose.materialIconsExtended` | Icon set |
| `kotlinx-serialization-json` | JSON/vault simulation |
| `apache-poi` + `poi-ooxml` | Excel file parsing |
| `kotlinx-coroutines-swing` | Async file I/O |
