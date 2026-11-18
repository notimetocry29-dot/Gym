package app.gymtracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.net.Uri
import app.gymtracker.ui.common.AppCard
import app.gymtracker.ui.common.AppScaffold
import app.gymtracker.ui.common.ConfirmDialog
import app.gymtracker.ui.common.LocalRepo
import app.gymtracker.ui.common.SectionTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val repo = LocalRepo.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            message = try {
                val text = repo.exportJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray()) }
                }
                "Backup saved."
            } catch (e: Exception) {
                "Export failed: ${e.message}"
            }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingImport = uri
    }

    AppScaffold(title = "Settings & backup", onBack = onBack) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("Backup")
            AppCard {
                Text(
                    "Everything is stored on this phone. Export a backup file now and then (for example to Google Drive) " +
                        "so you never lose your history, and to move your data to a new phone.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = { exporter.launch("gym-backup-${LocalDate.now()}.json") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Export backup")
                }
                OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Restore from backup")
                }
                message?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                }
            }
            SectionTitle("How progression works")
            AppCard {
                Text(
                    "Each exercise in a workout day has a target: sets × a rep range (for example 3 × 8–12). " +
                        "When you hit the top of the range on every set, next time the app suggests adding weight: " +
                        "+2.5 kg for upper-body lifts and +5 kg for lower-body lifts. You can change the step per exercise " +
                        "from its edit screen. Otherwise it suggests the same weight so you can add reps.",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    pendingImport?.let { uri ->
        ConfirmDialog(
            "Restore backup?",
            "This replaces ALL current data in the app (exercises, programs and history) with the backup file.",
            confirm = "Restore", destructive = true,
            onDismiss = { pendingImport = null },
        ) {
            scope.launch {
                message = try {
                    val text = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                    }
                    val b = repo.importJson(text)
                    "Restored ${b.sessions.size} workouts and ${b.plans.size} programs."
                } catch (e: Exception) {
                    "That file isn't a valid backup (${e.message?.take(80)})."
                }
            }
        }
    }
}
