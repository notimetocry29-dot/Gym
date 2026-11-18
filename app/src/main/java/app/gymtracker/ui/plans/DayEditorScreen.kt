package app.gymtracker.ui.plans

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.gymtracker.data.SeedData
import app.gymtracker.ui.common.AppCard
import app.gymtracker.ui.common.AppFilterChip
import app.gymtracker.ui.common.AppIcons
import app.gymtracker.ui.common.AppScaffold
import app.gymtracker.ui.common.ConfirmDialog
import app.gymtracker.ui.common.EmptyState
import app.gymtracker.ui.common.ExercisePickerDialog
import app.gymtracker.ui.common.LocalRepo
import app.gymtracker.ui.common.Stepper
import app.gymtracker.ui.common.TextInputDialog
import kotlinx.coroutines.launch

@Composable
fun DayEditorScreen(dayId: Long, onBack: () -> Unit, openDay: (Long) -> Unit, openWorkout: (Long) -> Unit) {
    val repo = LocalRepo.current
    val dao = repo.dao
    val scope = rememberCoroutineScope()
    val day by remember { dao.observeDay(dayId) }.collectAsStateWithLifecycle(null)
    val rows by remember { dao.observeDayExercises(dayId) }.collectAsStateWithLifecycle(emptyList())
    val exercises by remember { dao.observeExercises() }.collectAsStateWithLifecycle(emptyList())
    val allDays by remember { dao.observeDays() }.collectAsStateWithLifecycle(emptyList())

    var menu by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var group by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    val d = day
    AppScaffold(
        title = d?.name ?: "",
        subtitle = d?.category,
        onBack = onBack,
        actions = {
            IconButton(onClick = { rename = true }) { Icon(Icons.Filled.Edit, "Rename") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Change group") }, onClick = { menu = false; group = true })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; scope.launch { openDay(repo.duplicateDay(dayId)) } })
                    DropdownMenuItem(text = { Text("Delete day", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; delete = true })
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (rows.isEmpty()) item { EmptyState("No exercises yet", "Add the exercises you want to do on this day.") }
            itemsIndexed(rows, key = { _, r -> r.item.id }) { i, r ->
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${i + 1}. ${r.exerciseName}", style = MaterialTheme.typography.titleMedium)
                            Text(r.muscle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (i > 0) IconButton(onClick = { scope.launch { repo.moveDayExercise(dayId, r.item.id, -1) } }) { Icon(AppIcons.ArrowUp, "Move up") }
                        if (i < rows.lastIndex) IconButton(onClick = { scope.launch { repo.moveDayExercise(dayId, r.item.id, 1) } }) { Icon(AppIcons.ArrowDown, "Move down") }
                        IconButton(onClick = { scope.launch { dao.deleteDayExercise(r.item) } }) { Icon(Icons.Filled.Close, "Remove") }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Stepper("Sets", r.item.sets, { v -> scope.launch { dao.updateDayExercise(r.item.copy(sets = v)) } }, 1, 20)
                        Stepper("Min reps", r.item.repMin, { v ->
                            scope.launch { dao.updateDayExercise(r.item.copy(repMin = v, repMax = maxOf(v, r.item.repMax))) }
                        }, 1, 100)
                        Stepper("Max reps", r.item.repMax, { v ->
                            scope.launch { dao.updateDayExercise(r.item.copy(repMax = v, repMin = minOf(v, r.item.repMin))) }
                        }, 1, 100)
                    }
                }
            }
            item {
                OutlinedButton(onClick = { picker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add exercises")
                }
            }
            if (rows.isNotEmpty()) item {
                Button(
                    onClick = {
                        scope.launch {
                            if (dao.getOpenSession() != null) busy = true
                            else openWorkout(repo.startFromDay(dayId, null, null, null))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Start this workout now") }
            }
        }
    }

    if (picker) {
        ExercisePickerDialog(exercises = exercises, onDismiss = { picker = false }) { ids ->
            scope.launch { repo.addExercisesToDay(dayId, ids) }
        }
    }
    if (rename && d != null) {
        TextInputDialog("Rename day", initial = d.name, onDismiss = { rename = false }) { n -> scope.launch { dao.updateDay(d.copy(name = n)) } }
    }
    if (group && d != null) {
        val options = (listOf(SeedData.UL, SeedData.PPL, SeedData.FB, "Custom") + allDays.map { it.category }).distinct()
        var custom by remember { mutableStateOf(false) }
        if (custom) {
            TextInputDialog("New group", label = "Group name", onDismiss = { custom = false; group = false }) { g ->
                scope.launch { dao.updateDay(d.copy(category = g)) }
            }
        } else {
            AlertDialog(
                onDismissRequest = { group = false },
                title = { Text("Group") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        options.forEach { o ->
                            AppFilterChip(o, d.category == o) { group = false; scope.launch { dao.updateDay(d.copy(category = o)) } }
                        }
                        TextButton(onClick = { custom = true }) { Text("New group…") }
                    }
                },
                confirmButton = { TextButton(onClick = { group = false }) { Text("Close") } },
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            )
        }
    }
    if (delete && d != null) {
        ConfirmDialog(
            "Delete ${d.name}?", "It will also be removed from any program rotation. Past workouts stay in your history.",
            confirm = "Delete", destructive = true, onDismiss = { delete = false },
        ) { scope.launch { dao.deleteDay(d); onBack() } }
    }
    if (busy) {
        ConfirmDialog("Workout in progress", "Finish or discard your current workout first.", confirm = "OK", onDismiss = { busy = false }) {}
    }
}
