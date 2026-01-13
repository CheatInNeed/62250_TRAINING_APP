package com.example.gymlocker.ui.localization

/**
 * Centralized localization strings for the entire app.
 * Add new strings here as needed for each screen.
 */
interface AppStrings {
    // Common
    val back: String
    val save: String
    val cancel: String
    val delete: String
    val edit: String
    val confirm: String
    val yes: String
    val no: String
    val ok: String
    val loading: String
    val error: String
    val success: String

    // Bottom Navigation
    val navHome: String
    val navWorkout: String
    val navHistory: String
    val navProfile: String

    // Profile Screen
    val profile: String
    val editProfile: String
    val logout: String
    val height: String
    val weight: String
    val notSet: String
    val totalWorkouts: String
    val mostRecent: String
    val noWorkoutsYet: String
    val activeProfile: String
    val chooseProfile: String
    val noProfilesYet: String
    val createAnotherProfile: String
    val deleteProfileTitle: String
    val deleteProfileMessage: String
    val oops: String

    // Edit Profile Screen
    val noActiveProfile: String
    val displayName: String
    val heightLabel: String
    val weightLabel: String
    val language: String
    val englishLanguage: String
    val danishLanguage: String
    val resetProfile: String
    val resetProfileTitle: String
    val resetProfileMessage: String
    val reset: String
    val heightMustBeNumber: String
    val weightMustBeNumber: String

    // Create Profile Screen
    val createProfile: String
    val enterName: String
    val optional: String
    val create: String

    // Home Screen
    val welcome: String
    val startWorkout: String
    val continueWorkout: String
    val noActiveWorkout: String

    // Workout Screen
    val workout: String
    val exercises: String
    val templates: String
    val createExercise: String
    val createTemplate: String
    val addExercise: String
    val noExercises: String
    val noTemplates: String

    // Active Workout Screen
    val activeWorkout: String
    val finishWorkout: String
    val cancelWorkout: String
    val discardWorkout: String
    val workoutName: String
    val duration: String
    val sets: String
    val reps: String
    val kg: String
    val addSet: String
    val deleteSet: String
    val markComplete: String
    val restTimer: String

    // History Screen
    val history: String
    val workoutHistory: String
    val noWorkoutHistory: String
    val deleteWorkout: String
    val deleteWorkoutConfirm: String

    // Exercise Screen
    val exerciseDetails: String
    val muscleGroup: String
    val startWeight: String
    val startReps: String

    // Create Exercise Screen
    val exerciseName: String
    val selectMuscleGroup: String
    val exerciseNameError: String
    val exerciseAlreadyExists: String

    // Template Screen
    val templateName: String
    val templateDetails: String
    val editTemplate: String
    val deleteTemplate: String
    val deleteTemplateConfirm: String
    val useTemplate: String

    // Stats
    val stats: String
    val weeklyVolume: String
    val muscleDistribution: String

    // Login/Register
    val login: String
    val register: String
    val email: String
    val password: String
    val confirmPassword: String
    val forgotPassword: String
    val noAccount: String
    val haveAccount: String
    val loginError: String
    val registerError: String
    val passwordMismatch: String
}

object EnglishStrings : AppStrings {
    // Common
    override val back = "Back"
    override val save = "Save"
    override val cancel = "Cancel"
    override val delete = "Delete"
    override val edit = "Edit"
    override val confirm = "Confirm"
    override val yes = "Yes"
    override val no = "No"
    override val ok = "OK"
    override val loading = "Loading..."
    override val error = "Error"
    override val success = "Success"

    // Bottom Navigation
    override val navHome = "Home"
    override val navWorkout = "Workout"
    override val navHistory = "History"
    override val navProfile = "Profile"

    // Profile Screen
    override val profile = "Profile"
    override val editProfile = "Edit profile"
    override val logout = "Log out"
    override val height = "Height"
    override val weight = "Weight"
    override val notSet = "Not set"
    override val totalWorkouts = "Total workouts"
    override val mostRecent = "Most recent"
    override val noWorkoutsYet = "No workouts yet"
    override val activeProfile = "Active profile"
    override val chooseProfile = "Choose a profile"
    override val noProfilesYet = "No profiles yet.\nCreate one to get started."
    override val createAnotherProfile = "Create another profile"
    override val deleteProfileTitle = "Delete profile?"
    override val deleteProfileMessage = "This will delete \"%s\" and all workouts/templates linked to it.\n\nThis cannot be undone."
    override val oops = "Oops"

    // Edit Profile Screen
    override val noActiveProfile = "No active profile selected."
    override val displayName = "Display name"
    override val heightLabel = "Height (cm) — leave empty for Not set"
    override val weightLabel = "Weight (kg) — leave empty for Not set"
    override val language = "Language"
    override val englishLanguage = "English"
    override val danishLanguage = "Danish"
    override val resetProfile = "Reset profile"
    override val resetProfileTitle = "Reset profile?"
    override val resetProfileMessage = "This resets name/height/weight. Workouts will NOT be deleted."
    override val reset = "Reset"
    override val heightMustBeNumber = "Height must be a number."
    override val weightMustBeNumber = "Weight must be a number."

    // Create Profile Screen
    override val createProfile = "Create Profile"
    override val enterName = "Enter your name"
    override val optional = "(optional)"
    override val create = "Create"

    // Home Screen
    override val welcome = "Welcome"
    override val startWorkout = "Start Workout"
    override val continueWorkout = "Continue Workout"
    override val noActiveWorkout = "No active workout"

    // Workout Screen
    override val workout = "Workout"
    override val exercises = "Exercises"
    override val templates = "Templates"
    override val createExercise = "Create Exercise"
    override val createTemplate = "Create Template"
    override val addExercise = "Add Exercise"
    override val noExercises = "No exercises found"
    override val noTemplates = "No templates found"

    // Active Workout Screen
    override val activeWorkout = "Active Workout"
    override val finishWorkout = "Finish Workout"
    override val cancelWorkout = "Cancel Workout"
    override val discardWorkout = "Discard Workout"
    override val workoutName = "Workout name"
    override val duration = "Duration"
    override val sets = "Sets"
    override val reps = "Reps"
    override val kg = "kg"
    override val addSet = "Add Set"
    override val deleteSet = "Delete Set"
    override val markComplete = "Mark Complete"
    override val restTimer = "Rest Timer"

    // History Screen
    override val history = "History"
    override val workoutHistory = "Workout History"
    override val noWorkoutHistory = "No workout history"
    override val deleteWorkout = "Delete Workout"
    override val deleteWorkoutConfirm = "Are you sure you want to delete this workout?"

    // Exercise Screen
    override val exerciseDetails = "Exercise Details"
    override val muscleGroup = "Muscle Group"
    override val startWeight = "Start Weight"
    override val startReps = "Start Reps"

    // Create Exercise Screen
    override val exerciseName = "Exercise Name"
    override val selectMuscleGroup = "Select Muscle Group"
    override val exerciseNameError = "Name cannot be empty"
    override val exerciseAlreadyExists = "Exercise with this name already exists"

    // Template Screen
    override val templateName = "Template name"
    override val templateDetails = "Template Details"
    override val editTemplate = "Edit Template"
    override val deleteTemplate = "Delete Template"
    override val deleteTemplateConfirm = "Are you sure you want to delete this template?"
    override val useTemplate = "Use Template"

    // Stats
    override val stats = "Stats"
    override val weeklyVolume = "Weekly Volume"
    override val muscleDistribution = "Muscle Distribution"

    // Login/Register
    override val login = "Log in"
    override val register = "Register"
    override val email = "Email"
    override val password = "Password"
    override val confirmPassword = "Confirm Password"
    override val forgotPassword = "Forgot password?"
    override val noAccount = "Don't have an account?"
    override val haveAccount = "Already have an account?"
    override val loginError = "Invalid email or password"
    override val registerError = "Registration failed"
    override val passwordMismatch = "Passwords do not match"
}

object DanishStrings : AppStrings {
    // Common
    override val back = "Tilbage"
    override val save = "Gem"
    override val cancel = "Annuller"
    override val delete = "Slet"
    override val edit = "Rediger"
    override val confirm = "Bekræft"
    override val yes = "Ja"
    override val no = "Nej"
    override val ok = "OK"
    override val loading = "Indlæser..."
    override val error = "Fejl"
    override val success = "Succes"

    // Bottom Navigation
    override val navHome = "Hjem"
    override val navWorkout = "Træning"
    override val navHistory = "Historik"
    override val navProfile = "Profil"

    // Profile Screen
    override val profile = "Profil"
    override val editProfile = "Rediger profil"
    override val logout = "Log ud"
    override val height = "Højde"
    override val weight = "Vægt"
    override val notSet = "Ikke angivet"
    override val totalWorkouts = "Antal træninger"
    override val mostRecent = "Seneste"
    override val noWorkoutsYet = "Ingen træninger endnu"
    override val activeProfile = "Aktiv profil"
    override val chooseProfile = "Vælg en profil"
    override val noProfilesYet = "Ingen profiler endnu.\nOpret en for at komme i gang."
    override val createAnotherProfile = "Opret endnu en profil"
    override val deleteProfileTitle = "Slet profil?"
    override val deleteProfileMessage = "Dette vil slette \"%s\" og alle træninger/skabeloner tilknyttet.\n\nDette kan ikke fortrydes."
    override val oops = "Ups"

    // Edit Profile Screen
    override val noActiveProfile = "Ingen aktiv profil valgt."
    override val displayName = "Visningsnavn"
    override val heightLabel = "Højde (cm) — lad stå tom for Ikke angivet"
    override val weightLabel = "Vægt (kg) — lad stå tom for Ikke angivet"
    override val language = "Sprog"
    override val englishLanguage = "Engelsk"
    override val danishLanguage = "Dansk"
    override val resetProfile = "Nulstil profil"
    override val resetProfileTitle = "Nulstil profil?"
    override val resetProfileMessage = "Dette nulstiller navn/højde/vægt. Træninger slettes IKKE."
    override val reset = "Nulstil"
    override val heightMustBeNumber = "Højde skal være et tal."
    override val weightMustBeNumber = "Vægt skal være et tal."

    // Create Profile Screen
    override val createProfile = "Opret Profil"
    override val enterName = "Indtast dit navn"
    override val optional = "(valgfrit)"
    override val create = "Opret"

    // Home Screen
    override val welcome = "Velkommen"
    override val startWorkout = "Start Træning"
    override val continueWorkout = "Fortsæt Træning"
    override val noActiveWorkout = "Ingen aktiv træning"

    // Workout Screen
    override val workout = "Træning"
    override val exercises = "Øvelser"
    override val templates = "Skabeloner"
    override val createExercise = "Opret Øvelse"
    override val createTemplate = "Opret Skabelon"
    override val addExercise = "Tilføj Øvelse"
    override val noExercises = "Ingen øvelser fundet"
    override val noTemplates = "Ingen skabeloner fundet"

    // Active Workout Screen
    override val activeWorkout = "Aktiv Træning"
    override val finishWorkout = "Afslut Træning"
    override val cancelWorkout = "Annuller Træning"
    override val discardWorkout = "Kassér Træning"
    override val workoutName = "Træningsnavn"
    override val duration = "Varighed"
    override val sets = "Sæt"
    override val reps = "Reps"
    override val kg = "kg"
    override val addSet = "Tilføj Sæt"
    override val deleteSet = "Slet Sæt"
    override val markComplete = "Marker Færdig"
    override val restTimer = "Pause Timer"

    // History Screen
    override val history = "Historik"
    override val workoutHistory = "Træningshistorik"
    override val noWorkoutHistory = "Ingen træningshistorik"
    override val deleteWorkout = "Slet Træning"
    override val deleteWorkoutConfirm = "Er du sikker på, at du vil slette denne træning?"

    // Exercise Screen
    override val exerciseDetails = "Øvelsesdetaljer"
    override val muscleGroup = "Muskelgruppe"
    override val startWeight = "Startvægt"
    override val startReps = "Start Reps"

    // Create Exercise Screen
    override val exerciseName = "Øvelsesnavn"
    override val selectMuscleGroup = "Vælg Muskelgruppe"
    override val exerciseNameError = "Navn må ikke være tomt"
    override val exerciseAlreadyExists = "Øvelse med dette navn findes allerede"

    // Template Screen
    override val templateName = "Skabelonnavn"
    override val templateDetails = "Skabelondetaljer"
    override val editTemplate = "Rediger Skabelon"
    override val deleteTemplate = "Slet Skabelon"
    override val deleteTemplateConfirm = "Er du sikker på, at du vil slette denne skabelon?"
    override val useTemplate = "Brug Skabelon"

    // Stats
    override val stats = "Statistik"
    override val weeklyVolume = "Ugentlig Volumen"
    override val muscleDistribution = "Muskelfordeling"

    // Login/Register
    override val login = "Log ind"
    override val register = "Registrer"
    override val email = "E-mail"
    override val password = "Adgangskode"
    override val confirmPassword = "Bekræft Adgangskode"
    override val forgotPassword = "Glemt adgangskode?"
    override val noAccount = "Har du ikke en konto?"
    override val haveAccount = "Har du allerede en konto?"
    override val loginError = "Ugyldig e-mail eller adgangskode"
    override val registerError = "Registrering mislykkedes"
    override val passwordMismatch = "Adgangskoder matcher ikke"
}

/**
 * Helper function to get the correct strings based on language preference.
 */
fun getStrings(language: String): AppStrings {
    return if (language == "Danish") DanishStrings else EnglishStrings
}

