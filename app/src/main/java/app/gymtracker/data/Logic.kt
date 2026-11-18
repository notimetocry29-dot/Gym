package app.gymtracker.data

import java.time.LocalDate
import kotlin.math.roundToInt

/** Pure training logic, kept free of Android so it can be unit tested. */

data class Suggestion(
    /** Done sets from last time: weight to reps. */
    val lastSets: List<Pair<Double, Int>>,
    val lastDate: Long?,
    val suggestedWeight: Double?,
    val increase: Boolean,
    val message: String,
)

/**
 * Progressive overload rule: if every set at the top weight last time reached the top of
 * the rep range (and at least the target number of sets was done), add the exercise's increment.
 * Otherwise stay at the same weight and aim for more reps.
 */
fun suggest(
    lastSets: List<Pair<Double, Int>>,
    lastDate: Long?,
    targetSets: Int,
    repMax: Int,
    increment: Double,
): Suggestion {
    if (lastSets.isEmpty()) {
        return Suggestion(emptyList(), null, null, false, "First time: pick a weight you can do for ${repMax} clean reps")
    }
    val top = lastSets.maxOf { it.first }
    val topSets = lastSets.filter { it.first == top }
    val hitAll = topSets.size >= targetSets && topSets.all { it.second >= repMax }
    return if (hitAll && top > 0) {
        val next = top + increment
        Suggestion(
            lastSets, lastDate, next, true,
            "You hit $repMax reps on every set. Go up +${fmtKg(increment)} kg",
        )
    } else if (top <= 0) {
        Suggestion(lastSets, lastDate, null, false, "Beat last time's reps")
    } else {
        val missing = topSets.count { it.second < repMax }.coerceAtLeast(targetSets - topSets.size)
        Suggestion(
            lastSets, lastDate, top, false,
            "Stay at ${fmtKg(top)} kg. Reach $repMax reps on all sets to go up" +
                if (missing > 0) " ($missing set${if (missing > 1) "s" else ""} to go)" else "",
        )
    }
}

/** Epley estimated one-rep max. */
fun estimated1RM(weight: Double, reps: Int): Double =
    if (reps <= 1) weight else weight * (1 + reps / 30.0)

fun fmtKg(v: Double): String {
    val r = (v * 100).roundToInt() / 100.0
    return if (r % 1.0 == 0.0) r.toLong().toString() else r.toString().trimEnd('0').trimEnd('.')
}

enum class PlanState { NOT_STARTED, RUNNING, FINISHED }

data class PlanStatus(
    val plan: Plan,
    val state: PlanState,
    val phase: Phase?,
    val phaseIndex: Int,
    /** 1-based week number within the whole plan. */
    val week: Int,
    val totalWeeks: Int,
    /** 1-based week number within the current phase. */
    val weekInPhase: Int,
    val rotation: List<PhaseDayRow>,
    val next: PhaseDayRow?,
    val upcoming: List<PhaseDayRow>,
    val endDate: LocalDate,
)

data class PhaseRange(val phase: Phase, val start: LocalDate, val endInclusive: LocalDate)

fun phaseRanges(plan: Plan, phases: List<Phase>): List<PhaseRange> {
    var cursor = LocalDate.ofEpochDay(plan.startEpochDay)
    return phases.sortedBy { it.position }.map { p ->
        val start = cursor
        cursor = cursor.plusWeeks(p.weeks.toLong().coerceAtLeast(1))
        PhaseRange(p, start, cursor.minusDays(1))
    }
}

fun planStatus(plan: Plan, phases: List<Phase>, days: List<PhaseDayRow>, today: LocalDate): PlanStatus {
    val sorted = phases.sortedBy { it.position }
    val ranges = phaseRanges(plan, sorted)
    val start = LocalDate.ofEpochDay(plan.startEpochDay)
    val totalWeeks = sorted.sumOf { it.weeks.coerceAtLeast(1) }
    val end = start.plusWeeks(totalWeeks.toLong()).minusDays(1)
    val state = when {
        today.isBefore(start) -> PlanState.NOT_STARTED
        today.isAfter(end) -> PlanState.FINISHED
        else -> PlanState.RUNNING
    }
    val idx = when (state) {
        PlanState.NOT_STARTED -> 0
        PlanState.FINISHED -> sorted.lastIndex
        PlanState.RUNNING -> ranges.indexOfFirst { !today.isBefore(it.start) && !today.isAfter(it.endInclusive) }
    }
    val phase = sorted.getOrNull(idx)
    val week = when (state) {
        PlanState.NOT_STARTED -> 0
        PlanState.FINISHED -> totalWeeks
        PlanState.RUNNING -> ((today.toEpochDay() - plan.startEpochDay) / 7).toInt() + 1
    }
    val weekInPhase = ranges.getOrNull(idx)?.let { r ->
        if (state == PlanState.RUNNING) ((today.toEpochDay() - r.start.toEpochDay()) / 7).toInt() + 1 else 0
    } ?: 0
    val rotation = if (phase == null) emptyList() else days.filter { it.item.phaseId == phase.id }.sortedBy { it.item.position }
    val next = if (rotation.isEmpty()) null else rotation[phase!!.nextIndex.mod(rotation.size)]
    val upcoming = if (rotation.isEmpty()) emptyList() else
        (1..minOf(4, rotation.size - 1)).map { rotation[(phase!!.nextIndex + it).mod(rotation.size)] }
    return PlanStatus(plan, state, phase, idx, week, totalWeeks, weekInPhase, rotation, next, upcoming, end)
}
