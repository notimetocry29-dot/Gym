package app.gymtracker

import android.app.Application
import app.gymtracker.data.GymDatabase
import app.gymtracker.data.GymRepository

class GymApplication : Application() {
    val repository: GymRepository by lazy { GymRepository(GymDatabase.build(this)) }
}
