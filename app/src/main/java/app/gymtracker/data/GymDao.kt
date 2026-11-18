package app.gymtracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GymDao {

    // ---------- Exercises ----------
    @Query("SELECT * FROM exercises ORDER BY name COLLATE NOCASE")
    fun observeExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    fun observeExercise(id: Long): Flow<Exercise?>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExercise(id: Long): Exercise?

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun exerciseCount(): Int

    @Query("SELECT * FROM exercises")
    suspend fun allExercises(): List<Exercise>

    @Upsert
    suspend fun upsertExercise(e: Exercise): Long

    @Insert
    suspend fun insertExercises(list: List<Exercise>): List<Long>

    @Delete
    suspend fun deleteExercise(e: Exercise)

    // ---------- Day templates ----------
    @Query("SELECT * FROM day_templates ORDER BY category, name COLLATE NOCASE")
    fun observeDays(): Flow<List<DayTemplate>>

    @Query("SELECT * FROM day_templates")
    suspend fun allDays(): List<DayTemplate>

    @Query("SELECT * FROM day_templates WHERE id = :id")
    fun observeDay(id: Long): Flow<DayTemplate?>

    @Query("SELECT * FROM day_templates WHERE id = :id")
    suspend fun getDay(id: Long): DayTemplate?

    @Insert
    suspend fun insertDay(d: DayTemplate): Long

    @Update
    suspend fun updateDay(d: DayTemplate)

    @Delete
    suspend fun deleteDay(d: DayTemplate)

    @Query(
        "SELECT de.*, e.name AS exerciseName, e.primaryMuscle AS muscle FROM day_exercises de " +
            "JOIN exercises e ON e.id = de.exerciseId WHERE de.dayId = :dayId ORDER BY de.position"
    )
    fun observeDayExercises(dayId: Long): Flow<List<DayExerciseRow>>

    @Query("SELECT * FROM day_exercises WHERE dayId = :dayId ORDER BY position")
    suspend fun getDayExercises(dayId: Long): List<DayExercise>

    @Query("SELECT * FROM day_exercises")
    suspend fun allDayExercises(): List<DayExercise>

    @Query("SELECT dayId, COUNT(*) AS cnt FROM day_exercises GROUP BY dayId")
    fun observeDayCounts(): Flow<List<DayCount>>

    @Insert
    suspend fun insertDayExercise(x: DayExercise): Long

    @Insert
    suspend fun insertDayExercises(list: List<DayExercise>)

    @Update
    suspend fun updateDayExercise(x: DayExercise)

    @Update
    suspend fun updateDayExercises(list: List<DayExercise>)

    @Delete
    suspend fun deleteDayExercise(x: DayExercise)

    // ---------- Plans ----------
    @Query("SELECT * FROM plans ORDER BY isActive DESC, id DESC")
    fun observePlans(): Flow<List<Plan>>

    @Query("SELECT * FROM plans WHERE id = :id")
    fun observePlan(id: Long): Flow<Plan?>

    @Query("SELECT * FROM plans WHERE isActive = 1 LIMIT 1")
    fun observeActivePlan(): Flow<Plan?>

    @Query("SELECT * FROM plans")
    suspend fun allPlans(): List<Plan>

    @Insert
    suspend fun insertPlan(p: Plan): Long

    @Update
    suspend fun updatePlan(p: Plan)

    @Delete
    suspend fun deletePlan(p: Plan)

    @Query("UPDATE plans SET isActive = CASE WHEN id = :id THEN 1 ELSE 0 END")
    suspend fun setActivePlan(id: Long)

    @Query("UPDATE plans SET isActive = 0 WHERE id = :id")
    suspend fun deactivatePlan(id: Long)

    @Query("SELECT * FROM phases ORDER BY planId, position")
    fun observeAllPhases(): Flow<List<Phase>>

    @Query("SELECT * FROM phases WHERE planId = :planId ORDER BY position")
    fun observePhases(planId: Long): Flow<List<Phase>>

    @Query("SELECT * FROM phases WHERE planId = :planId ORDER BY position")
    suspend fun getPhases(planId: Long): List<Phase>

    @Query("SELECT * FROM phases WHERE id = :id")
    suspend fun getPhase(id: Long): Phase?

    @Query("SELECT * FROM phases")
    suspend fun allPhases(): List<Phase>

    @Insert
    suspend fun insertPhase(p: Phase): Long

    @Update
    suspend fun updatePhase(p: Phase)

    @Update
    suspend fun updatePhases(list: List<Phase>)

    @Delete
    suspend fun deletePhase(p: Phase)

    @Query(
        "SELECT pd.*, d.name AS dayName FROM phase_days pd JOIN day_templates d ON d.id = pd.dayId " +
            "WHERE pd.phaseId IN (SELECT id FROM phases WHERE planId = :planId) ORDER BY pd.position"
    )
    fun observePlanPhaseDays(planId: Long): Flow<List<PhaseDayRow>>

    @Query("SELECT * FROM phase_days WHERE phaseId = :phaseId ORDER BY position")
    suspend fun getPhaseDays(phaseId: Long): List<PhaseDay>

    @Query("SELECT * FROM phase_days")
    suspend fun allPhaseDays(): List<PhaseDay>

    @Insert
    suspend fun insertPhaseDay(x: PhaseDay): Long

    @Update
    suspend fun updatePhaseDays(list: List<PhaseDay>)

    @Delete
    suspend fun deletePhaseDay(x: PhaseDay)

    // ---------- Sessions ----------
    @Query("SELECT * FROM sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeOpenSession(): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    suspend fun getOpenSession(): Session?

    @Query("SELECT * FROM sessions WHERE id = :id")
    fun observeSession(id: Long): Flow<Session?>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: Long): Session?

    @Query("SELECT * FROM sessions")
    suspend fun allSessions(): List<Session>

    @Insert
    suspend fun insertSession(s: Session): Long

    @Update
    suspend fun updateSession(s: Session)

    @Delete
    suspend fun deleteSession(s: Session)

    @Query(
        """SELECT s.*,
        (SELECT COUNT(*) FROM session_exercises se WHERE se.sessionId = s.id) AS exerciseCount,
        (SELECT COUNT(*) FROM set_entries st JOIN session_exercises se ON se.id = st.sessionExerciseId
            WHERE se.sessionId = s.id AND st.done = 1) AS setCount,
        (SELECT COALESCE(SUM(st.weight * st.reps), 0) FROM set_entries st JOIN session_exercises se ON se.id = st.sessionExerciseId
            WHERE se.sessionId = s.id AND st.done = 1) AS volume
        FROM sessions s WHERE s.finishedAt IS NOT NULL ORDER BY s.startedAt DESC"""
    )
    fun observeHistory(): Flow<List<SessionSummary>>

    @Query(
        "SELECT se.*, e.name AS exerciseName, e.primaryMuscle AS muscle, e.increment AS increment " +
            "FROM session_exercises se JOIN exercises e ON e.id = se.exerciseId " +
            "WHERE se.sessionId = :sessionId ORDER BY se.position"
    )
    fun observeSessionExercises(sessionId: Long): Flow<List<SessionExerciseRow>>

    @Query("SELECT * FROM session_exercises WHERE sessionId = :sessionId ORDER BY position")
    suspend fun getSessionExercises(sessionId: Long): List<SessionExercise>

    @Query("SELECT * FROM session_exercises")
    suspend fun allSessionExercises(): List<SessionExercise>

    @Insert
    suspend fun insertSessionExercise(x: SessionExercise): Long

    @Update
    suspend fun updateSessionExercise(x: SessionExercise)

    @Update
    suspend fun updateSessionExercises(list: List<SessionExercise>)

    @Delete
    suspend fun deleteSessionExercise(x: SessionExercise)

    @Query(
        "SELECT st.* FROM set_entries st JOIN session_exercises se ON se.id = st.sessionExerciseId " +
            "WHERE se.sessionId = :sessionId ORDER BY st.position"
    )
    fun observeSessionSets(sessionId: Long): Flow<List<SetEntry>>

    @Query("SELECT * FROM set_entries WHERE sessionExerciseId = :id ORDER BY position")
    suspend fun getSets(id: Long): List<SetEntry>

    @Query("SELECT * FROM set_entries WHERE sessionExerciseId = :id AND done = 1 ORDER BY position")
    suspend fun getDoneSets(id: Long): List<SetEntry>

    @Query("SELECT * FROM set_entries")
    suspend fun allSets(): List<SetEntry>

    @Insert
    suspend fun insertSet(s: SetEntry): Long

    @Insert
    suspend fun insertSets(list: List<SetEntry>)

    @Update
    suspend fun updateSet(s: SetEntry)

    @Delete
    suspend fun deleteSet(s: SetEntry)

    @Query("UPDATE set_entries SET weight = :weight WHERE id = :id")
    suspend fun updateSetWeight(id: Long, weight: Double?)

    @Query("UPDATE set_entries SET reps = :reps WHERE id = :id")
    suspend fun updateSetReps(id: Long, reps: Int?)

    @Query("UPDATE set_entries SET done = :done, weight = :weight, reps = :reps WHERE id = :id")
    suspend fun updateSetDone(id: Long, done: Boolean, weight: Double?, reps: Int?)

    @Query("UPDATE set_entries SET done = 0 WHERE id = :id")
    suspend fun undoSet(id: Long)

    @Query(
        "UPDATE set_entries SET done = 1 WHERE reps IS NOT NULL AND reps > 0 AND sessionExerciseId IN " +
            "(SELECT id FROM session_exercises WHERE sessionId = :sessionId)"
    )
    suspend fun markFilledSetsDone(sessionId: Long)

    @Query(
        "DELETE FROM set_entries WHERE (done = 0 OR reps IS NULL OR reps <= 0) AND sessionExerciseId IN " +
            "(SELECT id FROM session_exercises WHERE sessionId = :sessionId)"
    )
    suspend fun deleteUnfinishedSets(sessionId: Long)

    @Query(
        "DELETE FROM session_exercises WHERE sessionId = :sessionId AND id NOT IN " +
            "(SELECT sessionExerciseId FROM set_entries)"
    )
    suspend fun deleteEmptySessionExercises(sessionId: Long)

    /** The most recent finished performance of an exercise (excluding a given session). */
    @Query(
        "SELECT se.* FROM session_exercises se JOIN sessions s ON s.id = se.sessionId " +
            "WHERE se.exerciseId = :exerciseId AND s.finishedAt IS NOT NULL AND s.id != :excludeSessionId " +
            "ORDER BY s.startedAt DESC LIMIT 1"
    )
    suspend fun lastSessionExercise(exerciseId: Long, excludeSessionId: Long): SessionExercise?

    // ---------- Progress ----------
    @Query(
        "SELECT s.id AS sessionId, s.startedAt AS startedAt, st.weight AS weight, st.reps AS reps " +
            "FROM set_entries st JOIN session_exercises se ON se.id = st.sessionExerciseId " +
            "JOIN sessions s ON s.id = se.sessionId " +
            "WHERE se.exerciseId = :exerciseId AND st.done = 1 AND s.finishedAt IS NOT NULL " +
            "ORDER BY s.startedAt, st.position"
    )
    fun observeExerciseSets(exerciseId: Long): Flow<List<ExerciseSetRow>>

    @Query(
        "SELECT DISTINCT e.* FROM exercises e JOIN session_exercises se ON se.exerciseId = e.id " +
            "JOIN sessions s ON s.id = se.sessionId WHERE s.finishedAt IS NOT NULL ORDER BY e.name COLLATE NOCASE"
    )
    fun observeTrainedExercises(): Flow<List<Exercise>>

    // ---------- Body weight ----------
    @Query("SELECT * FROM body_weights ORDER BY epochDay DESC, id DESC")
    fun observeBodyWeights(): Flow<List<BodyWeight>>

    @Query("SELECT * FROM body_weights")
    suspend fun allBodyWeights(): List<BodyWeight>

    @Insert
    suspend fun insertBodyWeight(b: BodyWeight): Long

    @Delete
    suspend fun deleteBodyWeight(b: BodyWeight)

    // ---------- Backup ----------
    @Insert suspend fun restoreExercises(l: List<Exercise>)
    @Insert suspend fun restoreDays(l: List<DayTemplate>)
    @Insert suspend fun restoreDayExercises(l: List<DayExercise>)
    @Insert suspend fun restorePlans(l: List<Plan>)
    @Insert suspend fun restorePhases(l: List<Phase>)
    @Insert suspend fun restorePhaseDays(l: List<PhaseDay>)
    @Insert suspend fun restoreSessions(l: List<Session>)
    @Insert suspend fun restoreSessionExercises(l: List<SessionExercise>)
    @Insert suspend fun restoreSets(l: List<SetEntry>)
    @Insert suspend fun restoreBodyWeights(l: List<BodyWeight>)

    @Query("DELETE FROM set_entries") suspend fun clearSets()
    @Query("DELETE FROM session_exercises") suspend fun clearSessionExercises()
    @Query("DELETE FROM sessions") suspend fun clearSessions()
    @Query("DELETE FROM phase_days") suspend fun clearPhaseDays()
    @Query("DELETE FROM phases") suspend fun clearPhases()
    @Query("DELETE FROM plans") suspend fun clearPlans()
    @Query("DELETE FROM day_exercises") suspend fun clearDayExercises()
    @Query("DELETE FROM day_templates") suspend fun clearDays()
    @Query("DELETE FROM exercises") suspend fun clearExercises()
    @Query("DELETE FROM body_weights") suspend fun clearBodyWeights()
}
