package app.gymtracker.data

object Muscles {
    val all = listOf(
        "Chest", "Back", "Shoulders", "Traps", "Biceps", "Triceps", "Forearms",
        "Quads", "Hamstrings", "Glutes", "Adductors", "Calves", "Abs", "Lower Back",
    )
    val lower = setOf("Quads", "Hamstrings", "Glutes", "Adductors", "Calves")
}

object Equipment {
    val all = listOf(
        "Barbell", "Dumbbell", "Machine", "Cable", "Bodyweight", "Smith Machine", "EZ Bar", "Kettlebell", "Other",
    )
}

object SeedData {

    private fun ex(
        name: String,
        muscle: String,
        secondary: String,
        equipment: String,
        steps: String,
        inc: Double? = null,
    ) = Exercise(
        name = name,
        primaryMuscle = muscle,
        secondaryMuscles = secondary,
        equipment = equipment,
        instructions = steps.split(" | ").joinToString("\n") { it.trim() },
        increment = inc ?: if (muscle in Muscles.lower) 5.0 else 2.5,
    )

    val exercises: List<Exercise> = listOf(
        // ---------------- Chest ----------------
        ex("Barbell Bench Press", "Chest", "Triceps, Front delts", "Barbell",
            "Lie on a flat bench with your eyes under the bar and feet planted. | Grip slightly wider than shoulder width, pull your shoulder blades back and down, then unrack. | Lower the bar under control to your mid-chest, elbows about 45-70 degrees from your torso. | Press back up to lockout while keeping your upper back tight."),
        ex("Incline Barbell Bench Press", "Chest", "Front delts, Triceps", "Barbell",
            "Set the bench to 30-45 degrees. | Unrack with a grip slightly wider than shoulder width. | Lower the bar to your upper chest just below the collarbones. | Press up and slightly back to lockout."),
        ex("Decline Barbell Bench Press", "Chest", "Triceps", "Barbell",
            "Secure your legs on a decline bench and unrack the bar. | Lower it to your lower chest with control. | Press back to lockout keeping your shoulders pinned to the bench."),
        ex("Dumbbell Bench Press", "Chest", "Triceps, Front delts", "Dumbbell",
            "Sit with the dumbbells on your thighs and kick them up as you lie back. | Start with arms extended over your chest. | Lower until the dumbbells are beside your chest with a deep stretch. | Press up and slightly together."),
        ex("Incline Dumbbell Press", "Chest", "Front delts, Triceps", "Dumbbell",
            "Set the bench to 30-45 degrees. | Lower the dumbbells to the sides of your upper chest. | Press up, bringing them together above your upper chest."),
        ex("Decline Dumbbell Press", "Chest", "Triceps", "Dumbbell",
            "Lie on a decline bench with legs secured. | Lower the dumbbells to the sides of your lower chest. | Press back up until your arms are straight."),
        ex("Dumbbell Fly", "Chest", "Front delts", "Dumbbell",
            "Lie flat holding dumbbells above your chest, palms facing each other. | With a slight bend in the elbows, open your arms in a wide arc until you feel a stretch. | Squeeze your chest to bring them back together."),
        ex("Incline Dumbbell Fly", "Chest", "Front delts", "Dumbbell",
            "Set the bench to 30 degrees and hold dumbbells above your upper chest. | Open your arms in a wide arc with soft elbows. | Bring them back together by squeezing the upper chest."),
        ex("Cable Fly", "Chest", "Front delts", "Cable",
            "Set both pulleys at shoulder height and step forward in a split stance. | Keeping elbows slightly bent, bring the handles together in front of your chest. | Return slowly until you feel a stretch across your chest."),
        ex("Low-to-High Cable Fly", "Chest", "Front delts", "Cable",
            "Set both pulleys at the lowest position. | Sweep the handles up and together to upper-chest height. | Lower under control back to the start."),
        ex("Pec Deck", "Chest", "Front delts", "Machine",
            "Adjust the seat so the handles are at chest height. | Bring the handles together in front of you, squeezing the chest. | Return slowly to a full stretch."),
        ex("Machine Chest Press", "Chest", "Triceps, Front delts", "Machine",
            "Adjust the seat so the handles line up with your mid-chest. | Press forward to full extension. | Return slowly until you feel a stretch."),
        ex("Smith Machine Bench Press", "Chest", "Triceps, Front delts", "Smith Machine",
            "Set a flat bench so the bar lines up with your mid-chest. | Unhook the bar and lower it to your chest. | Press back up and re-rack by rotating the bar."),
        ex("Push-Up", "Chest", "Triceps, Core", "Bodyweight",
            "Place your hands slightly wider than shoulders with your body in a straight line. | Lower your chest to just above the floor. | Push back up keeping your hips in line. Log added weight (0 if none)."),
        ex("Chest Dip", "Chest", "Triceps, Front delts", "Bodyweight",
            "Support yourself on parallel bars and lean your torso forward. | Lower until your shoulders are slightly below your elbows. | Press back up. Log added weight (0 if none)."),
        ex("Dumbbell Pullover", "Chest", "Lats", "Dumbbell",
            "Lie across or along a bench holding one dumbbell over your chest with both hands. | With slightly bent elbows, lower it back behind your head until you feel a stretch. | Pull it back over your chest."),

        // ---------------- Back ----------------
        ex("Conventional Deadlift", "Back", "Hamstrings, Glutes, Traps", "Barbell",
            "Stand with the bar over mid-foot, feet hip-width apart. | Hinge down and grip just outside your legs, flatten your back and pull the slack out of the bar. | Push the floor away keeping the bar close until hips and knees lock out. | Lower by pushing the hips back first, then bending the knees.", 5.0),
        ex("Rack Pull", "Back", "Traps, Glutes", "Barbell",
            "Set the bar on pins or blocks around knee height. | Grip it, brace and keep your back flat. | Drive your hips forward to lockout, then lower under control.", 5.0),
        ex("Pull-Up", "Back", "Biceps", "Bodyweight",
            "Hang from the bar with an overhand grip slightly wider than shoulders. | Pull your chest toward the bar by driving your elbows down. | Lower to a full hang under control. Log added weight (0 if none)."),
        ex("Chin-Up", "Back", "Biceps", "Bodyweight",
            "Hang from the bar with an underhand, shoulder-width grip. | Pull until your chin clears the bar. | Lower to a full hang under control. Log added weight (0 if none)."),
        ex("Lat Pulldown", "Back", "Biceps", "Cable",
            "Sit with thighs under the pads and grip the bar wider than shoulders. | Pull the bar to your upper chest, driving elbows down and back. | Let it rise slowly until your arms are straight."),
        ex("Close-Grip Lat Pulldown", "Back", "Biceps", "Cable",
            "Attach a V-handle and sit with thighs under the pads. | Pull the handle to your upper chest keeping elbows close. | Return slowly to a full stretch."),
        ex("Barbell Row", "Back", "Biceps, Rear delts", "Barbell",
            "Hinge forward to about 45 degrees with a flat back, bar hanging at arm's length. | Row the bar to your lower ribs, squeezing your shoulder blades. | Lower under control without standing up."),
        ex("Pendlay Row", "Back", "Biceps, Rear delts", "Barbell",
            "Set up with your torso nearly parallel to the floor and the bar on the ground. | Explosively row the bar to your lower chest. | Return it to the floor and reset each rep."),
        ex("One-Arm Dumbbell Row", "Back", "Biceps, Rear delts", "Dumbbell",
            "Place one knee and hand on a bench, back flat. | Row the dumbbell toward your hip, keeping the elbow close. | Lower until your arm is fully stretched."),
        ex("Chest-Supported Dumbbell Row", "Back", "Rear delts, Biceps", "Dumbbell",
            "Lie face down on an incline bench holding dumbbells. | Row them up by driving the elbows back. | Lower slowly to a full stretch."),
        ex("T-Bar Row", "Back", "Biceps, Rear delts", "Barbell",
            "Straddle the bar or machine with a flat back and grab the handle. | Row the weight toward your chest. | Lower under control."),
        ex("Seated Cable Row", "Back", "Biceps, Rear delts", "Cable",
            "Sit with feet on the platform and a slight bend in the knees. | Pull the handle to your stomach, squeezing your shoulder blades. | Let your arms extend fully and your shoulders reach forward slightly."),
        ex("Machine Row", "Back", "Biceps, Rear delts", "Machine",
            "Set the chest pad so you can just reach the handles. | Row the handles back, squeezing your upper back. | Return slowly to a full stretch."),
        ex("Straight-Arm Pulldown", "Back", "Triceps (long head)", "Cable",
            "Stand facing a high pulley holding a bar or rope with straight arms. | Hinge slightly and sweep the bar down to your thighs using your lats. | Let it rise slowly back to eye level."),
        ex("Back Extension", "Lower Back", "Glutes, Hamstrings", "Bodyweight",
            "Set the pad just below your hips on a back-extension bench. | Lower your torso by hinging at the hips. | Rise until your body is in a straight line. Log added weight (0 if none)."),
        ex("Good Morning", "Hamstrings", "Lower Back, Glutes", "Barbell",
            "Place the bar on your upper back like a squat. | With soft knees, push your hips back until your torso is near parallel. | Drive your hips forward to stand back up."),

        // ---------------- Shoulders ----------------
        ex("Overhead Press", "Shoulders", "Triceps, Upper chest", "Barbell",
            "Stand with the bar on your front shoulders, grip just outside shoulder width. | Brace your core and glutes and press the bar straight up, moving your head back out of the way. | Lock out overhead, then lower back to the shoulders."),
        ex("Seated Dumbbell Shoulder Press", "Shoulders", "Triceps", "Dumbbell",
            "Sit on an upright bench with dumbbells at shoulder height. | Press them overhead until your arms are straight. | Lower slowly back to shoulder level."),
        ex("Arnold Press", "Shoulders", "Triceps", "Dumbbell",
            "Start with dumbbells in front of your shoulders, palms facing you. | Press up while rotating your palms to face forward. | Reverse the rotation as you lower."),
        ex("Machine Shoulder Press", "Shoulders", "Triceps", "Machine",
            "Adjust the seat so the handles are at shoulder height. | Press up to full extension. | Lower under control."),
        ex("Dumbbell Lateral Raise", "Shoulders", "Traps", "Dumbbell",
            "Stand with dumbbells at your sides and a slight forward lean. | Raise your arms out to the sides until they are level with your shoulders. | Lower slowly without swinging."),
        ex("Cable Lateral Raise", "Shoulders", "", "Cable",
            "Stand side-on to a low pulley holding the handle with your far hand. | Raise your arm out to the side to shoulder height. | Lower slowly, keeping tension on the cable."),
        ex("Machine Lateral Raise", "Shoulders", "", "Machine",
            "Sit with the pads against the outside of your arms. | Raise your arms to shoulder height. | Lower under control."),
        ex("Dumbbell Front Raise", "Shoulders", "Upper chest", "Dumbbell",
            "Hold dumbbells in front of your thighs. | Raise them forward to eye level with straight arms. | Lower slowly."),
        ex("Reverse Pec Deck", "Shoulders", "Upper back", "Machine",
            "Sit facing the pec deck with handles set to the rear-delt position. | Sweep your arms back and out until they are in line with your body. | Return slowly."),
        ex("Rear Delt Dumbbell Fly", "Shoulders", "Upper back", "Dumbbell",
            "Hinge forward with a flat back, dumbbells hanging below you. | Raise them out to the sides leading with the elbows. | Lower under control."),
        ex("Face Pull", "Shoulders", "Upper back, Traps", "Cable",
            "Set a rope at upper-chest height or higher. | Pull the rope toward your face, splitting the ends and rotating your hands back. | Return slowly to full extension."),
        ex("Upright Row", "Shoulders", "Traps", "Barbell",
            "Hold the bar in front of your thighs with a shoulder-width grip. | Pull it up along your body until your elbows reach shoulder height. | Lower slowly."),

        // ---------------- Traps ----------------
        ex("Barbell Shrug", "Traps", "Forearms", "Barbell",
            "Hold the bar in front of you at arm's length. | Shrug your shoulders straight up toward your ears. | Pause, then lower fully."),
        ex("Dumbbell Shrug", "Traps", "Forearms", "Dumbbell",
            "Hold dumbbells at your sides. | Shrug your shoulders straight up. | Pause and lower fully."),

        // ---------------- Biceps ----------------
        ex("Barbell Curl", "Biceps", "Forearms", "Barbell",
            "Stand holding the bar with an underhand, shoulder-width grip. | Curl the bar up keeping your elbows at your sides. | Lower slowly to full extension."),
        ex("EZ-Bar Curl", "Biceps", "Forearms", "EZ Bar",
            "Hold the EZ bar on the angled grips. | Curl up without moving your elbows forward. | Lower under control."),
        ex("Dumbbell Curl", "Biceps", "Forearms", "Dumbbell",
            "Stand with dumbbells at your sides, palms forward. | Curl them up, keeping your elbows still. | Lower slowly."),
        ex("Hammer Curl", "Biceps", "Forearms (brachioradialis)", "Dumbbell",
            "Hold dumbbells with palms facing each other. | Curl up keeping the neutral grip. | Lower slowly."),
        ex("Incline Dumbbell Curl", "Biceps", "", "Dumbbell",
            "Sit back on a bench set to about 45 degrees with arms hanging straight. | Curl the dumbbells up without moving your upper arms. | Lower to a full stretch."),
        ex("Preacher Curl", "Biceps", "", "EZ Bar",
            "Sit at a preacher bench with your upper arms on the pad. | Curl the bar up. | Lower slowly until your arms are almost straight."),
        ex("Cable Curl", "Biceps", "Forearms", "Cable",
            "Stand facing a low pulley with a straight bar. | Curl the bar up keeping your elbows by your sides. | Lower under control."),
        ex("Concentration Curl", "Biceps", "", "Dumbbell",
            "Sit and brace your elbow on the inside of your thigh. | Curl the dumbbell up toward your shoulder. | Lower slowly."),
        ex("Spider Curl", "Biceps", "", "Dumbbell",
            "Lie chest-down on an incline bench with arms hanging straight. | Curl the weights up. | Lower slowly."),
        ex("Bayesian Cable Curl", "Biceps", "", "Cable",
            "Stand facing away from a low pulley, arm behind your body. | Curl the handle forward and up. | Lower to a deep stretch behind you."),

        // ---------------- Triceps ----------------
        ex("Triceps Pushdown", "Triceps", "", "Cable",
            "Stand at a high pulley with a straight or V bar. | Push the bar down until your arms are straight, elbows pinned at your sides. | Let it rise slowly to about chest height."),
        ex("Rope Pushdown", "Triceps", "", "Cable",
            "Attach a rope to a high pulley. | Push down and spread the rope ends apart at the bottom. | Return slowly."),
        ex("Overhead Cable Triceps Extension", "Triceps", "", "Cable",
            "Face away from the pulley holding a rope behind your head. | Extend your arms forward and up until straight. | Return slowly to a deep stretch."),
        ex("Skull Crusher", "Triceps", "", "EZ Bar",
            "Lie on a bench holding the bar above your chest. | Bend your elbows to lower the bar toward your forehead or just behind your head. | Extend your arms back up."),
        ex("Close-Grip Bench Press", "Triceps", "Chest, Front delts", "Barbell",
            "Lie on a bench and grip the bar about shoulder width. | Lower it to your lower chest, elbows close to your body. | Press back up to lockout."),
        ex("Triceps Dip", "Triceps", "Chest, Front delts", "Bodyweight",
            "Support yourself on parallel bars with an upright torso. | Lower until your elbows are at about 90 degrees. | Press back up. Log added weight (0 if none)."),
        ex("Overhead Dumbbell Extension", "Triceps", "", "Dumbbell",
            "Hold one dumbbell overhead with both hands. | Lower it behind your head by bending your elbows. | Extend back up."),
        ex("Dumbbell Kickback", "Triceps", "", "Dumbbell",
            "Hinge forward with your upper arm parallel to the floor. | Extend your forearm back until the arm is straight. | Return slowly."),

        // ---------------- Forearms ----------------
        ex("Wrist Curl", "Forearms", "", "Barbell",
            "Sit with your forearms on your thighs, palms up, holding a bar. | Curl your wrists up. | Lower slowly."),
        ex("Reverse Wrist Curl", "Forearms", "", "Barbell",
            "Sit with forearms on your thighs, palms down. | Raise the back of your hands. | Lower slowly."),
        ex("Reverse Curl", "Forearms", "Biceps", "EZ Bar",
            "Hold the bar with an overhand grip. | Curl it up keeping your wrists straight. | Lower slowly."),

        // ---------------- Quads ----------------
        ex("Barbell Back Squat", "Quads", "Glutes, Adductors, Lower Back", "Barbell",
            "Place the bar on your upper back and step out with feet about shoulder width. | Brace and sit down between your knees until your thighs are at least parallel. | Drive up through your whole foot, keeping your chest up."),
        ex("Front Squat", "Quads", "Glutes, Upper back", "Barbell",
            "Rest the bar on your front shoulders with elbows high. | Squat down keeping your torso upright. | Drive back up without letting your elbows drop."),
        ex("Hack Squat", "Quads", "Glutes", "Machine",
            "Stand on the platform with your back against the pad. | Lower until your thighs are below parallel. | Press back up without locking your knees hard."),
        ex("Leg Press", "Quads", "Glutes, Adductors", "Machine",
            "Sit with your feet shoulder-width on the platform. | Lower the sled until your knees are near your chest without your lower back lifting. | Press back up."),
        ex("Leg Extension", "Quads", "", "Machine",
            "Sit with the pad on your lower shins and knees in line with the machine pivot. | Extend your legs until straight and squeeze. | Lower slowly."),
        ex("Goblet Squat", "Quads", "Glutes", "Dumbbell",
            "Hold a dumbbell against your chest. | Squat down between your knees keeping your chest up. | Drive back up."),
        ex("Bulgarian Split Squat", "Quads", "Glutes", "Dumbbell",
            "Stand in front of a bench and place your rear foot on it. | Lower your back knee toward the floor. | Drive up through your front foot. Log weight per hand."),
        ex("Walking Lunge", "Quads", "Glutes", "Dumbbell",
            "Hold dumbbells at your sides. | Step forward and lower your back knee toward the floor. | Push up and step through into the next lunge."),
        ex("Reverse Lunge", "Quads", "Glutes", "Dumbbell",
            "Hold dumbbells at your sides. | Step back and lower your back knee toward the floor. | Drive back to standing through your front foot."),
        ex("Step-Up", "Quads", "Glutes", "Dumbbell",
            "Stand in front of a box or bench holding dumbbells. | Step up and drive through your top foot to stand. | Step down under control."),
        ex("Smith Machine Squat", "Quads", "Glutes", "Smith Machine",
            "Set the bar on your upper back with feet slightly in front of you. | Squat down to at least parallel. | Drive back up."),
        ex("Pendulum Squat", "Quads", "Glutes", "Machine",
            "Stand in the machine with shoulders under the pads. | Squat down as deep as possible. | Drive back up."),

        // ---------------- Hamstrings ----------------
        ex("Romanian Deadlift", "Hamstrings", "Glutes, Lower Back", "Barbell",
            "Stand holding the bar at hip height. | With soft knees, push your hips back and slide the bar down your thighs until you feel a strong stretch. | Drive your hips forward to stand back up."),
        ex("Stiff-Leg Deadlift", "Hamstrings", "Glutes, Lower Back", "Barbell",
            "Stand with the bar in front of you and knees almost straight. | Hinge down as far as your hamstrings allow with a flat back. | Return to standing."),
        ex("Dumbbell Romanian Deadlift", "Hamstrings", "Glutes", "Dumbbell",
            "Hold dumbbells in front of your thighs. | Push your hips back and lower them along your legs. | Drive the hips forward to stand."),
        ex("Lying Leg Curl", "Hamstrings", "Calves", "Machine",
            "Lie face down with the pad just above your heels. | Curl your heels toward your glutes. | Lower slowly."),
        ex("Seated Leg Curl", "Hamstrings", "", "Machine",
            "Sit with the pad behind your lower legs and the thigh pad locked down. | Curl your legs down and back. | Return slowly."),
        ex("Nordic Hamstring Curl", "Hamstrings", "", "Bodyweight",
            "Kneel with your ankles anchored. | Lower your body forward as slowly as possible. | Use your hands to push back up and pull with your hamstrings."),

        // ---------------- Glutes ----------------
        ex("Hip Thrust", "Glutes", "Hamstrings", "Barbell",
            "Sit with your upper back against a bench and a padded bar across your hips. | Drive through your heels to lift your hips until your body is flat. | Squeeze, then lower."),
        ex("Glute Bridge", "Glutes", "Hamstrings", "Barbell",
            "Lie on the floor with knees bent and a bar across your hips. | Lift your hips by squeezing your glutes. | Lower under control."),
        ex("Cable Glute Kickback", "Glutes", "", "Cable",
            "Attach an ankle strap to a low pulley. | Kick your leg back and up, squeezing the glute. | Return slowly."),
        ex("Hip Abduction Machine", "Glutes", "", "Machine",
            "Sit with the pads on the outside of your knees. | Push your knees apart. | Return slowly."),
        ex("Sumo Deadlift", "Glutes", "Quads, Adductors, Back", "Barbell",
            "Take a wide stance with toes turned out and grip the bar inside your knees. | Push your knees out and drive the floor away. | Lock out your hips, then lower under control."),
        ex("Cable Pull-Through", "Glutes", "Hamstrings", "Cable",
            "Face away from a low pulley holding a rope between your legs. | Hinge back, then drive your hips forward to stand tall. | Squeeze your glutes at the top."),
        ex("Kettlebell Swing", "Glutes", "Hamstrings, Lower Back", "Kettlebell",
            "Hike the kettlebell back between your legs. | Snap your hips forward to swing it to chest height. | Let it fall back and repeat."),

        // ---------------- Adductors ----------------
        ex("Hip Adduction Machine", "Adductors", "", "Machine",
            "Sit with the pads on the inside of your knees. | Squeeze your legs together. | Return slowly to a stretch."),

        // ---------------- Calves ----------------
        ex("Standing Calf Raise", "Calves", "", "Machine",
            "Stand with the balls of your feet on the edge and shoulders under the pads. | Lower your heels for a deep stretch. | Rise up as high as possible and pause."),
        ex("Seated Calf Raise", "Calves", "", "Machine",
            "Sit with the pad on your lower thighs. | Lower your heels to a full stretch. | Rise up and pause at the top."),
        ex("Leg Press Calf Raise", "Calves", "", "Machine",
            "Sit in the leg press with the balls of your feet on the bottom edge. | Let your toes come toward you for a stretch. | Press through your toes."),
        ex("Smith Machine Calf Raise", "Calves", "", "Smith Machine",
            "Stand on a step under the Smith bar. | Lower your heels for a stretch. | Rise up and pause."),

        // ---------------- Abs ----------------
        ex("Hanging Leg Raise", "Abs", "Hip flexors", "Bodyweight",
            "Hang from a bar with straight arms. | Raise your legs by curling your pelvis up. | Lower slowly without swinging."),
        ex("Captain's Chair Knee Raise", "Abs", "Hip flexors", "Bodyweight",
            "Support yourself on the forearm pads. | Raise your knees toward your chest. | Lower slowly."),
        ex("Cable Crunch", "Abs", "", "Cable",
            "Kneel facing a high pulley holding a rope beside your head. | Crunch down by curling your spine. | Return slowly."),
        ex("Crunch", "Abs", "", "Bodyweight",
            "Lie on your back with knees bent. | Curl your shoulders off the floor. | Lower slowly."),
        ex("Ab Wheel Rollout", "Abs", "Lats", "Other",
            "Kneel holding the ab wheel under your shoulders. | Roll forward as far as you can with a flat back. | Pull back using your abs."),
        ex("Decline Sit-Up", "Abs", "Hip flexors", "Bodyweight",
            "Secure your feet on a decline bench. | Sit up all the way. | Lower under control. Log added weight (0 if none)."),
        ex("Russian Twist", "Abs", "Obliques", "Other",
            "Sit leaning back with feet raised, holding a weight. | Rotate the weight from side to side. | Each side counts as one rep."),
        ex("Pallof Press", "Abs", "Obliques", "Cable",
            "Stand side-on to a pulley at chest height holding the handle at your chest. | Press it straight out and resist the rotation. | Return to your chest."),
    )

    /** Ready-made workout days: name, category, and exercise targets (name, sets, repMin, repMax). */
    data class DaySeed(val name: String, val category: String, val items: List<Item>) {
        data class Item(val exercise: String, val sets: Int, val repMin: Int, val repMax: Int)
    }

    private fun day(name: String, category: String, vararg items: Pair<String, String>) =
        DaySeed(name, category, items.map { (n, scheme) ->
            val (sets, reps) = scheme.split("x")
            val (lo, hi) = reps.split("-").map { it.toInt() }
            DaySeed.Item(n, sets.toInt(), lo, hi)
        })

    const val UL = "Upper / Lower"
    const val PPL = "Push / Pull / Legs"
    const val FB = "Full Body"

    val days: List<DaySeed> = listOf(
        day("Upper A", UL,
            "Barbell Bench Press" to "4x6-8", "Barbell Row" to "4x6-8", "Overhead Press" to "3x8-10",
            "Lat Pulldown" to "3x8-12", "Dumbbell Lateral Raise" to "3x12-15", "EZ-Bar Curl" to "3x10-12",
            "Triceps Pushdown" to "3x10-12"),
        day("Upper B", UL,
            "Incline Dumbbell Press" to "4x8-10", "Pull-Up" to "4x6-10", "Seated Dumbbell Shoulder Press" to "3x8-10",
            "Seated Cable Row" to "3x10-12", "Cable Fly" to "3x12-15", "Hammer Curl" to "3x10-12",
            "Overhead Cable Triceps Extension" to "3x10-12"),
        day("Lower A", UL,
            "Barbell Back Squat" to "4x5-8", "Romanian Deadlift" to "3x8-10", "Leg Press" to "3x10-12",
            "Lying Leg Curl" to "3x10-12", "Standing Calf Raise" to "4x10-15", "Hanging Leg Raise" to "3x10-15"),
        day("Lower B", UL,
            "Conventional Deadlift" to "3x4-6", "Bulgarian Split Squat" to "3x8-10", "Leg Extension" to "3x12-15",
            "Seated Leg Curl" to "3x10-12", "Hip Thrust" to "3x8-12", "Seated Calf Raise" to "4x12-15",
            "Cable Crunch" to "3x12-15"),

        day("Push A", PPL,
            "Barbell Bench Press" to "4x6-8", "Overhead Press" to "3x8-10", "Incline Dumbbell Press" to "3x8-12",
            "Dumbbell Lateral Raise" to "4x12-15", "Triceps Pushdown" to "3x10-12",
            "Overhead Cable Triceps Extension" to "3x10-12"),
        day("Push B", PPL,
            "Overhead Press" to "4x6-8", "Incline Barbell Bench Press" to "3x8-10", "Machine Chest Press" to "3x10-12",
            "Cable Lateral Raise" to "3x12-15", "Chest Dip" to "3x8-12", "Skull Crusher" to "3x10-12"),
        day("Pull A", PPL,
            "Barbell Row" to "4x6-8", "Pull-Up" to "3x6-10", "Seated Cable Row" to "3x10-12",
            "Face Pull" to "3x12-15", "Barbell Curl" to "3x8-10", "Hammer Curl" to "3x10-12"),
        day("Pull B", PPL,
            "Conventional Deadlift" to "3x4-6", "Lat Pulldown" to "4x8-12", "Chest-Supported Dumbbell Row" to "3x10-12",
            "Reverse Pec Deck" to "3x12-15", "Incline Dumbbell Curl" to "3x10-12", "Barbell Shrug" to "3x10-12"),
        day("Legs A", PPL,
            "Barbell Back Squat" to "4x5-8", "Romanian Deadlift" to "3x8-10", "Leg Press" to "3x10-12",
            "Lying Leg Curl" to "3x10-12", "Standing Calf Raise" to "4x10-15"),
        day("Legs B", PPL,
            "Front Squat" to "3x6-8", "Hip Thrust" to "3x8-12", "Walking Lunge" to "3x10-12",
            "Leg Extension" to "3x12-15", "Seated Leg Curl" to "3x10-12", "Seated Calf Raise" to "4x12-15"),

        day("Full Body A", FB,
            "Barbell Back Squat" to "3x5-8", "Barbell Bench Press" to "3x6-8", "Barbell Row" to "3x8-10",
            "Dumbbell Lateral Raise" to "3x12-15", "Barbell Curl" to "2x10-12"),
        day("Full Body B", FB,
            "Conventional Deadlift" to "3x4-6", "Overhead Press" to "3x6-8", "Pull-Up" to "3x6-10",
            "Lying Leg Curl" to "3x10-12", "Triceps Pushdown" to "2x10-12"),
        day("Full Body C", FB,
            "Leg Press" to "3x10-12", "Incline Dumbbell Press" to "3x8-10", "Seated Cable Row" to "3x10-12",
            "Hip Thrust" to "3x8-12", "Cable Crunch" to "3x12-15"),
    )

    /** Program templates: name and phases (phase name, weeks, rotation of day names). */
    data class PlanSeed(val name: String, val description: String, val phases: List<PhaseSeed>)
    data class PhaseSeed(val name: String, val weeks: Int, val days: List<String>)

    val plans: List<PlanSeed> = listOf(
        PlanSeed("Upper / Lower", "4-day rotation: Upper A, Lower A, Upper B, Lower B",
            listOf(PhaseSeed("Upper / Lower", 8, listOf("Upper A", "Lower A", "Upper B", "Lower B")))),
        PlanSeed("Push / Pull / Legs", "6-day rotation with A and B versions",
            listOf(PhaseSeed("PPL", 8, listOf("Push A", "Pull A", "Legs A", "Push B", "Pull B", "Legs B")))),
        PlanSeed("Full Body", "3-day rotation: A, B, C",
            listOf(PhaseSeed("Full Body", 8, listOf("Full Body A", "Full Body B", "Full Body C")))),
        PlanSeed("PPL + Upper / Lower hybrid", "Mixed 5-day rotation: Push, Pull, Legs, Upper, Lower",
            listOf(PhaseSeed("Hybrid", 8, listOf("Push A", "Pull A", "Legs A", "Upper A", "Lower A")))),
        PlanSeed("Upper / Lower then PPL", "Two phases: 4 weeks Upper/Lower, then 4 weeks PPL",
            listOf(
                PhaseSeed("Phase 1: Upper / Lower", 4, listOf("Upper A", "Lower A", "Upper B", "Lower B")),
                PhaseSeed("Phase 2: PPL", 4, listOf("Push A", "Pull A", "Legs A", "Push B", "Pull B", "Legs B")),
            )),
    )
}
