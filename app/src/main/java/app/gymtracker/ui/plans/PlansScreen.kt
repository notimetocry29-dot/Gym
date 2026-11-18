package app.gymtracker.ui.plans

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.gymtracker.data.SeedData
import app.gymtracker.data.phaseRanges
import app.gymtracker.ui.common.AppCard
import app.gymtracker.ui.common.AppScaffold
import app.gymtracker.ui.common.EmptyState
import app.gymtracker.ui.common.LocalRepo
import app.gymtracker.ui.common.Pill
import app.gymtracker.ui.common.SectionTitle
import app.gymtracker.ui.common.TextInputDialog
import app.gymtracker.ui.common.fmtDayYear
import app.gymtracker.ui.common.fmtShort
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun PlansScreen(openPlan: (Long) -> Unit, openDay: (Long) -> Unit) {
    val repo = LocalRepo.current
    val dao = repo.dao
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val plans by remember { dao.observePlans() }.collectAsStateWithLifecycle(emptyList())
    val phases by remember { dao.observeAllPhases() }.collectAsStateWithLifecycle(emptyList())
    val days by remember { dao.observeDays() }.collectAsStateWithLifecycle(emptyList())
    val counts by remember { dao.observeDayCounts() }.collectAsStateWithLifecycle(emptyList())
    val countMap = remember(counts) { counts.associate { it.dayId to it.cnt } }
    var newPlan by remember { mutableStateOf(false) }
    var newDay by remember { mutableStateOf(false) }

    AppScaffold(
        title = "Plans",
        fab = {
            ExtendedFloatingActionButton(
                onClick = { if (tab == 0) newPlan = true else newDay = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(if (tab == 0) "New program" else "New day") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.background,
                indicator = { pos ->
                    TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(pos[tab]), color = MaterialTheme.colorScheme.primary)
                },
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Programs") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Workout days") })
            }
            if (tab == 0) {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (plans.isEmpty()) item {
                        EmptyState("No programs yet", "Create one from a template (Upper / Lower, PPL, Full Body, or a mix) and set how many weeks it runs.")
                    }
                    items(plans, key = { it.id }) { p ->
                        val ph = phases.filter { it.planId == p.id }.sortedBy { it.position }
                        val ranges = phaseRanges(p, ph)
                        val end = ranges.lastOrNull()?.endInclusive
                        AppCard(onClick = { openPlan(p.id) }, highlight = p.isActive) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                                if (p.isActive) Pill("Active", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Text(
                                fmtDayYear(LocalDate.ofEpochDay(p.startEpochDay)) + (end?.let { " → " + fmtDayYear(it) } ?: "") +
                                    " · ${ph.sumOf { it.weeks }} weeks",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (ranges.size > 1) {
                                Spacer(Modifier.height(6.dp))
                                ranges.forEach { r ->
                                    Text(
                                        "• ${r.phase.name}: ${fmtShort(r.start)} – ${fmtShort(r.endInclusive)}",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
                    days.groupBy { it.category }.forEach { (cat, list) ->
                        item(key = "h-$cat") { SectionTitle(cat, Modifier.padding(horizontal = 16.dp)) }
                        items(list, key = { it.id }) { d ->
                            Row(
                                Modifier.fillMaxWidth().clickable { openDay(d.id) }.padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(d.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Text("${countMap[d.id] ?: 0} exercises", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }
                }
            }
        }
    }

    if (newPlan) {
        AlertDialog(
            onDismissRequest = { newPlan = false },
            title = { Text("New program") },
            text = {
                LazyColumn {
                    items(SeedData.plans) { seed ->
                        Column(
                            Modifier.fillMaxWidth().clickable {
                                newPlan = false
                                scope.launch { openPlan(repo.createPlan(seed, LocalDate.now().toEpochDay())) }
                            }.padding(vertical = 10.dp),
                        ) {
                            Text(seed.name, style = MaterialTheme.typography.titleMedium)
                            Text(seed.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    item {
                        Column(
                            Modifier.fillMaxWidth().clickable {
                                newPlan = false
                                scope.launch { openPlan(repo.createPlan(null, LocalDate.now().toEpochDay())) }
                            }.padding(vertical = 10.dp),
                        ) {
                            Text("Blank program", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Text("Build your own rotation from any days, with as many phases as you like", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { newPlan = false }) { Text("Cancel") } },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        )
    }
    if (newDay) {
        TextInputDialog("New workout day", label = "Name, e.g. Upper C", confirm = "Create", onDismiss = { newDay = false }) { name ->
            scope.launch { openDay(repo.createDay(name, "Custom")) }
        }
    }
}
