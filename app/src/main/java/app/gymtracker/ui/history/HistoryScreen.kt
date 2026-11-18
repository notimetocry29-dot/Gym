package app.gymtracker.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gymtracker.data.fmtKg
import app.gymtracker.ui.common.AppCard
import app.gymtracker.ui.common.AppScaffold
import app.gymtracker.ui.common.ConfirmDialog
import app.gymtracker.ui.common.EmptyState
import app.gymtracker.ui.common.LocalRepo
import app.gymtracker.ui.common.SectionTitle
import app.gymtracker.ui.common.StatBox
import app.gymtracker.ui.common.TextInputDialog
import app.gymtracker.ui.common.fmtDay
import app.gymtracker.ui.common.fmtDayYear
import app.gymtracker.ui.common.fmtDuration
import app.gymtracker.ui.common.fmtMonth
import app.gymtracker.ui.common.fmtVolume
import app.gymtracker.ui.common.toLocalDate
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun HistoryScreen(openSession: (Long) -> Unit) {
    val dao = LocalRepo.current.dao
    val history by remember { dao.observeHistory() }.collectAsStateWithLifecycle(emptyList())
    val thisWeek = remember(history) {
        val monday = LocalDate.now().minusDays((LocalDate.now().dayOfWeek.value - 1).toLong())
        history.count { !it.session.startedAt.toLocalDate().isBefore(monday) }
    }
    val last30 = remember(history) {
        val from = LocalDate.now().minus(30, ChronoUnit.DAYS)
        history.count { it.session.startedAt.toLocalDate().isAfter(from) }
    }

    AppScaffold(title = "History", subtitle = "${history.size} workouts") { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (history.isEmpty()) {
                item { EmptyState("No workouts yet", "Finished workouts show up here with every set you logged.") }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatBox("This week", "$thisWeek", Modifier.weight(1f))
                        StatBox("Last 30 days", "$last30", Modifier.weight(1f))
                        StatBox("All time", "${history.size}", Modifier.weight(1f))
                    }
                }
            }
            history.groupBy { fmtMonth(it.session.startedAt.toLocalDate()) }.forEach { (month, list) ->
                item(key = "m-$month") { SectionTitle(month) }
                items(list, key = { it.session.id }) { h ->
                    val s = h.session
                    AppCard(onClick = { openSession(s.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text(fmtDay(s.startedAt.toLocalDate()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            listOfNotNull(
                                s.finishedAt?.let { fmtDuration(it - s.startedAt) },
                                "${h.exerciseCount} exercises",
                                "${h.setCount} sets",
                                fmtVolume(h.volume),
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionDetailScreen(sessionId: Long, onBack: () -> Unit, openWorkout: (Long) -> Unit, openExercise: (Long) -> Unit) {
    val repo = LocalRepo.current
    val dao = repo.dao
    val scope = rememberCoroutineScope()
    val session by remember { dao.observeSession(sessionId) }.collectAsStateWithLifecycle(null)
    val rows by remember { dao.observeSessionExercises(sessionId) }.collectAsStateWithLifecycle(emptyList())
    val sets by remember { dao.observeSessionSets(sessionId) }.collectAsStateWithLifecycle(emptyList())
    val bySe = remember(sets) { sets.groupBy { it.sessionExerciseId } }
    var menu by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val s = session
    AppScaffold(
        title = s?.name ?: "",
        subtitle = s?.let { fmtDayYear(it.startedAt.toLocalDate()) },
        onBack = onBack,
        actions = {
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; rename = true })
                    DropdownMenuItem(
                        text = { Text("Edit sets") },
                        onClick = {
                            menu = false
                            scope.launch {
                                // Re-open the workout for editing; finishing it again saves the changes.
                                if (dao.getOpenSession() != null) busy = true
                                else s?.let { dao.updateSession(it.copy(finishedAt = null, phaseId = null, rotationPos = null)); openWorkout(it.id) }
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete workout", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; delete = true },
                    )
                }
            }
        },
    ) { padding ->
        if (s == null) return@AppScaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                val done = sets.filter { it.done }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox("Duration", s.finishedAt?.let { fmtDuration(it - s.startedAt) } ?: "In progress", Modifier.weight(1f))
                    StatBox("Sets", "${done.size}", Modifier.weight(1f))
                    StatBox("Volume", fmtVolume(done.sumOf { (it.weight ?: 0.0) * (it.reps ?: 0) }), Modifier.weight(1f))
                }
            }
            items(rows, key = { it.item.id }) { r ->
                AppCard {
                    Text(
                        r.exerciseName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { openExercise(r.item.exerciseId) },
                    )
                    Spacer(Modifier.height(4.dp))
                    bySe[r.item.id].orEmpty().forEachIndexed { i, st ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text("${i + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(24.dp))
                            Text("${st.weight?.let { fmtKg(it) } ?: "—"} kg × ${st.reps ?: "—"}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                    if (r.item.notes.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(r.item.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (s.notes.isNotBlank()) item {
                AppCard {
                    Text("Notes", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(s.notes, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (s.finishedAt != null) item {
                Button(
                    onClick = {
                        scope.launch {
                            if (dao.getOpenSession() != null) busy = true
                            else openWorkout(repo.repeatSession(s.id))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Icon(Icons.Filled.Refresh, null); Spacer(Modifier.width(6.dp)); Text("Repeat this workout") }
            }
        }
    }

    if (delete) {
        ConfirmDialog("Delete workout?", "This workout and all its sets will be removed from your history.", confirm = "Delete", destructive = true, onDismiss = { delete = false }) {
            scope.launch { repo.discardSession(sessionId); onBack() }
        }
    }
    if (rename && s != null) {
        TextInputDialog("Rename workout", initial = s.name, onDismiss = { rename = false }) { n -> scope.launch { dao.updateSession(s.copy(name = n)) } }
    }
    if (busy) {
        ConfirmDialog("Workout in progress", "Finish or discard your current workout first.", confirm = "OK", onDismiss = { busy = false }) {}
    }
}
