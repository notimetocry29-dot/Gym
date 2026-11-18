package app.gymtracker.data

import androidx.room.withTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

@Serializable
data class Backup(
    val version: Int = 1,
    val exportedAt: Long,
    val exercises: List<Exercise>,
    val days: List<DayTemplate>,
    val dayExercises: List<DayExercise>,
    val plans: List<Plan>,
    val phases: List<Phase>,
    val phaseDays: List<PhaseDay>,
    val sessions: List<Session>,
    val sessionExercises: List<SessionExercise>,
    val sets: List<SetEntry>,
    val bodyWeights: List<BodyWeight>,
)

class GymRepository(private val db: GymDatabase) {
    val dao: GymDao = db.dao()

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    // ---------------- Seeding ----------------

    suspend fun ensureSeeded() {
        if (dao.exerciseCount() > 0) return
        db.withTransaction {
            val ids = dao.insertExercises(SeedData.exercises)
            val byName = SeedData.exercises.map { it.name }.zip(ids).toMap()
            for (d in SeedData.days) {
                val dayId = dao.insertDay(DayTemplate(name = d.name, category = d.category))
                dao.insertDayExercises(d.items.mapIndexedNotNull { i, it ->
                    val exId = byName[it.exercise] ?: return@mapIndexedNotNull null
                    DayExercise(dayId = dayId, exerciseId = exId, position = i, sets = it.sets, repMin = it.repMin, repMax = it.repMax)
                })
            }
        }
    }

    // ---------------- Suggestions ----------------

    suspend fun suggestionFor(exerciseId: Long, excludeSessionId: Long, targetSets: Int, repMax: Int, increment: Double): Suggestion {
        val last = dao.lastSessionExercise(exerciseId, excludeSessionId)
        val sets = last?.let { dao.getDoneSets(it.id) }.orEmpty()
            .filter { it.reps != null && it.reps > 0 }
            .map { (it.weight ?: 0.0) to (it.reps ?: 0) }
        val date = last?.let { dao.getSession(it.sessionId)?.startedAt }
        return suggest(sets, date, targetSets, repMax, increment)
    }

    // ---------------- Sessions ----------------

    private suspend fun addExerciseWithSets(sessionId: Long, exerciseId: Long, position: Int, sets: Int, repMin: Int, repMax: Int) {
        val ex = dao.getExercise(exerciseId) ?: return
        val seId = dao.insertSessionExercise(
            SessionExercise(sessionId = sessionId, exerciseId = exerciseId, position = position, targetSets = sets, repMin = repMin, repMax = repMax)
        )
        val s = suggestionFor(exerciseId, sessionId, sets, repMax, ex.increment)
        dao.insertSets((0 until sets).map { i ->
            SetEntry(sessionExerciseId = seId, position = i, weight = s.suggestedWeight ?: s.lastSets.getOrNull(i)?.first)
        })
    }

    suspend fun startFromDay(dayId: Long, planId: Long?, phaseId: Long?, rotationPos: Int?): Long = db.withTransaction {
        val day = dao.getDay(dayId)
        val sessionId = dao.insertSession(
            Session(
                name = day?.name ?: "Workout", dayId = dayId, planId = planId, phaseId = phaseId,
                rotationPos = rotationPos, startedAt = System.currentTimeMillis(),
            )
        )
        dao.getDayExercises(dayId).forEachIndexed { i, de ->
            addExerciseWithSets(sessionId, de.exerciseId, i, de.sets, de.repMin, de.repMax)
        }
        sessionId
    }

    suspend fun startEmpty(): Long =
        dao.insertSession(Session(name = "Workout", startedAt = System.currentTimeMillis()))

    suspend fun repeatSession(sourceId: Long): Long = db.withTransaction {
        val src = dao.getSession(sourceId)
        val id = dao.insertSession(
            Session(name = src?.name ?: "Workout", dayId = src?.dayId, startedAt = System.currentTimeMillis())
        )
        dao.getSessionExercises(sourceId).forEachIndexed { i, se ->
            addExerciseWithSets(id, se.exerciseId, i, se.targetSets, se.repMin, se.repMax)
        }
        id
    }

    suspend fun addExercisesToSession(sessionId: Long, exerciseIds: List<Long>) = db.withTransaction {
        var pos = dao.getSessionExercises(sessionId).size
        for (exId in exerciseIds) {
            val last = dao.lastSessionExercise(exId, sessionId)
            addExerciseWithSets(sessionId, exId, pos++, last?.targetSets ?: 3, last?.repMin ?: 8, last?.repMax ?: 12)
        }
    }

    suspend fun addSet(sessionExerciseId: Long) {
        val sets = dao.getSets(sessionExerciseId)
        val prev = sets.lastOrNull()
        dao.insertSet(SetEntry(sessionExerciseId = sessionExerciseId, position = (prev?.position ?: -1) + 1, weight = prev?.weight))
    }

    suspend fun moveSessionExercise(sessionId: Long, seId: Long, delta: Int) = db.withTransaction {
        val list = dao.getSessionExercises(sessionId).toMutableList()
        val i = list.indexOfFirst { it.id == seId }
        val j = i + delta
        if (i < 0 || j !in list.indices) return@withTransaction
        list.add(j, list.removeAt(i))
        dao.updateSessionExercises(list.mapIndexed { idx, se -> se.copy(position = idx) })
    }

    suspend fun finishSession(sessionId: Long) = db.withTransaction {
        dao.markFilledSetsDone(sessionId)
        dao.deleteUnfinishedSets(sessionId)
        dao.deleteEmptySessionExercises(sessionId)
        val s = dao.getSession(sessionId) ?: return@withTransaction
        dao.updateSession(s.copy(finishedAt = System.currentTimeMillis()))
        val phaseId = s.phaseId
        val pos = s.rotationPos
        if (phaseId != null && pos != null) {
            val phase = dao.getPhase(phaseId)
            val size = dao.getPhaseDays(phaseId).size
            if (phase != null && size > 0) dao.updatePhase(phase.copy(nextIndex = (pos + 1).mod(size)))
        }
    }

    suspend fun discardSession(sessionId: Long) {
        dao.getSession(sessionId)?.let { dao.deleteSession(it) }
    }

    suspend fun skipNext(phase: Phase, rotationSize: Int, delta: Int = 1) {
        if (rotationSize <= 0) return
        dao.updatePhase(phase.copy(nextIndex = (phase.nextIndex + delta).mod(rotationSize)))
    }

    // ---------------- Day templates ----------------

    suspend fun createDay(name: String, category: String): Long = dao.insertDay(DayTemplate(name = name, category = category))

    suspend fun duplicateDay(dayId: Long): Long = db.withTransaction {
        val d = dao.getDay(dayId)!!
        val newId = dao.insertDay(d.copy(id = 0, name = d.name + " (copy)"))
        dao.insertDayExercises(dao.getDayExercises(dayId).map { it.copy(id = 0, dayId = newId) })
        newId
    }

    suspend fun addExercisesToDay(dayId: Long, exerciseIds: List<Long>) = db.withTransaction {
        var pos = dao.getDayExercises(dayId).size
        dao.insertDayExercises(exerciseIds.map { DayExercise(dayId = dayId, exerciseId = it, position = pos++) })
    }

    suspend fun moveDayExercise(dayId: Long, id: Long, delta: Int) = db.withTransaction {
        val list = dao.getDayExercises(dayId).toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in list.indices) return@withTransaction
        list.add(j, list.removeAt(i))
        dao.updateDayExercises(list.mapIndexed { idx, x -> x.copy(position = idx) })
    }

    // ---------------- Plans ----------------

    suspend fun createPlan(seed: SeedData.PlanSeed?, startEpochDay: Long): Long = db.withTransaction {
        val hasActive = dao.allPlans().any { it.isActive }
        val planId = dao.insertPlan(
            Plan(name = seed?.name ?: "My program", startEpochDay = startEpochDay, isActive = !hasActive)
        )
        val daysByName = dao.allDays().associateBy { it.name }
        val phases = seed?.phases ?: listOf(SeedData.PhaseSeed("Phase 1", 8, emptyList()))
        phases.forEachIndexed { i, ps ->
            val phaseId = dao.insertPhase(Phase(planId = planId, position = i, name = ps.name, weeks = ps.weeks))
            var pos = 0
            ps.days.forEach { dn -> daysByName[dn]?.let { dao.insertPhaseDay(PhaseDay(phaseId = phaseId, position = pos++, dayId = it.id)) } }
        }
        planId
    }

    suspend fun addPhase(planId: Long) {
        val n = dao.getPhases(planId).size
        dao.insertPhase(Phase(planId = planId, position = n, name = "Phase ${n + 1}", weeks = 4))
    }

    suspend fun movePhase(planId: Long, phaseId: Long, delta: Int) = db.withTransaction {
        val list = dao.getPhases(planId).toMutableList()
        val i = list.indexOfFirst { it.id == phaseId }
        val j = i + delta
        if (i < 0 || j !in list.indices) return@withTransaction
        list.add(j, list.removeAt(i))
        dao.updatePhases(list.mapIndexed { idx, x -> x.copy(position = idx) })
    }

    suspend fun addDayToPhase(phaseId: Long, dayId: Long) {
        val n = dao.getPhaseDays(phaseId).size
        dao.insertPhaseDay(PhaseDay(phaseId = phaseId, position = n, dayId = dayId))
    }

    suspend fun movePhaseDay(phaseId: Long, id: Long, delta: Int) = db.withTransaction {
        val list = dao.getPhaseDays(phaseId).toMutableList()
        val i = list.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in list.indices) return@withTransaction
        list.add(j, list.removeAt(i))
        dao.updatePhaseDays(list.mapIndexed { idx, x -> x.copy(position = idx) })
    }

    suspend fun removePhaseDay(phaseId: Long, id: Long) = db.withTransaction {
        dao.getPhaseDays(phaseId).firstOrNull { it.id == id }?.let { dao.deletePhaseDay(it) }
        val rest = dao.getPhaseDays(phaseId)
        dao.updatePhaseDays(rest.mapIndexed { idx, x -> x.copy(position = idx) })
        dao.getPhase(phaseId)?.let { p ->
            if (rest.isEmpty() || p.nextIndex >= rest.size) dao.updatePhase(p.copy(nextIndex = 0))
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeActivePlanStatus(): Flow<PlanStatus?> =
        dao.observeActivePlan().flatMapLatest { plan ->
            if (plan == null) flowOf(null)
            else combine(dao.observePhases(plan.id), dao.observePlanPhaseDays(plan.id)) { phases, days ->
                planStatus(plan, phases, days, LocalDate.now())
            }
        }

    // ---------------- Backup ----------------

    suspend fun exportJson(): String {
        val b = Backup(
            exportedAt = System.currentTimeMillis(),
            exercises = dao.allExercises(),
            days = dao.allDays(),
            dayExercises = dao.allDayExercises(),
            plans = dao.allPlans(),
            phases = dao.allPhases(),
            phaseDays = dao.allPhaseDays(),
            sessions = dao.allSessions(),
            sessionExercises = dao.allSessionExercises(),
            sets = dao.allSets(),
            bodyWeights = dao.allBodyWeights(),
        )
        return json.encodeToString(b)
    }

    /** Replaces all data with the backup. Throws if the file is not a valid backup. */
    suspend fun importJson(text: String): Backup {
        val b = json.decodeFromString<Backup>(text)
        db.withTransaction {
            dao.clearSets(); dao.clearSessionExercises(); dao.clearSessions()
            dao.clearPhaseDays(); dao.clearPhases(); dao.clearPlans()
            dao.clearDayExercises(); dao.clearDays(); dao.clearExercises(); dao.clearBodyWeights()
            dao.restoreExercises(b.exercises)
            dao.restoreDays(b.days)
            dao.restoreDayExercises(b.dayExercises)
            dao.restorePlans(b.plans)
            dao.restorePhases(b.phases)
            dao.restorePhaseDays(b.phaseDays)
            dao.restoreSessions(b.sessions)
            dao.restoreSessionExercises(b.sessionExercises)
            dao.restoreSets(b.sets)
            dao.restoreBodyWeights(b.bodyWeights)
        }
        return b
    }
}
