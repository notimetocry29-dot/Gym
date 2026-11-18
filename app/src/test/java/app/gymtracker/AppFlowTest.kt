package app.gymtracker

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Drives the real app end to end: pick a program, run a workout, log sets, finish it,
 * and check that the next workout in the rotation and the weight suggestion are right.
 * Screenshots of each step are written to app/build/screens.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h860dp-xhdpi")
class AppFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val outDir = File("build/screens").apply { mkdirs() }

    private fun shot(name: String) {
        rule.waitForIdle()
        val bmp = rule.onAllNodes(isRoot()).onLast().captureToImage().asAndroidBitmap()
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun waitFor(matcher: SemanticsMatcher, timeout: Long = 15_000) {
        try {
            rule.waitUntil(timeout) { rule.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        } catch (e: ComposeTimeoutException) {
            shot("zz-timeout")
            throw AssertionError("Timed out waiting for $matcher", e)
        }
    }

    private fun waitText(t: String) = waitFor(hasText(t, substring = true))

    @Test
    fun fullWorkoutFlow() {
        waitText("No active program")
        shot("01-today-empty")

        // Library is seeded.
        rule.onNodeWithText("Exercises").performClick()
        waitText("Barbell Bench Press")
        shot("02-library")
        rule.onNodeWithText("Barbell Bench Press").performClick()
        waitText("How to")
        shot("03-exercise-detail")
        rule.onNodeWithContentDescriptionFirst("Back")

        // Create an Upper / Lower program from the template.
        rule.onNodeWithText("Plans").performClick()
        waitText("New program")
        rule.onNodeWithText("New program").performClick()
        waitText("4-day rotation")
        shot("04-new-program-dialog")
        rule.onNodeWithText("4-day rotation", substring = true).performClick()
        waitText("Active program")
        waitText("Lower B")
        shot("05-plan-editor")
        rule.onNodeWithContentDescriptionFirst("Back")

        rule.onNodeWithText("Workout days").performClick()
        waitText("Push A")
        shot("06-workout-days")
        rule.onNodeWithText("Upper A").performClick()
        waitText("Min reps")
        shot("07-day-editor")
        rule.onNodeWithContentDescriptionFirst("Back")

        // Today shows the next workout.
        rule.onNodeWithText("Today").performClick()
        waitText("Start Upper A")
        shot("08-today-plan")

        rule.onNodeWithText("Start Upper A").performClick()
        waitText("First time")
        shot("09-workout-start")

        // Log all 4 bench sets at 60 kg x 8 (top of the 6-8 range).
        val fields = rule.onAllNodes(hasSetTextAction())
        for (i in 0 until 4) {
            fields[i * 2].performTextInput("60")
            fields[i * 2 + 1].performTextInput("8")
        }
        val done = rule.onAllNodes(hasContentDescription("Done"))
        for (i in 0 until 4) done[i].performClick()
        rule.waitForIdle()
        shot("10-workout-logged")

        rule.onAllNodesWithText("Finish").onFirst().performClick()
        waitText("sets will be saved")
        shot("11-finish-dialog")
        rule.onAllNodesWithText("Finish").onLast().performClick()

        waitText("Repeat this workout")
        waitText("60 kg × 8")
        shot("12-session-detail")
        rule.onNodeWithContentDescriptionFirst("Back")

        // Rotation moved on to Lower A.
        waitText("Start Lower A")
        shot("13-today-next")

        rule.onNodeWithText("History").performClick()
        waitText("Upper A")
        shot("14-history")

        rule.onNodeWithText("Progress").performClick()
        waitText("Personal records")
        shot("15-progress")
        rule.onNodeWithText("Body weight").performClick()
        rule.onNodeWithText("Log weight").performClick()
        waitText("Log body weight")
        rule.onAllNodes(hasSetTextAction()).onLast().performTextInput("82.5")
        rule.onNodeWithText("Save").performClick()
        waitText("82.5 kg")
        shot("16-body-weight")

        // Doing Upper A again suggests +2.5 kg on bench.
        rule.onNodeWithText("Today").performClick()
        waitText("Do another day")
        rule.onNodeWithText("Do another day").performClick()
        waitText("From your rotation")
        rule.onAllNodesWithText("Upper A").onFirst().performClick()
        waitText("Go up +2.5 kg")
        waitText("62.5 kg")
        shot("17-suggestion")

    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.onNodeWithContentDescriptionFirst(desc: String) {
        onAllNodes(hasContentDescription(desc)).onFirst().performClick()
        waitForIdle()
    }
}
