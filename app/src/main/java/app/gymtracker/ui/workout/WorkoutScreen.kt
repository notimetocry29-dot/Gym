package app.gymtracker.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gymtracker.data.SessionExerciseRow
import app.gymtracker.data.SetEntry
import app.gymtracker.data.Suggestion
import app.gymtracker.data.fmtKg
import app.gymtracker.ui.common.AppCard
import app.gymtracker.ui.common.AppIcons
import app.gymtracker.ui.common.AppScaffold
import app.gymtracker.ui.common.ConfirmDialog
import app.gymtracker.ui.common.ExercisePickerDialog
import app.gymtracker.ui.common.LocalRepo
import app.gymtracker.ui.common.Stepper
import app.gymtracker.ui.common.TextInputDialog
import app.gymtracker.ui.common.parseDecimal
import app.gymtracker.ui.common.relativeDay
import app.gymtracker.ui.common.toLocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WorkoutScreen(sessionId: Long, onBack: () -> Unit, onFinished: (Long) -> Unit, openExercise: (Long) -> Unit) {
    val repo = LocalRepo.current
    val dao = repo.dao
    val scope = rememberCoroutineScope()
    val session by remember { dao.observeSession(sessionId) }.collectAsStateWithLifecycle(null)
    val rows by remember { dao.observeSessionExercises(sessionId) }.collectAsStateWithLifecycle(emptyList())
    val allSets by remember { dao.observeSessionSets(sessionId) }.collectAsStateWithLifecycle(emptyList())
    val exercises by remember { dao.observeExercises() }.collectAsStateWithLifecycle(emptyList())
    val setsBySe = remember(allSets) { allSets.groupBy { it.sessionExerciseId } }

    // Suggestions are keyed by session-exercise and recomputed when its targets change.
    val suggestions = remember { mutableStateMapOf<String, Suggestion>() }
    LaunchedEffect(rows) {
        rows.forEach { r ->
            val key = "${r.item.id}-${r.item.targetSets}-${r.item.repMax}"
            if (key !in suggestions) {
                suggestions[key] = repo.suggestionFor(r.item.exerciseId, sessionId, r.item.targetSets, r.item.repMax, r.increment)
            }
        }
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(1000) } }

    var menu by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var editTargets by remember { mutableStateOf<SessionExerciseRow?>(null) }

    val s = session
    val elapsed = s?.let { ((now - it.startedAt) / 1000).coerceAtLeast(0) } ?: 0
    val clock = "%d:%02d:%02d".format(elapsed / 3600, (elapsed / 60) % 60, elapsed % 60)
    val doneCount = allSets.count { it.done || (it.reps ?: 0) > 0 }

    AppScaffold(
        title = s?.name ?: "Workout",
        subtitle = "$clock · $doneCount sets logged",
        onBack = onBack,
        actions = {
            TextButton(onClick = { confirmFinish = true }) { Text("Finish", fontWeight = FontWeight.SemiBold) }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Rename workout") }, onClick = { menu = false; rename = true })
                    DropdownMenuItem(text = { Text("Discard workout", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; confirmDiscard = true })
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).imePadding(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(rows, key = { it.item.id }) { row ->
                val key = "${row.item.id}-${row.item.targetSets}-${row.item.repMax}"
                ExerciseBlock(
                    row = row,
                    sets = setsBySe[row.item.id].orEmpty(),
                    suggestion = suggestions[key],
                    isFirst = row == rows.firstOrNull(),
                    isLast = row == rows.lastOrNull(),
                    onAddSet = { scope.launch { repo.addSet(row.item.id) } },
                    onWeight = { id, w -> scope.launch { dao.updateSetWeight(id, w) } },
                    onReps = { id, r -> scope.launch { dao.updateSetReps(id, r) } },
                    onDone = { id, w, r -> scope.launch { dao.updateSetDone(id, true, w, r) } },
                    onUndo = { id -> scope.launch { dao.undoSet(id) } },
                    onDeleteSet = { set -> scope.launch { dao.deleteSet(set) } },
                    onMove = { d -> scope.launch { repo.moveSessionExercise(sessionId, row.item.id, d) } },
                    onRemove = { scope.launch { dao.deleteSessionExercise(row.item) } },
                    onEditTargets = { editTargets = row },
                    onInfo = { openExercise(row.item.exerciseId) },
                    onNotes = { n -> scope.launch { dao.updateSessionExercise(row.item.copy(notes = n)) } },
                )
            }
            item {
                OutlinedButton(onClick = { picker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add exercise")
                }
            }
            if (s != null) item {
                var notes by remember(s.id) { mutableStateOf(s.notes) }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it; scope.launch { dao.updateSession(s.copy(notes = it)) } },
                    label = { Text("Workout notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
            }
            item {
                Button(onClick = { confirmFinish = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Finish workout", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }

    if (picker) {
        ExercisePickerDialog(exercises = exercises, onDismiss = { picker = false }) { ids ->
            scope.launch { repo.addExercisesToSession(sessionId, ids) }
        }
    }
    if (rename && s != null) {
        TextInputDialog("Rename workout", initial = s.name, onDismiss = { rename = false }) { n ->
            scope.launch { dao.updateSession(s.copy(name = n)) }
        }
    }
    if (confirmFinish) {
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            title = { Text("Finish workout?") },
            text = {
                Text(
                    if (doneCount == 0) "No sets have reps logged yet. Finishing will save an empty workout."
                    else "$doneCount sets will be saved. Sets without reps are dropped.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    scope.launch { repo.finishSession(sessionId); onFinished(sessionId) }
                }) { Text("Finish") }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Keep going") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
    if (confirmDiscard) {
        ConfirmDialog(
            "Discard workout?", "Everything logged in this workout will be deleted.", confirm = "Discard", destructive = true,
            onDismiss = { confirmDiscard = false },
        ) { scope.launch { repo.discardSession(sessionId); onBack() } }
    }
    editTargets?.let { r ->
        TargetsDialog(r, onDismiss = { editTargets = null }) { sets, lo, hi ->
            scope.launch { dao.updateSessionExercise(r.item.copy(targetSets = sets, repMin = lo, repMax = hi)) }
        }
    }
}

@Composable
private fun ExerciseBlock(
    row: SessionExerciseRow,
    sets: List<SetEntry>,
    suggestion: Suggestion?,
    isFirst: Boolean,
    isLast: Boolean,
    onAddSet: () -> Unit,
    onWeight: (Long, Double?) -> Unit,
    onReps: (Long, Int?) -> Unit,
    onDone: (Long, Double?, Int?) -> Unit,
    onUndo: (Long) -> Unit,
    onDeleteSet: (SetEntry) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    onEditTargets: () -> Unit,
    onInfo: () -> Unit,
    onNotes: (String) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    var showNotes by remember { mutableStateOf(row.item.notes.isNotBlank()) }
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClick = onInfo)) {
                Text(row.exerciseName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    "${row.muscle} · target ${row.item.targetSets} × ${row.item.repMin}–${row.item.repMax}",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Exercise options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit sets & reps target") }, onClick = { menu = false; onEditTargets() })
                    DropdownMenuItem(text = { Text(if (showNotes) "Hide note" else "Add note") }, onClick = { menu = false; showNotes = !showNotes })
                    if (!isFirst) DropdownMenuItem(text = { Text("Move up") }, leadingIcon = { Icon(AppIcons.ArrowUp, null) }, onClick = { menu = false; onMove(-1) })
                    if (!isLast) DropdownMenuItem(text = { Text("Move down") }, leadingIcon = { Icon(AppIcons.ArrowDown, null) }, onClick = { menu = false; onMove(1) })
                    DropdownMenuItem(text = { Text("Exercise info & history") }, onClick = { menu = false; onInfo() })
                    DropdownMenuItem(text = { Text("Remove exercise", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onRemove() })
                }
            }
        }

        if (suggestion != null) {
            Spacer(Modifier.height(8.dp))
            SuggestionBox(suggestion)
        }

        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            HeaderCell("SET", Modifier.width(34.dp))
            HeaderCell("LAST", Modifier.width(72.dp))
            HeaderCell("KG", Modifier.weight(1f))
            HeaderCell("REPS", Modifier.weight(1f))
            Spacer(Modifier.width(44.dp))
        }
        sets.forEachIndexed { i, set ->
            SetRow(
                index = i,
                set = set,
                last = suggestion?.lastSets?.getOrNull(i),
                weightHint = suggestion?.suggestedWeight,
                repHint = suggestion?.lastSets?.getOrNull(i)?.second,
                repMin = row.item.repMin,
                repMax = row.item.repMax,
                onWeight = { onWeight(set.id, it) },
                onReps = { onReps(set.id, it) },
                onDone = { w, r -> onDone(set.id, w, r) },
                onUndo = { onUndo(set.id) },
                onDelete = { onDeleteSet(set) },
            )
        }
        TextButton(onClick = onAddSet, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Add set")
        }
        if (showNotes) {
            var notes by remember(row.item.id) { mutableStateOf(row.item.notes) }
            OutlinedTextField(
                value = notes, onValueChange = { notes = it; onNotes(it) },
                placeholder = { Text("Note for this exercise (seat height, grip, how it felt…)") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SuggestionBox(s: Suggestion) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth()
            .background(if (s.increase) colors.primaryContainer else colors.surfaceContainerHigh, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (s.lastSets.isNotEmpty()) {
            val date = s.lastDate?.let { relativeDay(it.toLocalDate()) } ?: ""
            Text(
                "Last time ($date): " + s.lastSets.joinToString("  ·  ") { "${fmtKg(it.first)}×${it.second}" },
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (s.suggestedWeight != null) {
                Text(
                    (if (s.increase) "↑ " else "") + "${fmtKg(s.suggestedWeight)} kg",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (s.increase) colors.onPrimaryContainer else colors.onSurface,
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(s.message, style = MaterialTheme.typography.bodySmall, color = if (s.increase) colors.onPrimaryContainer else colors.onSurfaceVariant)
        }
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

@Composable
private fun SetRow(
    index: Int,
    set: SetEntry,
    last: Pair<Double, Int>?,
    weightHint: Double?,
    repHint: Int?,
    repMin: Int,
    repMax: Int,
    onWeight: (Double?) -> Unit,
    onReps: (Int?) -> Unit,
    onDone: (Double?, Int?) -> Unit,
    onUndo: () -> Unit,
    onDelete: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var weightText by remember(set.id, set.done) { mutableStateOf(set.weight?.let { fmtKg(it) } ?: "") }
    var repsText by remember(set.id, set.done) { mutableStateOf(set.reps?.toString() ?: "") }
    var menu by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp)
            .background(if (set.done) colors.tertiaryContainer else colors.surfaceContainer, RoundedCornerShape(10.dp))
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(34.dp)) {
            Text(
                "${index + 1}", textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().clickable { menu = true },
            )
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Delete set", color = colors.error) }, onClick = { menu = false; onDelete() })
            }
        }
        Text(
            last?.let { "${fmtKg(it.first)}×${it.second}" } ?: "—",
            modifier = Modifier.width(72.dp), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant,
        )
        CompactField(
            value = weightText,
            placeholder = weightHint?.let { fmtKg(it) } ?: "kg",
            decimal = true,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        ) { t ->
            weightText = t
            onWeight(parseDecimal(t))
        }
        CompactField(
            value = repsText,
            placeholder = repHint?.toString() ?: "$repMin-$repMax",
            decimal = false,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
        ) { t ->
            val clean = t.filter { it.isDigit() }.take(3)
            repsText = clean
            onReps(clean.toIntOrNull())
        }
        Box(
            Modifier.padding(horizontal = 6.dp).size(32.dp)
                .background(if (set.done) colors.tertiary else colors.surfaceContainerHigh, CircleShape)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (set.done) {
                        onUndo()
                    } else {
                        // Tapping done on an empty set fills it with the shown hints.
                        val w = parseDecimal(weightText) ?: weightHint ?: last?.first
                        val r = repsText.toIntOrNull() ?: repHint ?: repMin
                        onDone(w, r)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Check, "Done", tint = if (set.done) colors.onTertiary else colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun CompactField(value: String, placeholder: String, decimal: Boolean, modifier: Modifier, onChange: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    BasicTextField(
        value = value,
        onValueChange = { t -> if (!decimal || t.count { it == '.' || it == ',' } <= 1) onChange(t.take(6)) },
        singleLine = true,
        textStyle = MaterialTheme.typography.titleMedium.copy(color = colors.onSurface, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier
            .height(38.dp)
            .background(colors.surfaceContainerHighest, RoundedCornerShape(8.dp))
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(8.dp)),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize().padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                if (value.isEmpty()) {
                    Text(placeholder, style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                }
                inner()
            }
        },
    )
}

@Composable
fun TargetsDialog(row: SessionExerciseRow, onDismiss: () -> Unit, onSave: (Int, Int, Int) -> Unit) {
    var sets by remember { mutableStateOf(row.item.targetSets) }
    var lo by remember { mutableStateOf(row.item.repMin) }
    var hi by remember { mutableStateOf(row.item.repMax) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(row.exerciseName) },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stepper("Sets", sets, { sets = it }, 1, 20)
                Stepper("Min reps", lo, { lo = it; if (hi < it) hi = it }, 1, 100)
                Stepper("Max reps", hi, { hi = it; if (lo > it) lo = it }, 1, 100)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(sets, lo, hi); onDismiss() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}
