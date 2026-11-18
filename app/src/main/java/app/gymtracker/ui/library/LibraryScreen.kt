package app.gymtracker.ui.library

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gymtracker.data.Equipment
import app.gymtracker.data.Exercise
import app.gymtracker.data.Muscles
import app.gymtracker.data.fmtKg
import app.gymtracker.ui.common.AppFilterChip
import app.gymtracker.ui.common.AppScaffold
import app.gymtracker.ui.common.LocalRepo
import app.gymtracker.ui.common.MuscleFilterRow
import app.gymtracker.ui.common.Pill
import app.gymtracker.ui.common.SectionTitle
import app.gymtracker.ui.common.parseDecimal
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(openExercise: (Long) -> Unit) {
    val repo = LocalRepo.current
    val scope = rememberCoroutineScope()
    val exercises by remember { repo.dao.observeExercises() }.collectAsStateWithLifecycle(emptyList())
    var query by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }

    val filtered = exercises.filter {
        (muscle == null || it.primaryMuscle == muscle) &&
            (query.isBlank() || it.name.contains(query, true) || it.equipment.contains(query, true) || it.secondaryMuscles.contains(query, true))
    }

    AppScaffold(
        title = "Exercises",
        subtitle = "${exercises.size} exercises",
        fab = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Custom") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Search by name, muscle or equipment") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(Modifier.height(8.dp))
            MuscleFilterRow(muscle, { muscle = it })
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                val groups = filtered.groupBy { it.primaryMuscle }.toSortedMap(compareBy { Muscles.all.indexOf(it).let { i -> if (i < 0) 99 else i } })
                groups.forEach { (m, list) ->
                    item(key = "h-$m") { SectionTitle(m, Modifier.padding(horizontal = 16.dp)) }
                    items(list, key = { it.id }) { ex ->
                        Row(
                            Modifier.fillMaxWidth().clickable { openExercise(ex.id) }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    ex.equipment + if (ex.secondaryMuscles.isNotBlank()) " · ${ex.secondaryMuscles}" else "",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (ex.isCustom) Pill("Custom", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    if (creating) {
        ExerciseEditDialog(
            initial = null,
            onDismiss = { creating = false },
        ) { e -> scope.launch { val id = repo.dao.upsertExercise(e); openExercise(id) } }
    }
}

/** Create or edit an exercise. */
@Composable
fun ExerciseEditDialog(initial: Exercise?, onDismiss: () -> Unit, onSave: (Exercise) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var muscle by rememberSaveable { mutableStateOf(initial?.primaryMuscle ?: "Chest") }
    var secondary by rememberSaveable { mutableStateOf(initial?.secondaryMuscles ?: "") }
    var equipment by rememberSaveable { mutableStateOf(initial?.equipment ?: "Barbell") }
    var instructions by rememberSaveable { mutableStateOf(initial?.instructions ?: "") }
    var increment by rememberSaveable { mutableStateOf(initial?.increment?.let { fmtKg(it) } ?: "") }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close") }
                    Text(if (initial == null) "New exercise" else "Edit exercise", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    TextButton(
                        enabled = name.isNotBlank(),
                        onClick = {
                            val inc = parseDecimal(increment) ?: if (muscle in Muscles.lower) 5.0 else 2.5
                            onSave(
                                (initial ?: Exercise(name = "", primaryMuscle = "", equipment = "", isCustom = true)).copy(
                                    name = name.trim(), primaryMuscle = muscle, secondaryMuscles = secondary.trim(),
                                    equipment = equipment, instructions = instructions.trim(), increment = inc,
                                )
                            )
                            onDismiss()
                        },
                    ) { Text("Save") }
                }
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    SectionTitle("Target muscle")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Muscles.all.forEach { m -> AppFilterChip(m, muscle == m) { muscle = m } }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(secondary, { secondary = it }, label = { Text("Secondary muscles (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    SectionTitle("Equipment")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Equipment.all.forEach { e -> AppFilterChip(e, equipment == e) { equipment = e } }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        increment, { increment = it }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        label = { Text("Weight increase step (kg)") },
                        placeholder = { Text(if (muscle in Muscles.lower) "5" else "2.5") },
                        supportingText = { Text("Added to the suggestion when you hit the top of the rep range on every set") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        instructions, { instructions = it }, label = { Text("How to perform (one step per line)") },
                        minLines = 4, modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}
