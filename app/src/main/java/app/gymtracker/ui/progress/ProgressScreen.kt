package app.gymtracker.ui.progress

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gymtracker.data.BodyWeight
import app.gymtracker.data.Exercise
import app.gymtracker.data.fmtKg
import app.gymtracker.ui.common.AppCard
import app.gymtracker.ui.common.AppFilterChip
import app.gymtracker.ui.common.AppScaffold
import app.gymtracker.ui.common.ChartPoint
import app.gymtracker.ui.common.EmptyState
import app.gymtracker.ui.common.LineChart
import app.gymtracker.ui.common.LocalRepo
import app.gymtracker.ui.common.SectionTitle
import app.gymtracker.ui.common.StatBox
import app.gymtracker.ui.common.fmtDayYear
import app.gymtracker.ui.common.parseDecimal
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun ProgressScreen(openExercise: (Long) -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var addWeight by remember { mutableStateOf(false) }
    AppScaffold(
        title = "Progress",
        fab = {
            if (tab == 1) ExtendedFloatingActionButton(
                onClick = { addWeight = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text("Log weight") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.background,
                indicator = { pos -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[tab]), color = MaterialTheme.colorScheme.primary) },
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Exercises") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Body weight") })
            }
            if (tab == 0) ExerciseProgress(openExercise) else BodyWeightTab(addWeight) { addWeight = false }
        }
    }
}

@Composable
private fun ExerciseProgress(openExercise: (Long) -> Unit) {
    val dao = LocalRepo.current.dao
    val trained by remember { dao.observeTrainedExercises() }.collectAsStateWithLifecycle(emptyList())
    var selectedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var metric by rememberSaveable { mutableStateOf(Metric.E1RM) }
    var menu by remember { mutableStateOf(false) }

    if (trained.isEmpty()) {
        EmptyState("Nothing to chart yet", "Finish a workout and your progress for each exercise will appear here.")
        return
    }
    val selected: Exercise = trained.firstOrNull { it.id == selectedId } ?: trained.first()
    val rows by remember(selected.id) { dao.observeExerciseSets(selected.id) }.collectAsStateWithLifecycle(emptyList())
    val perfs = remember(rows) { sessionPerfs(rows) }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Box {
                AppCard(onClick = { menu = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Exercise", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(selected.name, style = MaterialTheme.typography.titleMedium)
                        }
                        Icon(Icons.Filled.ArrowDropDown, null)
                    }
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    trained.forEach { e ->
                        DropdownMenuItem(text = { Text(e.name) }, onClick = { selectedId = e.id; menu = false })
                    }
                }
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Metric.entries.forEach { m -> AppFilterChip(m.label, metric == m) { metric = m } }
            }
        }
        item { AppCard { LineChart(chartPoints(perfs, metric)) } }
        item { SectionTitle("Personal records") }
        item { PersonalRecords(perfs) }
        item {
            TextButton(onClick = { openExercise(selected.id) }, modifier = Modifier.fillMaxWidth()) { Text("See full history for ${selected.name}") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BodyWeightTab(adding: Boolean, onDoneAdding: () -> Unit) {
    val dao = LocalRepo.current.dao
    val scope = rememberCoroutineScope()
    val entries by remember { dao.observeBodyWeights() }.collectAsStateWithLifecycle(emptyList())
    val points = remember(entries) { entries.sortedBy { it.epochDay }.map { ChartPoint(it.epochDay, it.kg) } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (entries.isEmpty()) {
            item { EmptyState("No weigh-ins yet", "Log your body weight to see the trend over time.") }
        } else {
            item {
                val latest = entries.first()
                val first = entries.last()
                val monthAgo = entries.firstOrNull { it.epochDay <= LocalDate.now().toEpochDay() - 30 }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox("Current", "${fmtKg(latest.kg)} kg", Modifier.weight(1f))
                    StatBox("30-day change", monthAgo?.let { signed(latest.kg - it.kg) } ?: "—", Modifier.weight(1f))
                    StatBox("Total change", signed(latest.kg - first.kg), Modifier.weight(1f))
                }
            }
            item { AppCard { LineChart(points) } }
            item { SectionTitle("Entries") }
            items(entries, key = { it.id }) { b ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(fmtDayYear(LocalDate.ofEpochDay(b.epochDay)), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("${fmtKg(b.kg)} kg", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { scope.launch { dao.deleteBodyWeight(b) } }) { Icon(Icons.Filled.Close, "Delete entry") }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }

    if (adding) {
        var text by rememberSaveable { mutableStateOf(entries.firstOrNull()?.let { fmtKg(it.kg) } ?: "") }
        var day by rememberSaveable { mutableStateOf(LocalDate.now().toEpochDay()) }
        var pickDate by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = onDoneAdding,
            title = { Text("Log body weight") },
            text = {
                Column {
                    OutlinedTextField(
                        text, { text = it }, label = { Text("Weight (kg)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Date: ${fmtDayYear(LocalDate.ofEpochDay(day))}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { pickDate = true }.padding(vertical = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = parseDecimal(text) != null, onClick = {
                    parseDecimal(text)?.let { kg -> scope.launch { dao.insertBodyWeight(BodyWeight(epochDay = day, kg = kg)) } }
                    onDoneAdding()
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = onDoneAdding) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
        if (pickDate) {
            val state = rememberDatePickerState(initialSelectedDateMillis = day * 86_400_000L)
            DatePickerDialog(
                onDismissRequest = { pickDate = false },
                confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { day = it / 86_400_000L }; pickDate = false }) { Text("OK") } },
                dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
                colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) { DatePicker(state = state) }
        }
    }
}

private fun signed(v: Double): String {
    val r = Math.round(v * 10) / 10.0
    return (if (r > 0) "+" else "") + fmtKg(r) + " kg"
}

