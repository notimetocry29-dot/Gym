package app.gymtracker.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.gymtracker.data.ExerciseSetRow
import app.gymtracker.data.estimated1RM
import app.gymtracker.data.fmtKg
import app.gymtracker.ui.common.ChartPoint
import app.gymtracker.ui.common.StatBox
import app.gymtracker.ui.common.fmtVolume
import app.gymtracker.ui.common.toLocalDate
import java.time.LocalDate

data class SessionPerf(
    val sessionId: Long,
    val date: LocalDate,
    val sets: List<Pair<Double, Int>>,
) {
    val topWeight: Double get() = sets.maxOfOrNull { it.first } ?: 0.0
    val bestE1rm: Double get() = sets.maxOfOrNull { estimated1RM(it.first, it.second) } ?: 0.0
    val volume: Double get() = sets.sumOf { it.first * it.second }
    val totalReps: Int get() = sets.sumOf { it.second }
}

enum class Metric(val label: String) { E1RM("Est. 1RM"), TOP("Top weight"), VOLUME("Volume"), REPS("Total reps") }

fun sessionPerfs(rows: List<ExerciseSetRow>): List<SessionPerf> =
    rows.filter { (it.reps ?: 0) > 0 }
        .groupBy { it.sessionId }
        .map { (id, list) -> SessionPerf(id, list.first().startedAt.toLocalDate(), list.map { (it.weight ?: 0.0) to (it.reps ?: 0) }) }
        .sortedBy { it.date }

fun chartPoints(perfs: List<SessionPerf>, metric: Metric): List<ChartPoint> = perfs.map {
    ChartPoint(
        it.date.toEpochDay(),
        when (metric) {
            Metric.E1RM -> it.bestE1rm
            Metric.TOP -> it.topWeight
            Metric.VOLUME -> it.volume
            Metric.REPS -> it.totalReps.toDouble()
        },
    )
}

@Composable
fun PersonalRecords(perfs: List<SessionPerf>) {
    if (perfs.isEmpty()) return
    val allSets = perfs.flatMap { it.sets }
    val heaviest = allSets.maxWith(compareBy<Pair<Double, Int>> { it.first }.thenBy { it.second })
    val bestE1rm = perfs.maxOf { it.bestE1rm }
    val bestVol = perfs.maxOf { it.volume }
    val mostReps = allSets.maxWith(compareBy<Pair<Double, Int>> { it.second }.thenBy { it.first })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("Heaviest set", "${fmtKg(heaviest.first)} kg × ${heaviest.second}", Modifier.weight(1f))
            StatBox("Best est. 1RM", "${fmtKg(Math.round(bestE1rm * 10) / 10.0)} kg", Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox("Most reps", "${mostReps.second} @ ${fmtKg(mostReps.first)} kg", Modifier.weight(1f))
            StatBox("Best session volume", fmtVolume(bestVol), Modifier.weight(1f))
        }
    }
}
