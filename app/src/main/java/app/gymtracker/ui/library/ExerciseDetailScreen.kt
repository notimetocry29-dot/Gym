package app.gymtracker.ui.library

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gymtracker.data.fmtKg
import app.gymtracker.ui.common.AppCard
import app.gymtracker.ui.common.AppFilterChip
import app.gymtracker.ui.common.AppScaffold
import app.gymtracker.ui.common.ConfirmDialog
import app.gymtracker.ui.common.LineChart
import app.gymtracker.ui.common.LocalRepo
import app.gymtracker.ui.common.Pill
import app.gymtracker.ui.common.SectionTitle
import app.gymtracker.ui.common.fmtDayYear
import app.gymtracker.ui.progress.Metric
import app.gymtracker.ui.progress.PersonalRecords
import app.gymtracker.ui.progress.chartPoints
import app.gymtracker.ui.progress.sessionPerfs
import kotlinx.coroutines.launch

@Composable
fun ExerciseDetailScreen(exerciseId: Long, onBack: () -> Unit, openSession: (Long) -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val exercise by remember { repo.dao.observeExercise(exerciseId) }.collectAsStateWithLifecycle(null)
    val rows by remember { repo.dao.observeExerciseSets(exerciseId) }.collectAsStateWithLifecycle(emptyList())
    val perfs = remember(rows) { sessionPerfs(rows) }
    var metric by remember { mutableStateOf(Metric.E1RM) }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    val ex = exercise
    AppScaffold(
        title = ex?.name ?: "",
        onBack = onBack,
        actions = {
            IconButton(onClick = { editing = true }) { Icon(Icons.Filled.Edit, "Edit") }
            if (ex?.isCustom == true) IconButton(onClick = { deleting = true }) { Icon(Icons.Filled.Delete, "Delete") }
        },
    ) { padding ->
        if (ex == null) return@AppScaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(ex.primaryMuscle, MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                    Pill(ex.equipment)
                    Pill("+${fmtKg(ex.increment)} kg step")
                }
                if (ex.secondaryMuscles.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Also works: ${ex.secondaryMuscles}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (ex.instructions.isNotBlank()) {
                item { SectionTitle("How to") }
                item {
                    AppCard {
                        ex.instructions.lines().filter { it.isNotBlank() }.forEachIndexed { i, line ->
                            Row(Modifier.padding(vertical = 4.dp)) {
                                Text("${i + 1}.", color = MaterialTheme.colorScheme.primary, modifier = Modifier.width(22.dp))
                                Text(line, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
            item { SectionTitle("Progress") }
            if (perfs.isEmpty()) {
                item { Text("You haven't logged this exercise yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                item { PersonalRecords(perfs) }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Metric.entries.forEach { m -> AppFilterChip(m.label, metric == m) { metric = m } }
                    }
                }
                item { AppCard { LineChart(chartPoints(perfs, metric)) } }
                item { SectionTitle("History") }
                items(perfs.reversed(), key = { it.sessionId }) { p ->
                    AppCard(onClick = { openSession(p.sessionId) }) {
                        Text(fmtDayYear(p.date), style = MaterialTheme.typography.titleMedium)
                        Text(
                            p.sets.joinToString("  ·  ") { "${fmtKg(it.first)}×${it.second}" },
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (editing && ex != null) {
        ExerciseEditDialog(initial = ex, onDismiss = { editing = false }) { e -> scope.launch { repo.dao.upsertExercise(e) } }
    }
    if (deleting && ex != null) {
        ConfirmDialog(
            "Delete ${ex.name}?",
            "This also removes it from your workout days and deletes its logged history.",
            confirm = "Delete", destructive = true, onDismiss = { deleting = false },
        ) { scope.launch { repo.dao.deleteExercise(ex); onBack() } }
    }
}

