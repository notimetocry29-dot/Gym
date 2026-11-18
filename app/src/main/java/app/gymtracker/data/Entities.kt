package app.gymtracker.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val primaryMuscle: String,
    val secondaryMuscles: String = "",
    val equipment: String,
    val instructions: String = "",
    val isCustom: Boolean = false,
    /** Weight (kg) suggested on top of last time once the top of the rep range is reached. */
    val increment: Double = 2.5,
)

/** A reusable workout day, e.g. "Upper A" or "Push B". */
@Serializable
@Entity(tableName = "day_templates")
data class DayTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Custom",
)

@Serializable
@Entity(
    tableName = "day_exercises",
    foreignKeys = [
        ForeignKey(DayTemplate::class, ["id"], ["dayId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Exercise::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("dayId"), Index("exerciseId")],
)
data class DayExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val exerciseId: Long,
    val position: Int,
    val sets: Int = 3,
    val repMin: Int = 8,
    val repMax: Int = 12,
)

/** A training program that runs for a period of time and is split into phases. */
@Serializable
@Entity(tableName = "plans")
data class Plan(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startEpochDay: Long,
    val isActive: Boolean = false,
)

/** A block of weeks inside a plan with its own rotation of days. */
@Serializable
@Entity(
    tableName = "phases",
    foreignKeys = [ForeignKey(Plan::class, ["id"], ["planId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("planId")],
)
data class Phase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planId: Long,
    val position: Int,
    val name: String,
    val weeks: Int,
    /** Index into the rotation of the next workout to do. */
    val nextIndex: Int = 0,
)

@Serializable
@Entity(
    tableName = "phase_days",
    foreignKeys = [
        ForeignKey(Phase::class, ["id"], ["phaseId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(DayTemplate::class, ["id"], ["dayId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("phaseId"), Index("dayId")],
)
data class PhaseDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val phaseId: Long,
    val position: Int,
    val dayId: Long,
)

@Serializable
@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dayId: Long? = null,
    val planId: Long? = null,
    val phaseId: Long? = null,
    val rotationPos: Int? = null,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val notes: String = "",
)

@Serializable
@Entity(
    tableName = "session_exercises",
    foreignKeys = [
        ForeignKey(Session::class, ["id"], ["sessionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(Exercise::class, ["id"], ["exerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SessionExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val position: Int,
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    val notes: String = "",
)

@Serializable
@Entity(
    tableName = "set_entries",
    foreignKeys = [
        ForeignKey(SessionExercise::class, ["id"], ["sessionExerciseId"], onDelete = ForeignKey.CASCADE),
    ],
    indices = [Index("sessionExerciseId")],
)
data class SetEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val position: Int,
    val weight: Double? = null,
    val reps: Int? = null,
    val done: Boolean = false,
)

@Serializable
@Entity(tableName = "body_weights")
data class BodyWeight(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val kg: Double,
)

// ---- Query result rows ----

data class DayExerciseRow(
    @Embedded val item: DayExercise,
    val exerciseName: String,
    val muscle: String,
)

data class SessionExerciseRow(
    @Embedded val item: SessionExercise,
    val exerciseName: String,
    val muscle: String,
    val increment: Double,
)

data class PhaseDayRow(
    @Embedded val item: PhaseDay,
    val dayName: String,
)

data class DayCount(val dayId: Long, val cnt: Int)

data class SessionSummary(
    @Embedded val session: Session,
    val exerciseCount: Int,
    val setCount: Int,
    val volume: Double,
)

data class ExerciseSetRow(
    val sessionId: Long,
    val startedAt: Long,
    val weight: Double?,
    val reps: Int?,
)
