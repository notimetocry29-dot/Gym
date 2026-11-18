package app.gymtracker.ui.plans

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import app.gymtracker.data.Phase
import app.gymtracker.data.PhaseDayRow
import app.gymtracker.data.phaseRanges
import app.gymtracker.ui.common.AppCard
import app.gymtracker.ui.common.AppIcons
import app.gymtracker.ui.common.AppScaffold
import app.gymtracker.ui.common.ConfirmDialog
import app.gymtracker.ui.common.LocalRepo
import app.gymtracker.ui.common.Pill
import app.gymtracker.ui.common.SectionTitle
import app.gymtracker.ui.common.Stepper
import app.gymtracker.ui.common.TextInputDialog
import app.gymtracker.ui.common.fmtDayYear
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanEditorScreen(planId: Long, onBack: () -> Unit, openDay: (Long) -> Unit) {
    val repo = LocalRepo.current
    val dao = repo.dao
    val scope = rememberCoroutineScope()
    val plan by remember { dao.observePlan(planId) }.collectAsStateWithLifecycle(null)
    val phases by remember { dao.observePhases(planId) }.collectAsStateWithLifecycle(emptyList())
    val phaseDays by remember { dao.observePlanPhaseDays(planId) }.collectAsStateWithLifecycle(emptyList())
    val days by remember { dao.observeDays() }.collectAsStateWithLifecycle(emptyList())

    var menu by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var pickDate by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var renamePhase by remember { mutableStateOf<Phase?>(null) }
    var deletePhase by remember { mutableStateOf<Phase?>(null) }
    var addDayTo by remember { mutableStateOf<Phase?>(null) }

    val p = plan ?: run {
        AppScaffold(title = "", onBack = onBack) {}
        return
    }
    val ranges = phaseRanges(p, phases)
    val totalWeeks = phases.sumOf { it.weeks }
    val today = LocalDate.now()

    AppScaffold(
        title = p.name,
        onBack = onBack,
        actions = {
            IconButton(onClick = { rename = true }) { Icon(Icons.Filled.Edit, "Rename") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "More") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Delete program", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; delete = true })
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Active program", style = MaterialTheme.typography.titleMedium)
                            Text("The Today tab follows the active program", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = p.isActive,
                            onCheckedChange = { on -> scope.launch { if (on) dao.setActivePlan(p.id) else dao.deactivatePlan(p.id) } },
                            colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary),
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(vertical = 12.dp))
                    Row(Modifier.fillMaxWidth().clickable { pickDate = true }, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.DateRange, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Starts ${fmtDayYear(LocalDate.ofEpochDay(p.startEpochDay))}", style = MaterialTheme.typography.bodyLarge)
                            ranges.lastOrNull()?.let {
                                Text("Ends ${fmtDayYear(it.endInclusive)} · $totalWeeks weeks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text("Change", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            item {
                SectionTitle("Phases")
                Text(
                    "Each phase runs for a number of weeks and has its own rotation. Days repeat in order " +
                        "no matter which weekday you train. You can mix days from any split.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(phases, key = { it.id }) { ph ->
                val range = ranges.firstOrNull { it.phase.id == ph.id }
                val current = range != null && !today.isBefore(range.start) && !today.isAfter(range.endInclusive)
                val rotation = phaseDays.filter { it.item.phaseId == ph.id }.sortedBy { it.item.position }
                PhaseCard(
                    phase = ph,
                    dates = range?.let { "${fmtDayYear(it.start)} – ${fmtDayYear(it.endInclusive)}" } ?: "",
                    current = current,
                    rotation = rotation,
                    canMoveUp = ph != phases.first(),
                    canMoveDown = ph != phases.last(),
                    onWeeks = { w -> scope.launch { dao.updatePhase(ph.copy(weeks = w)) } },
                    onRename = { renamePhase = ph },
                    onMove = { d -> scope.launch { repo.movePhase(planId, ph.id, d) } },
                    onDelete = { deletePhase = ph },
                    onAddDay = { addDayTo = ph },
                    onOpenDay = openDay,
                    onMoveDay = { id, d -> scope.launch { repo.movePhaseDay(ph.id, id, d) } },
                    onRemoveDay = { id -> scope.launch { repo.removePhaseDay(ph.id, id) } },
                    onSetNext = { i -> scope.launch { dao.updatePhase(ph.copy(nextIndex = i)) } },
                )
            }
            item {
                OutlinedButton(onClick = { scope.launch { repo.addPhase(planId) } }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Filled.Add, null); Spacer(Modifier.width(6.dp)); Text("Add phase")
                }
            }
        }
    }

    if (rename) {
        TextInputDialog("Rename program", initial = p.name, onDismiss = { rename = false }) { n -> scope.launch { dao.updatePlan(p.copy(name = n)) } }
    }
    if (pickDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = p.startEpochDay * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms -> scope.launch { dao.updatePlan(p.copy(startEpochDay = ms / 86_400_000L)) } }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) { DatePicker(state = state) }
    }
    renamePhase?.let { ph ->
        TextInputDialog("Rename phase", initial = ph.name, onDismiss = { renamePhase = null }) { n -> scope.launch { dao.updatePhase(ph.copy(name = n)) } }
    }
    deletePhase?.let { ph ->
        ConfirmDialog("Delete ${ph.name}?", "Its rotation will be removed from this program.", confirm = "Delete", destructive = true, onDismiss = { deletePhase = null }) {
            scope.launch {
                dao.deletePhase(ph)
                dao.updatePhases(dao.getPhases(planId).mapIndexed { i, x -> x.copy(position = i) })
            }
        }
    }
    if (delete) {
        ConfirmDialog("Delete ${p.name}?", "The program will be deleted. Your workout history is kept.", confirm = "Delete", destructive = true, onDismiss = { delete = false }) {
            scope.launch { dao.deletePlan(p); onBack() }
        }
    }
    addDayTo?.let { ph ->
        AlertDialog(
            onDismissRequest = { addDayTo = null },
            title = { Text("Add to ${ph.name}") },
            text = {
                LazyColumn {
                    days.groupBy { it.category }.forEach { (cat, list) ->
                        item { SectionTitle(cat) }
                        items(list, key = { it.id }) { d ->
                            Text(
                                d.name, style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.fillMaxWidth().clickable { scope.launch { repo.addDayToPhase(ph.id, d.id) } }.padding(vertical = 10.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { addDayTo = null }) { Text("Done") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
}

@Composable
private fun PhaseCard(
    phase: Phase,
    dates: String,
    current: Boolean,
    rotation: List<PhaseDayRow>,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onWeeks: (Int) -> Unit,
    onRename: () -> Unit,
    onMove: (Int) -> Unit,
    onDelete: () -> Unit,
    onAddDay: () -> Unit,
    onOpenDay: (Long) -> Unit,
    onMoveDay: (Long, Int) -> Unit,
    onRemoveDay: (Long) -> Unit,
    onSetNext: (Int) -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    AppCard(highlight = current) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).clickable(onClick = onRename)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(phase.name, style = MaterialTheme.typography.titleMedium)
                    if (current) { Spacer(Modifier.width(8.dp)); Pill("Now", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer) }
                }
                Text(dates, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Stepper("Weeks", phase.weeks, onWeeks, 1, 52)
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Phase options") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename() })
                    if (canMoveUp) DropdownMenuItem(text = { Text("Move earlier") }, onClick = { menu = false; onMove(-1) })
                    if (canMoveDown) DropdownMenuItem(text = { Text("Move later") }, onClick = { menu = false; onMove(1) })
                    DropdownMenuItem(text = { Text("Delete phase", color = MaterialTheme.colorScheme.error) }, onClick = { menu = false; onDelete() })
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        val nextIdx = if (rotation.isEmpty()) -1 else phase.nextIndex.mod(rotation.size)
        rotation.forEachIndexed { i, d ->
            Row(Modifier.fillMaxWidth().clickable { onOpenDay(d.item.dayId) }.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${i + 1}", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(24.dp))
                Text(d.dayName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (i == nextIdx) Pill("Next", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                else TextButton(onClick = { onSetNext(i) }) { Text("Set next", style = MaterialTheme.typography.labelSmall) }
                if (i > 0) IconButton(onClick = { onMoveDay(d.item.id, -1) }, modifier = Modifier.size(36.dp)) { Icon(AppIcons.ArrowUp, "Move up") }
                if (i < rotation.lastIndex) IconButton(onClick = { onMoveDay(d.item.id, 1) }, modifier = Modifier.size(36.dp)) { Icon(AppIcons.ArrowDown, "Move down") }
                IconButton(onClick = { onRemoveDay(d.item.id) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Filled.Close, "Remove") }
            }
        }
        if (rotation.isEmpty()) {
            Text("No days in this rotation yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = onAddDay) { Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("Add day to rotation") }
    }
}
