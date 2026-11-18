package app.gymtracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Exercise::class, DayTemplate::class, DayExercise::class,
        Plan::class, Phase::class, PhaseDay::class,
        Session::class, SessionExercise::class, SetEntry::class,
        BodyWeight::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun dao(): GymDao

    companion object {
        fun build(context: Context): GymDatabase =
            Room.databaseBuilder(context, GymDatabase::class.java, "gym.db").build()
    }
}
