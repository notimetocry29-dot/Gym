package app.gymtracker

import app.gymtracker.data.Phase
import app.gymtracker.data.PhaseDay
import app.gymtracker.data.PhaseDayRow
import app.gymtracker.data.Plan
import app.gymtracker.data.PlanState
import app.gymtracker.data.SeedData
import app.gymtracker.data.fmtKg
import app.gymtracker.data.planStatus
import app.gymtracker.data.suggest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LogicTest {
    @Test fun increasesWhenTopOfRangeHitOnAllSets() {
        val s = suggest(listOf(60.0 to 10, 60.0 to 10, 60.0 to 10), 0, 3, 10, 2.5)
        assertTrue(s.increase)
        assertEquals(62.5, s.suggestedWeight!!, 1e-9)
    }

    @Test fun staysWhenOneSetShort() {
        val s = suggest(listOf(60.0 to 10, 60.0 to 9, 60.0 to 8), 0, 3, 10, 2.5)
        assertFalse(s.increase)
        assertEquals(60.0, s.suggestedWeight!!, 1e-9)
    }

    @Test fun staysWhenFewerSetsThanTarget() {
        val s = suggest(listOf(100.0 to 8, 100.0 to 8), 0, 3, 8, 5.0)
        assertFalse(s.increase)
    }

    @Test fun lowerBodyUsesLargerStep() {
        val s = suggest(listOf(100.0 to 8, 100.0 to 8, 100.0 to 8), 0, 3, 8, 5.0)
        assertEquals(105.0, s.suggestedWeight!!, 1e-9)
    }

    @Test fun firstTimeHasNoSuggestion() {
        assertNull(suggest(emptyList(), null, 3, 10, 2.5).suggestedWeight)
    }

    @Test fun formatsKg() {
        assertEquals("62.5", fmtKg(62.5))
        assertEquals("60", fmtKg(60.0))
    }

    @Test fun rotationAndPhases() {
        val start = LocalDate.of(2026, 1, 5)
        val plan = Plan(1, "P", start.toEpochDay(), true)
        val p1 = Phase(10, 1, 0, "UL", 4, nextIndex = 3)
        val p2 = Phase(11, 1, 1, "PPL", 4, nextIndex = 0)
        val days = listOf("UA", "LA", "UB", "LB").mapIndexed { i, n -> PhaseDayRow(PhaseDay(100L + i, 10, i, 200L + i), n) } +
            listOf("Push", "Pull", "Legs").mapIndexed { i, n -> PhaseDayRow(PhaseDay(300L + i, 11, i, 400L + i), n) }

        val week2 = planStatus(plan, listOf(p1, p2), days, start.plusDays(8))
        assertEquals(PlanState.RUNNING, week2.state)
        assertEquals(2, week2.week)
        assertEquals("LB", week2.next!!.dayName)
        assertEquals(listOf("UA", "LA", "UB"), week2.upcoming.map { it.dayName })

        val week6 = planStatus(plan, listOf(p1, p2), days, start.plusWeeks(5))
        assertEquals("PPL", week6.phase!!.name)
        assertEquals("Push", week6.next!!.dayName)
        assertEquals(2, week6.weekInPhase)

        assertEquals(PlanState.FINISHED, planStatus(plan, listOf(p1, p2), days, start.plusWeeks(9)).state)
        assertEquals(PlanState.NOT_STARTED, planStatus(plan, listOf(p1, p2), days, start.minusDays(1)).state)
    }

    @Test fun seedTemplatesReferenceExistingExercisesAndDays() {
        val names = SeedData.exercises.map { it.name }.toSet()
        assertEquals("duplicate exercise names", SeedData.exercises.size, names.size)
        SeedData.days.forEach { d -> d.items.forEach { assertTrue("${d.name}: ${it.exercise}", it.exercise in names) } }
        val dayNames = SeedData.days.map { it.name }.toSet()
        SeedData.plans.forEach { p -> p.phases.forEach { ph -> ph.days.forEach { assertTrue("${p.name}: $it", it in dayNames) } } }
    }
}
