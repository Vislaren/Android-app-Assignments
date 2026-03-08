import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import model.Student
import logic.*
import ui.AppShell

// ─────────────────────────────────────────────────────────────────
//  Application Entry Point
// ─────────────────────────────────────────────────────────────────

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Student Grade Calculator",
        state = rememberWindowState(width = 1100.dp, height = 750.dp)
    ) {
        AppShell()
    }
}