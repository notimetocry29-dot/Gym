package app.gymtracker.ui.today

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gymtracker.data.DayTemplate
import app.gymtracker.data.PlanState
import app.gymtracker.data.PlanStatus
import app.gymtracker.ui.common.AppCard
import app.gymtracker.ui.common.AppIcons
import app.gymtracker.ui.common.LocalRepo
import app.gymtracker.ui.common.Pill
import app.gymtracker.ui.common.SectionTitle
import app.gymtracker.ui.common.fmtDay
import app.gymtracker.ui.common.fmtDayYear
import app.gymtracker.ui.common.fmtDuration
import app.gymtracker.ui.common.relativeDay
import app.gymtracker.ui.common.toLocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun TodayScreen(
    openWorkout: (Long) -> Unit,
    openPlan: (Long) -> Unit,
    openPlans: () -> Unit,
    openSettings: () -> Unit,
) {
    val repo = LocalRepo.current
    val dao = repo.dao
    val scope = rememberCoroutineScope()
    val status by remember { repo.observeActivePlanStatus() }.collectAsStateWithLifecycle(null)
    val open by remember { dao.observeOpenSession() }.collectAsStateWithLifecycle(null)
    val history by remember { dao.observeHistory() }.collectAsStateWithLifecycle(emptyList())
    val allDays by remember { dao.observeDays() }.collectAsStateWithLifecycle(emptyList())

    var showBusy by remember { mutableStateOf(false) }
    var pickDay by remember { mutableStateOf(false) }

    fun guarded(action: suspend () -> Long) {
        if (open != null) { showBusy = true; return }
        scope.launch { openWorkout(action()) }
    }

    val s = status
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth().padding(top = 32.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(fmtDay(LocalDate.now()), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Today", style = MaterialTheme.typography.headlineMedium)
                }
                IconButton(onClick = openSettings) { Icon(Icons.Filled.Settings, "Settings & backup") }
            }
        }

        open?.let { session ->
            item {
                var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                LaunchedEffect(session.id) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
                AppCard(highlight = true, onClick = { openWorkout(session.id) }) {
                    Text("Workout in progress", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(session.name, style = MaterialTheme.typography.titleLarge)
                    Text("Started ${fmtDuration(now - session.startedAt)} ago", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { openWorkout(session.id) }, modifier = Modifier.fillMaxWidth()) { Text("Resume workout") }
                }
            }
        }

        if (s == null) {
            item {
                AppCard {
                    Text("No active program", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Pick a split like Upper / Lower or PPL, or build your own. The app will then tell you which workout is next.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = openPlans, modifier = Modifier.fillMaxWidth()) { Text("Choose a program") }
                }
            }
        } else {
            item { PlanCard(s, onOpen = { openPlan(s.plan.id) }) }
            item {
                NextWorkoutCard(
                    s,
                    onStart = { pos, dayId ->
                        guarded { repo.startFromDay(dayId, s.plan.id, s.phase?.id, pos) }
                    },
                    onSkip = { scope.launch { s.phase?.let { repo.skipNext(it, s.rotation.size) } } },
                    onPickOther = { pickDay = true },
                )
            }
        }

        item { SectionTitle("Quick start") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { guarded { repo.startEmpty() } }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Empty workout")
                }
                OutlinedButton(onClick = { pickDay = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Any day")
                }
            }
        }

        if (history.isNotEmpty()) {
            item { SectionTitle("Recent") }
            items(history.take(3), key = { it.session.id }) { h ->
                val d = h.session.startedAt.toLocalDate()
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(h.session.name, style = MaterialTheme.typography.bodyLarge)
                        Text("${h.exerciseCount} exercises · ${h.setCount} sets", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(relativeDay(d), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    if (showBusy) {
        AlertDialog(
            onDismissRequest = { showBusy = false },
            title = { Text("Workout in progress") },
            text = { Text("Finish or discard your current workout before starting a new one.") },
            confirmButton = { TextButton(onClick = { showBusy = false; open?.let { openWorkout(it.id) } }) { Text("Resume") } },
            dismissButton = { TextButton(onClick = { showBusy = false }) { Text("Close") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }

    if (pickDay) {
        DayPickerDialog(
            status = s,
            allDays = allDays,
            onDismiss = { pickDay = false },
            onPickRotation = { pos, dayId -> pickDay = false; guarded { repo.startFromDay(dayId, s?.plan?.id, s?.phase?.id, pos) } },
            onPickAny = { dayId -> pickDay = false; guarded { repo.startFromDay(dayId, null, null, null) } },
        )
    }
}

@Composable
private fun PlanCard(s: PlanStatus, onOpen: () -> Unit) {
    AppCard(onClick = onOpen) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(s.plan.name, style = MaterialTheme.typography.titleLarge)
                s.phase?.let { Text(it.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            when (s.state) {
                PlanState.NOT_STARTED -> Pill("Starts ${fmtDayYear(LocalDate.ofEpochDay(s.plan.startEpochDay))}")
                PlanState.FINISHED -> Pill("Finished", MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
                PlanState.RUNNING -> Pill("Week ${s.week} of ${s.totalWeeks}", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Spacer(Modifier.height(12.dp))
        val progress = if (s.totalWeeks == 0) 0f else (s.week.toFloat() / s.totalWeeks).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            drawStopIndicator = {},
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Ends ${fmtDayYear(s.endDate)}" + if (s.state == PlanState.RUNNING && s.weekInPhase > 0) " · week ${s.weekInPhase} of ${s.phase?.weeks} in this phase" else "",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NextWorkoutCard(s: PlanStatus, onStart: (Int, Long) -> Unit, onSkip: () -> Unit, onPickOther: () -> Unit) {
    val dao = LocalRepo.current.dao
    val next = s.next
    if (next == null) {
        AppCard {
            Text("This phase has no workout days yet", style = MaterialTheme.typography.titleMedium)
            Text("Open the plan to add days to the rotation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    val exercises by remember(next.item.dayId) { dao.observeDayExercises(next.item.dayId) }.collectAsStateWithLifecycle(emptyList())
    val pos = s.rotation.indexOf(next)
    AppCard(highlight = true) {
        Text("NEXT UP", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(next.dayName, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        exercises.forEach { e ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(e.exerciseName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text("${e.item.sets} × ${e.item.repMin}–${e.item.repMax}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (exercises.isEmpty()) Text("No exercises in this day yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Button(onClick = { onStart(pos, next.item.dayId) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Start ${next.dayName}")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onSkip) { Icon(AppIcons.SkipNext, null); Spacer(Modifier.width(4.dp)); Text("Skip") }
            TextButton(onClick = onPickOther) { Text("Do another day") }
        }
        if (s.upcoming.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 8.dp))
            Text("Then: " + s.upcoming.joinToString("  →  ") { it.dayName }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DayPickerDialog(
    status: PlanStatus?,
    allDays: List<DayTemplate>,
    onDismiss: () -> Unit,
    onPickRotation: (Int, Long) -> Unit,
    onPickAny: (Long) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose a workout") },
        text = {
            LazyColumn {
                if (status != null && status.rotation.isNotEmpty()) {
                    item { SectionTitle("From your rotation") }
                    items(status.rotation.size) { i ->
                        val d = status.rotation[i]
                        Text(
                            d.dayName + if (d == status.next) "  (next)" else "",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().clickable { onPickRotation(i, d.item.dayId) }.padding(vertical = 10.dp),
                        )
                    }
                    item {
                        Text(
                            "Doing a rotation day moves the plan forward from that day.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                val groups = allDays.groupBy { it.category }
                groups.forEach { (cat, days) ->
                    item { SectionTitle(cat) }
                    items(days, key = { it.id }) { d ->
                        Text(
                            d.name, style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth().clickable { onPickAny(d.id) }.padding(vertical = 10.dp),
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(20.dp),
    )
}

