package app.gymtracker.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.gymtracker.data.Exercise
import app.gymtracker.data.GymRepository
import app.gymtracker.data.Muscles
import app.gymtracker.data.fmtKg
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

val LocalRepo = staticCompositionLocalOf<GymRepository> { error("No repository") }

// ---------------- Formatting ----------------

private val dayFmt = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.ENGLISH)
private val dayYearFmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
private val shortFmt = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private val monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
fun fmtDay(d: LocalDate): String = dayFmt.format(d)
fun fmtDayYear(d: LocalDate): String = dayYearFmt.format(d)
fun fmtShort(d: LocalDate): String = shortFmt.format(d)
fun fmtMonth(d: LocalDate): String = monthFmt.format(d)
fun fmtDuration(ms: Long): String {
    val totalMin = (ms / 60000).coerceAtLeast(0)
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
fun fmtVolume(v: Double): String = if (v >= 10000) "${fmtKg(v / 1000.0).take(5)}t" else "${fmtKg(v)} kg"
fun relativeDay(d: LocalDate): String {
    val diff = LocalDate.now().toEpochDay() - d.toEpochDay()
    return when (diff) {
        0L -> "Today"
        1L -> "Yesterday"
        in 2..6 -> "$diff days ago"
        else -> fmtShort(d)
    }
}

fun parseDecimal(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

// ---------------- Layout ----------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    subtitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {},
    fab: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = fab,
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, trailing: @Composable () -> Unit = {}) {
    Row(modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp, modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, highlight: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val border = BorderStroke(1.dp, if (highlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant)
    val shape = RoundedCornerShape(16.dp)
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(16.dp), content = content)
        }
    } else {
        Card(modifier = modifier.fillMaxWidth(), shape = shape, colors = colors, border = border) {
            Column(Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun Pill(text: String, color: Color = MaterialTheme.colorScheme.surfaceContainerHigh, textColor: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Surface(color = color, shape = RoundedCornerShape(50)) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
    }
}

@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String = "Confirm", destructive: Boolean = false, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String = "",
    label: String = "Name",
    confirm: String = "Save",
    keyboardType: KeyboardType = KeyboardType.Text,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value, onValueChange = { value = it }, label = { Text(label) }, singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            )
        },
        confirmButton = {
            TextButton(enabled = value.isNotBlank(), onClick = { onConfirm(value.trim()); onDismiss() }) { Text(confirm) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    )
}

/** Small "- value +" control used for sets, reps and weeks. */
@Composable
fun Stepper(label: String, value: Int, onChange: (Int) -> Unit, min: Int = 1, max: Int = 100) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepButton("−") { if (value > min) onChange(value - 1) }
            Text(value.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(30.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            StepButton("+") { if (value < max) onChange(value + 1) }
        }
    }
}

@Composable
private fun StepButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary) }
}

@Composable
fun MuscleFilterRow(selected: String?, onSelect: (String?) -> Unit, options: List<String> = Muscles.all) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AppFilterChip("All", selected == null) { onSelect(null) }
        options.forEach { m -> AppFilterChip(m, selected == m) { onSelect(if (selected == m) null else m) } }
    }
}

@Composable
fun AppFilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick, label = { Text(text) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true, selected = selected,
            borderColor = MaterialTheme.colorScheme.outline, selectedBorderColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

/** Full-screen searchable exercise picker. Supports picking several exercises at once. */
@Composable
fun ExercisePickerDialog(
    exercises: List<Exercise>,
    title: String = "Add exercises",
    multiSelect: Boolean = true,
    onDismiss: () -> Unit,
    onCreateNew: (() -> Unit)? = null,
    onPick: (List<Long>) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var muscle by rememberSaveable { mutableStateOf<String?>(null) }
    val picked = remember { mutableStateListOf<Long>() }
    val filtered = exercises.filter {
        (muscle == null || it.primaryMuscle == muscle) &&
            (query.isBlank() || it.name.contains(query, true) || it.equipment.contains(query, true) || it.secondaryMuscles.contains(query, true))
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close") }
                    Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    if (onCreateNew != null) IconButton(onClick = onCreateNew) { Icon(Icons.Filled.Add, "New exercise") }
                    if (multiSelect) {
                        TextButton(enabled = picked.isNotEmpty(), onClick = { onPick(picked.toList()); onDismiss() }) {
                            Text(if (picked.isEmpty()) "Add" else "Add (${picked.size})")
                        }
                    }
                }
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null) }, placeholder = { Text("Search exercises") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                )
                Spacer(Modifier.height(8.dp))
                MuscleFilterRow(muscle, { muscle = it })
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(filtered, key = { it.id }) { ex ->
                        val isPicked = ex.id in picked
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                if (multiSelect) { if (isPicked) picked.remove(ex.id) else picked.add(ex.id) }
                                else { onPick(listOf(ex.id)); onDismiss() }
                            }.padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(ex.name, style = MaterialTheme.typography.bodyLarge)
                                Text("${ex.primaryMuscle} · ${ex.equipment}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (multiSelect) Checkbox(checked = isPicked, onCheckedChange = { if (it) picked.add(ex.id) else picked.remove(ex.id) })
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

// ---------------- Chart ----------------

data class ChartPoint(val x: Long, val y: Double)

/** Minimal line chart: x is epoch day, y is the value. */
@Composable
fun LineChart(points: List<ChartPoint>, modifier: Modifier = Modifier, unit: String = "kg") {
    val color = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 11.sp, color = labelColor)
    if (points.size < 2) {
        Box(modifier.fillMaxWidth().heightIn(min = 80.dp), contentAlignment = Alignment.Center) {
            Text(
                if (points.isEmpty()) "No data yet" else "Log one more session to see a trend",
                style = MaterialTheme.typography.bodyMedium, color = labelColor,
            )
        }
        return
    }
    Canvas(modifier.fillMaxWidth().height(200.dp)) {
        val left = 44.dp.toPx()
        val bottom = size.height - 20.dp.toPx()
        val top = 8.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        var minY = points.minOf { it.y }
        var maxY = points.maxOf { it.y }
        if (maxY - minY < 1e-6) { minY -= 1; maxY += 1 }
        val pad = (maxY - minY) * 0.1
        minY -= pad; maxY += pad
        fun px(x: Long) = if (maxX == minX) (left + right) / 2 else left + (x - minX).toFloat() / (maxX - minX) * (right - left)
        fun py(y: Double) = bottom - ((y - minY) / (maxY - minY)).toFloat() * (bottom - top)

        for (i in 0..3) {
            val v = minY + (maxY - minY) * i / 3
            val y = py(v)
            drawLine(grid, Offset(left, y), Offset(right, y), strokeWidth = 1f)
            drawText(measurer, fmtKg(Math.round(v * 10) / 10.0), Offset(0f, y - 8.dp.toPx()), labelStyle)
        }
        drawText(measurer, fmtShort(LocalDate.ofEpochDay(minX)), Offset(left, bottom + 4.dp.toPx()), labelStyle)
        val endLabel = measurer.measure(fmtShort(LocalDate.ofEpochDay(maxX)), labelStyle)
        drawText(endLabel, topLeft = Offset(right - endLabel.size.width, bottom + 4.dp.toPx()))

        val path = Path()
        points.forEachIndexed { i, p -> if (i == 0) path.moveTo(px(p.x), py(p.y)) else path.lineTo(px(p.x), py(p.y)) }
        drawPath(path, color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
        points.forEach { p -> drawCircle(color, 3.5.dp.toPx(), Offset(px(p.x), py(p.y))) }
    }
}

@Composable
fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(14.dp)).padding(12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
fun CheckIcon() = Icon(Icons.Filled.Check, null)
